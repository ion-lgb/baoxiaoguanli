package cn.loxx.expense.data.model

import androidx.room3.Embedded
import cn.loxx.expense.data.local.TripEntity

data class TripWithTotal(
    @Embedded val trip: TripEntity,
    val totalCents: Long,
)
