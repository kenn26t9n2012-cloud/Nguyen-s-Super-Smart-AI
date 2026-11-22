package com.nguyen_super_app.nguyen_super_smart_ai.engine.browser

import android.content.Context
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.MultiFormatCompressionEngine
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.AiModelFormat
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.BinaryInspectionReport
import com.nguyen_super_app.nguyen_super_smart_ai.engine.localai.MultiFormatBinaryInspector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID

enum class DownloadPipelinePhase {
    IDLE,
    REJECTED_INVALID_EXTENSION,
    DOWNLOADING,
    EXTRACTING,
    SCANNING_MAGIC_BYTES,
    REGISTERED_SUCCESS,
    PURGED_NO_VALID_MODELS,
    FAILED
}

data class BrowserDownloadState(
    val phase: DownloadPipelinePhase = DownloadPipelinePhase.IDLE,
    val targetFileName: String = "",
    val sourceUrl: String = "",
    val progress: Float = 0f,
    val statusMessage: String = "Sẵn sàng tải mô hình AI từ trình duyệt (hỗ trợ 50 định dạng + 15+ tệp nén).",
    val registeredModels: List<BinaryInspectionReport> = emptyList(),
    val lastPurgedReason: String? = null
)

/**
 * Handles WebView DownloadListener validation, downloading, archive extraction,
 * recursive Top-50 magic-byte scanning, and automatic permanent deletion of invalid archives.
 */
