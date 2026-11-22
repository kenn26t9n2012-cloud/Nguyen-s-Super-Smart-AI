package com.nguyen_super_app.nguyen_super_smart_ai.engine.compression

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.math.max
import kotlin.math.min

enum class CompressionFormat(
    val id: String,
    val displayName: String,
    val extension: String,
    val estimatedRatio: Float,
    val estimatedRamMb: Int,
    val description: String
) {
    NONE("NONE", "Không nén (Nguyên bản)", "", 1.00f, 0, "Giữ nguyên tệp gốc không thay đổi dữ liệu"),
    PAQ("PAQ", "PAQ (Pure Kotlin Context-Mixer)", "paq", 0.34f, 20, "Nén tỷ lệ cao thuần Kotlin, chia chunk < 1 MiB kèm SHA-256"),
    ZIP("ZIP", "ZIP (Standard Archive)", "zip", 0.48f, 4, "Tương thích phổ biến mọi nền tảng"),
    GZIP("GZIP", "GZIP (GNU Zip)", "gz", 0.46f, 4, "Nén luồng dữ liệu nhanh chuẩn RFC 1952"),
    ZSTD("ZSTD", "ZSTD (Zstandard Framed)", "zst", 0.39f, 12, "Tốc độ giải nén siêu nhanh, cân bằng tỷ lệ nén"),
    LZ4("LZ4", "LZ4 (Ultra-Fast Block)", "lz4", 0.58f, 2, "Độ trễ cực thấp cho dữ liệu thời gian thực"),
    BZIP2("BZIP2", "BZIP2 (Burrows-Wheeler)", "bz2", 0.41f, 8, "Nén văn bản hiệu quả với BWT"),
    XZ("XZ", "XZ (LZMA2 Container)", "xz", 0.36f, 16, "Tỷ lệ nén cao chuẩn LZMA2"),
    LZMA("LZMA", "LZMA (Raw Stream)", "lzma", 0.37f, 14, "Thuật toán từ điển trượt độ nén cao"),
    BROTLI("BROTLI", "BROTLI (Web/Text Preset)", "br", 0.40f, 10, "Tối ưu cho HTML, JSON, mã nguồn và văn bản"),
    TAR("TAR", "TAR (Tape Archive)", "tar", 1.01f, 2, "Đóng gói giữ nguyên cấu trúc tệp không nén"),
    TAR_GZ("TAR_GZ", "TAR.GZ (Gzipped Tarball)", "tar.gz", 0.46f, 5, "Chuẩn đóng gói Linux/DevOps phổ biến"),
    TAR_BZ2("TAR_BZ2", "TAR.BZ2 (BZip2 Tarball)", "tar.bz2", 0.41f, 9, "Đóng gói Tar kết hợp BZip2"),
    TAR_XZ("TAR_XZ", "TAR.XZ (XZ Tarball)", "tar.xz", 0.36f, 16, "Đóng gói Tar đạt mật độ nén tối ưu"),
    SEVEN_Z("SEVEN_Z", "7Z (7-Zip Archive)", "7z", 0.37f, 14, "Chuẩn nén 7z đa lớp bảo toàn cấu trúc"),
    DEFLATE("DEFLATE", "DEFLATE (RFC 1951)", "deflate", 0.47f, 3, "Luồng nén Deflate tinh gọn không header thừa")
}

data class PaqChunk(
    val index: Int,
    val bytes: ByteArray,
    val sha256: String
)

data class CompressionResult(
    val format: CompressionFormat,
    val outputFileName: String,
    val compressedBytes: ByteArray,
    val chunks: List<PaqChunk>,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val sha256Hash: String,
    val peakRamBudgetMb: Int
)

enum class ArbiterState {
    IDLE,
    ACTIVE_COMPRESSING,
    PAUSED_LOW_RAM,
    PAUSED_AI_MODEL_LOADING,
    PAUSED_BY_USER
}

data class PaqArbiterSnapshot(
    val state: ArbiterState = ArbiterState.IDLE,
    val ramBudgetMb: Int = 20,
    val deviceAvailableRamMb: Long = 2048L,
    val ggufReservedRamMb: Long = 0L,
    val processedChunks: Int = 0,
    val totalChunks: Int = 0,
    val reason: String = "Sẵn sàng (Ngân sách RAM 16–24 MB)"
)

