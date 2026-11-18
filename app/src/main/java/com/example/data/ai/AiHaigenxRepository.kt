package com.example.data.ai

import android.util.Log
import com.example.core.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

private const val TAG = "AiHaigenxRepository"

class AiHaigenxRepository {

    companion object {
        const val DEFAULT_CHAT_MODEL = "gemini-2.5-flash"
        val FALLBACK_MODELS = listOf(
            DEFAULT_CHAT_MODEL,
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash-lite"
        )
        val RETRY_BACKOFF_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)

        fun isOverloadOrTransientError(error: Throwable): Boolean {
            if (error is TimeoutCancellationException) return true
            val combinedMessage = generateSequence(error) { it.cause }
                .mapNotNull { it.message }
                .joinToString(" ")
                .lowercase()
            return combinedMessage.contains("503") ||
                combinedMessage.contains("429") ||
                combinedMessage.contains("high demand") ||
                combinedMessage.contains("overloaded") ||
                combinedMessage.contains("unavailable") ||
                combinedMessage.contains("resource exhausted") ||
                combinedMessage.contains("resource_exhausted") ||
                combinedMessage.contains("too many requests") ||
                combinedMessage.contains("try again")
        }
    }

    private val modelCache = mutableMapOf<String, GenerativeModel>()

    private fun getOrCreateModel(modelName: String): GenerativeModel {
        return modelCache.getOrPut(modelName) {
            Firebase.ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 320
                },
                systemInstruction = content {
                    text(
                        """
                        Bạn là "AI HAIGENX1", trợ lý ảo thông minh trong game mô phỏng đào coin giải trí "HAIGENX1 Miner" (phiên bản 0.0.1).
                        Quy tắc bắt buộc:
                        1. Mọi coin HAIGENX1, USDT và bảng giá Sàn 1 đều là mô phỏng giải trí, KHÔNG có giá trị tiền thật và KHÔNG thể rút tiền. Luôn giữ tinh thần vui vẻ, minh bạch nếu người chơi hỏi về rút tiền hoặc tiền thật.
                        2. Trả lời NGẮN GỌN, súc tích (tối đa 3-4 câu hoặc gạch đầu dòng ngắn), dễ hiểu trên màn hình điện thoại.
                        3. Ngôn ngữ trả lời: mặc định dùng tiếng Việt tự nhiên (nếu ngữ cảnh yêu cầu tiếng Anh thì trả lời bằng tiếng Anh).
                        4. Nhiệm vụ: gợi ý mẹo đào hiệu quả (quản lý thanh năng lượng, kết hợp tự đào nền và bấm nút Đào, kích hoạt AI Boost 3x trong 10 phút), giải thích biến động giá ngẫu nhiên 10-20% trên "Sàn 1 - mô phỏng" và cơ chế chạy ngược chiều với "Chỉ số USDT" (Chỉ số USDT tăng thì giá HAIGENX1 giảm mạnh).
                        """.trimIndent()
                    )
                }
            )
        }
    }

    suspend fun streamReplyWithRetryAndFallback(
        userPrompt: String,
        gameContextSummary: String,
        language: AppLanguage,
        onWaitingForRetry: () -> Unit,
        onStreamingText: (String) -> Unit
    ): String {
        val langInstruction = if (language == AppLanguage.VI) {
            "Trả lời ngắn gọn bằng tiếng Việt."
        } else {
            "Reply concisely in English."
        }

        val fullPrompt = buildString {
            appendLine("[Trạng thái game hiện tại / Live Game Context]")
            appendLine(gameContextSummary)
            appendLine()
            appendLine("[$langInstruction]")
            append("Người chơi hỏi: ")
            append(userPrompt)
        }

        var lastError: Throwable? = null

        for (modelName in FALLBACK_MODELS) {
            val maxRetries = RETRY_BACKOFF_DELAYS_MS.size
            for (attempt in 0..maxRetries) {
                val accumulated = StringBuilder()
                try {
                    val model = getOrCreateModel(modelName)
                    withTimeout(45_000L) {
                        model.generateContentStream(fullPrompt).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            if (chunkText.isNotEmpty()) {
                                accumulated.append(chunkText)
                                onStreamingText(accumulated.toString())
                            }
                        }
                    }
                    val result = accumulated.toString().trim()
                    if (result.isNotEmpty()) {
                        return result
                    }
                    lastError = IllegalStateException("Empty response from $modelName")
                    Log.w(TAG, "Empty response from $modelName on attempt ${attempt + 1}")
                } catch (e: TimeoutCancellationException) {
                    lastError = e
                    Log.w(TAG, "Timeout on model=$modelName attempt=${attempt + 1}", e)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "Error on model=$modelName attempt=${attempt + 1}: ${e.message}", e)
                    if (!isOverloadOrTransientError(e)) {
                        // Non-overload error (e.g., model unavailable/404): break to next fallback model
                        break
                    }
                }

                if (attempt < maxRetries) {
                    onWaitingForRetry()
                    val delayMs = RETRY_BACKOFF_DELAYS_MS[attempt]
                    Log.d(TAG, "Retrying $modelName in ${delayMs}ms (retry ${attempt + 1}/$maxRetries)")
                    delay(delayMs)
                }
            }
            Log.w(TAG, "Exhausted attempts for model=$modelName, trying next fallback if available.")
        }

        throw lastError ?: IllegalStateException("All AI models failed")
    }
}
