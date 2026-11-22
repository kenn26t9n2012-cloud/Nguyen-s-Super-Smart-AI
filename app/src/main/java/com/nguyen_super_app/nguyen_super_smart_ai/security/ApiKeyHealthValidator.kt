package com.nguyen_super_app.nguyen_super_smart_ai.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Arrays
import java.util.concurrent.TimeUnit

enum class KeyValidationState {
    UNVERIFIED,
    CHECKING,
    VALID,
    INVALID
}

data class ApiKeyValidationResult(
    val keyId: String,
    val state: KeyValidationState,
    val message: String,
    val checkedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Validates stored API keys using transient CharArray reads and wipes buffers immediately after probe.
 */
object ApiKeyHealthValidator {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun validateKey(
        keyId: String,
        storageManager: EncryptedStorageManager
    ): ApiKeyValidationResult = withContext(Dispatchers.IO) {
        var keyChars: CharArray? = null
        try {
            storageManager.withDecryptedApiKey(keyId) { chars ->
                keyChars = chars?.copyOf()
            }
            val tokenChars = keyChars
            if (tokenChars == null || tokenChars.isEmpty()) {
                return@withContext ApiKeyValidationResult(
                    keyId = keyId,
                    state = KeyValidationState.INVALID,
                    message = "Khóa chưa được thiết lập."
                )
            }
            val token = String(tokenChars).trim()
            if (token.any { it.isWhitespace() || it.isISOControl() }) {
                return@withContext ApiKeyValidationResult(
                    keyId = keyId,
                    state = KeyValidationState.INVALID,
                    message = "Khóa chứa khoảng trắng hoặc ký tự điều khiển không hợp lệ."
                )
            }
            when (keyId) {
                EncryptedStorageManager.KEY_GEMINI_OVERRIDE -> probeGeminiKey(keyId, token)
                EncryptedStorageManager.KEY_HUGGINGFACE_TOKEN -> probeHuggingFace(keyId, token)
                EncryptedStorageManager.KEY_GITHUB_PAT -> probeGitHub(keyId, token)
                EncryptedStorageManager.KEY_WEB_SEARCH_TOKEN -> probeWebSearch(keyId, token)
                EncryptedStorageManager.KEY_CUSTOM_OPENAI_COMPAT -> validateCustomBearerFormat(keyId, token)
                else -> validateCustomBearerFormat(keyId, token)
            }
        } catch (e: Exception) {
            ApiKeyValidationResult(
                keyId = keyId,
                state = KeyValidationState.INVALID,
                message = "Không thể kết nối máy chủ kiểm tra (${e.localizedMessage ?: "lỗi mạng"})."
            )
        } finally {
            keyChars?.let { Arrays.fill(it, '\u0000') }
        }
    }

    private fun probeGeminiKey(keyId: String, token: String): ApiKeyValidationResult {
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$token")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) {
                ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Hợp lệ (Google Gemini API HTTP ${resp.code})")
            } else {
                ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "Bị từ chối bởi Gemini API (HTTP ${resp.code})")
            }
        }
    }

    private fun probeHuggingFace(keyId: String, token: String): ApiKeyValidationResult {
        val req = Request.Builder()
            .url("https://huggingface.co/api/whoami-v2")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) {
                ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Hợp lệ (Hugging Face xác thực thành công)")
            } else {
                ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "Token Hugging Face không hợp lệ (HTTP ${resp.code})")
            }
        }
    }

    private fun probeGitHub(keyId: String, token: String): ApiKeyValidationResult {
        val req = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) {
                ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Hợp lệ (GitHub PAT đã được xác thực)")
            } else {
                ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "GitHub PAT bị từ chối (HTTP ${resp.code})")
            }
        }
    }

    private fun probeWebSearch(keyId: String, token: String): ApiKeyValidationResult {
        val jsonMedia = "application/json; charset=utf-8".toMediaType()
        val req = if (token.startsWith("tvly-", ignoreCase = true)) {
            val body = """{"api_key":"$token","query":"test","max_results":1}""".toRequestBody(jsonMedia)
            Request.Builder().url("https://api.tavily.com/search").post(body).build()
        } else {
            val body = """{"q":"test","num":1}""".toRequestBody(jsonMedia)
            Request.Builder()
                .url("https://google.serper.dev/search")
                .header("X-API-KEY", token)
                .post(body)
                .build()
        }
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) {
                ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Hợp lệ (API Tìm kiếm Web phản hồi HTTP ${resp.code})")
            } else if (resp.code == 401 || resp.code == 403) {
                ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "Khóa Tìm kiếm Web không hợp lệ (HTTP ${resp.code})")
            } else {
                ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "Máy chủ tìm kiếm trả về HTTP ${resp.code}")
            }
        }
    }

    private fun validateCustomBearerFormat(keyId: String, token: String): ApiKeyValidationResult {
        return if (token.length >= 8) {
            ApiKeyValidationResult(keyId, KeyValidationState.VALID, "Định dạng Bearer Token hợp lệ (${token.length} ký tự)")
        } else {
            ApiKeyValidationResult(keyId, KeyValidationState.INVALID, "Bearer Token quá ngắn (< 8 ký tự)")
        }
    }
}
