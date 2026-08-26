package cn.loxx.expense

import cn.loxx.expense.data.export.ExcelExporter
import cn.loxx.expense.data.export.ReceiptExport
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.TripEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExcelExporterTest {
    @Before
    fun configureStaxFactories() {
        System.setProperty(
            "javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl",
        )
        System.setProperty(
            "javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl",
        )
        System.setProperty(
            "javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.evt.EventFactoryImpl",
        )
    }

    @Test
    fun generate_writesXlsxWithHeaderAndTotalRows() {
        val now = System.currentTimeMillis()
        val trip = TripEntity(
            id = 1,
            title = "测试",
            destination = "上海",
            startDate = now,
            endDate = now,
            status = "ongoing",
            note = "",
            createdAt = now,
            updatedAt = now,
        )
        val expenses = listOf(
            ExpenseEntity(
                id = 1,
                tripId = 1,
                categoryId = 1,
                amountCents = 5000,
                description = "午餐",
                date = now,
                createdAt = now,
                updatedAt = now,
            ),
            ExpenseEntity(
                id = 2,
                tripId = 1,
                categoryId = 2,
                amountCents = 12000,
                description = "打车",
                date = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val categories = listOf(
            CategoryEntity(
                id = 1,
                name = "餐饮",
                icon = "food",
                isBuiltin = true,
                sortOrder = 1,
                createdAt = now,
            ),
            CategoryEntity(
                id = 2,
                name = "交通",
                icon = "car",
                isBuiltin = true,
                sortOrder = 2,
                createdAt = now,
            ),
        )
        val receipt = ReceiptExport(
            expenseId = expenses.first().id,
            fileType = "image",
            originalName = "a.jpg",
            bytes = ByteArray(10) { 1 },
        )

        val result = ExcelExporter().generate(trip, expenses, categories, listOf(receipt))

        val xlsxBytes = ZipInputStream(ByteArrayInputStream(result)).use { zip ->
            generateSequence { zip.nextEntry }
                .first { it.name.endsWith("费用明细.xlsx") }
            zip.readBytes()
        }

        val workbook = XSSFWorkbook(ByteArrayInputStream(xlsxBytes))
        val sheet = workbook.getSheetAt(0)
        assertEquals(expenses.size + 2, sheet.lastRowNum + 1)
        workbook.close()
    }
}
