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
}
