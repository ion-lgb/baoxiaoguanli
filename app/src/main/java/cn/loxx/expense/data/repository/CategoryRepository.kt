package cn.loxx.expense.data.repository

import cn.loxx.expense.data.local.CategoryDao
import cn.loxx.expense.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun getById(id: Long): Flow<CategoryEntity?> = categoryDao.getById(id)

    suspend fun add(name: String, icon: String): Long {
        val current = categoryDao.getAll().first()
        val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
        return categoryDao.insert(
            CategoryEntity(
                name = name,
                icon = icon,
                isBuiltin = false,
                sortOrder = maxOrder + 1,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun rename(category: CategoryEntity, newName: String) {
        categoryDao.update(category.copy(name = newName))
    }

    suspend fun delete(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    /** Inserts the six preset categories exactly once on first launch. */
    suspend fun ensureSeeded() {
        if (categoryDao.getAll().first().isNotEmpty()) return
        val now = System.currentTimeMillis()
        val presets = listOf(
            "交通" to "directions_car",
            "住宿" to "hotel",
            "餐饮" to "restaurant",
            "通讯" to "phone",
            "办公用品" to "inventory_2",
            "其他" to "more_horiz",
        )
        presets.forEachIndexed { index, (name, icon) ->
            categoryDao.insert(
                CategoryEntity(
                    name = name,
                    icon = icon,
                    isBuiltin = true,
                    sortOrder = index,
                    createdAt = now,
                ),
            )
        }
    }
}
