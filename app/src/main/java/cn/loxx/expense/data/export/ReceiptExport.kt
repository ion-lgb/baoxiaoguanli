package cn.loxx.expense.data.export

/** A receipt's bytes plus the expense it belongs to, for export generation. */
data class ReceiptExport(
    val expenseId: Long,
    val fileType: String,
    val originalName: String,
    val bytes: ByteArray,
)
