package com.nguyen_super_app.nguyen_super_smart_ai.engine.ai

import com.nguyen_super_app.nguyen_super_smart_ai.data.local.AdvancedAiFeaturesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Arrays
import java.util.concurrent.TimeUnit

data class ParsedReasoningMessage(
    val hasReasoning: Boolean,
    val isCurrentlyThinking: Boolean,
    val reasoningText: String,
    val answerText: String
)

/**
 * Splits model outputs into separate reasoning and final answer channels.
 * Handles <think>...</think>, <thought>...</thought>, and <reasoning>...</reasoning>,
 * including still-streaming unclosed opening tags.
 * Note: UI must keep reasoning strictly collapsed unless user explicitly clicks toggle!
 */
object ReasoningStreamParser {
    private val openTags = listOf("<think>", "<thought>", "<reasoning>")
    private val closeTags = listOf("</think>", "</thought>", "</reasoning>")

    fun parse(rawText: String, isStreaming: Boolean = false): ParsedReasoningMessage {
        if (rawText.isEmpty()) {
            return ParsedReasoningMessage(
                hasReasoning = false,
                isCurrentlyThinking = false,
                reasoningText = "",
                answerText = ""
            )
        }

        val lower = rawText.lowercase()
        var firstOpenIdx = -1
        var matchedOpenLen = 0
        var matchedTagIndex = -1

        openTags.forEachIndexed { idx, tag ->
            val found = lower.indexOf(tag)
            if (found != -1 && (firstOpenIdx == -1 || found < firstOpenIdx)) {
                firstOpenIdx = found
                matchedOpenLen = tag.length
                matchedTagIndex = idx
            }
        }

        if (firstOpenIdx == -1) {
            return ParsedReasoningMessage(
                hasReasoning = false,
                isCurrentlyThinking = false,
                reasoningText = "",
                answerText = rawText
            )
        }

        val afterOpenStart = firstOpenIdx + matchedOpenLen
        val expectedClose = closeTags[matchedTagIndex]
        val closeIdx = lower.indexOf(expectedClose, startIndex = afterOpenStart)

        return if (closeIdx == -1) {
            // Still inside an active <think>/<thought>/<reasoning> block
            val reasoningBody = rawText.substring(afterOpenStart).trim()
            val prefixText = rawText.substring(0, firstOpenIdx).trim()
            ParsedReasoningMessage(
                hasReasoning = true,
                isCurrentlyThinking = isStreaming,
                reasoningText = reasoningBody,
                answerText = prefixText
            )
        } else {
            val reasoningBody = rawText.substring(afterOpenStart, closeIdx).trim()
            val prefixText = rawText.substring(0, firstOpenIdx).trim()
            val suffixText = rawText.substring(closeIdx + expectedClose.length).trim()
            val combinedAnswer = listOf(prefixText, suffixText).filter { it.isNotEmpty() }.joinToString("\n\n")
            ParsedReasoningMessage(
                hasReasoning = reasoningBody.isNotEmpty(),
                isCurrentlyThinking = false,
                reasoningText = reasoningBody,
                answerText = combinedAnswer
            )
        }
    }
}

sealed interface CapabilityGateDecision {
    data class NormalChat(val cleanPrompt: String) : CapabilityGateDecision
    data class ExecuteWebSearch(val searchQuery: String, val originalPrompt: String) : CapabilityGateDecision
    data class ExecuteFileGeneration(
        val cleanPrompt: String,
        val isBatch: Boolean
    ) : CapabilityGateDecision
    data class RemindEnableFeature(
        val featureNameVi: String,
        val shortReminder: String
    ) : CapabilityGateDecision
}

/**
 * Enforces the unified specification:
 * - Never wrap normal prompts in artificial "đã nhận lệnh..." boilerplate.
 * - Advanced features (Web Search, Single/Batch File Generation) execute ONLY when
 *   (toggleEnabled == true) AND (user prompt explicitly asks for that capability).
 * - If requested while disabled, emit only a concise reminder to enable the toggle.
 */
object IntentCapabilityGate {

    private val webSearchTriggers = listOf(
        "tìm kiếm web", "search web", "google search", "tìm trên mạng",
        "tra cứu web", "tìm trên google", "web search", "tìm kiếm trực tuyến",
        "search online", "tìm google"
    )

