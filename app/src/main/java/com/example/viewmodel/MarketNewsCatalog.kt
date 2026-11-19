package com.example.viewmodel

import com.example.core.AppLanguage
import kotlin.random.Random

data class NewsTemplate(
    val headlineVi: String,
    val headlineEn: String,
    val isPositive: Boolean
) {
    fun textFor(language: AppLanguage): String =
        if (language == AppLanguage.VI) headlineVi else headlineEn
}

object MarketNewsCatalog {
    val templates: List<NewsTemplate> = listOf(
        // Positive headlines
        NewsTemplate(
            headlineVi = "Quỹ lớn công bố đầu tư vào HAIGENX1",
            headlineEn = "Major fund announces strategic investment in HAIGENX1",
            isPositive = true
        ),
        NewsTemplate(
            headlineVi = "HAIGENX1 được nâng cấp giao thức siêu tốc v2",
            headlineEn = "HAIGENX1 ships high-throughput v2 network upgrade",
            isPositive = true
        ),
        NewsTemplate(
            headlineVi = "Cá voi đồng loạt gom mạnh HAIGENX1 trên Sàn 1",
            headlineEn = "Whales aggressively accumulate HAIGENX1 on Exchange 1",
            isPositive = true
        ),
        NewsTemplate(
            headlineVi = "Cộng đồng thợ mỏ HAIGENX1 cán mốc kỷ lục mới",
            headlineEn = "HAIGENX1 miner community hits a new all-time milestone",
            isPositive = true
        ),
        NewsTemplate(
            headlineVi = "Báo cáo nguồn cung cho thấy HAIGENX1 ngày càng khan hiếm",
            headlineEn = "Supply report highlights tightening HAIGENX1 circulation",
            isPositive = true
        ),
        NewsTemplate(
            headlineVi = "Đối tác công nghệ tích hợp phần thưởng HAIGENX1",
            headlineEn = "Tech partner integrates HAIGENX1 in-app reward rails",
            isPositive = true
        ),
        // Negative headlines
        NewsTemplate(
            headlineVi = "Sàn lớn bị hack, thị trường hoảng loạn",
            headlineEn = "Major exchange breached, market enters panic mode",
            isPositive = false
        ),
        NewsTemplate(
            headlineVi = "Ví cá voi bất ngờ xả hàng triệu HAIGENX1",
            headlineEn = "Whale wallet dumps millions of HAIGENX1 onto the orderbook",
            isPositive = false
        ),
        NewsTemplate(
            headlineVi = "Tin đồn siết chặt hoạt động khai thác coin mô phỏng",
            headlineEn = "Rumors of stricter rules on simulated coin mining spread",
            isPositive = false
        ),
        NewsTemplate(
            headlineVi = "Chỉ số USDT tăng vọt gây áp lực bán tháo HAIGENX1",
            headlineEn = "Surging USDT Index triggers heavy HAIGENX1 sell-off",
            isPositive = false
        ),
        NewsTemplate(
            headlineVi = "Mạng lưới tạm thời nghẽn giao dịch trong giờ cao điểm",
            headlineEn = "Temporary peak-hour congestion spooks short-term traders",
            isPositive = false
        ),
        NewsTemplate(
            headlineVi = "Quỹ đầu cơ đồng loạt chốt lời HAIGENX1",
            headlineEn = "Hedge desks take heavy profits across HAIGENX1 pairs",
            isPositive = false
        )
    )

    fun pickRandomTemplate(lastHeadlineVi: String?, random: Random = Random): NewsTemplate {
        val candidates = templates.filter { it.headlineVi != lastHeadlineVi }
        return (if (candidates.isNotEmpty()) candidates else templates).random(random)
    }
}
