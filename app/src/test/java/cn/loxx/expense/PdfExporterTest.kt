package cn.loxx.expense

import cn.loxx.expense.data.export.PdfExporter
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.TripEntity
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import java.io.ByteArrayInputStream
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExporterTest {
    @Test
    fun generate_producesNonEmptyPdf() {
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

        val result = PdfExporter().generate(
            trip,
            expenses,
            categories,
            "张三",
            fontBytes = null,
            receipts = emptyList(),
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.size > 1024)

        PdfDocument(PdfReader(ByteArrayInputStream(result))).use { pdf ->
            assertTrue(pdf.documentInfo.producer.contains("iText", ignoreCase = true))
            assertTrue(pdf.documentInfo.subject.contains("github.com/ion-lgb/baoxiaoguanli"))
            assertTrue(pdf.documentInfo.keywords.contains("AGPL-3.0-only"))
        }
    }
}