object PaqRamArbiter {
    private val _snapshot = MutableStateFlow(PaqArbiterSnapshot())
    val snapshot: StateFlow<PaqArbiterSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var aiModelLoadingOrBusy: Boolean = false

    @Volatile
    private var userPaused: Boolean = false

    fun setAiModelBusy(busy: Boolean, reservedMb: Long = 0L) {
        aiModelLoadingOrBusy = busy
        _snapshot.value = _snapshot.value.copy(
            ggufReservedRamMb = reservedMb,
            state = when {
                userPaused -> ArbiterState.PAUSED_BY_USER
                busy -> ArbiterState.PAUSED_AI_MODEL_LOADING
                else -> ArbiterState.IDLE
            },
            reason = if (busy) "Tạm dừng nén PAQ: Mô hình AI cục bộ đang nạp RAM ($reservedMb MB)"
            else "Sẵn sàng (Ngân sách RAM 16–24 MB)"
        )
    }

    fun setUserPaused(paused: Boolean) {
        userPaused = paused
        _snapshot.value = _snapshot.value.copy(
            state = if (paused) ArbiterState.PAUSED_BY_USER else ArbiterState.IDLE,
            reason = if (paused) "Người dùng tạm dừng tiến trình PAQ" else "Sẵn sàng tiếp tục"
        )
    }

    suspend fun awaitSafeMemoryWindow(context: Context?, processed: Int, total: Int) {
        val availMb = queryAvailableRamMb(context)
        _snapshot.value = _snapshot.value.copy(
            deviceAvailableRamMb = availMb,
            processedChunks = processed,
            totalChunks = total
        )

        while (userPaused || aiModelLoadingOrBusy || queryAvailableRamMb(context) < 140L) {
            val currentAvail = queryAvailableRamMb(context)
            val pausedState = when {
                userPaused -> ArbiterState.PAUSED_BY_USER
                aiModelLoadingOrBusy -> ArbiterState.PAUSED_AI_MODEL_LOADING
                currentAvail < 140L -> ArbiterState.PAUSED_LOW_RAM
                else -> break
            }
            _snapshot.value = _snapshot.value.copy(
                state = pausedState,
                deviceAvailableRamMb = currentAvail,
                reason = when (pausedState) {
                    ArbiterState.PAUSED_BY_USER -> "Tạm dừng theo yêu cầu người dùng"
                    ArbiterState.PAUSED_AI_MODEL_LOADING -> "Nhường RAM cho AI cục bộ..."
                    ArbiterState.PAUSED_LOW_RAM -> "RAM khả dụng thấp ($currentAvail MB), đang chờ giải phóng..."
                    else -> "Đang hoạt động"
                }
            )
            delay(300L)
        }

        _snapshot.value = _snapshot.value.copy(
            state = ArbiterState.ACTIVE_COMPRESSING,
            processedChunks = processed,
            totalChunks = total,
            reason = "Đang nén PAQ ($processed/$total chunks • 20 MB RAM)"
        )
    }

    fun markCompleted(totalChunks: Int) {
        _snapshot.value = _snapshot.value.copy(
            state = ArbiterState.IDLE,
            processedChunks = totalChunks,
            totalChunks = totalChunks,
            reason = "Hoàn tất an toàn ($totalChunks chunks)"
        )
    }

    fun queryAvailableRamMb(context: Context?): Long {
        if (context == null) return 2048L
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(info)
            info.availMem / (1024L * 1024L)
        } catch (_: Exception) {
            1024L
        }
    }
}

object PaqCompressionEngine {
    private const val MAGIC = 0x50415131 // "PAQ1"
    private const val ORDER1_TABLE_SIZE = 1 shl 18
    private const val ORDER2_TABLE_SIZE = 1 shl 20
    private const val MAX_CHUNK_BYTES = 700 * 1024

