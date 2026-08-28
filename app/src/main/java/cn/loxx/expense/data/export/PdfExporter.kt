package cn.loxx.expense.data.export

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.model.AmountFormatter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a reimbursement report PDF (cover, detail table, category summary,
 * receipt appendix). [fontBytes] should be a CJK-capable font (e.g.
 * NotoSansSC-Regular.otf); when null, the built-in Helvetica is used (no CJK
 * glyphs, but still a structurally valid PDF — used by unit tests).
 */
class PdfExporter {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun generate(
        trip: TripEntity,
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        userName: String,
        fontBytes: ByteArray?,
        receipts: List<ReceiptExport>,
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(baos))
        pdf.documentInfo
            .setSubject(
                "Source: https://github.com/ion-lgb/baoxiaoguanli — licensed AGPL-3.0-only",
            )
            .setKeywords("iText, AGPL-3.0-only, open source reimbursement report")
        val document = Document(pdf, PageSize.A4)

        val font: PdfFont = if (fontBytes != null) {
            PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H)
        } else {
            PdfFontFactory.createFont(StandardFonts.HELVETICA)
        }
        document.setFont(font)

        val categoryById = categories.associateBy { it.id }

        document.add(
            Paragraph("费用报销单")
                .setFontSize(22f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER),
        )
        document.add(Paragraph("\n"))
        document.add(Paragraph("行程：${trip.title}"))
        document.add(Paragraph("报销人：${userName.ifBlank { "—" }}"))
        document.add(Paragraph("日期范围：${dateRange(trip)}"))
        document.add(Paragraph("制表日期：${dayFormat.format(Date())}"))
        document.add(Paragraph("\n"))

        document.add(Paragraph("费用明细").setFontSize(14f).setBold())
        val detail = Table(floatArrayOf(1f, 2f, 2f, 3f, 2f)).useAllAvailableWidth()
        // NOTE: "元" instead of "¥" — the subsetted CJK asset font lacks the U+00A5 glyph.
        listOf("序号", "日期", "分类", "描述", "金额(元)").forEach {
            detail.addHeaderCell(Cell().add(Paragraph(it).setBold()))
        }
        expenses.forEachIndexed { index, e ->
            detail.addCell(Cell().add(Paragraph("${index + 1}")))
            detail.addCell(Cell().add(Paragraph(dayFormat.format(Date(e.date)))))
            detail.addCell(Cell().add(Paragraph(categoryById[e.categoryId]?.name ?: "其他")))
            detail.addCell(Cell().add(Paragraph(e.description)))
            detail.addCell(Cell().add(Paragraph(AmountFormatter.formatCents(e.amountCents))))
        }
        val total = expenses.sumOf { it.amountCents }
        detail.addCell(Cell(1, 4).add(Paragraph("合计").setBold()))
        detail.addCell(Cell().add(Paragraph(AmountFormatter.formatCents(total)).setBold()))
        document.add(detail)
        document.add(Paragraph("\n"))

        document.add(Paragraph("按分类汇总").setFontSize(14f).setBold())
        val summary = Table(floatArrayOf(2f, 2f, 2f)).useAllAvailableWidth()
        listOf("分类", "笔数", "小计(元)").forEach {
            summary.addHeaderCell(Cell().add(Paragraph(it).setBold()))
        }
        expenses.groupBy { it.categoryId }.forEach { (catId, list) ->
            summary.addCell(Cell().add(Paragraph(categoryById[catId]?.name ?: "其他")))
            summary.addCell(Cell().add(Paragraph("${list.size}")))
            summary.addCell(
                Cell().add(Paragraph(AmountFormatter.formatCents(list.sumOf { it.amountCents }))),
            )
        }
        document.add(summary)

        receipts.forEach { receipt ->
            document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
            document.add(Paragraph(receipt.originalName))
            if (receipt.fileType == "pdf") {
                val reader = PdfReader(ByteArrayInputStream(receipt.bytes))
                val readerDoc = PdfDocument(reader)
                val form = readerDoc.getFirstPage().copyAsFormXObject(pdf)
                document.add(Image(form).setAutoScale(true))
                readerDoc.close()
            } else {
                document.add(Image(ImageDataFactory.create(receipt.bytes)).setAutoScale(true))
            }
        }

        document.close()
        return baos.toByteArray()
    }

    private fun dateRange(trip: TripEntity): String {
        val start = dayFormat.format(Date(trip.startDate))
        return if (trip.endDate == 0L) {
            "$start 起"
        } else {
            "$start ~ ${dayFormat.format(Date(trip.endDate))}"
        }
    }
}
