package cn.loxx.expense.data.export

import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.TripEntity
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Generates an xlsx expense sheet and packages it with receipts into a ZIP. */
class ExcelExporter {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun generate(
        trip: TripEntity,
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        receipts: List<ReceiptExport>,
    ): ByteArray {
        val categoryById = categories.associateBy { it.id }
        val indexByExpenseId = expenses.mapIndexed { index, e -> e.id to index }.toMap()

        fun zipName(receipt: ReceiptExport): String {
            val index = indexByExpenseId[receipt.expenseId] ?: 0
            val expense = expenses.getOrNull(index)
            val categoryName = categoryById[expense?.categoryId]?.name ?: "其他"
            val desc = (expense?.description ?: "")
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(20)
            val seq = (index + 1).toString().padStart(3, '0')
            val ext = if (receipt.fileType == "pdf") "pdf" else "jpg"
            return "${seq}_${categoryName}_${desc}.$ext"
        }

        val xlsxBytes = buildXlsx(expenses, categoryById, receipts, ::zipName)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            val root = "报销单_${trip.title}"
            zip.putNextEntry(ZipEntry("$root/费用明细.xlsx"))
            zip.write(xlsxBytes)
            zip.closeEntry()
            receipts.forEach { receipt ->
                zip.putNextEntry(ZipEntry("$root/凭证/${zipName(receipt)}"))
                zip.write(receipt.bytes)
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun buildXlsx(
        expenses: List<ExpenseEntity>,
        categoryById: Map<Long, CategoryEntity>,
        receipts: List<ReceiptExport>,
        zipName: (ReceiptExport) -> String,
    ): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("费用明细")

        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true })
        }
        val moneyStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
        }

        val headerRow = sheet.createRow(0)
        listOf("序号", "日期", "分类", "描述", "金额(¥)", "凭证文件名").forEachIndexed { i, label ->
            headerRow.createCell(i).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }

        val receiptNamesByExpense = receipts
            .groupBy { it.expenseId }
            .mapValues { (_, list) -> list.joinToString(", ") { zipName(it) } }

        expenses.forEachIndexed { index, e ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(dayFormat.format(Date(e.date)))
            row.createCell(2).setCellValue(categoryById[e.categoryId]?.name ?: "其他")
            row.createCell(3).setCellValue(e.description)
            row.createCell(4).apply {
                setCellValue(e.amountCents / 100.0)
                cellStyle = moneyStyle
            }
            row.createCell(5).setCellValue(receiptNamesByExpense[e.id] ?: "")
        }

        val totalRow = sheet.createRow(expenses.size + 1)
        totalRow.createCell(3).setCellValue("合计")
        totalRow.createCell(4).apply {
            cellFormula = if (expenses.isEmpty()) "0" else "SUM(E2:E${expenses.size + 1})"
            cellStyle = moneyStyle
        }

        sheet.setColumnWidth(0, 8 * 256)
        sheet.setColumnWidth(1, 12 * 256)
        sheet.setColumnWidth(2, 12 * 256)
        sheet.setColumnWidth(3, 32 * 256)
        sheet.setColumnWidth(4, 14 * 256)
        sheet.setColumnWidth(5, 40 * 256)

        val baos = ByteArrayOutputStream()
        workbook.write(baos)
        workbook.close()
        return baos.toByteArray()
    }
}
