package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.CategoryDao
import com.example.truetrackfinance.data.db.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CategoryRepository"

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeCategories(userId: Long): Flow<List<Category>> = 
        categoryDao.observeCategories(userId)

    suspend fun addCategory(category: Category): Long {
        val existing = categoryDao.getCategoriesForUser(category.userId)
        val nextSortOrder = if (existing.isEmpty()) 0 else (existing.maxOf { it.sortOrder } + 1)
        
        Log.d(TAG, "Inserting category '${category.name}' with sort order $nextSortOrder")
        return categoryDao.insertCategory(category.copy(sortOrder = nextSortOrder))
    }

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    /**
     * Delete category and reassign all associated expenses to Uncategorised.
     */
    suspend fun deleteCategory(category: Category) {
        Log.w(TAG, "Action: Deleting category ID ${category.id} - ${category.name}")
        categoryDao.reassignExpensesToUncategorised(category.id)
        categoryDao.deleteCategory(category)
    }

    suspend fun countExpensesForCategory(categoryId: Long): Int = 
        categoryDao.countExpensesForCategory(categoryId)

    suspend fun updateSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            categoryDao.updateSortOrder(id, index)
        }
    }

    /** Pre-load default categories on user first launch. */
    suspend fun seedDefaultCategories(userId: Long) {
        val defaults = listOf(
            Category(userId = userId, name = "Groceries", colorHex = "#006D5B", emoji = "🛒", isDefault = true, sortOrder = 0),
            Category(userId = userId, name = "Transport", colorHex = "#0284C7", emoji = "🚗", isDefault = true, sortOrder = 1),
            Category(userId = userId, name = "Entertainment", colorHex = "#7C3AED", emoji = "🎬", isDefault = true, sortOrder = 2),
            Category(userId = userId, name = "Utilities", colorHex = "#DC2626", emoji = "💡", isDefault = true, sortOrder = 3),
            Category(userId = userId, name = "Health", colorHex = "#DB2777", emoji = "💊", isDefault = true, sortOrder = 4)
        )
        categoryDao.insertCategories(defaults)
        Log.i(TAG, "Seeded 5 default categories for user $userId")
    }
}
