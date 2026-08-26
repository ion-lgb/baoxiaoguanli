package cn.loxx.expense.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Owns the physical receipt files under `filesDir/receipts/{expenseId}/...`.
 * Database rows are managed by repositories; files are managed here so that
 * both trip- and expense-level deletion clean up the same way.
 */
class ReceiptStorage(private val context: Context) {
    fun fileFor(relativePath: String): File = File(context.filesDir, relativePath)

    fun copyIn(expenseId: Long, sourceUri: Uri, fileType: String): String {
        val ext = if (fileType == "pdf") "pdf" else "jpg"
        val dir = File(context.filesDir, "receipts/$expenseId")
        dir.mkdirs()
        val relativePath = "receipts/$expenseId/${UUID.randomUUID()}.$ext"
        val dest = File(context.filesDir, relativePath)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("无法读取所选凭证文件")
        return relativePath
    }

    fun delete(relativePath: String) {
        File(context.filesDir, relativePath).delete()
    }
}