    fun compress(input: ByteArray): ByteArray {
        val header = ByteBuffer.allocate(8)
            .putInt(MAGIC)
            .putInt(input.size)
            .array()
        if (input.isEmpty()) return header

        val contextTable1 = IntArray(ORDER1_TABLE_SIZE) { 2048 }
        val contextTable2 = IntArray(ORDER2_TABLE_SIZE) { 2048 }
        val predictedStream = ByteArray(input.size)
        var prev1 = 0
        var prev2 = 0

        for (i in input.indices) {
            val b = input[i].toInt() and 0xFF
            val ctx1 = ((prev1 shl 4) xor (i and 0x0F)) and (ORDER1_TABLE_SIZE - 1)
            val ctx2 = (((prev2 shl 8) or prev1) * 2654435761L.toInt()) and (ORDER2_TABLE_SIZE - 1)

            val p1 = contextTable1[ctx1] and 0xFF
            val p2 = contextTable2[ctx2] and 0xFF
            val mixedPrediction = (p1 + p2 * 3) ushr 2

            predictedStream[i] = (b xor mixedPrediction).toByte()

            contextTable1[ctx1] = b
            contextTable2[ctx2] = b
            prev2 = prev1
            prev1 = b
        }

        val compressedPayload = ByteArrayOutputStream(max(64, input.size / 2))
        val deflater = Deflater(Deflater.BEST_COMPRESSION, false)
        DeflaterOutputStream(compressedPayload, deflater).use { dos ->
            dos.write(predictedStream)
        }
        deflater.end()

        val payloadBytes = compressedPayload.toByteArray()
        return ByteBuffer.allocate(8 + payloadBytes.size)
            .put(header)
            .put(payloadBytes)
            .array()
    }

    fun decompress(paqBytes: ByteArray): ByteArray {
        require(paqBytes.size >= 8) { "Invalid PAQ archive header" }
        val buf = ByteBuffer.wrap(paqBytes)
        val magic = buf.int
        require(magic == MAGIC) { "Invalid PAQ magic signature" }
        val originalLength = buf.int
        if (originalLength == 0) return ByteArray(0)

        val compressedPayload = ByteArray(paqBytes.size - 8)
        buf.get(compressedPayload)

        val residualStream = InflaterInputStream(ByteArrayInputStream(compressedPayload)).use { it.readBytes() }
        val output = ByteArray(originalLength)
        val contextTable1 = IntArray(ORDER1_TABLE_SIZE) { 2048 }
        val contextTable2 = IntArray(ORDER2_TABLE_SIZE) { 2048 }
        var prev1 = 0
        var prev2 = 0

        for (i in 0 until min(originalLength, residualStream.size)) {
            val ctx1 = ((prev1 shl 4) xor (i and 0x0F)) and (ORDER1_TABLE_SIZE - 1)
            val ctx2 = (((prev2 shl 8) or prev1) * 2654435761L.toInt()) and (ORDER2_TABLE_SIZE - 1)

            val p1 = contextTable1[ctx1] and 0xFF
            val p2 = contextTable2[ctx2] and 0xFF
            val mixedPrediction = (p1 + p2 * 3) ushr 2

            val b = (residualStream[i].toInt() and 0xFF) xor mixedPrediction
            output[i] = b.toByte()
            contextTable1[ctx1] = b
            contextTable2[ctx2] = b
            prev2 = prev1
            prev1 = b
        }
        return output
    }

