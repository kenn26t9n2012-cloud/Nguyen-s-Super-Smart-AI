package com.nguyen_super_app.nguyen_super_smart_ai.engine.localai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalAiModelEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqRamArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class GgufQuantization(
    val id: String,
    val label: String,
    val sizeFactor: Float,
    val qualityScore: Int
) {
    Q2_K("Q2_K", "Q2_K (Siêu nhẹ - Tiết kiệm RAM)", 0.35f, 68),
    Q4_K_M("Q4_K_M", "Q4_K_M (Cân bằng Chuẩn Mobile)", 0.54f, 88),
    Q5_K_M("Q5_K_M", "Q5_K_M (Chất lượng cao)", 0.68f, 94),
    Q8_0("Q8_0", "Q8_0 (Gần như nguyên bản FP16)", 0.96f, 99)
}

enum class GgufLoadStatus(val label: String) {
    UNLOADED("Chưa nạp (Ngủ đông 0 MB RAM)"),
    WARMING_UP("Đang pre-warm thầm lặng..."),
    ACTIVE_READY("Đang hoạt động (Vulkan/OpenCL Ready)"),
    SUSPENDED_IDLE("Tự động tạm ngưng (10p không hoạt động)"),
    SUSPENDED_LOW_BATTERY("Tự động tạm ngưng (Pin <= ngưỡng an toàn)"),
    THERMAL_THROTTLED("Hạ xung phần cứng (Nhiệt độ thiết bị cao)")
}

data class CatalogModelSpec(
    val id: String,
    val name: String,
    val paramsBillion: Float,
    val defaultFormat: AiModelFormat,
    val baseSizeMb: Int,
    val downloadUrl: String,
    val description: String
)

data class GgufModelState(
    val activeModelId: String = "builtin_gemma3_2b_q4",
    val activeModelName: String = "Gemma-3-2B-Instruct (GGUF)",
    val activeFormat: AiModelFormat = AiModelFormat.GGUF,
    val quantization: GgufQuantization = GgufQuantization.Q4_K_M,
    val status: GgufLoadStatus = GgufLoadStatus.UNLOADED,
    val vulkanSupported: Boolean = true,
    val openClAvailable: Boolean = true,
    val gpuOffloadLayers: Int = 24,
    val estimatedVramUsedMb: Int = 0,
    val deviceAvailableRamMb: Long = 3072L,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val batteryThresholdPercent: Int = 15,
    val autoSuspendMinutes: Int = 10,
    val lastActivityMillis: Long = System.currentTimeMillis(),
    val downloadedModels: List<LocalAiModelEntity> = emptyList(),
    val trashedModels: List<LocalAiModelEntity> = emptyList()
)

/**
 * Manages Top-50 local AI model formats (GGUF/SafeTensors/ONNX/TFLite/ExecuTorch/etc.),
 * silent pre-warming on typing, Vulkan/OpenCL layer offloading, 10m idle + <=15% battery
 * auto-suspension, PAQ RAM coordination, and 2-mode delete (hide UI vs model_trash/).
 */
class GgufLifecycleManager(private val context: Context) {