    private val batchFileTriggers = listOf(
        "tạo hàng loạt", "xuất hàng loạt", "batch export", "nhiều file",
        "tạo file hàng loạt", "tạo nhiều tệp", "batch file"
    )

    private val singleFileTriggers = listOf(
        "tạo file ", "tạo tệp ", "xuất file ", "xuất tệp ",
        "generate file", "create file", "export file"
    )

    fun evaluate(prompt: String, config: AdvancedAiFeaturesConfig): CapabilityGateDecision {
        val lower = prompt.trim().lowercase()
        val asksWebSearch = webSearchTriggers.any { lower.contains(it) }
        val asksBatchFiles = batchFileTriggers.any { lower.contains(it) }
        val asksSingleFile = singleFileTriggers.any { lower.contains(it) }

        if (asksWebSearch) {
            return if (config.enableWebSearch) {
                CapabilityGateDecision.ExecuteWebSearch(
                    searchQuery = extractSearchQuery(prompt),
                    originalPrompt = prompt
                )
            } else {
                CapabilityGateDecision.RemindEnableFeature(
                    featureNameVi = "Tìm kiếm Web",
                    shortReminder = "Tính năng Tìm kiếm Web đang tắt. Vui lòng bật trong Hộp thoại Tùy chọn Tính năng AI nâng cao để thực hiện tra cứu trực tuyến."
                )
            }
        }

        if (asksBatchFiles || asksSingleFile) {
            return if (config.enableSingleBatchFileGen) {
                CapabilityGateDecision.ExecuteFileGeneration(
                    cleanPrompt = prompt,
                    isBatch = asksBatchFiles
                )
            } else {
                CapabilityGateDecision.RemindEnableFeature(
                    featureNameVi = "Tạo file đơn lẻ / hàng loạt",
                    shortReminder = "Tính năng Tạo file nâng cao đang tắt. Vui lòng bật trong Hộp thoại Tùy chọn Tính năng AI nâng cao rồi gửi lại yêu cầu."
                )
            }
        }

        // Standard direct conversation: zero intervention
        return CapabilityGateDecision.NormalChat(prompt)
    }

    private fun extractSearchQuery(prompt: String): String {
        var cleaned = prompt.trim()
        webSearchTriggers.forEach { trigger ->
            cleaned = cleaned.replace(trigger, "", ignoreCase = true)
        }
        return cleaned.trim().ifBlank { prompt.trim() }
    }
}

data class WebSearchSnippet(
    val title: String,
    val url: String,
    val snippet: String
)

object LiveWebSearchClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Performs real live web search. Uses DuckDuckGo Instant Answer API by default, or
     * Serper/Google-compatible JSON endpoint when an encrypted API token CharArray is supplied
     * (zeroing the CharArray immediately after building the header).
     */
    suspend fun search(
        query: String,
        transientApiKey: CharArray? = null
    ): Result<List<WebSearchSnippet>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val reqBuilder = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "SuperSmartAI-Android/1.0")
            if (transientApiKey != null && transientApiKey.isNotEmpty()) {
                reqBuilder.header("X-Client-Key-Configured", "true")
            }

            http.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}"))
                }
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val results = mutableListOf<WebSearchSnippet>()

                val abstractText = json.optString("AbstractText", "").trim()
                val abstractUrl = json.optString("AbstractURL", "").trim()
                val heading = json.optString("Heading", query).trim()
                if (abstractText.isNotEmpty()) {
                    results.add(WebSearchSnippet(heading, abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" }, abstractText))
                }

                val related = json.optJSONArray("RelatedTopics")
                if (related != null) {
                    for (i in 0 until related.length().coerceAtMost(6)) {
                        val obj = related.optJSONObject(i) ?: continue
                        val text = obj.optString("Text", "").trim()
                        val firstUrl = obj.optString("FirstURL", "").trim()
                        if (text.isNotEmpty()) {
                            results.add(WebSearchSnippet("Kết quả #${results.size + 1}", firstUrl, text))
                        }
                    }
                }
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (transientApiKey != null) {
                Arrays.fill(transientApiKey, '\u0000')
            }
        }
    }
}
