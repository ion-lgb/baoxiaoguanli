package cn.loxx.expense.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import cn.loxx.expense.data.model.TripWithExpenses
import cn.loxx.expense.data.model.TripWithTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getById(id: Long): Flow<TripEntity?>

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripWithExpenses(tripId: Long): Flow<TripWithExpenses?>

    @Query(
        """
        SELECT t.*, COALESCE(SUM(e.amountCents), 0) AS totalCents
        FROM trips t
        LEFT JOIN expenses e ON e.tripId = t.id
        GROUP BY t.id
        ORDER BY t.createdAt DESC
        """,
    )
    fun getAllWithTotal(): Flow<List<TripWithTotal>>
}
