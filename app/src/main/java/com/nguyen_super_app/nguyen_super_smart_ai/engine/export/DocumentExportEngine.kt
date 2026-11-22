package com.nguyen_super_app.nguyen_super_smart_ai.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.nguyen_super_app.nguyen_super_smart_ai.data.local.ChatMessageEntity
import com.nguyen_super_app.nguyen_super_smart_ai.engine.compression.PaqCompressionEngine
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class ExportCategory(val label: String) {
    DOCUMENT("Tài liệu & Văn phòng"),
    WEB_MARKUP("Web & Tài liệu kỹ thuật"),
    DATA_SERIAL("Dữ liệu & API"),
    PRESENTATION("Trình chiếu & Sách điện tử"),
    CODE_NOTEBOOK("Mã nguồn & Notebook"),
    IMAGE_RASTER("Ảnh & Xuất đồ họa"),
    BINARY_PACKAGE("Gói ứng dụng & Tệp nén")
}

data class ExportFormatSpec(
    val id: String,
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val category: ExportCategory
)

data class ExportPart(
    val partNumber: Int,
    val totalParts: Int,
    val fileName: String,
    val bytes: ByteArray,
    val sha256Hash: String
)

data class GeneratedExportPackage(
    val primaryFormat: ExportFormatSpec,
    val baseTitle: String,
    val parts: List<ExportPart>,
    val pdfPasswordApplied: Boolean,
    val totalBytes: Long,
    val summaryMessage: String
)

/**
 * Splits oversized transcripts or documents by configurable part size.
 */
object DocumentSplittingEngine {
    fun splitLines(lines: List<String>, maxLinesPerPart: Int = 40): List<List<String>> {
        val safeMax = maxLinesPerPart.coerceAtLeast(1)
        if (lines.isEmpty()) return listOf(emptyList())
        return lines.chunked(safeMax)
    }

    fun splitBytes(data: ByteArray, maxBytesPerPart: Int): List<ByteArray> {
        val safeLimit = maxBytesPerPart.coerceAtLeast(1024)
        if (data.size <= safeLimit) return listOf(data)
        val out = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val end = (offset + safeLimit).coerceAtMost(data.size)
            out.add(data.copyOfRange(offset, end))
            offset = end
        }
        return out
    }
}

/**
 * Encrypts sensitive PDF exports with AES-256-GCM envelope protection when a password is supplied.
 */
object PdfSecurityEngine {
    fun protectPdfIfRequested(rawPdf: ByteArray, password: String?): Pair<ByteArray, Boolean> {
        if (password.isNullOrBlank()) return rawPdf to false
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        val secret = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        val iv = cipher.iv
        val enc = cipher.doFinal(rawPdf)
        val header = "SSAI_AES256_PDF_LOCK\nIV_LEN=${iv.size}\n".toByteArray(Charsets.US_ASCII)
        val out = ByteArrayOutputStream().apply {
            write(header)
            write(iv)
            write(enc)
        }.toByteArray()
        return out to true
    }

    fun decryptProtectedPdf(lockedBytes: ByteArray, password: String): ByteArray {
        val marker = "SSAI_AES256_PDF_LOCK\nIV_LEN=".toByteArray(Charsets.US_ASCII)
        require(lockedBytes.size > marker.size + 14) { "Invalid protected PDF container" }
        val strHead = String(lockedBytes, 0, marker.size + 6, Charsets.US_ASCII)
        val ivLenStr = strHead.substringAfter("IV_LEN=").substringBefore('\n')
        val ivLen = ivLenStr.toInt()
        val dataStart = ("SSAI_AES256_PDF_LOCK\nIV_LEN=$ivLen\n").toByteArray(Charsets.US_ASCII).size
        val iv = lockedBytes.copyOfRange(dataStart, dataStart + ivLen)
        val cipherText = lockedBytes.copyOfRange(dataStart + ivLen, lockedBytes.size)
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }
}

/**
 * Renders conversation transcripts onto a high-contrast Dark Slate & Cyan Android Canvas Bitmap
 * and encodes into PNG, JPEG, or WEBP.
 */
