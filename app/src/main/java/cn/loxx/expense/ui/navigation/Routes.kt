package cn.loxx.expense.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class TripDetailRoute(val tripId: Long)

@Serializable
data class AddEditExpenseRoute(val tripId: Long, val expenseId: Long = 0)

@Serializable
data class ReceiptViewRoute(val receiptId: Long)

@Serializable
object SettingsRoute

@Serializable
data class ReportPreviewRoute(val tripId: Long)
