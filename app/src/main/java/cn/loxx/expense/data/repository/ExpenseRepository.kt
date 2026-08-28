package cn.loxx.expense.data.repository

import android.net.Uri
import cn.loxx.expense.data.local.ExpenseDao
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptDao
import cn.loxx.expense.data.local.ReceiptEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val receiptDao: ReceiptDao,
    private val storage: ReceiptStorage,
) {
    fun getExpensesByTrip(tripId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getByTripId(tripId)

    fun getExpense(expenseId: Long): Flow<ExpenseEntity?> = expenseDao.getById(expenseId)

    fun getReceipt(receiptId: Long): Flow<ReceiptEntity?> = receiptDao.getById(receiptId)

    fun getReceiptsByTrip(tripId: Long): Flow<List<ReceiptEntity>> =
        receiptDao.getByTripId(tripId)

    fun getReceiptsByExpense(expenseId: Long): Flow<List<ReceiptEntity>> =
        receiptDao.getByExpenseId(expenseId)

    fun getTotalCentsByTrip(tripId: Long): Flow<Long> =
        expenseDao.getTotalCentsByTrip(tripId)

    suspend fun addExpense(
        tripId: Long,
        categoryId: Long,
        amountCents: Long,
        description: String,
        date: Long,
    ): Long {
        val now = System.currentTimeMillis()
        return expenseDao.insert(
            ExpenseEntity(
                tripId = tripId,
                categoryId = categoryId,
                amountCents = amountCents,
                description = description,
                date = date,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.update(expense.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        receiptDao.getByExpenseId(expense.id).first().forEach { storage.delete(it.filePath) }
        expenseDao.delete(expense)
    }

    suspend fun addReceipt(
        expenseId: Long,
        sourceUri: Uri,
        fileType: String,
        originalName: String,
    ): Long {
        val relativePath = storage.copyIn(expenseId, sourceUri, fileType)
        return receiptDao.insert(
            ReceiptEntity(
                expenseId = expenseId,
                filePath = relativePath,
                fileType = fileType,
                originalName = originalName,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        storage.delete(receipt.filePath)
        receiptDao.delete(receipt)
    }
}
