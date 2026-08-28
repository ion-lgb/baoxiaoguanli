package cn.loxx.expense.data.repository

import cn.loxx.expense.data.local.CategoryDao
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.local.ExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
) {
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
        require(!category.isBuiltin) { "内置分类不能删除" }
        check(expenseDao.countByCategoryId(category.id) == 0L) {
            "该分类已有费用记录，不能删除"
        }
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