    companion object {
        val defaultCatalog = listOf(
            CatalogModelSpec(
                id = "builtin_gemma3_2b_q4",
                name = "Gemma-3-2B-Instruct (GGUF)",
                paramsBillion = 2.0f,
                defaultFormat = AiModelFormat.GGUF,
                baseSizeMb = 1480,
                downloadUrl = "https://huggingface.co/google/gemma-3-2b-it-qat-q4_0-gguf",
                description = "Tối ưu cho Vulkan 1.3 Mobile & suy luận tiếng Việt mượt mà"
            ),
            CatalogModelSpec(
                id = "builtin_qwen25_3b_q4",
                name = "Qwen-2.5-Coder-3B (GGUF)",
                paramsBillion = 3.0f,
                defaultFormat = AiModelFormat.GGUF,
                baseSizeMb = 1950,
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                description = "Chuyên sinh mã Kotlin, Python, SQL ngoại tuyến"
            ),
            CatalogModelSpec(
                id = "builtin_phi4_mini_onnx",
                name = "Phi-4-Mini-Reasoning (ONNX)",
                paramsBillion = 3.8f,
                defaultFormat = AiModelFormat.ONNX,
                baseSizeMb = 2120,
                downloadUrl = "https://huggingface.co/microsoft/Phi-4-mini-instruct-onnx",
                description = "Suy luận logic <think> tăng tốc qua ONNX Runtime NNAPI"
            )
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var idleWatchJob: Job? = null
    private val modelsDir = File(context.filesDir, "downloaded_ai_models").apply { mkdirs() }
    private val trashDir = File(context.filesDir, "model_trash").apply { mkdirs() }

    private val _state = MutableStateFlow(GgufModelState())
    val state: StateFlow<GgufModelState> = _state.asStateFlow()

    init {
        refreshDeviceTelemetry()
    }

    fun refreshDeviceTelemetry() {
        val vulkanOk = try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE)
        } catch (_: Exception) {
            true
        }
        val availRam = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mem = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mem)
            mem.availMem / (1024L * 1024L)
        } catch (_: Exception) {
            2048L
        }
        val (pct, charging) = readBatteryStatus()
        _state.value = _state.value.copy(
            vulkanSupported = vulkanOk,
            deviceAvailableRamMb = availRam,
            batteryPercent = pct,
            isCharging = charging
        )
    }

    fun syncPersistedModels(allModels: List<LocalAiModelEntity>) {
        val visible = allModels.filter { !it.isInTrash && it.isVisibleInList }
        val trashed = allModels.filter { it.isInTrash }
        val activeStillPresent = visible.firstOrNull { it.id == _state.value.activeModelId }
        _state.value = _state.value.copy(
            downloadedModels = visible,
            trashedModels = trashed,
            activeModelName = activeStillPresent?.name ?: _state.value.activeModelName,
            activeFormat = activeStillPresent?.let { fmt ->
                AiModelFormat.entries.firstOrNull { it.id == fmt.formatId } ?: _state.value.activeFormat
            } ?: _state.value.activeFormat
        )
    }

    fun selectModel(modelId: String, modelName: String, format: AiModelFormat = AiModelFormat.GGUF) {
        _state.value = _state.value.copy(
            activeModelId = modelId,
            activeModelName = modelName,
            activeFormat = format,
            status = GgufLoadStatus.UNLOADED,
            estimatedVramUsedMb = 0
        )
    }

    fun updateTuning(
        quantization: GgufQuantization,
        gpuOffloadLayers: Int,
        batteryThreshold: Int,
        autoSuspendMinutes: Int
    ) {
        _state.value = _state.value.copy(
            quantization = quantization,
            gpuOffloadLayers = gpuOffloadLayers.coerceIn(0, 48),
            batteryThresholdPercent = batteryThreshold.coerceIn(5, 40),
            autoSuspendMinutes = autoSuspendMinutes.coerceIn(1, 60)
        )
    }

    fun onUserTypingPrewarm(thermalElevated: Boolean = false) {
        val cur = _state.value
        val (pct, charging) = readBatteryStatus()
        if (!charging && pct <= cur.batteryThresholdPercent) {
            suspendModel(GgufLoadStatus.SUSPENDED_LOW_BATTERY)
            return
        }
        _state.value = cur.copy(
            lastActivityMillis = System.currentTimeMillis(),
            batteryPercent = pct,
            isCharging = charging
        )
        if (cur.status == GgufLoadStatus.UNLOADED ||
            cur.status == GgufLoadStatus.SUSPENDED_IDLE ||
            cur.status == GgufLoadStatus.SUSPENDED_LOW_BATTERY
        ) {
            scope.launch {
                val estimatedMb = computeEstimatedMemoryMb()
                PaqRamArbiter.setAiModelBusy(true, estimatedMb.toLong())
                _state.value = _state.value.copy(status = GgufLoadStatus.WARMING_UP)
                delay(180L)
                val targetStatus = if (thermalElevated) GgufLoadStatus.THERMAL_THROTTLED else GgufLoadStatus.ACTIVE_READY
                _state.value = _state.value.copy(
                    status = targetStatus,
                    estimatedVramUsedMb = estimatedMb
                )
                PaqRamArbiter.setAiModelBusy(false, estimatedMb.toLong())
                startIdleWatchdog()
            }
        }
    }

    suspend fun generateLocalResponse(prompt: String, thermalElevated: Boolean = false): String {
        refreshDeviceTelemetry()
        val cur = _state.value
        if (!cur.isCharging && cur.batteryPercent <= cur.batteryThresholdPercent) {
            suspendModel(GgufLoadStatus.SUSPENDED_LOW_BATTERY)
            return "[Cảnh báo Pin ${cur.batteryPercent}%] Mô hình ${cur.activeModelName} đã tự động tạm ngưng theo ngưỡng an toàn (${cur.batteryThresholdPercent}%). Hãy cắm sạc hoặc chuyển sang Gemini Cloud."
        }
        if (cur.status != GgufLoadStatus.ACTIVE_READY && cur.status != GgufLoadStatus.THERMAL_THROTTLED) {
            val estMb = computeEstimatedMemoryMb()
            PaqRamArbiter.setAiModelBusy(true, estMb.toLong())
            delay(220L)
            _state.value = _state.value.copy(
                status = if (thermalElevated) GgufLoadStatus.THERMAL_THROTTLED else GgufLoadStatus.ACTIVE_READY,
                estimatedVramUsedMb = estMb,
                lastActivityMillis = System.currentTimeMillis()
            )
            PaqRamArbiter.setAiModelBusy(false, estMb.toLong())
            startIdleWatchdog()
        } else {
            _state.value = cur.copy(lastActivityMillis = System.currentTimeMillis())
        }

        val updated = _state.value
        val plan = MultiFormatHardwareOptimizer.optimizeForDevice(
            format = updated.activeFormat,
            vulkanSupported = updated.vulkanSupported,
            availableRamMb = updated.deviceAvailableRamMb,
            batteryPercent = updated.batteryPercent,
            thermalElevated = thermalElevated
        )
        return MultiFormatInferenceEngine.inferLocal(
            modelName = updated.activeModelName,
            format = updated.activeFormat,
            quantLabel = updated.quantization.id,
            plan = plan,
            prompt = prompt
        )
    }

    fun suspendModel(reason: GgufLoadStatus = GgufLoadStatus.UNLOADED) {
        idleWatchJob?.cancel()
        PaqRamArbiter.setAiModelBusy(false, 0L)
        _state.value = _state.value.copy(
            status = reason,
            estimatedVramUsedMb = 0
        )
    }

    fun moveFileToTrash(localPath: String): String? {
        val src = File(localPath)
        if (!src.exists()) return null
        val dest = File(trashDir, "${UUID.randomUUID().toString().take(6)}_${src.name}")
        return if (src.renameTo(dest)) {
            dest.absolutePath
        } else {
            src.copyTo(dest, overwrite = true)
            src.delete()
            dest.absolutePath
        }
    }

    fun restoreFileFromTrash(trashPath: String): String? {
        val src = File(trashPath)
        if (!src.exists()) return null
        val cleanName = src.name.substringAfter('_').ifBlank { src.name }
        val dest = File(modelsDir, cleanName)
        return if (src.renameTo(dest)) {
            dest.absolutePath
        } else {
            src.copyTo(dest, overwrite = true)
            src.delete()
            dest.absolutePath
        }
    }

    fun permanentlyDeleteFile(path: String): Boolean {
        val f = File(path)
        return if (f.exists()) f.deleteRecursively() else true
    }

    fun computeEstimatedMemoryMb(): Int {
        val cur = _state.value
        val downloaded = cur.downloadedModels.firstOrNull { it.id == cur.activeModelId }
        val base = if (downloaded != null) {
            max(120, (downloaded.sizeBytes / (1024L * 1024L)).toInt())
        } else {
            defaultCatalog.firstOrNull { it.id == cur.activeModelId }?.baseSizeMb ?: 1480
        }
        val scaled = (base * cur.quantization.sizeFactor).toInt()
        return min(max(120, scaled), max(512, (cur.deviceAvailableRamMb * 0.75f).toInt()))
    }

    private fun startIdleWatchdog() {
        idleWatchJob?.cancel()
        idleWatchJob = scope.launch {
            while (true) {
                delay(15_000L)
                val s = _state.value
                if (s.status != GgufLoadStatus.ACTIVE_READY && s.status != GgufLoadStatus.THERMAL_THROTTLED) break
                val idleMs = System.currentTimeMillis() - s.lastActivityMillis
                val thresholdMs = s.autoSuspendMinutes * 60_000L
                val (pct, charging) = readBatteryStatus()
                if (!charging && pct <= s.batteryThresholdPercent) {
                    suspendModel(GgufLoadStatus.SUSPENDED_LOW_BATTERY)
                    break
                }
                if (idleMs >= thresholdMs) {
                    suspendModel(GgufLoadStatus.SUSPENDED_IDLE)
                    break
                }
            }
        }
    }

    private fun readBatteryStatus(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val pct = ((level * 100f) / scale.coerceAtLeast(1)).toInt().coerceIn(0, 100)
            pct to charging
        } catch (_: Exception) {
            100 to false
        }
    }
}
