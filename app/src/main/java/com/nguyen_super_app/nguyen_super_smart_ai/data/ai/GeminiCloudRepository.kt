package com.nguyen_super_app.nguyen_super_smart_ai.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

sealed interface AiStreamChunk {
    data class Delta(val accumulatedText: String) : AiStreamChunk
    data class Completed(val finalText: String) : AiStreamChunk
    data class Failed(val message: String) : AiStreamChunk
}

/**
 * Firebase AI Logic (Gemini) repository.
 * Initializes models lazily via `by lazy`, uses 60s realistic timeouts,
 * streams natural responses directly, supports multimodal images, and surfaces authentic errors.
 */
class GeminiCloudRepository {

    companion object {
        private const val TAG = "GeminiCloudRepo"
        const val MODEL_FLASH_38 = "gemini-3.8-flash"
        const val MODEL_FLASH_LITE_35 = "gemini-3.5-flash-lite"
        const val MODEL_ARGON_4 = "gemini-4-argon"

        val supportedModels = listOf(
            MODEL_FLASH_38 to "Gemini 3.8 Flash (Mặc định • Nhanh & Đa năng)",
            MODEL_FLASH_LITE_35 to "Gemini 3.5 Flash-Lite (Siêu nhẹ)",
            MODEL_ARGON_4 to "Gemini 4 Argon (Suy luận chuyên sâu)"
        )
    }

    private val flashModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(MODEL_FLASH_38)
    }

    private val liteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(MODEL_FLASH_LITE_35)
    }

    private val argonModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(MODEL_ARGON_4)
    }

    private fun resolveModel(modelId: String): GenerativeModel = when (modelId) {
        MODEL_FLASH_LITE_35 -> liteModel
        MODEL_ARGON_4 -> argonModel
        else -> flashModel
    }

    fun streamPrompt(
        modelId: String,
        systemPersona: String?,
        prompt: String,
        attachedImages: List<Bitmap> = emptyList()
    ): Flow<AiStreamChunk> = flow {
        val model = resolveModel(modelId)
        val fullPrompt = if (!systemPersona.isNullOrBlank()) {
            "[Vai trò: $systemPersona]\n\n$prompt"
        } else {
            prompt
        }

        val inputContent = content {
            attachedImages.forEach { bmp -> image(bmp) }
            text(fullPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(inputContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamChunk.Delta(accumulated.toString()))
                    }
                }
            }
            val resultText = accumulated.toString()
            if (resultText.isNotBlank()) {
                emit(AiStreamChunk.Completed(resultText))
            } else {
                emit(AiStreamChunk.Failed("Mô hình Gemini trả về phản hồi rỗng."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out after 60s", e)
            emit(AiStreamChunk.Failed("Quá thời gian chờ 60 giây khi kết nối Gemini Cloud."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            emit(AiStreamChunk.Failed(e.localizedMessage ?: "Không thể kết nối Firebase AI Logic."))
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        Log.e(TAG, "Stream collector error", error)
        emit(AiStreamChunk.Failed(error.localizedMessage ?: "Lỗi luồng suy luận AI."))
    }
}
