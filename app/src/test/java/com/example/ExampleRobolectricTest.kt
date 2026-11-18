package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppLanguage
import com.example.core.FormatUtils
import com.example.core.stringsFor
import com.example.data.ai.AiHaigenxRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches HAIGENX1 Miner`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HAIGENX1 Miner", appName)
    }

    @Test
    fun `banner, labels, withdraw notice, and version strings match specification`() {
        val vi = stringsFor(AppLanguage.VI)
        assertEquals("HAIGENX1 MANG LẠI NIỀM VUI LAN TỎA CHO MỌI NGƯỜI", vi.disclaimerBanner)
        assertEquals("HAIGENX1/USDT", vi.marketPairTitle)
        assertEquals("Ví HAIGENX1", vi.walletTitle)
        assertEquals("Số dư HAIGENX1", vi.walletVirtualBalance)
        assertEquals("Quy đổi USDT", vi.walletEstimatedUsdt)
        assertEquals("Chỉ số USDT", vi.marketUsdtIndexTitle)
        assertEquals("Thông báo", vi.walletWithdrawDialogTitle)
        assertEquals("Tính năng sẽ sớm ra mắt", vi.walletWithdrawDialogBody)
        assertEquals("Đã hiểu", vi.walletWithdrawDialogConfirm)
        assertEquals("Phiên bản 0.0.1", vi.settingsVersionValue)
        assertEquals("AI đang trả lời...", vi.aiLoadingText)
        assertEquals("AI đang quá tải, bạn thử lại sau ít phút nhé", vi.aiOverloadError)
        assertEquals("Tổng cung", vi.marketTotalSupplyTitle)
        assertEquals("100.000.000 HAIGENX1", vi.marketTotalSupplyValue)
        assertFalse(vi.marketPairTitle.contains("ảo"))
        assertFalse(vi.walletTitle.contains("ảo"))
        assertFalse(vi.walletVirtualBalance.contains("ảo"))
        assertFalse(vi.walletEstimatedUsdt.contains("ảo"))
        assertFalse(vi.marketUsdtIndexTitle.contains("ảo"))
    }

    @Test
    fun `ai retry backoff and fallback models match specification`() {
        assertEquals("gemini-2.5-flash", AiHaigenxRepository.DEFAULT_CHAT_MODEL)
        assertEquals(
            listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3.5-flash-lite"),
            AiHaigenxRepository.FALLBACK_MODELS
        )
        assertEquals(listOf(1_000L, 2_000L, 4_000L), AiHaigenxRepository.RETRY_BACKOFF_DELAYS_MS)

        assertTrue(
            AiHaigenxRepository.isOverloadOrTransientError(
                RuntimeException("This model is currently experiencing high demand. Please try again later.")
            )
        )
        assertTrue(
            AiHaigenxRepository.isOverloadOrTransientError(
                RuntimeException("HTTP 503 Service Unavailable")
            )
        )
        assertTrue(
            AiHaigenxRepository.isOverloadOrTransientError(
                RuntimeException("429 Too Many Requests")
            )
        )
        assertFalse(
            AiHaigenxRepository.isOverloadOrTransientError(
                IllegalArgumentException("Invalid prompt schema")
            )
        )
    }

    @Test
    fun `initial price formatting preserves 6 decimals`() {
        val formattedVi = FormatUtils.formatUsdtPrice(0.000001, AppLanguage.VI)
        val formattedEn = FormatUtils.formatUsdtPrice(0.000001, AppLanguage.EN)
        assertTrue(formattedVi == "0,000001")
        assertTrue(formattedEn == "0.000001")
    }
}
