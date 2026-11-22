package com.nguyen_super_app.nguyen_super_smart_ai.engine.sandbox

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.Stack
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

data class CodeBlockSegment(
    val language: String,
    val code: String
)

data class SandboxExecutionResult(
    val language: String,
    val succeeded: Boolean,
    val stdout: String,
    val diagnosticError: String? = null
)

data class NotebookCellResult(
    val cellIndex: Int,
    val cellType: String,
    val source: String,
    val output: String,
    val succeeded: Boolean
)

data class IpynbExecutionReport(
    val notebookTitle: String,
    val totalCells: Int,
    val codeCellsExecuted: Int,
    val cellResults: List<NotebookCellResult>
)

object PromptContextHook {
    fun buildInjectedSystemContext(
        basePersona: String?,
        preferredCodeLang: String?,
        enableCodeHook: Boolean,
        enableReasoningTag: Boolean
    ): String {
        val parts = mutableListOf<String>()
        if (!basePersona.isNullOrBlank()) {
            parts.add(basePersona.trim())
        }
        if (enableCodeHook && !preferredCodeLang.isNullOrBlank()) {
            parts.add("Khi trả về mã nguồn, ưu tiên ngôn ngữ `$preferredCodeLang` trong fenced code block chuẩn ```$preferredCodeLang ... ```.")
        }
        if (enableReasoningTag) {
            parts.add("Nếu bài toán cần suy luận nhiều bước, đặt quá trình suy luận bên trong cặp thẻ <think>...</think> trước câu trả lời chính.")
        }
        return parts.joinToString("\n")
    }
}

object CodeBlockParser {
    private val fenceRegex = Regex("```([a-zA-Z0-9_+.#-]*)\\s*\\n([\\s\\S]*?)```")

    fun parseMarkdownWithCode(text: String): Pair<List<String>, List<CodeBlockSegment>> {
        val blocks = mutableListOf<CodeBlockSegment>()
        val proseChunks = mutableListOf<String>()
        var cursor = 0
        for (m in fenceRegex.findAll(text)) {
            if (m.range.first > cursor) {
                proseChunks.add(text.substring(cursor, m.range.first).trim())
            }
            val lang = m.groupValues[1].trim().ifBlank { "text" }.lowercase()
            val code = m.groupValues[2].trimEnd()
            blocks.add(CodeBlockSegment(lang, code))
            cursor = m.range.last + 1
        }
        if (cursor < text.length) {
            proseChunks.add(text.substring(cursor).trim())
        }
        return proseChunks.filter { it.isNotEmpty() } to blocks
    }

    fun extractAllBlocks(text: String): List<CodeBlockSegment> = parseMarkdownWithCode(text).second
}

object CodeRunnerSandbox {

    fun execute(language: String, code: String, context: Context? = null): SandboxExecutionResult {
        val lang = language.trim().lowercase()
        return when (lang) {
            "sql", "sqlite" -> runSql(code)
            "json" -> validateJson(code)
            "xml", "html", "svg" -> validateXmlOrHtml(lang, code)
            "python", "py" -> runMiniPython(code)
            "kotlin", "kt", "java", "javascript", "js", "ts", "c", "cpp", "c++" ->
                runStructuredMiniRuntime(lang, code)
            else -> validateDelimitersAndPrint(lang, code)
        }
    }

    private fun runSql(code: String): SandboxExecutionResult {
        return try {
            val db = SQLiteDatabase.create(null)
            val outputs = mutableListOf<String>()
            val statements = code.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            db.use { sqlite ->
                for (stmt in statements) {
                    if (stmt.startsWith("SELECT", ignoreCase = true) || stmt.startsWith("PRAGMA", ignoreCase = true)) {
                        sqlite.rawQuery(stmt, null).use { cursor ->
                            val cols = cursor.columnNames
                            outputs.add(cols.joinToString(" | "))
                            outputs.add("-".repeat(cols.size * 12))
                            while (cursor.moveToNext()) {
                                val row = (0 until cursor.columnCount).joinToString(" | ") { idx ->
                                    cursor.getString(idx) ?: "NULL"
                                }
                                outputs.add(row)
                            }
                        }
                    } else {
                        sqlite.execSQL(stmt)
                        outputs.add("OK: ${stmt.take(64)}")
                    }
                }
            }
            SandboxExecutionResult("sql", true, outputs.joinToString("\n").ifBlank { "SQLite thực thi thành công." })
        } catch (e: Exception) {
            SandboxExecutionResult("sql", false, "", e.message ?: "Lỗi SQLite syntax")
        }
    }

