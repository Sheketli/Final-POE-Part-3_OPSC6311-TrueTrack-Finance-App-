package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.Budget
import com.example.truetrackfinance.data.db.entity.CategoryLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: Budget): Long

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND month_key = :monthKey LIMIT 1")
    suspend fun getBudgetForMonth(userId: Long, monthKey: String): Budget?

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND month_key = :monthKey LIMIT 1")
    fun observeBudgetForMonth(userId: Long, monthKey: String): Flow<Budget?>

    // ── Category limits ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategoryLimit(limit: CategoryLimit): Long

    @Query("""
        SELECT * FROM category_limits
        WHERE user_id = :userId AND month_key = :monthKey
    """)
    fun observeCategoryLimitsForMonth(userId: Long, monthKey: String): Flow<List<CategoryLimit>>

    @Query("""
        SELECT * FROM category_limits
        WHERE user_id = :userId AND month_key = :monthKey
    """)
    suspend fun getCategoryLimitsForMonth(userId: Long, monthKey: String): List<CategoryLimit>

    @Query("""
        SELECT limit_amount FROM category_limits
        WHERE user_id = :userId AND category_id = :categoryId AND month_key = :monthKey
        LIMIT 1
    """)
    suspend fun getLimitForCategory(userId: Long, categoryId: Long, monthKey: String): Double?

    @Delete
    suspend fun deleteCategoryLimit(limit: CategoryLimit)
}
