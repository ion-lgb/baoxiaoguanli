package cn.loxx.expense.ui.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.loxx.expense.data.export.ExcelExporter
import cn.loxx.expense.data.export.PdfExporter
import cn.loxx.expense.data.export.ReceiptExport
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.repository.CategoryRepository
import cn.loxx.expense.data.repository.ExpenseRepository
import cn.loxx.expense.data.repository.SettingsRepository
import cn.loxx.expense.data.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategorySummary(
    val name: String,
    val count: Int,
    val totalCents: Long,
)

data class ReportUiState(
    val trip: TripEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val userName: String = "",
) {
    val totalCents: Long get() = expenses.sumOf { it.amountCents }

    val categorySummary: List<CategorySummary>
        get() {
            val nameById = categories.associateBy { it.id }
            return expenses
                .groupBy { it.categoryId }
                .map { (categoryId, grouped) ->
                    CategorySummary(
                        name = nameById[categoryId]?.name ?: "其他",
                        count = grouped.size,
                        totalCents = grouped.sumOf { it.amountCents },
                    )
                }
        }
}

class ReportViewModel(
    private val tripId: Long,
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    context: Context,
) : ViewModel() {
    private val appContext = context.applicationContext

    val uiState: StateFlow<ReportUiState> = combine(
        tripRepository.getTripWithExpenses(tripId),
        expenseRepository.getReceiptsByTrip(tripId),
        categoryRepository.getAll(),
    ) { tripWithExpenses, receipts, categories ->
        ReportUiState(
            trip = tripWithExpenses?.trip,
            expenses = tripWithExpenses?.expenses ?: emptyList(),
            receipts = receipts,
            categories = categories,
            userName = settingsRepository.userName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportPdf(onResult: (File) -> Unit, onError: (String) -> Unit) {
        val state = uiState.value
        val trip = state.trip
        if (trip == null) {
            onError("行程不存在")
            return
        }
        launchExport(onResult, onError) {
            val receipts = loadReceipts(state.receipts)
            val fontBytes = loadFont()
            val bytes = PdfExporter().generate(
                trip, state.expenses, state.categories, state.userName, fontBytes, receipts,
            )
            writeExport(exportFileName(trip.title, "pdf"), bytes)
        }
    }

    fun exportZip(onResult: (File) -> Unit, onError: (String) -> Unit) {
        val state = uiState.value
        val trip = state.trip
        if (trip == null) {
            onError("行程不存在")
            return
        }
        launchExport(onResult, onError) {
            val receipts = loadReceipts(state.receipts)
            val bytes = ExcelExporter().generate(trip, state.expenses, state.categories, receipts)
            writeExport(exportFileName(trip.title, "zip"), bytes)
        }
    }

    private fun launchExport(
        onResult: (File) -> Unit,
        onError: (String) -> Unit,
        generate: () -> File,
    ) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                onResult(withContext(Dispatchers.IO) { generate() })
            } catch (e: Exception) {
                onError(e.message ?: "导出失败")
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun loadReceipts(receipts: List<ReceiptEntity>): List<ReceiptExport> =
        receipts.mapNotNull { receipt ->
            val file = File(appContext.filesDir, receipt.filePath)
            if (file.exists()) {
                ReceiptExport(
                    expenseId = receipt.expenseId,
                    fileType = receipt.fileType,
                    originalName = receipt.originalName,
                    bytes = file.readBytes(),
                )
            } else {
                null
            }
        }

    private fun loadFont(): ByteArray? =
        try {
            appContext.assets.open("NotoSansSC-Regular.otf").use { it.readBytes() }
        } catch (e: Exception) {
            null
        }

    private fun writeExport(name: String, bytes: ByteArray): File {
        val dir = File(appContext.cacheDir, "exports")
        dir.mkdirs()
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file
    }

    companion object {
        /** Human-readable, collision-free cache file name: 报销单_标题_时间戳.ext */
        fun exportFileName(tripTitle: String, extension: String): String {
            val safeTitle = tripTitle
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .trim()
                .ifBlank { "未命名" }
                .take(20)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            return "报销单_${safeTitle}_${stamp}.$extension"
        }
    }
}
