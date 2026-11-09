package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object MoneyFormatUtils {
    private val ruLocale: Locale = Locale.forLanguageTag("ru-RU")

    private val symbols = DecimalFormatSymbols(ruLocale).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }

    private val wholeFormatter = DecimalFormat("#,##0", symbols)
    private val decimalFormatter = DecimalFormat("#,##0.##", symbols)

    fun formatMoney(amount: Double, currencySymbol: String = "₽"): String {
        val isWhole = abs(amount - amount.roundToLong()) < 0.005
        val formattedNumber = if (isWhole) {
            wholeFormatter.format(amount.roundToLong())
        } else {
            decimalFormatter.format(amount)
        }
        return "$formattedNumber $currencySymbol"
    }

    fun formatSignedMoney(amount: Double, currencySymbol: String = "₽"): String {
        val prefix = if (amount >= 0) "+" else "−"
        return "$prefix${formatMoney(abs(amount), currencySymbol)}"
    }

    fun formatCompactMoney(amount: Double, currencySymbol: String = "₽"): String {
        val absVal = abs(amount)
        return when {
            absVal >= 1_000_000_000 -> String.format(ruLocale, "%.1f млрд %s", amount / 1_000_000_000.0, currencySymbol)
            absVal >= 10_000_000 -> String.format(ruLocale, "%.1f млн %s", amount / 1_000_000.0, currencySymbol)
            else -> formatMoney(amount, currencySymbol)
        }
    }

    fun formatDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", ruLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", ruLocale)
        return sdf.format(Date(epochMillis))
    }
}