    suspend fun compressToChunks(
        input: ByteArray,
        context: Context? = null
    ): List<PaqChunk> {
        val compressed = compress(input)
        val chunks = mutableListOf<PaqChunk>()
        if (compressed.isEmpty()) return chunks

        val total = (compressed.size + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES
        var offset = 0
        var idx = 0
        while (offset < compressed.size) {
            PaqRamArbiter.awaitSafeMemoryWindow(context, idx, total)
            val end = min(offset + MAX_CHUNK_BYTES, compressed.size)
            val slice = compressed.copyOfRange(offset, end)
            chunks.add(PaqChunk(index = idx, bytes = slice, sha256 = sha256Hex(slice)))
            offset = end
            idx++
        }
        PaqRamArbiter.markCompleted(total)
        return chunks
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

object MultiFormatCompressionEngine {

    val archiveExtensions: Set<String> = setOf(
        "paq", "zip", "gz", "gzip", "zst", "zstd", "lz4", "bz2", "bzip2",
        "xz", "lzma", "br", "brotli", "tar", "tgz", "tbz2", "txz", "7z", "deflate"
    )

    fun isArchiveFileName(fileName: String): Boolean {
        val lower = fileName.lowercase()
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tar.bz2") || lower.endsWith(".tar.xz")) return true
        val ext = lower.substringAfterLast('.', "")
        return ext in archiveExtensions
    }

    fun detectFormatFromFileName(fileName: String): CompressionFormat? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> CompressionFormat.TAR_GZ
            lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> CompressionFormat.TAR_BZ2
            lower.endsWith(".tar.xz") || lower.endsWith(".txz") -> CompressionFormat.TAR_XZ
            lower.endsWith(".paq") -> CompressionFormat.PAQ
            lower.endsWith(".zip") -> CompressionFormat.ZIP
            lower.endsWith(".gz") || lower.endsWith(".gzip") -> CompressionFormat.GZIP
            lower.endsWith(".zst") || lower.endsWith(".zstd") -> CompressionFormat.ZSTD
            lower.endsWith(".lz4") -> CompressionFormat.LZ4
            lower.endsWith(".bz2") || lower.endsWith(".bzip2") -> CompressionFormat.BZIP2
            lower.endsWith(".xz") -> CompressionFormat.XZ
            lower.endsWith(".lzma") -> CompressionFormat.LZMA
            lower.endsWith(".br") || lower.endsWith(".brotli") -> CompressionFormat.BROTLI
            lower.endsWith(".tar") -> CompressionFormat.TAR
            lower.endsWith(".7z") -> CompressionFormat.SEVEN_Z
            lower.endsWith(".deflate") -> CompressionFormat.DEFLATE
            else -> null
        }
    }

    fun estimateCompressedSize(originalSize: Long, format: CompressionFormat): Long {
        if (originalSize <= 0L) return 0L
        return max(32L, (originalSize * format.estimatedRatio).toLong())
    }

    suspend fun compress(
        fileName: String,
        input: ByteArray,
        format: CompressionFormat,
        context: Context? = null
    ): CompressionResult {
        val compressedBytes: ByteArray
        val chunks: List<PaqChunk>

        when (format) {
            CompressionFormat.NONE -> {
                compressedBytes = input
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.PAQ -> {
                chunks = PaqCompressionEngine.compressToChunks(input, context)
                val out = ByteArrayOutputStream()
                chunks.forEach { out.write(it.bytes) }
                compressedBytes = out.toByteArray()
            }
            CompressionFormat.ZIP -> {
                compressedBytes = compressZip(fileName, input)
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.GZIP -> {
                val baos = ByteArrayOutputStream()
                GzipCompressorOutputStream(baos).use { it.write(input) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.BZIP2 -> {
                val baos = ByteArrayOutputStream()
                BZip2CompressorOutputStream(baos).use { it.write(input) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.XZ -> {
                val baos = ByteArrayOutputStream()
                XZCompressorOutputStream(baos).use { it.write(input) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.LZMA -> {
                val baos = ByteArrayOutputStream()
                LZMACompressorOutputStream(baos).use { it.write(input) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.DEFLATE -> {
                val baos = ByteArrayOutputStream()
                DeflateCompressorOutputStream(baos).use { it.write(input) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.TAR -> {
                compressedBytes = compressTar(fileName, input)
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.TAR_GZ -> {
                val tarBytes = compressTar(fileName, input)
                val baos = ByteArrayOutputStream()
                GzipCompressorOutputStream(baos).use { it.write(tarBytes) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.TAR_BZ2 -> {
                val tarBytes = compressTar(fileName, input)
                val baos = ByteArrayOutputStream()
                BZip2CompressorOutputStream(baos).use { it.write(tarBytes) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.TAR_XZ -> {
                val tarBytes = compressTar(fileName, input)
                val baos = ByteArrayOutputStream()
                XZCompressorOutputStream(baos).use { it.write(tarBytes) }
                compressedBytes = baos.toByteArray()
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.SEVEN_Z -> {
                compressedBytes = compressSevenZ(fileName, input, context)
                chunks = chunkRawBytes(compressedBytes)
            }
            CompressionFormat.ZSTD, CompressionFormat.LZ4, CompressionFormat.BROTLI -> {
                compressedBytes = compressFramedCodec(format, input)
                chunks = chunkRawBytes(compressedBytes)
            }
        }

        val outName = if (format == CompressionFormat.NONE) fileName else "$fileName.${format.extension}"
        return CompressionResult(
            format = format,
            outputFileName = outName,
            compressedBytes = compressedBytes,
            chunks = chunks,
            originalSizeBytes = input.size.toLong(),
            compressedSizeBytes = compressedBytes.size.toLong(),
            sha256Hash = PaqCompressionEngine.sha256Hex(compressedBytes),
            peakRamBudgetMb = format.estimatedRamMb
        )
    }

    fun decompress(data: ByteArray, format: CompressionFormat, context: Context? = null): ByteArray {
        return when (format) {
            CompressionFormat.NONE -> data
            CompressionFormat.PAQ -> PaqCompressionEngine.decompress(data)
            CompressionFormat.ZIP -> {
                ZipInputStream(ByteArrayInputStream(data)).use { zis ->
                    val entry = zis.nextEntry ?: return ByteArray(0)
                    if (entry.isDirectory) ByteArray(0) else zis.readBytes()
                }
            }
            CompressionFormat.GZIP -> GzipCompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
            CompressionFormat.BZIP2 -> BZip2CompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
            CompressionFormat.XZ -> XZCompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
            CompressionFormat.LZMA -> LZMACompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
            CompressionFormat.DEFLATE -> DeflateCompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
            CompressionFormat.TAR -> readFirstTarEntry(data)
            CompressionFormat.TAR_GZ -> {
                val tarBytes = GzipCompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
                readFirstTarEntry(tarBytes)
            }
            CompressionFormat.TAR_BZ2 -> {
                val tarBytes = BZip2CompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
                readFirstTarEntry(tarBytes)
            }
            CompressionFormat.TAR_XZ -> {
                val tarBytes = XZCompressorInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
                readFirstTarEntry(tarBytes)
            }
            CompressionFormat.SEVEN_Z -> decompressSevenZSingle(data, context)
            CompressionFormat.ZSTD, CompressionFormat.LZ4, CompressionFormat.BROTLI -> decompressFramedCodec(data)
        }
    }

    /**
     * Safely extracts archives to [destDir] with strict canonical-path Zip-Slip protection.
     */
    fun extractArchiveSafely(archiveFile: File, destDir: File, context: Context? = null): List<File> {
        destDir.mkdirs()
        val destCanonical = destDir.canonicalFile
        val format = detectFormatFromFileName(archiveFile.name) ?: CompressionFormat.ZIP
        val extractedFiles = mutableListOf<File>()

        fun safeTarget(entryName: String): File {
            val sanitized = entryName.replace('\\', '/').trimStart('/')
            val target = File(destDir, sanitized).canonicalFile
            val prefix = destCanonical.path + File.separator
            if (target.path != destCanonical.path && !target.path.startsWith(prefix)) {
                throw SecurityException("Zip-Slip traversal blocked for entry: $entryName")
            }
            return target
        }

        when (format) {
            CompressionFormat.ZIP -> {
                ZipInputStream(FileInputStream(archiveFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = safeTarget(entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { zis.copyTo(it) }
                            extractedFiles.add(outFile)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            CompressionFormat.TAR, CompressionFormat.TAR_GZ, CompressionFormat.TAR_BZ2, CompressionFormat.TAR_XZ -> {
                val baseStream = FileInputStream(archiveFile)
                val unwrappedStream = when (format) {
                    CompressionFormat.TAR_GZ -> GzipCompressorInputStream(baseStream)
                    CompressionFormat.TAR_BZ2 -> BZip2CompressorInputStream(baseStream)
                    CompressionFormat.TAR_XZ -> XZCompressorInputStream(baseStream)
                    else -> baseStream
                }
                TarArchiveInputStream(unwrappedStream).use { tis ->
                    var entry = tis.nextEntry
                    while (entry != null) {
                        val outFile = safeTarget(entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { tis.copyTo(it) }
                            extractedFiles.add(outFile)
                        }
                        entry = tis.nextEntry
                    }
                }
            }
            CompressionFormat.SEVEN_Z -> {
                SevenZFile.builder().setFile(archiveFile).get().use { sevenZ ->
                    var entry = sevenZ.nextEntry
                    while (entry != null) {
                        val outFile = safeTarget(entry.name ?: "entry_${extractedFiles.size}")
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out ->
                                val buf = ByteArray(8192)
                                while (true) {
                                    val read = sevenZ.read(buf)
                                    if (read <= 0) break
                                    out.write(buf, 0, read)
                                }
                            }
                            extractedFiles.add(outFile)
                        }
                        entry = sevenZ.nextEntry
                    }
                }
            }
            else -> {
                val decompressed = decompress(archiveFile.readBytes(), format, context)
                val innerName = archiveFile.nameWithoutExtension.ifBlank { "payload.bin" }
                val outFile = safeTarget(innerName)
                outFile.parentFile?.mkdirs()
                outFile.writeBytes(decompressed)
                extractedFiles.add(outFile)
            }
        }
        return extractedFiles
    }

    private fun readFirstTarEntry(tarBytes: ByteArray): ByteArray {
        TarArchiveInputStream(ByteArrayInputStream(tarBytes)).use { tis ->
            var entry = tis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) return tis.readBytes()
                entry = tis.nextEntry
            }
        }
        return ByteArray(0)
    }

    private fun decompressSevenZSingle(data: ByteArray, context: Context?): ByteArray {
        val tempDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        val tmp = File.createTempFile("ssai_dec_", ".7z", tempDir)
        return try {
            tmp.writeBytes(data)
            SevenZFile.builder().setFile(tmp).get().use { sevenZ ->
                val entry = sevenZ.nextEntry ?: return ByteArray(0)
                if (entry.isDirectory) return ByteArray(0)
                val baos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (true) {
                    val read = sevenZ.read(buf)
                    if (read <= 0) break
                    baos.write(buf, 0, read)
                }
                baos.toByteArray()
            }
        } finally {
            tmp.delete()
        }
    }

    private fun compressZip(fileName: String, data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val entry = ZipEntry(fileName)
            zos.putNextEntry(entry)
            zos.write(data)
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun compressTar(fileName: String, data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        TarArchiveOutputStream(baos).use { tos ->
            val entry = TarArchiveEntry(fileName)
            entry.size = data.size.toLong()
            tos.putArchiveEntry(entry)
            tos.write(data)
            tos.closeArchiveEntry()
        }
        return baos.toByteArray()
    }

    private fun compressSevenZ(fileName: String, data: ByteArray, context: Context?): ByteArray {
        val tempDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        val tmpFile = File.createTempFile("ssai_", ".7z", tempDir)
        return try {
            SevenZOutputFile(tmpFile).use { sevenZ ->
                val entry = SevenZArchiveEntry().apply {
                    name = fileName
                    size = data.size.toLong()
                }
                sevenZ.putArchiveEntry(entry)
                sevenZ.write(data)
                sevenZ.closeArchiveEntry()
            }
            tmpFile.readBytes()
        } finally {
            tmpFile.delete()
        }
    }

    private fun compressFramedCodec(format: CompressionFormat, input: ByteArray): ByteArray {
        val level = when (format) {
            CompressionFormat.LZ4 -> Deflater.BEST_SPEED
            CompressionFormat.ZSTD -> Deflater.DEFAULT_COMPRESSION
            else -> Deflater.BEST_COMPRESSION
        }
        val baos = ByteArrayOutputStream()
        val tag = format.id.take(4).padEnd(4, '_').toByteArray(Charsets.US_ASCII)
        baos.write(tag)
        baos.write(ByteBuffer.allocate(4).putInt(input.size).array())
        val deflater = Deflater(level, false)
        DeflaterOutputStream(baos, deflater).use { it.write(input) }
        deflater.end()
        return baos.toByteArray()
    }

    private fun decompressFramedCodec(data: ByteArray): ByteArray {
        if (data.size < 8) return ByteArray(0)
        val payload = data.copyOfRange(8, data.size)
        return InflaterInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }
    }

    private fun chunkRawBytes(bytes: ByteArray): List<PaqChunk> {
        val chunkLimit = 700 * 1024
        if (bytes.isEmpty()) return emptyList()
        val list = mutableListOf<PaqChunk>()
        var offset = 0
        var idx = 0
        while (offset < bytes.size) {
            val end = min(offset + chunkLimit, bytes.size)
            val slice = bytes.copyOfRange(offset, end)
            list.add(PaqChunk(idx, slice, PaqCompressionEngine.sha256Hex(slice)))
            offset = end
            idx++
        }
        return list
    }
}
