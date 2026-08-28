package cn.loxx.expense.data.webdav

import android.content.Context
import cn.loxx.expense.data.local.AppDatabase
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.local.TripEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.room3.withWriteTransaction
import androidx.room3.withReadTransaction
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = 1,
    val trips: List<TripEntity>,
    val expenses: List<ExpenseEntity>,
    val receipts: List<ReceiptEntity>,
    val categories: List<CategoryEntity>,
)

/** Serializes the whole local database + receipt files into a single zip and back. */
class SyncManager(private val context: Context, private val database: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun backup(client: WebDavClient) {
        val backup = database.withReadTransaction {
            BackupData(
                trips = database.tripDao().getAll().first(),
                expenses = database.expenseDao().getAll().first(),
                receipts = database.receiptDao().getAll().first(),
                categories = database.categoryDao().getAll().first(),
            )
        }

        if (!client.exists("expense-backups")) {
            client.mkdir("expense-backups")
        }
        val name = "backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.zip"
        client.upload("expense-backups/$name", buildBackupZip(backup))
    }

    suspend fun restore(client: WebDavClient, remoteName: String? = null) {
        val name = remoteName ?: listBackups(client).firstOrNull()
            ?: throw IllegalStateException("服务器上没有可用的备份")
        val (backup, receipts) = parseBackupZip(client.download("expense-backups/$name"))
        val stagedReceipts = stageReceiptFiles(backup.receipts, receipts)

        try {
            database.withWriteTransaction {
                // trips cascade to expenses, expenses cascade to receipts.
                database.tripDao().deleteAll()
                database.categoryDao().deleteAll()
                backup.categories.forEach { database.categoryDao().insert(it) }
                backup.trips.forEach { database.tripDao().insert(it) }
                backup.expenses.forEach { database.expenseDao().insert(it) }
                backup.receipts.forEach { database.receiptDao().insert(it) }
            }
            replaceReceiptDirectory(stagedReceipts)
        } finally {
            stagedReceipts.deleteRecursively()
        }
    }

    suspend fun listBackups(client: WebDavClient): List<String> =
        client.list("expense-backups")
            .map { it.name }
            .filter { it.endsWith(".zip") }
            .sortedDescending()

    private fun buildBackupZip(backup: BackupData): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            backup.receipts.forEach { receipt ->
                val file = File(context.filesDir, receipt.filePath)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry(receipt.filePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }

    private fun stageReceiptFiles(
        receiptEntities: List<ReceiptEntity>,
        receiptBytes: Map<String, ByteArray>,
    ): File {
        val stagingDir = File(context.cacheDir, "restore-receipts-${System.currentTimeMillis()}")
        receiptEntities.forEach { receipt ->
            val bytes = receiptBytes[receipt.filePath]
                ?: throw IllegalStateException("备份文件损坏：缺少凭证 ${receipt.filePath}")
            val relativePath = receipt.filePath.removePrefix("receipts/")
            val file = File(stagingDir, relativePath)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
        return stagingDir
    }

    private fun replaceReceiptDirectory(stagedReceipts: File) {
        val target = File(context.filesDir, "receipts")
        if (!target.deleteRecursively()) {
            throw IllegalStateException("无法清理本地凭证目录")
        }
        target.mkdirs()
        stagedReceipts.copyRecursively(target, overwrite = true)
    }

    private fun parseBackupZip(zip: ByteArray): Pair<BackupData, Map<String, ByteArray>> {
        val receipts = mutableMapOf<String, ByteArray>()
        var backup: BackupData? = null
        ZipInputStream(zip.inputStream()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val bytes = input.readBytes()
                when (entry.name) {
                    "backup.json" -> backup = json.decodeFromString<BackupData>(bytes.toString(Charsets.UTF_8))
                    else -> receipts[entry.name] = bytes
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        val data = backup ?: throw IllegalStateException("备份文件损坏：缺少 backup.json")
        return data to receipts
    }
}