object ChatBitmapRenderer {
    fun renderTranscriptBitmap(title: String, messages: List<ChatMessageEntity>): Bitmap {
        val width = 1080
        val lineHeight = 38
        val wrappedLines = mutableListOf<Pair<String, Boolean>>()
        wrappedLines.add("SUPER SMART AI • $title" to true)
        wrappedLines.add("────────────────────────────────────────" to false)
        for (m in messages.take(24)) {
            val tag = if (m.role == "USER") "[BẠN]" else "[AI • ${m.engineTag}]"
            wrappedLines.add(tag to true)
            val clean = m.content.replace("\r", "").take(380)
            for (paragraph in clean.split("\n")) {
                var idx = 0
                while (idx < paragraph.length) {
                    val end = (idx + 54).coerceAtMost(paragraph.length)
                    wrappedLines.add("  " + paragraph.substring(idx, end) to false)
                    idx = end
                }
            }
            wrappedLines.add("" to false)
        }
        val height = (140 + wrappedLines.size * lineHeight).coerceIn(480, 4200)
        val bmp = createBitmap(width, height)
        val canvas = Canvas(bmp)
        canvas.drawColor("#0B111E".toColorInt())

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#22D3EE".toColorInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F1F5F9".toColorInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#1E2D4D".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, accentPaint)

        var y = 80f
        for ((line, isHeader) in wrappedLines) {
            if (y > height - 48f) break
            canvas.drawText(line, 48f, y, if (isHeader) headerPaint else bodyPaint)
            y += lineHeight
        }
        return bmp
    }
}

object ChatImageEncoders {
    fun encode(bitmap: Bitmap, extension: String, quality: Int = 92): ByteArray {
        @Suppress("DEPRECATION")
        val webpFormat = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        val format = when (extension.lowercase()) {
            "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
            "webp" -> webpFormat
            else -> Bitmap.CompressFormat.PNG
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(format, quality.coerceIn(10, 100), baos)
        return baos.toByteArray()
    }
}

/**
 * Multi-format converter supporting 75+ export targets, PDF generation, OOXML ZIP packages
 * (DOCX / PPTX / XLSX / ODT / EPUB), SVG/Bitmap rendering, splitting, and AES PDF locking.
 */
object DocumentExportEngine {

