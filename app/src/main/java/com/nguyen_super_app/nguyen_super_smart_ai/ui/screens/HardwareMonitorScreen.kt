package com.nguyen_super_app.nguyen_super_smart_ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.LocalAiModelEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.BenchmarkEntry
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.HardwareHudSnapshot
import com.nguyen_super_app.nguyen_super_smart_ai.engine.hardware.ThermalSeverity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.AiModelFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufLifecycleManager
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufModelState
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.GgufQuantization
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.BenchmarkBarCanvas
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.RealTimeHardwareHudSparkline
import com.nguyen_super_app.nguyen_super_smart_ai.ui.graphics.ThermalGaugeCanvas
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.AmberWarn
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CoralError
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanBright
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanContainer
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.CyanPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.EmeraldOk
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateBorder
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateCard
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateDeep
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateElevated
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.SlateSurface
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextPrimary
import com.nguyen_super_app.nguyen_super_smart_ai.ui.theme.TextSecondary

/**
 * Dedicated Hardware Monitor ("Giám sát phần cứng") screen housing:
 * - The full `RealTimeHardwareHud` card (Thermal, MemoryWatchdog, BatteryMonitor, IdleMemoryManager)
 * - Vulkan/OpenCL GPU layer & quantization tuner
 * - Top 50 AI format registry, downloaded models, and 2-mode delete / model_trash manager
 * - `BenchmarkComparisonDialog`
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HardwareMonitorScreen(
    hudSnapshot: HardwareHudSnapshot,
    ggufState: GgufModelState,
    benchmarkEntries: List<BenchmarkEntry>,
    onRefreshHud: () -> Unit,
    onSelectModel: (String) -> Unit,
    onUpdateTuning: (GgufQuantization, Int, Int, Int) -> Unit,
    onSuspendModelNow: () -> Unit,
    onHideModel: (LocalAiModelEntity) -> Unit,
    onTrashModel: (LocalAiModelEntity) -> Unit,
    onRestoreModel: (LocalAiModelEntity) -> Unit,
    onPermanentDeleteModel: (LocalAiModelEntity) -> Unit,
    onOpenBrowser: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onRefreshHud() }

    var showBenchmarkDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<LocalAiModelEntity?>(null) }

    var selectedQuant by remember(ggufState.quantization) { mutableStateOf(ggufState.quantization) }
    var gpuLayers by remember(ggufState.gpuOffloadLayers) { mutableFloatStateOf(ggufState.gpuOffloadLayers.toFloat()) }
    var batteryLimit by remember(ggufState.batteryThresholdPercent) { mutableFloatStateOf(ggufState.batteryThresholdPercent.toFloat()) }

    Scaffold(
        containerColor = SlateDeep,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("hardware_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = { Text("Giám sát Phần cứng & AI Cục bộ", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    TextButton(
                        onClick = {
                            onRefreshHud()
                            showBenchmarkDialog = true
                        },
                        modifier = Modifier.testTag("open_benchmark_dialog_button")
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = CyanBright)
                        Spacer(Modifier.width(4.dp))
                        Text("Benchmark", color = CyanBright)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // FULL RealTimeHardwareHud Card
            RealTimeHardwareHudCard(
                hud = hudSnapshot,
                onRefresh = onRefreshHud,
                onSuspendNow = onSuspendModelNow
            )

            // Vulkan/OpenCL GPU Layers & Quantization Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tối ưu Vulkan 1.3 / OpenCL & Lượng tử hóa", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GgufQuantization.entries.forEach { q ->
                            FilterChip(
                                selected = selectedQuant == q,
                                onClick = { selectedQuant = q },
                                label = { Text(q.id) }
                            )
                        }
                    }
                    Text("GPU Offload Layers: ${gpuLayers.toInt()} layers", style = MaterialTheme.typography.bodySmall)
                    Slider(value = gpuLayers, onValueChange = { gpuLayers = it }, valueRange = 0f..48f)

                    Text("Ngưỡng pin tự động tạm ngưng: ${batteryLimit.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = batteryLimit, onValueChange = { batteryLimit = it }, valueRange = 5f..35f)

                    Button(
                        onClick = {
                            onUpdateTuning(selectedQuant, gpuLayers.toInt(), batteryLimit.toInt(), 10)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lưu cấu hình Phần cứng")
                    }
                }
            }

            // Built-in + Downloaded Models (with 2-mode delete)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kho Mô hình (${AiModelFormat.entries.size} chuẩn AI)", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onOpenBrowser) { Text("Tải thêm từ Web") }
                    }

                    GgufLifecycleManager.defaultCatalog.forEach { cat ->
                        val active = ggufState.activeModelId == cat.id
                        Surface(
                            onClick = { onSelectModel(cat.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) CyanContainer else SlateElevated,
                            border = BorderStroke(1.dp, if (active) CyanPrimary else SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${cat.name} • ${cat.baseSizeMb} MB", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${cat.defaultFormat.id} • ${cat.description}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    if (ggufState.downloadedModels.isNotEmpty()) {
                        HorizontalDivider(color = SlateBorder)
                        Text("Mô hình đã tải & xác thực Magic Bytes", style = MaterialTheme.typography.labelLarge, color = EmeraldOk)
                        ggufState.downloadedModels.forEach { mdl ->
                            Surface(
                                onClick = { onSelectModel(mdl.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (ggufState.activeModelId == mdl.id) CyanContainer else SlateElevated,
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${mdl.name} [${mdl.formatId}]", style = MaterialTheme.typography.titleSmall)
                                        Text(mdl.magicSignatureSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    IconButton(onClick = { deleteCandidate = mdl }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Tùy chọn xóa", tint = CoralError)
                                    }
                                }
                            }
                        }
                    }

                    if (ggufState.trashedModels.isNotEmpty()) {
                        HorizontalDivider(color = SlateBorder)
                        Text("Thùng rác (model_trash/)", style = MaterialTheme.typography.labelLarge, color = AmberWarn)
                        ggufState.trashedModels.forEach { trashed ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(trashed.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { onRestoreModel(trashed) }) {
                                        Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Khôi phục", tint = EmeraldOk)
                                    }
                                    IconButton(onClick = { onPermanentDeleteModel(trashed) }) {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Xóa vĩnh viễn", tint = CoralError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBenchmarkDialog) {
        BenchmarkComparisonDialog(
            entries = benchmarkEntries,
            onDismiss = { showBenchmarkDialog = false }
        )
    }

    deleteCandidate?.let { mdl ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = SlateCard,
            title = { Text("Xóa mô hình ${mdl.name}") },
            text = { Text("Chọn 1 trong 2 chế độ quản lý bộ nhớ:") },
            confirmButton = {
                Button(onClick = {
                    onTrashModel(mdl)
                    deleteCandidate = null
                }) {
                    Text("Chuyển vào model_trash")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    onHideModel(mdl)
                    deleteCandidate = null
                }) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chỉ ẩn khỏi danh sách")
                }
            }
        )
    }
}

@Composable
fun RealTimeHardwareHudCard(
    hud: HardwareHudSnapshot,
    onRefresh: () -> Unit,
    onSuspendNow: () -> Unit
) {
    val thermalColor = when (hud.thermal.severity) {
        ThermalSeverity.NOMINAL -> EmeraldOk
        ThermalSeverity.MODERATE -> AmberWarn
        ThermalSeverity.SEVERE, ThermalSeverity.CRITICAL -> CoralError
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realtime_hardware_hud_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RealTimeHardwareHud", style = MaterialTheme.typography.titleMedium, color = CyanBright)
                    Text("${hud.activeModelName} • ${hud.activeAccelerator}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                OutlinedButton(onClick = onRefresh) { Text("Làm mới") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThermalGaugeCanvas(tempCelsius = hud.thermal.batteryTempCelsius)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = thermalColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${hud.thermal.batteryTempCelsius}°C • ${hud.thermal.severity.label}", style = MaterialTheme.typography.labelMedium, color = thermalColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RAM trống: ${hud.memory.availableRamMb} / ${hud.memory.totalRamMb} MB (${hud.memory.pressurePercent}%)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = EmeraldOk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pin: ${hud.battery.percent}% ${if (hud.battery.isCharging) "(Đang sạc)" else ""} • ${hud.cpuCores} Cores", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text("Áp lực RAM thời gian thực (${hud.modelLoadStatus.label})", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            RealTimeHardwareHudSparkline(values = hud.ramHistoryPoints, lineColor = CyanBright)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tốc độ ước tính: ${hud.estimatedTokensPerSec} tok/s • Tự ngắt sau ${hud.idleSecondsRemaining}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanBright
                )
                TextButton(onClick = onSuspendNow) { Text("Ngủ đông AI") }
            }
        }
    }
}

@Composable
fun BenchmarkComparisonDialog(
    entries: List<BenchmarkEntry>,
    onDismiss: () -> Unit
) {
    val maxTps = remember(entries) { entries.maxOfOrNull { it.tokensPerSec }?.coerceAtLeast(10f) ?: 100f }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("So sánh Benchmark AI", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                entries.forEach { e ->
                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(e.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${e.category} • ${e.accelerator}", style = MaterialTheme.typography.labelSmall, color = CyanBright)
                            BenchmarkBarCanvas(ratio = e.tokensPerSec / maxTps)
                            Text(
                                "${e.tokensPerSec} tok/s • TTFT ${e.ttftMs}ms • RAM ${e.ramMb} MB • Điểm ${e.scorePercent}/100",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