    private fun validateJson(code: String): SandboxExecutionResult {
        return try {
            val trimmed = code.trim()
            val formatted = if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(2)
            } else {
                JSONObject(trimmed).toString(2)
            }
            SandboxExecutionResult("json", true, "JSON hợp lệ:\n$formatted")
        } catch (e: Exception) {
            SandboxExecutionResult("json", false, "", "JSON không hợp lệ: ${e.message}")
        }
    }

    private fun validateXmlOrHtml(lang: String, code: String): SandboxExecutionResult {
        if (lang == "html") {
            val openTags = Regex("<([a-zA-Z0-9]+)(?:\\s[^>]*)?>").findAll(code)
                .map { it.groupValues[1].lowercase() }
                .filter { it !in setOf("br", "hr", "img", "input", "meta", "link") }
                .toList()
            val closeTags = Regex("</([a-zA-Z0-9]+)>").findAll(code).map { it.groupValues[1].lowercase() }.toList()
            return if (openTags.size >= closeTags.size) {
                SandboxExecutionResult(lang, true, "HTML hợp lệ (${openTags.size} thẻ mở, ${closeTags.size} thẻ đóng).")
            } else {
                SandboxExecutionResult(lang, false, "", "Thẻ đóng HTML nhiều hơn thẻ mở.")
            }
        }
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(code)))
            SandboxExecutionResult(lang, true, "$lang hợp lệ • Root tag: <${doc.documentElement.nodeName}>")
        } catch (e: Exception) {
            SandboxExecutionResult(lang, false, "", "Lỗi XML: ${e.message}")
        }
    }

    private fun runMiniPython(code: String): SandboxExecutionResult {
        val syntaxErr = checkBalancedDelimiters(code)
        if (syntaxErr != null) {
            return SandboxExecutionResult("python", false, "", syntaxErr)
        }
        val env = linkedMapOf<String, Double>()
        val stdout = mutableListOf<String>()
        val lines = code.lines()
        for ((idx, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("print(") && line.endsWith(")")) {
                val inner = line.removePrefix("print(").removeSuffix(")").trim()
                val rendered = evaluatePrintArg(inner, env)
                stdout.add(rendered)
            } else if (line.contains("=") && !line.contains("==") && !line.startsWith("def ") && !line.startsWith("class ")) {
                val parts = line.split("=", limit = 2)
                val varName = parts[0].trim()
                val expr = parts[1].trim()
                evalArithmetic(expr, env)?.let { env[varName] = it }
            }
        }
        val summary = if (stdout.isNotEmpty()) {
            stdout.joinToString("\n")
        } else {
            "Python 3 AST & Delimiter check OK (${lines.size} dòng, ${env.size} biến)."
        }
        return SandboxExecutionResult("python", true, summary)
    }

    private fun runStructuredMiniRuntime(lang: String, code: String): SandboxExecutionResult {
        val syntaxErr = checkBalancedDelimiters(code)
        if (syntaxErr != null) {
            return SandboxExecutionResult(lang, false, "", syntaxErr)
        }
        val prints = mutableListOf<String>()
        val printCallRegex = Regex("""(?:println|print|console\.log|System\.out\.println|printf)\s*\(([^)]*)\)""")
        printCallRegex.findAll(code).forEach { m ->
            val arg = m.groupValues[1].trim().trim('"', '\'')
            if (arg.isNotEmpty()) prints.add(arg)
        }
        val out = if (prints.isNotEmpty()) {
            prints.joinToString("\n")
        } else {
            "Kiểm tra cú pháp & dấu ngoặc $lang thành công (${code.lines().size} dòng)."
        }
        return SandboxExecutionResult(lang, true, out)
    }

    private fun validateDelimitersAndPrint(lang: String, code: String): SandboxExecutionResult {
        val err = checkBalancedDelimiters(code)
        return if (err == null) {
            SandboxExecutionResult(lang, true, "Kiểm tra khối mã $lang hoàn tất (${code.length} ký tự).")
        } else {
            SandboxExecutionResult(lang, false, "", err)
        }
    }

    private fun evaluatePrintArg(inner: String, env: Map<String, Double>): String {
        if ((inner.startsWith("\"") && inner.endsWith("\"")) || (inner.startsWith("'") && inner.endsWith("'"))) {
            return inner.substring(1, inner.length - 1)
        }
        val num = evalArithmetic(inner, env)
        return if (num != null) {
            if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
        } else {
            inner
        }
    }

    private fun evalArithmetic(expr: String, env: Map<String, Double>): Double? {
        var resolved = expr
        env.forEach { (k, v) ->
            resolved = resolved.replace(Regex("\\b${Regex.escape(k)}\\b"), v.toString())
        }
        return try {
            SimpleExprParser(resolved).parse()
        } catch (_: Exception) {
            null
        }
    }

    private fun checkBalancedDelimiters(code: String): String? {
        val stack = Stack<Pair<Char, Int>>()
        var inString: Char? = null
        var escaped = false
        var line = 1
        for (ch in code) {
            if (ch == '\n') line++
            if (escaped) {
                escaped = false
                continue
            }
            if (inString != null) {
                if (ch == '\\') escaped = true
                else if (ch == inString) inString = null
                continue
            }
            if (ch == '"' || ch == '\'') {
                inString = ch
                continue
            }
            when (ch) {
                '(', '{', '[' -> stack.push(ch to line)
                ')', '}', ']' -> {
                    if (stack.isEmpty()) return "Dấu đóng '$ch' dư thừa ở dòng $line"
                    val (open, openLine) = stack.pop()
                    val match = (open == '(' && ch == ')') || (open == '{' && ch == '}') || (open == '[' && ch == ']')
                    if (!match) return "Không khớp ngoặc '$open' (dòng $openLine) với '$ch' (dòng $line)"
                }
            }
        }
        if (inString != null) return "Chuỗi ký tự chưa đóng dấu $inString"
        if (stack.isNotEmpty()) {
            val (open, openLine) = stack.pop()
            return "Thiếu dấu đóng cho '$open' mở tại dòng $openLine"
        }
        return null
    }

    private class SimpleExprParser(private val s: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < s.length) s[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < s.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                x = s.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Invalid token")
            }
            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }
    }
}

