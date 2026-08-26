package cn.loxx.expense.ui.expense

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.repository.CategoryRepository
import cn.loxx.expense.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A receipt picked but not yet persisted (still awaiting an expense id). */
data class PendingReceipt(
    val uri: Uri,
    val fileType: String,
    val originalName: String,
)

data class AddEditExpenseUiState(
    val expense: ExpenseEntity? = null,
    val receipts: List<ReceiptEntity> = emptyList(),
)

class AddEditExpenseViewModel(
    private val tripId: Long,
    private val expenseId: Long,
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<AddEditExpenseUiState> = combine(
        expenseRepository.getExpense(expenseId),
        expenseRepository.getReceiptsByExpense(expenseId),
    ) { expense, receipts -> AddEditExpenseUiState(expense, receipts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEditExpenseUiState())

    fun save(
        amountCents: Long,
        categoryId: Long,
        description: String,
        date: Long,
        pending: List<PendingReceipt>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val id = if (expenseId > 0) {
                val existing = uiState.value.expense ?: return@launch
                expenseRepository.updateExpense(
                    existing.copy(
                        categoryId = categoryId,
                        amountCents = amountCents,
                        description = description,
                        date = date,
                    ),
                )
                existing.id
            } else {
                expenseRepository.addExpense(tripId, categoryId, amountCents, description, date)
            }
            pending.forEach { p ->
                expenseRepository.addReceipt(id, p.uri, p.fileType, p.originalName)
            }
            onSaved()
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch { expenseRepository.deleteReceipt(receipt) }
    }
}
