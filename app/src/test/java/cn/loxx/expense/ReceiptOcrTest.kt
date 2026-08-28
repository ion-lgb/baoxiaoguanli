package cn.loxx.expense

import cn.loxx.expense.data.ocr.ReceiptOcr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptOcrTest {
    @Test
    fun parsesTaxiReceipt() {
        val parsed = ReceiptOcr.parse(
            listOf(
                "北京出租车发票",
                "BEIJING TAXI RECEIPT",
                "----------------------------",
                "车号: B-T8829    日期: 2026-08-11",
                "上车: 14:32      下车: 15:06",
                "单价: 2.60元/km  里程: 18.4km",
                "金额: ￥58.00",
                "----------------------------",
                "发票代码: 1100263A",
                "发票号码: 88291145",
            ),
        )
        assertEquals(5800L, parsed.amountCents)
        assertEquals("2026-08-11", formatDate(parsed.dateMillis))
        assertEquals("北京出租车发票", parsed.description)
    }

    @Test
    fun parsesHotelFolioTotal() {
        val parsed = ReceiptOcr.parse(
            listOf(
                "住宿发票",
                "HOTEL FOLIO / INVOICE",
                "客户名称: 罗晓明        部门: 研发部",
                "房号: 1208             入住: 2026-08-10",
                "房费 450.00 x 2晚            900.00",
                "合计（人民币）              ￥900.00",
                "付款方式: 公司协议月结",
                "北京国贸大酒店          税号: 91110105MA",
            ),
        )
        assertEquals(90000L, parsed.amountCents)
        assertEquals("住宿发票", parsed.description)
    }

    @Test
    fun handlesGroupedThousands() {
        val parsed = ReceiptOcr.parse(listOf("合计: ¥1,234.56"))
        assertEquals(123_456L, parsed.amountCents)
    }

    @Test
    fun ignoresYearAndPicksDecimalAmount() {
        val parsed = ReceiptOcr.parse(listOf("金额: 58.00 日期: 2026-08-11"))
        assertEquals(5800L, parsed.amountCents)
    }

    @Test
    fun fallsBackToLargestDecimalWithoutKeyword() {
        val parsed = ReceiptOcr.parse(
            listOf("共 3 件商品", "矿泉水 2.00", "画册 12.50", "台灯 45.00"),
        )
        assertEquals(4500L, parsed.amountCents)
    }

    @Test
    fun emptyInputParsesToNulls() {
        val parsed = ReceiptOcr.parse(emptyList())
        assertNull(parsed.amountCents)
        assertNull(parsed.dateMillis)
        assertNull(parsed.description)
    }

    @Test
    fun parsesElectronicInvoice() {
        // Lines as they come out of a 数电发票 PDF (both pdftotext and OCR orderings)
        val parsed = ReceiptOcr.parse(
            listOf(
                "电子发票（普通发票）",
                "发票号码：26317000002966471360",
                "开票日期：2026年08月25日",
                "购 名称：广州天翱信息科技有限公司",
                "销 名称：上海三快智送科技有限公司",
                "统一社会信用代码/纳税人识别号：91310000MA1FW9A80N",
                "*生产生活服务*配送服务",
                "费",
                "合      计  ¥0.94  ¥0.06",
                "价税合计（大写）壹圆整 （小写）¥1.00",
                "开票人：何娟",
            ),
        )
        assertEquals(100L, parsed.amountCents)
        assertEquals("2026-08-25", formatDate(parsed.dateMillis))
        assertEquals("配送服务", parsed.description)
    }

    @Test
    fun electronicInvoiceWithoutItemFallsBackToSeller() {
        val parsed = ReceiptOcr.parse(
            listOf(
                "电子发票（普通发票）",
                "开票日期：2026年08月25日",
                "购 名称：广州天翱信息科技有限公司",
                "销 名称：上海三快智送科技有限公司",
                "价税合计（大写）壹圆整 （小写）¥1.00",
            ),
        )
        assertEquals(100L, parsed.amountCents)
        assertEquals("上海三快智送科技有限公司", parsed.description)
    }

    @Test
    fun parsesGarbledInvoiceOcr() {
        // Verbatim ML Kit output from a photographed 数电发票: scattered spaces
        // inside words and decimals, traditional-character noise, reordered blocks.
        val parsed = ReceiptOcr.parse(
            listOf(
                "购买方信 息一",
                "购名称:广州天翱信息科技有限公司",
                "個统一社会信用代码/纳稅人识别号:914401066777726354",
                "*生产生活服务*配送服务",
                "费",
                "项目名称",
                "合",
                "价稅合含计(大写)",
                "开票人:何娟!",
                "电予发素(普通发票)",
                "規格型号",
                "单 位",
                "③壹园整",
                "海市税务局,",
                "教量",
                "1",
                "售",
                "方",
                "名称:上海三快智送科技有限公司",
                "统一社会信用代码/纳稅人识别号:91310000MA1FW9A80N",
                "单 价",
                "发票号码:26317000002966471360",
                "开票日期:2026年08月25日",
                "0.94",
                "金 额 稅率/征狂收率",
                "0.94",
                "¥0.94",
                "6%",
                "(小写)1. 00",
                "稅额",
                "0. 06",
                "Y0. 06",
            ),
        )
        assertEquals(100L, parsed.amountCents)
        assertEquals("2026-08-25", formatDate(parsed.dateMillis))
        assertEquals("配送服务", parsed.description)
    }

    private fun formatDate(millis: Long?): String? {
        millis ?: return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(millis))
    }
}