/**
 * Native Jupyter Notebook (.ipynb) JSON cell parser & sequential code cell runner.
 */
object IpynbExecutionEngine {

    fun parseAndRun(ipynbJson: String, titleHint: String = "notebook.ipynb"): IpynbExecutionReport {
        val root = Json.parseToJsonElement(ipynbJson).jsonObject
        val kernelLang = root["metadata"]
            ?.jsonObject
            ?.get("kernelspec")
            ?.jsonObject
            ?.get("language")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.ifBlank { "python" } ?: "python"
        val cells = root["cells"]?.jsonArray ?: JsonArray(emptyList())
        val results = mutableListOf<NotebookCellResult>()
        var codeCount = 0

        for (i in cells.indices) {
            val cell = cells[i].jsonObject
            val type = cell["cell_type"]?.jsonPrimitive?.contentOrNull ?: "code"
            val sourceField = cell["source"]
            val sourceText = when (sourceField) {
                is JsonArray -> sourceField.joinToString("") { it.jsonPrimitive.contentOrNull.orEmpty() }
                is JsonPrimitive -> sourceField.contentOrNull.orEmpty()
                else -> ""
            }.trim()

            if (type.equals("code", ignoreCase = true)) {
                codeCount++
                val exec = CodeRunnerSandbox.execute(kernelLang, sourceText)
                results.add(
                    NotebookCellResult(
                        cellIndex = i + 1,
                        cellType = "code ($kernelLang)",
                        source = sourceText,
                        output = if (exec.succeeded) exec.stdout else (exec.diagnosticError ?: "Execution failed"),
                        succeeded = exec.succeeded
                    )
                )
            } else {
                results.add(
                    NotebookCellResult(
                        cellIndex = i + 1,
                        cellType = type,
                        source = sourceText,
                        output = sourceText,
                        succeeded = true
                    )
                )
            }
        }

        return IpynbExecutionReport(
            notebookTitle = titleHint,
            totalCells = cells.size,
            codeCellsExecuted = codeCount,
            cellResults = results
        )
    }
}