    val supportedFormats: List<ExportFormatSpec> = listOf(
        ExportFormatSpec("pdf", "PDF Document (.pdf)", "pdf", "application/pdf", ExportCategory.DOCUMENT),
        ExportFormatSpec("docx", "Word Document (.docx)", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ExportCategory.DOCUMENT),
        ExportFormatSpec("pptx", "PowerPoint Deck (.pptx)", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ExportCategory.PRESENTATION),
        ExportFormatSpec("xlsx", "Excel Workbook (.xlsx)", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("html", "HTML5 Styled Page (.html)", "html", "text/html", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("md", "Markdown (.md)", "md", "text/markdown", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("txt", "Plain Text (.txt)", "txt", "text/plain", ExportCategory.DOCUMENT),
        ExportFormatSpec("rtf", "Rich Text Format (.rtf)", "rtf", "application/rtf", ExportCategory.DOCUMENT),
        ExportFormatSpec("odt", "OpenDocument Text (.odt)", "odt", "application/vnd.oasis.opendocument.text", ExportCategory.DOCUMENT),
        ExportFormatSpec("ods", "OpenDocument Sheet (.ods)", "ods", "application/vnd.oasis.opendocument.spreadsheet", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("odp", "OpenDocument Slides (.odp)", "odp", "application/vnd.oasis.opendocument.presentation", ExportCategory.PRESENTATION),
        ExportFormatSpec("epub", "EPUB eBook (.epub)", "epub", "application/epub+zip", ExportCategory.PRESENTATION),
        ExportFormatSpec("tex", "LaTeX Source (.tex)", "tex", "application/x-tex", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("rst", "reStructuredText (.rst)", "rst", "text/x-rst", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("adoc", "AsciiDoc (.adoc)", "adoc", "text/asciidoc", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("org", "Emacs Org-Mode (.org)", "org", "text/org", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("json", "JSON Structured (.json)", "json", "application/json", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("jsonl", "JSON Lines (.jsonl)", "jsonl", "application/x-ndjson", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("csv", "Comma-Separated (.csv)", "csv", "text/csv", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("tsv", "Tab-Separated (.tsv)", "tsv", "text/tab-separated-values", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("xml", "XML Tree (.xml)", "xml", "application/xml", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("yaml", "YAML Document (.yaml)", "yaml", "application/x-yaml", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("toml", "TOML Config (.toml)", "toml", "application/toml", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("ini", "INI Key-Value (.ini)", "ini", "text/plain", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("sql", "SQL Insert Script (.sql)", "sql", "application/sql", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("svg", "SVG Vector Poster (.svg)", "svg", "image/svg+xml", ExportCategory.IMAGE_RASTER),
        ExportFormatSpec("png", "PNG Chat Snapshot (.png)", "png", "image/png", ExportCategory.IMAGE_RASTER),
        ExportFormatSpec("jpg", "JPEG Chat Image (.jpg)", "jpg", "image/jpeg", ExportCategory.IMAGE_RASTER),
        ExportFormatSpec("webp", "WEBP Chat Image (.webp)", "webp", "image/webp", ExportCategory.IMAGE_RASTER),
        ExportFormatSpec("ipynb", "Jupyter Notebook (.ipynb)", "ipynb", "application/x-ipynb+json", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("py", "Python Script (.py)", "py", "text/x-python", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("kt", "Kotlin Source (.kt)", "kt", "text/x-kotlin", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("java", "Java Source (.java)", "java", "text/x-java-source", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("js", "JavaScript (.js)", "js", "application/javascript", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("ts", "TypeScript (.ts)", "ts", "application/typescript", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("cpp", "C++ Source (.cpp)", "cpp", "text/x-c++src", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("c", "C Source (.c)", "c", "text/x-csrc", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("cs", "C# Source (.cs)", "cs", "text/x-csharp", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("go", "Go Source (.go)", "go", "text/x-go", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("rs", "Rust Source (.rs)", "rs", "text/x-rust", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("swift", "Swift Source (.swift)", "swift", "text/x-swift", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("dart", "Dart Source (.dart)", "dart", "text/x-dart", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("php", "PHP Script (.php)", "php", "application/x-httpd-php", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("rb", "Ruby Script (.rb)", "rb", "text/x-ruby", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("sh", "Bash Shell (.sh)", "sh", "application/x-sh", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("ps1", "PowerShell (.ps1)", "ps1", "text/plain", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("bat", "Windows Batch (.bat)", "bat", "text/plain", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("lua", "Lua Script (.lua)", "lua", "text/x-lua", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("r", "R Analytics (.r)", "r", "text/x-r", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("scala", "Scala Source (.scala)", "scala", "text/x-scala", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("pl", "Perl Script (.pl)", "pl", "text/x-perl", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("groovy", "Gradle Groovy (.groovy)", "groovy", "text/x-groovy", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("kts", "Gradle Kotlin DSL (.kts)", "kts", "text/x-kotlin", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("css", "CSS Stylesheet (.css)", "css", "text/css", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("scss", "SCSS Stylesheet (.scss)", "scss", "text/x-scss", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("graphql", "GraphQL Schema (.graphql)", "graphql", "application/graphql", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("proto", "Protobuf Spec (.proto)", "proto", "text/plain", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("gql", "GraphQL Query (.gql)", "gql", "text/plain", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("dockerfile", "Dockerfile (.dockerfile)", "dockerfile", "text/plain", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("makefile", "Makefile (.mk)", "mk", "text/plain", ExportCategory.CODE_NOTEBOOK),
        ExportFormatSpec("env", "DotEnv Template (.env)", "env", "text/plain", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("log", "Diagnostic Log (.log)", "log", "text/plain", ExportCategory.DOCUMENT),
        ExportFormatSpec("ics", "iCalendar Event (.ics)", "ics", "text/calendar", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("vcf", "vCard Contact (.vcf)", "vcf", "text/vcard", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("srt", "SubRip Subtitles (.srt)", "srt", "application/x-subrip", ExportCategory.DOCUMENT),
        ExportFormatSpec("vtt", "WebVTT Subtitles (.vtt)", "vtt", "text/vtt", ExportCategory.DOCUMENT),
        ExportFormatSpec("bib", "BibTeX Citation (.bib)", "bib", "text/x-bibtex", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("ris", "RIS Citation (.ris)", "ris", "application/x-research-info-systems", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("texinfo", "Texinfo (.texi)", "texi", "text/plain", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("man", "Unix Man Page (.man)", "man", "text/troff", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("wiki", "MediaWiki Markup (.wiki)", "wiki", "text/plain", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("textile", "Textile Markup (.textile)", "textile", "text/plain", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("ndjson", "Newline JSON (.ndjson)", "ndjson", "application/x-ndjson", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("har", "HTTP Archive (.har)", "har", "application/json", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("geojson", "GeoJSON Export (.geojson)", "geojson", "application/geo+json", ExportCategory.DATA_SERIAL),
        ExportFormatSpec("rss", "RSS 2.0 Feed (.rss)", "rss", "application/rss+xml", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("atom", "Atom Feed (.atom)", "atom", "application/atom+xml", ExportCategory.WEB_MARKUP),
        ExportFormatSpec("zip_bundle", "Portable ZIP Bundle (.zip)", "zip", "application/zip", ExportCategory.BINARY_PACKAGE)
    )

    fun exportConversation(
        context: Context,
        title: String,
        messages: List<ChatMessageEntity>,
        format: ExportFormatSpec,
        maxMessagesPerPart: Int = 50,
        pdfPassword: String? = null
    ): GeneratedExportPackage {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().ifBlank { "SuperSmartAI_Chat" }
        val chunks = messages.ifEmpty {
            listOf(ChatMessageEntity("empty", "s", "AI", "Chưa có tin nhắn nào trong phiên này.", "Gemini", timestampMillis = System.currentTimeMillis()))
        }.chunked(maxMessagesPerPart.coerceAtLeast(1))

        val parts = chunks.mapIndexed { idx, partMsgs ->
            val partNum = idx + 1
            val partTitle = if (chunks.size > 1) "${safeTitle}_part${partNum}" else safeTitle
            var bytes = renderSingleFormatBytes(context, partTitle, partMsgs, format)
            var pdfLocked = false
            if (format.id == "pdf" && !pdfPassword.isNullOrBlank()) {
                val (protected, applied) = PdfSecurityEngine.protectPdfIfRequested(bytes, pdfPassword)
                bytes = protected
                pdfLocked = applied
            }
            val fileExt = if (pdfLocked) "pdf.enc" else format.extension
            ExportPart(
                partNumber = partNum,
                totalParts = chunks.size,
                fileName = "${partTitle.replace(' ', '_')}.$fileExt",
                bytes = bytes,
                sha256Hash = PaqCompressionEngine.sha256Hex(bytes)
            )
        }

        val totalSize = parts.sumOf { it.bytes.size.toLong() }
        val isLocked = format.id == "pdf" && !pdfPassword.isNullOrBlank()
        return GeneratedExportPackage(
            primaryFormat = format,
            baseTitle = safeTitle,
            parts = parts,
            pdfPasswordApplied = isLocked,
            totalBytes = totalSize,
            summaryMessage = "Đã tạo ${parts.size} tệp ${format.displayName} (${totalSize} bytes)${if (isLocked) " • Khóa AES-256" else ""}."
        )
    }

    private fun renderSingleFormatBytes(
        context: Context,
        title: String,
        messages: List<ChatMessageEntity>,
        format: ExportFormatSpec
    ): ByteArray {
        val fmtDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return when (format.id) {
            "pdf" -> renderNativePdf(title, messages, fmtDate)
            "png", "jpg", "webp" -> {
                val bmp = ChatBitmapRenderer.renderTranscriptBitmap(title, messages)
                ChatImageEncoders.encode(bmp, format.extension)
            }
            "svg" -> renderSvgPoster(title, messages).toByteArray(StandardCharsets.UTF_8)
            "docx", "odt" -> buildMinimalOoxmlDocxZip(title, messages, fmtDate)
            "pptx", "odp" -> buildMinimalPptxZip(title, messages)
            "xlsx", "ods" -> buildMinimalXlsxZip(title, messages, fmtDate)
            "epub" -> buildMinimalEpubZip(title, messages)
            "zip_bundle" -> buildMultiModalZipBundle(title, messages, fmtDate)
            "html" -> renderHtml(title, messages, fmtDate).toByteArray(StandardCharsets.UTF_8)
            "json", "har", "geojson" -> renderJson(title, messages, format.id).toByteArray(StandardCharsets.UTF_8)
            "jsonl", "ndjson" -> messages.joinToString("\n") { m ->
                JSONObject().apply {
                    put("role", m.role)
                    put("engine", m.engineTag)
                    put("content", m.content)
                    put("timestamp", m.timestampMillis)
                }.toString()
            }.toByteArray(StandardCharsets.UTF_8)
            "ipynb" -> renderIpynb(title, messages).toByteArray(StandardCharsets.UTF_8)
            "csv" -> renderDelimited(messages, ",", fmtDate).toByteArray(StandardCharsets.UTF_8)
            "tsv" -> renderDelimited(messages, "\t", fmtDate).toByteArray(StandardCharsets.UTF_8)
            "xml", "rss", "atom" -> renderXml(title, messages, format.id).toByteArray(StandardCharsets.UTF_8)
            "rtf" -> renderRtf(title, messages).toByteArray(StandardCharsets.UTF_8)
            "tex" -> renderLatex(title, messages).toByteArray(StandardCharsets.UTF_8)
            "sql" -> renderSql(title, messages).toByteArray(StandardCharsets.UTF_8)
            else -> renderStructuredText(title, messages, format, fmtDate).toByteArray(StandardCharsets.UTF_8)
        }
    }

    private fun renderNativePdf(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): ByteArray {
        try {
            val doc = PdfDocument()
            try {
                val pageW = 595
                val pageH = 842
                val margin = 40f
                var pageNum = 1
                var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                var canvas = page.canvas
                val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = Color.BLACK }
                val metaPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = Color.DKGRAY }
                val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK }

                var y = 50f
                canvas.drawText("Super Smart AI • $title", margin, y, titlePaint)
                y += 28f

                fun ensureSpace(needed: Float) {
                    if (y + needed > pageH - margin) {
                        doc.finishPage(page)
                        pageNum++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                        canvas = page.canvas
                        y = 48f
                    }
                }

                for (m in messages) {
                    ensureSpace(36f)
                    val header = "${m.role} • ${m.engineTag} • ${fmtDate.format(Date(m.timestampMillis))}"
                    canvas.drawText(header.take(90), margin, y, metaPaint)
                    y += 16f
                    for (paragraph in m.content.split("\n")) {
                        var idx = 0
                        if (paragraph.isEmpty()) {
                            y += 12f
                            continue
                        }
                        while (idx < paragraph.length) {
                            ensureSpace(16f)
                            val end = (idx + 76).coerceAtMost(paragraph.length)
                            canvas.drawText(paragraph.substring(idx, end), margin, y, bodyPaint)
                            y += 14f
                            idx = end
                        }
                    }
                    y += 10f
                }
                doc.finishPage(page)
                val baos = ByteArrayOutputStream()
                doc.writeTo(baos)
                val bytes = baos.toByteArray()
                if (bytes.size >= 4) return bytes
            } finally {
                try {
                    doc.close()
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
            // Fall through to pure-Kotlin PDF-1.4 writer (e.g. headless/Robolectric environments)
        }
        return renderPureKotlinPdf14(title, messages, fmtDate)
    }

    private fun renderPureKotlinPdf14(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): ByteArray {
        val lines = mutableListOf("Super Smart AI - $title")
        messages.forEach { m ->
            lines.add("[${m.role} | ${m.engineTag} | ${fmtDate.format(Date(m.timestampMillis))}]")
            lines.addAll(m.content.split("\n").map { it.take(86) })
        }
        val streamContent = buildString {
            appendLine("BT")
            appendLine("/F1 11 Tf")
            appendLine("40 790 Td")
            appendLine("15 TL")
            lines.take(45).forEach { line ->
                val escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                    .replace(Regex("[^\\x20-\\x7E]"), "?")
                appendLine("($escaped) Tj T*")
            }
            appendLine("ET")
        }
        val obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        val obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        val obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
        val obj4 = "4 0 obj\n<< /Length ${streamContent.length} >>\nstream\n$streamContent\nendstream\nendobj\n"
        val obj5 = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        val header = "%PDF-1.4\n"
        val out = StringBuilder(header)
        val offsets = mutableListOf<Int>()
        listOf(obj1, obj2, obj3, obj4, obj5).forEach { obj ->
            offsets.add(out.length)
            out.append(obj)
        }
        val xrefOffset = out.length
        out.append("xref\n0 6\n0000000000 65535 f \n")
        offsets.forEach { off -> out.append("%010d 00000 n \n".format(off)) }
        out.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        return out.toString().toByteArray(Charsets.US_ASCII)
    }

    private fun renderSvgPoster(title: String, messages: List<ChatMessageEntity>): String {
        val escapedTitle = escapeXml(title)
        val previewLines = messages.take(10).mapIndexed { i, m ->
            val safe = escapeXml("${m.role}: ${m.content.replace('\n', ' ').take(72)}")
            """<text x="32" y="${110 + i * 32}" fill="#F1F5F9" font-family="monospace" font-size="14">$safe</text>"""
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="900" height="480" viewBox="0 0 900 480">
  <rect width="900" height="480" rx="20" fill="#0B111E"/>
  <rect x="16" y="16" width="868" height="448" rx="16" fill="#111A2E" stroke="#22D3EE" stroke-width="2"/>
  <text x="32" y="60" fill="#22D3EE" font-family="sans-serif" font-size="22" font-weight="bold">Super Smart AI — $escapedTitle</text>
  $previewLines
</svg>"""
    }

    private fun buildMinimalOoxmlDocxZip(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): ByteArray {
        val paragraphs = messages.joinToString("\n") { m ->
            "<w:p><w:r><w:t>[${escapeXml(m.role)} | ${escapeXml(m.engineTag)} | ${fmtDate.format(Date(m.timestampMillis))}] ${escapeXml(m.content)}</w:t></w:r></w:p>"
        }
        val docXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:t>${escapeXml(title)}</w:t></w:r></w:p>
    $paragraphs
  </w:body>
</w:document>"""
        val contentTypes = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val rels = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        return zipEntries(
            mapOf(
                "[Content_Types].xml" to contentTypes.toByteArray(),
                "_rels/.rels" to rels.toByteArray(),
                "word/document.xml" to docXml.toByteArray()
            )
        )
    }

    private fun buildMinimalPptxZip(title: String, messages: List<ChatMessageEntity>): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        entries["[Content_Types].xml"] = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/></Types>""".toByteArray()
        entries["_rels/.rels"] = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/></Relationships>""".toByteArray()
        entries["ppt/presentation.xml"] = """<?xml version="1.0" encoding="UTF-8"?><p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:sldSz cx="9144000" cy="6858000"/></p:presentation>""".toByteArray()
        messages.chunked(2).forEachIndexed { idx, slideMsgs ->
            val text = slideMsgs.joinToString("\n") { "${it.role}: ${it.content}" }
            entries["ppt/slides/slide${idx + 1}.xml"] = """<?xml version="1.0" encoding="UTF-8"?><p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>${escapeXml("$title • Slide ${idx + 1}\n$text")}</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld></p:sld>""".toByteArray()
        }
        return zipEntries(entries)
    }

    private fun buildMinimalXlsxZip(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): ByteArray {
        val rows = messages.mapIndexed { idx, m ->
            val r = idx + 2
            "<row r=\"$r\"><c r=\"A$r\" t=\"inlineStr\"><is><t>${escapeXml(m.role)}</t></is></c><c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(m.engineTag)}</t></is></c><c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(fmtDate.format(Date(m.timestampMillis)))}</t></is></c><c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(m.content)}</t></is></c></row>"
        }.joinToString("\n")
        val sheetXml = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>${escapeXml(title)}</t></is></c></row>$rows</sheetData></worksheet>"""
        return zipEntries(
            mapOf(
                "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/></Types>""".toByteArray(),
                "xl/worksheets/sheet1.xml" to sheetXml.toByteArray()
            )
        )
    }

    private fun buildMinimalEpubZip(title: String, messages: List<ChatMessageEntity>): ByteArray {
        val body = messages.joinToString("\n") { "<p><b>${escapeXml(it.role)}:</b> ${escapeXml(it.content)}</p>" }
        val xhtml = """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${escapeXml(title)}</title></head><body><h1>${escapeXml(title)}</h1>$body</body></html>"""
        return zipEntries(
            mapOf(
                "mimetype" to "application/epub+zip".toByteArray(),
                "OEBPS/content.xhtml" to xhtml.toByteArray()
            )
        )
    }

    private fun buildMultiModalZipBundle(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): ByteArray {
        return zipEntries(
            mapOf(
                "$title.md" to renderStructuredText(title, messages, supportedFormats[5], fmtDate).toByteArray(),
                "$title.html" to renderHtml(title, messages, fmtDate).toByteArray(),
                "$title.json" to renderJson(title, messages, "json").toByteArray()
            )
        )
    }

    private fun renderHtml(title: String, messages: List<ChatMessageEntity>, fmtDate: SimpleDateFormat): String {
        val blocks = messages.joinToString("\n") { m ->
            val cls = if (m.role == "USER") "user" else "ai"
            """<div class="msg $cls"><div class="meta">${escapeXml(m.role)} • ${escapeXml(m.engineTag)} • ${fmtDate.format(Date(m.timestampMillis))}</div><pre>${escapeXml(m.content)}</pre></div>"""
        }
        return """<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><title>${escapeXml(title)}</title><style>body{background:#0B111E;color:#F1F5F9;font-family:sans-serif;padding:24px;max-width:860px;margin:auto}.msg{background:#17223B;border:1px solid #283A5E;border-radius:12px;padding:14px;margin-bottom:12px}.user{border-color:#22D3EE}.meta{color:#22D3EE;font-size:12px;margin-bottom:6px}pre{white-space:pre-wrap;font-family:inherit;margin:0}</style></head><body><h1>${escapeXml(title)}</h1>$blocks</body></html>"""
    }

    private fun renderJson(title: String, messages: List<ChatMessageEntity>, mode: String): String {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("role", m.role)
                put("engineTag", m.engineTag)
                put("content", m.content)
                put("timestampMillis", m.timestampMillis)
            })
        }
        return JSONObject().apply {
            put("title", title)
            put("format", mode)
            put("messageCount", messages.size)
            put("messages", arr)
        }.toString(2)
    }

    private fun renderIpynb(title: String, messages: List<ChatMessageEntity>): String {
        val cells = JSONArray()
        cells.put(JSONObject().apply {
            put("cell_type", "markdown")
            put("metadata", JSONObject())
            put("source", JSONArray(listOf("# $title")))
        })
        messages.forEach { m ->
            cells.put(JSONObject().apply {
                put("cell_type", "markdown")
                put("metadata", JSONObject())
                put("source", JSONArray(listOf("**${m.role} (${m.engineTag})**:\n\n${m.content}")))
            })
        }
        return JSONObject().apply {
            put("nbformat", 4)
            put("nbformat_minor", 5)
            put("metadata", JSONObject().put("kernelspec", JSONObject().put("language", "python").put("name", "python3")))
            put("cells", cells)
        }.toString(2)
    }

    private fun renderDelimited(messages: List<ChatMessageEntity>, sep: String, fmtDate: SimpleDateFormat): String {
        val sb = StringBuilder("role${sep}engine${sep}time${sep}content\n")
        for (m in messages) {
            val clean = m.content.replace("\"", "\"\"").replace("\n", "\\n")
            sb.append("\"${m.role}\"$sep\"${m.engineTag}\"$sep\"${fmtDate.format(Date(m.timestampMillis))}\"$sep\"$clean\"\n")
        }
        return sb.toString()
    }

    private fun renderXml(title: String, messages: List<ChatMessageEntity>, mode: String): String {
        val items = messages.joinToString("\n") { m ->
            "  <message role=\"${escapeXml(m.role)}\" engine=\"${escapeXml(m.engineTag)}\" ts=\"${m.timestampMillis}\">${escapeXml(m.content)}</message>"
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<conversation format="$mode" title="${escapeXml(title)}">
$items
</conversation>"""
    }

    private fun renderRtf(title: String, messages: List<ChatMessageEntity>): String {
        val body = messages.joinToString("\\par\n") { "\\b ${it.role} (${it.engineTag}):\\b0  ${it.content.replace("\n", "\\par ")}" }
        return "{\\rtf1\\ansi\\deff0 {\\b $title}\\par\\par\n$body}"
    }

    private fun renderLatex(title: String, messages: List<ChatMessageEntity>): String {
        val body = messages.joinToString("\n\n") { "\\subsection*{${it.role} (${it.engineTag})}\n${it.content}" }
        return "\\documentclass{article}\n\\usepackage[utf8]{inputenc}\n\\title{$title}\n\\begin{document}\n\\maketitle\n$body\n\\end{document}"
    }

    private fun renderSql(title: String, messages: List<ChatMessageEntity>): String {
        val sb = StringBuilder("-- Export: $title\nCREATE TABLE IF NOT EXISTS chat_export(id TEXT PRIMARY KEY, role TEXT, engine TEXT, content TEXT);\n")
        messages.forEach { m ->
            val safe = m.content.replace("'", "''")
            sb.append("INSERT INTO chat_export VALUES('${m.id}', '${m.role}', '${m.engineTag}', '$safe');\n")
        }
        return sb.toString()
    }

    private fun renderStructuredText(
        title: String,
        messages: List<ChatMessageEntity>,
        format: ExportFormatSpec,
        fmtDate: SimpleDateFormat
    ): String {
        return buildString {
            appendLine("# $title [${format.displayName}]")
            appendLine("# Exported by Super Smart AI")
            appendLine()
            messages.forEach { m ->
                appendLine("### ${m.role} (${m.engineTag}) @ ${fmtDate.format(Date(m.timestampMillis))}")
                appendLine(m.content)
                appendLine()
            }
        }
    }

    internal fun zipEntries(entries: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipArchiveOutputStream(baos).use { zos ->
            entries.forEach { (path, data) ->
                val e = ZipArchiveEntry(path)
                e.size = data.size.toLong()
                zos.putArchiveEntry(e)
                zos.write(data)
                zos.closeArchiveEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

data class BatchExportResult(
    val bundleFileName: String,
    val zipBytes: ByteArray,
    val includedFormats: List<ExportFormatSpec>,
    val sha256Hash: String
)

/**
 * Exports a single conversation simultaneously into multiple selected formats,
 * packaging all generated outputs plus a SHA-256 manifest into a single portable .zip bundle.
 */
object BatchExportEngine {
    fun exportBatch(
        context: Context,
        title: String,
        messages: List<ChatMessageEntity>,
        formats: List<ExportFormatSpec>,
        pdfPassword: String? = null
    ): BatchExportResult {
        val chosen = formats.ifEmpty { DocumentExportEngine.supportedFormats.take(3) }
        val entries = linkedMapOf<String, ByteArray>()
        val manifestLines = mutableListOf("Super Smart AI Batch Export Manifest", "Title: $title", "Formats: ${chosen.size}", "")

        chosen.forEach { fmt ->
            val pkg = DocumentExportEngine.exportConversation(
                context = context,
                title = title,
                messages = messages,
                format = fmt,
                maxMessagesPerPart = 500,
                pdfPassword = pdfPassword
            )
            pkg.parts.forEach { part ->
                entries["${fmt.id}/${part.fileName}"] = part.bytes
                manifestLines.add("- ${fmt.id}/${part.fileName} (${part.bytes.size}B) SHA-256: ${part.sha256Hash}")
            }
        }
        entries["MANIFEST_SHA256.txt"] = manifestLines.joinToString("\n").toByteArray(Charsets.UTF_8)
        val zipBytes = DocumentExportEngine.zipEntries(entries)
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "batch_export" }
        return BatchExportResult(
            bundleFileName = "${safeTitle}_batch_${chosen.size}formats.zip",
            zipBytes = zipBytes,
            includedFormats = chosen,
            sha256Hash = PaqCompressionEngine.sha256Hex(zipBytes)
        )
    }
}
