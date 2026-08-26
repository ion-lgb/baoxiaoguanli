package cn.loxx.expense.data.repository

import cn.loxx.expense.data.local.ReceiptDao
import cn.loxx.expense.data.local.TripDao
import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.model.TripWithExpenses
import cn.loxx.expense.data.model.TripWithTotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TripRepository(
    private val tripDao: TripDao,
    private val receiptDao: ReceiptDao,
    private val storage: ReceiptStorage,
) {
    fun getTripsWithTotal(): Flow<List<TripWithTotal>> = tripDao.getAllWithTotal()

    fun getTrip(tripId: Long): Flow<TripEntity?> = tripDao.getById(tripId)

    fun getTripWithExpenses(tripId: Long): Flow<TripWithExpenses?> =
        tripDao.getTripWithExpenses(tripId)

    suspend fun createTrip(
        title: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        note: String,
    ): Long {
        val now = System.currentTimeMillis()
        return tripDao.insert(
            TripEntity(
                title = title,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                status = "ongoing",
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.update(trip.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTrip(trip: TripEntity) {
        receiptDao.getByTripId(trip.id).first().forEach { storage.delete(it.filePath) }
        tripDao.delete(trip)
    }
}
