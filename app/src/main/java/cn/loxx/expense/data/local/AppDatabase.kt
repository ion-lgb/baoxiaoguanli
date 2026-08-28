package cn.loxx.expense.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TripEntity::class,
        ExpenseEntity::class,
        ReceiptEntity::class,
        CategoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun receiptDao(): ReceiptDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder<AppDatabase>(context, "expense.db")
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
