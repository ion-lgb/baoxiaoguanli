package cn.loxx.expense.ui.expense

import androidx.lifecycle.ViewModel
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class ReceiptViewModel(
    expenseRepository: ExpenseRepository,
    receiptId: Long,
) : ViewModel() {
    val receipt: StateFlow<ReceiptEntity?> = expenseRepository.getReceipt(receiptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
