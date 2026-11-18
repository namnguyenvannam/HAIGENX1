package com.example.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    private fun symbolsFor(language: AppLanguage): DecimalFormatSymbols {
        val locale = if (language == AppLanguage.VI) Locale("vi", "VN") else Locale.US
        return DecimalFormatSymbols(locale)
    }

    fun formatHx1(amount: Double, language: AppLanguage, decimals: Int = 4): String {
        val pattern = buildString {
            append("#,##0")
            if (decimals > 0) {
                append('.')
                repeat(decimals) { append('0') }
            }
        }
        return DecimalFormat(pattern, symbolsFor(language)).format(amount)
    }

    fun formatUsdtPrice(price: Double, language: AppLanguage): String {
        val df = DecimalFormat("0.000000##", symbolsFor(language))
        return df.format(price)
    }

    fun formatUsdtValue(value: Double, language: AppLanguage): String {
        val pattern = if (value >= 0.01) "#,##0.0000" else "0.00000000"
        return DecimalFormat(pattern, symbolsFor(language)).format(value)
    }

    fun formatHashrate(hashrate: Double, language: AppLanguage): String {
        return DecimalFormat("#,##0.00", symbolsFor(language)).format(hashrate)
    }

    fun formatPercent(pct: Double, language: AppLanguage): String {
        val sign = if (pct >= 0) "+" else ""
        val formatted = DecimalFormat("0.0", symbolsFor(language)).format(pct)
        return "$sign$formatted%"
    }

    fun formatCountdown(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatTimestamp(millis: Long, language: AppLanguage): String {
        val locale = if (language == AppLanguage.VI) Locale("vi", "VN") else Locale.US
        val sdf = SimpleDateFormat("HH:mm:ss • dd/MM", locale)
        return sdf.format(Date(millis))
    }
}
