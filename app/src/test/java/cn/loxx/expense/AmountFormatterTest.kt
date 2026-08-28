package cn.loxx.expense

import cn.loxx.expense.data.model.AmountFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountFormatterTest {
    @Test
    fun formatCents_rendersYuanWithTwoDecimals() {
        assertEquals("123.45", AmountFormatter.formatCents(12345L))
    }

    @Test
    fun parseToCents_convertsYuanStringToCents() {
        assertEquals(12345L, AmountFormatter.parseToCents("123.45"))
    }

    @Test
    fun parseToCents_handlesSingleCent() {
        assertEquals(1L, AmountFormatter.parseToCents("0.01"))
    }

    @Test
    fun parseToCents_returnsNullForMalformedInput() {
        assertNull(AmountFormatter.parseToCents("abc"))
    }

    @Test
    fun formatCentsGrouped_groupsWholeYuanWithoutDecimals() {
        assertEquals("5,214", AmountFormatter.formatCentsGrouped(521_400L))
        assertEquals("0", AmountFormatter.formatCentsGrouped(0L))
    }

    @Test
    fun formatCentsGrouped_keepsDecimalsWhenPresent() {
        assertEquals("1,234.56", AmountFormatter.formatCentsGrouped(123_456L))
        assertEquals("0.01", AmountFormatter.formatCentsGrouped(1L))
    }
}
