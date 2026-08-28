package cn.loxx.expense.data.model

import androidx.room3.Embedded
import androidx.room3.Relation
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.TripEntity

data class TripWithExpenses(
    @Embedded val trip: TripEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["tripId"])
    val expenses: List<ExpenseEntity>,
)