class ModelDownloadPipeline(
    private val context: Context,
    private val authTokenProvider: () -> String? = { null },
    private val onModelsRegistered: suspend (models: List<Pair<File, BinaryInspectionReport>>, sourceUrl: String) -> Unit
) {
    companion object {
        const val INVALID_EXTENSION_ERROR = "Đuôi mô hình không hợp lệ, vui lòng tải lại."
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private val modelsRootDir = File(context.filesDir, "downloaded_ai_models").apply { mkdirs() }
    private val stagingDir = File(context.filesDir, "browser_staging").apply { mkdirs() }

    private val _state = MutableStateFlow(BrowserDownloadState())
    val state: StateFlow<BrowserDownloadState> = _state.asStateFlow()

    fun validateDownloadCandidate(url: String, contentDisposition: String?, mimeType: String?): Pair<Boolean, String> {
        val fileName = inferFileName(url, contentDisposition, mimeType)
        val isModelExt = MultiFormatBinaryInspector.isAllowedExtension(fileName)
        val isArchiveExt = MultiFormatCompressionEngine.isArchiveFileName(fileName)
        return if (isModelExt || isArchiveExt) {
            true to fileName
        } else {
            false to fileName
        }
    }

    fun handleWebViewDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val (allowed, fileName) = validateDownloadCandidate(url, contentDisposition, mimeType)
        if (!allowed) {
            _state.value = BrowserDownloadState(
                phase = DownloadPipelinePhase.REJECTED_INVALID_EXTENSION,
                targetFileName = fileName,
                sourceUrl = url,
                progress = 0f,
                statusMessage = "$INVALID_EXTENSION_ERROR ($fileName)"
            )
            return
        }

        scope.launch {
            processDownloadAndVerify(url, fileName, userAgent)
        }
    }

    suspend fun processLocalArchiveOrModelFile(file: File, sourceUrl: String = "local://import"): BrowserDownloadState {
        return inspectAndRegisterOrPurge(file, sourceUrl, isArchive = MultiFormatCompressionEngine.isArchiveFileName(file.name))
    }

    private suspend fun processDownloadAndVerify(url: String, fileName: String, userAgent: String?) {
        _state.value = BrowserDownloadState(
            phase = DownloadPipelinePhase.DOWNLOADING,
            targetFileName = fileName,
            sourceUrl = url,
            progress = 0.05f,
            statusMessage = "Đang tải $fileName..."
        )

        val jobId = UUID.randomUUID().toString().take(8)
        val downloadedFile = File(stagingDir, "${jobId}_$fileName")
        try {
            val reqBuilder = Request.Builder().url(url)
            if (!userAgent.isNullOrBlank()) reqBuilder.header("User-Agent", userAgent)
            authTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                if (url.contains("huggingface.co", ignoreCase = true)) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}: ${resp.message}")
                }
                val body = resp.body ?: throw IllegalStateException("Phản hồi tải xuống rỗng")
                val totalLen = body.contentLength().coerceAtLeast(1L)
                body.byteStream().use { input ->
                    downloadedFile.outputStream().use { output ->
                        val buf = ByteArray(16 * 1024)
                        var copied = 0L
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            output.write(buf, 0, r)
                            copied += r
                            val pct = (copied.toFloat() / totalLen.toFloat()).coerceIn(0.05f, 0.92f)
                            _state.value = _state.value.copy(
                                progress = pct,
                                statusMessage = "Đang tải $fileName (${(pct * 100).toInt()}%)..."
                            )
                        }
                    }
                }
            }

            val finalState = inspectAndRegisterOrPurge(
                downloadedFile = downloadedFile,
                sourceUrl = url,
                isArchive = MultiFormatCompressionEngine.isArchiveFileName(fileName)
            )
            _state.value = finalState
        } catch (e: Exception) {
            downloadedFile.delete()
            _state.value = BrowserDownloadState(
                phase = DownloadPipelinePhase.FAILED,
                targetFileName = fileName,
                sourceUrl = url,
                progress = 0f,
                statusMessage = "Lỗi tải xuống $fileName: ${e.message}"
            )
        }
    }

    /**
     * Extracts archives safely, scans recursively across all nested subdirectories by
     * extension AND magic bytes, and permanently deletes BOTH archive and extracted dir if 0 valid models found.
     */
    suspend fun inspectAndRegisterOrPurge(
        downloadedFile: File,
        sourceUrl: String,
        isArchive: Boolean
    ): BrowserDownloadState {
        if (!isArchive) {
            _state.value = _state.value.copy(
                phase = DownloadPipelinePhase.SCANNING_MAGIC_BYTES,
                progress = 0.96f,
                statusMessage = "Đang kiểm tra chữ ký nhị phân (Magic Bytes) cho ${downloadedFile.name}..."
            )
            val report = MultiFormatBinaryInspector.inspectFile(downloadedFile)
            if (!report.isValidModel) {
                downloadedFile.delete()
                val errState = BrowserDownloadState(
                    phase = DownloadPipelinePhase.PURGED_NO_VALID_MODELS,
                    targetFileName = downloadedFile.name,
                    sourceUrl = sourceUrl,
                    progress = 0f,
                    statusMessage = "$INVALID_EXTENSION_ERROR (${report.signatureDetails}). Đã tự động xóa tệp không hợp lệ.",
                    lastPurgedReason = report.signatureDetails
                )
                _state.value = errState
                return errState
            }
            val finalFile = File(modelsRootDir, downloadedFile.name)
            downloadedFile.copyTo(finalFile, overwrite = true)
            downloadedFile.delete()
            onModelsRegistered(listOf(finalFile to report), sourceUrl)
            val okState = BrowserDownloadState(
                phase = DownloadPipelinePhase.REGISTERED_SUCCESS,
                targetFileName = finalFile.name,
                sourceUrl = sourceUrl,
                progress = 1f,
                statusMessage = "Đã xác thực ${report.format?.id} (${report.signatureDetails}) và nạp vào Kho AI.",
                registeredModels = listOf(report)
            )
            _state.value = okState
            return okState
        }

        // Archive pipeline: extract -> recursive scan -> register valid models OR purge ALL
        val extractDir = File(stagingDir, "extract_${UUID.randomUUID().toString().take(8)}")
        return try {
            _state.value = _state.value.copy(
                phase = DownloadPipelinePhase.EXTRACTING,
                progress = 0.70f,
                statusMessage = "Đang giải nén an toàn ${downloadedFile.name}..."
            )
            MultiFormatCompressionEngine.extractArchiveSafely(downloadedFile, extractDir, context)

            _state.value = _state.value.copy(
                phase = DownloadPipelinePhase.SCANNING_MAGIC_BYTES,
                progress = 0.90f,
                statusMessage = "Đang quét đệ quy 50 định dạng AI trong thư mục giải nén..."
            )

            val validModels = mutableListOf<Pair<File, BinaryInspectionReport>>()
            scanDirectoryRecursively(extractDir, validModels)

            if (validModels.isEmpty()) {
                // STRICT RULE: Warn & permanently delete BOTH archive and extracted directory
                downloadedFile.delete()
                extractDir.deleteRecursively()
                val purgeState = BrowserDownloadState(
                    phase = DownloadPipelinePhase.PURGED_NO_VALID_MODELS,
                    targetFileName = downloadedFile.name,
                    sourceUrl = sourceUrl,
                    progress = 0f,
                    statusMessage = "Cảnh báo nghiêm ngặt: Không tìm thấy mô hình AI hợp lệ nào trong ${downloadedFile.name}. Đã xóa vĩnh viễn gói nén và thư mục giải nén.",
                    lastPurgedReason = "0 mô hình hợp lệ thuộc 50 định dạng AI"
                )
                _state.value = purgeState
                purgeState
            } else {
                val movedModels = validModels.map { (srcFile, report) ->
                    val dest = File(modelsRootDir, "${System.currentTimeMillis()}_${srcFile.name}")
                    srcFile.copyTo(dest, overwrite = true)
                    dest to report
                }
                // Release temporary staging archive & extract folder after registering models
                downloadedFile.delete()
                extractDir.deleteRecursively()
                onModelsRegistered(movedModels, sourceUrl)
                val okState = BrowserDownloadState(
                    phase = DownloadPipelinePhase.REGISTERED_SUCCESS,
                    targetFileName = downloadedFile.name,
                    sourceUrl = sourceUrl,
                    progress = 1f,
                    statusMessage = "Giải nén thành công: Đã xác thực & đăng ký ${movedModels.size} mô hình AI (${movedModels.joinToString { it.second.format?.id ?: "" }}).",
                    registeredModels = movedModels.map { it.second }
                )
                _state.value = okState
                okState
            }
        } catch (e: Exception) {
            downloadedFile.delete()
            extractDir.deleteRecursively()
            val errState = BrowserDownloadState(
                phase = DownloadPipelinePhase.FAILED,
                targetFileName = downloadedFile.name,
                sourceUrl = sourceUrl,
                progress = 0f,
                statusMessage = "Giải nén thất bại & đã dọn sạch bộ nhớ: ${e.message}"
            )
            _state.value = errState
            errState
        }
    }

    private fun scanDirectoryRecursively(
        dir: File,
        accumulator: MutableList<Pair<File, BinaryInspectionReport>>
    ) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                // Support folder-packaged formats like CoreML .mlpackage
                if (child.name.endsWith(".mlpackage", ignoreCase = true)) {
                    val info = BinaryInspectionReport(
                        isValidModel = true,
                        format = AiModelFormat.COREML,
                        fileName = child.name,
                        fileSizeBytes = computeDirSize(child),
                        matchedByExtension = true,
                        matchedByMagicBytes = true,
                        signatureDetails = "CoreML .mlpackage directory bundle",
                        recommendedBackend = AiModelFormat.COREML.preferredBackend
                    )
                    accumulator.add(child to info)
                } else {
                    scanDirectoryRecursively(child, accumulator)
                }
            } else {
                val report = MultiFormatBinaryInspector.inspectFile(child)
                if (report.isValidModel) {
                    accumulator.add(child to report)
                }
            }
        }
    }

    private fun computeDirSize(dir: File): Long {
        var total = 0L
        dir.walkTopDown().forEach { if (it.isFile) total += it.length() }
        return total
    }

    fun inferFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        if (!contentDisposition.isNullOrBlank()) {
            val match = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";\\r\\n]+)\"?", RegexOption.IGNORE_CASE)
                .find(contentDisposition)
            val extracted = match?.groupValues?.getOrNull(1)?.trim()
            if (!extracted.isNullOrBlank()) return extracted
        }
        val cleanPath = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val segment = cleanPath.substringAfterLast('/', "")
        if (segment.contains('.')) return segment
        val fallbackExt = when {
            mimeType?.contains("zip") == true -> "zip"
            mimeType?.contains("gzip") == true -> "gz"
            mimeType?.contains("html") == true -> "html"
            else -> "bin"
        }
        return "download_${System.currentTimeMillis()}.$fallbackExt"
    }

    fun dismissBanner() {
        _state.value = _state.value.copy(phase = DownloadPipelinePhase.IDLE)
    }
}
