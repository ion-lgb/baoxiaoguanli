package cn.loxx.expense.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts")
    fun getAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getById(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts WHERE expenseId = :expenseId ORDER BY createdAt ASC, id ASC")
    fun getByExpenseId(expenseId: Long): Flow<List<ReceiptEntity>>

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    @Query(
        """
        SELECT r.* FROM receipts r
        INNER JOIN expenses e ON e.id = r.expenseId
        WHERE e.tripId = :tripId
        ORDER BY r.createdAt ASC, r.id ASC
        """,
    )
    fun getByTripId(tripId: Long): Flow<List<ReceiptEntity>>
}
