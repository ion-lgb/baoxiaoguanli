package cn.loxx.expense.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import kotlin.coroutines.resume

/**
 * Extracts expense fields from receipt photos using on-device ML Kit OCR.
 * [parse] is pure and unit-tested; [recognize] only wraps the recognizer.
 */
object ReceiptOcr {

    data class Parsed(
        val amountCents: Long?,
        val dateMillis: Long?,
        val description: String?,
    )

    private val amountKeyword = Regex(
        pattern = "(金额|合计|总价|总计|实收|应收|小计|小写|总额|total|amount)",
        option = RegexOption.IGNORE_CASE,
    )
    private val decimal = Regex("""\d{1,3}(?:,\d{3})*(?:\.\d{1,2})|\d+(?:\.\d{1,2})?""")
    private val datePattern = Regex("""(\d{4})[-/年.](\d{1,2})[-/月.](\d{1,2})""")
    private val noise = Regex("(发票代码|发票号码|发票号|No\\.?|税号|车号|单号)")
    private val invoiceItem = Regex("""\*[^\*\s]+\*([^\*]{1,24})""")
    private val sellerName = Regex("""销\s*名称[:：]\s*(.+)""")

    /** Best-effort extraction; every field is nullable. */
    fun parse(lines: List<String>): Parsed = Parsed(
        amountCents = parseAmount(lines),
        dateMillis = parseDate(lines),
        description = parseDescription(lines),
    )

    private fun parseAmount(rawLines: List<String>): Long? {
        // OCR scatters spaces into words and decimals ("金 额", "1. 00") — strip them all
        val lines = rawLines.map { it.replace("￥", "¥").replace(" ", "").replace("\u3000", "") }
        val candidates = lines.filter { it.contains(amountKeyword) }
        val pool = if (candidates.isNotEmpty()) candidates else lines
        var best: BigDecimal? = null
        for (line in pool) {
            decimal.findAll(line.replace("¥", "")).forEach { match ->
                val value = runCatching {
                    BigDecimal(match.value.replace(",", ""))
                }.getOrNull() ?: return@forEach
                // ignore years (2026), phone fragments without decimals, absurd values
                val looksLikeYear = value.scale() == 0 && value >= BigDecimal(1900) &&
                    value <= BigDecimal(2100)
                if (looksLikeYear || value <= BigDecimal.ZERO || value > BigDecimal(1_000_000)) {
                    return@forEach
                }
                if (best == null || value > best) best = value
            }
        }
        best ?: return null
        return best.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    private fun parseDate(lines: List<String>): Long? {
        for (line in lines) {
            val match = datePattern.find(line) ?: continue
            val (year, month, day) = match.destructured
            val calendar = Calendar.getInstance().apply { clear() }
            calendar.set(year.toInt(), month.toInt() - 1, day.toInt(), 12, 0, 0)
            return calendar.timeInMillis
        }
        return null
    }

    private fun parseDescription(lines: List<String>): String? {
        // 电子发票: 项目名称 is the most specific — *分类*项目名
        for (line in lines) {
            invoiceItem.find(line)?.let { return it.groupValues[1].trim().take(24) }
        }
        // 电子发票: seller (收款方) name beats a generic title line
        for (line in lines) {
            sellerName.find(line)?.let { return it.groupValues[1].trim().take(24) }
        }
        return lines.firstOrNull { line ->
            val text = line.trim()
            text.length in 2..24 &&
                text.contains(Regex("[\\u4e00-\\u9fa5A-Za-z]")) &&
                !text.contains(amountKeyword) &&
                !text.contains(noise) &&
                !datePattern.containsMatchIn(text) &&
                decimal.findAll(text).count() <= 1
        }?.trim()
    }

    /** Runs on-device Chinese OCR over an image [uri]; null when recognition fails. */
    suspend fun recognize(context: Context, uri: Uri): Parsed? {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            ?: return null
        return recognizeImage(image)
    }

    /**
     * OCR for PDF receipts (电子发票 etc.): renders the first page to a bitmap
     * with the platform [android.graphics.pdf.PdfRenderer], then runs the same
     * recognizer. Null when the file cannot be opened or recognition fails.
     */
    suspend fun recognizePdf(context: Context, uri: Uri): Parsed? {
        val bitmap = runCatching {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { descriptor ->
                val renderer = android.graphics.pdf.PdfRenderer(descriptor)
                renderer.use { pdf ->
                    val page = pdf.openPage(0)
                    page.use { p ->
                        val scale = RENDER_TARGET_WIDTH_PX / p.width.coerceAtLeast(1)
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            p.width * scale,
                            p.height * scale,
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                        // PdfRenderer paints a black background by default
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        p.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull() ?: return null
        return recognizeImage(InputImage.fromBitmap(bitmap, 0))
    }

    private suspend fun recognizeImage(image: InputImage): Parsed? {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) {
                            continuation.resume(
                                parse(text.textBlocks.flatMap { block -> block.lines.map { it.text } }),
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        } finally {
            recognizer.close()
        }
    }

    private const val RENDER_TARGET_WIDTH_PX = 1600
}
