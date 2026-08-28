package cn.loxx.expense.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Amount helpers. All money is stored as integer cents (Long); display/input use
 * yuan as a decimal string with two fractional digits.
 */
object AmountFormatter {
    fun formatCents(cents: Long): String =
        BigDecimal.valueOf(cents)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .toPlainString()

    /** Grouped display for summaries: "1,234" when whole yuan, "1,234.56" otherwise. */
    fun formatCentsGrouped(cents: Long): String {
        val whole = cents / 100
        return if (cents % 100 == 0L) {
            String.format(Locale.CHINA, "%,d", whole)
        } else {
            String.format(Locale.CHINA, "%,.2f", cents / 100.0)
        }
    }

    /** Returns cents, or null when [yuan] is blank, malformed, or negative. */
    fun parseToCents(yuan: String): Long? {
        val trimmed = yuan.trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
