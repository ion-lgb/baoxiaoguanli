package cn.loxx.expense

import android.content.Context
import cn.loxx.expense.data.local.AppDatabase
import cn.loxx.expense.data.repository.CategoryRepository
import cn.loxx.expense.data.repository.ExpenseRepository
import cn.loxx.expense.data.repository.ReceiptStorage
import cn.loxx.expense.data.repository.SettingsRepository
import cn.loxx.expense.data.repository.TripRepository
import cn.loxx.expense.data.webdav.SyncManager

/** Manual dependency container; no DI framework. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.build(appContext)
    private val storage = ReceiptStorage(appContext)

    val tripRepository = TripRepository(database.tripDao(), database.receiptDao(), storage)
    val expenseRepository = ExpenseRepository(database.expenseDao(), database.receiptDao(), storage)
    val categoryRepository = CategoryRepository(database.categoryDao(), database.expenseDao())
    val settingsRepository = SettingsRepository(appContext)
    val syncManager = SyncManager(appContext, database)
}
