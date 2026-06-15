package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY sort_order ASC")
    fun observeCategories(userId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY sort_order ASC")
    suspend fun getCategoriesForUser(userId: Long): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    /** Reassign all expenses in a deleted category to null (Uncategorised). */
    @Query("UPDATE expenses SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun reassignExpensesToUncategorised(categoryId: Long)

    /** Count how many expenses belong to this category (for the delete confirmation dialog). */
    @Query("SELECT COUNT(*) FROM expenses WHERE category_id = :categoryId")
    suspend fun countExpensesForCategory(categoryId: Long): Int

    /** Update sort orders after drag-and-drop reordering. */
    @Query("UPDATE categories SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)
}
