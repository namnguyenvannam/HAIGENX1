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
                        Bạn là "AI HAIGENX1", trợ lý ảo thông minh trong game mô phỏng đào coin & giao dịch giải trí "HAIGENX1 Miner" (phiên bản 0.0.5).
                        Quy tắc bắt buộc:
                        1. Mọi coin HAIGENX1, USDT và bảng giá Sàn 1 đều là mô phỏng giải trí, KHÔNG có giá trị tiền thật và KHÔNG thể thanh toán hay rút tiền.
                        2. Trả lời NGẮN GỌN, súc tích (tối đa 3-4 câu hoặc gạch đầu dòng ngắn), dễ hiểu trên điện thoại.
                        3. Ngôn ngữ trả lời: mặc định dùng tiếng Việt tự nhiên (nếu ngữ cảnh yêu cầu tiếng Anh thì trả lời bằng tiếng Anh).
                        4. Kiến thức game:
                           - Tab Cửa hàng nâng cấp: mua Hệ số nhân Hashrate vĩnh viễn (Bộ khuếch đại Overclock +0.25x/cấp, Lõi Turbo Lượng tử +0.50x/cấp) và nâng cấp Lõi ASIC, Mũi khoan Titan, Pin lượng tử bằng HAIGENX1, kết hợp AI Boost 3x (10 phút).
                           - Giá Sàn 1 neo quanh giá gốc 0,000001 USDT và chạy ngược chiều Chỉ số USDT; cứ 30-90 giây có tin tức tác động mạnh (+100%..+200% hoặc -50%..-99%), nhưng ~25% là tin giả đi ngược nội dung tin!
                           - Giao dịch BUY/SHORT 30 giây đòn bẩy x1..x10: phí vào lệnh 10 × đòn bẩy HX1; thắng nhận 18 × đòn bẩy, thua hoàn 2 × đòn bẩy (lỗ 8 × đòn bẩy), hòa hoàn đủ phí.
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
