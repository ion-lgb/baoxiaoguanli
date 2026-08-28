package cn.loxx.expense.data.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Amount helpers. All money is stored as integer cents (Long); display/input use
 * yuan as a decimal string with two fractional digits.
 */
object AmountFormatter {
    fun formatCents(cents: Long): String =
        BigDecimal.valueOf(cents)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .toPlainString()

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
