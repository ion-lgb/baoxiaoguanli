package cn.loxx.expense.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.repository.CategoryRepository
import cn.loxx.expense.data.repository.ExpenseRepository
import cn.loxx.expense.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val trip: TripEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
) {
    val totalCents: Long get() = expenses.sumOf { it.amountCents }

    fun categoryIcon(categoryId: Long): String =
        categories.firstOrNull { it.id == categoryId }?.icon ?: "more_horiz"
}

class TripDetailViewModel(
    tripId: Long,
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.getTripWithExpenses(tripId),
        expenseRepository.getReceiptsByTrip(tripId),
        categoryRepository.getAll(),
    ) { tripWithExpenses, receipts, categories ->
        TripDetailUiState(
            trip = tripWithExpenses?.trip,
            expenses = tripWithExpenses?.expenses ?: emptyList(),
            receipts = receipts,
            categories = categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    fun updateTrip(title: String, destination: String, startDate: Long, endDate: Long, note: String) {
        val trip = uiState.value.trip ?: return
        viewModelScope.launch {
            tripRepository.updateTrip(
                trip.copy(
                    title = title,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    note = note,
                ),
            )
        }
    }

    fun markCompleted() {
        setTripStatus("completed") { trip ->
            trip.copy(
                endDate = if (trip.endDate == 0L) System.currentTimeMillis() else trip.endDate,
            )
        }
    }

    fun markReported() {
        setTripStatus("reported")
    }

    private fun setTripStatus(status: String, transform: (TripEntity) -> TripEntity = { it }) {
        val trip = uiState.value.trip ?: return
        viewModelScope.launch {
            tripRepository.updateTrip(transform(trip).copy(status = status))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { expenseRepository.deleteExpense(expense) }
    }
}
