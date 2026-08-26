package cn.loxx.expense.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val isBuiltin: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
)
