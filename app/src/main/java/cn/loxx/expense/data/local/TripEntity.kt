package cn.loxx.expense.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val status: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
