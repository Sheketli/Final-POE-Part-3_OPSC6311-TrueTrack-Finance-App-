package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import com.example.truetrackfinance.data.model.CategorySpending
import com.example.truetrackfinance.data.model.DailySpending
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): Expense?

    // ── Full list with category join ─────────────────────────────────────────

    @Query("""
        SELECT e.*, c.name AS category_name, c.color_hex AS category_color, c.emoji AS category_emoji
        FROM expenses e
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = :userId
        ORDER BY e.date DESC
    """)
    fun observeAllExpenses(userId: Long): Flow<List<ExpenseWithCategory>>

    // ── Filtered queries ─────────────────────────────────────────────────────

    @Query("""
        SELECT e.*, c.name AS category_name, c.color_hex AS category_color, c.emoji AS category_emoji
        FROM expenses e
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = :userId
          AND (:fromDate IS NULL OR e.date >= :fromDate)
          AND (:toDate IS NULL OR e.date <= :toDate)
          AND (:minAmount IS NULL OR e.amount >= :minAmount)
          AND (:maxAmount IS NULL OR e.amount <= :maxAmount)
          AND (:search IS NULL OR e.description LIKE '%' || :search || '%')
        ORDER BY e.date DESC
    """)
    fun observeFilteredExpenses(
        userId: Long,
        fromDate: Long?,
        toDate: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        search: String?
    ): Flow<List<ExpenseWithCategory>>

    // ── Aggregate queries ────────────────────────────────────────────────────

    /** Total spent in a calendar month (monthStart..monthEnd epoch ms). */
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE user_id = :userId AND date >= :monthStart AND date < :monthEnd
    """)
    fun observeTotalSpentInMonth(userId: Long, monthStart: Long, monthEnd: Long): Flow<Double>

    /** Per-category totals for a period (for doughnut chart and summary table). */
    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.color_hex AS colorHex,
               COALESCE(SUM(e.amount), 0) AS totalSpent
        FROM categories c
        LEFT JOIN expenses e ON e.category_id = c.id
            AND e.user_id = :userId
            AND e.date >= :fromDate AND e.date < :toDate
        WHERE c.user_id = :userId
        GROUP BY c.id
        ORDER BY totalSpent DESC
    """)
    fun observeCategorySpending(
        userId: Long,
        fromDate: Long,
        toDate: Long
    ): Flow<List<CategorySpending>>

    /** Daily totals grouped by day (for bar chart). */
    @Query("""
        SELECT strftime('%Y-%m-%d', date / 1000, 'unixepoch') AS day,
               COALESCE(SUM(amount), 0) AS totalSpent
        FROM expenses
        WHERE user_id = :userId AND date >= :fromDate AND date < :toDate
        GROUP BY day
        ORDER BY day ASC
    """)
    fun observeDailySpending(userId: Long, fromDate: Long, toDate: Long): Flow<List<DailySpending>>

    /** Recurring expenses due before a given timestamp (for WorkManager scheduling). */
    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId AND is_recurring = 1 AND next_recurrence_date <= :before
    """)
    suspend fun getRecurringExpensesDueBefore(userId: Long, before: Long): List<Expense>

    /** Expenses logged today (for streak calculation). */
    @Query("""
        SELECT COUNT(*) FROM expenses
        WHERE user_id = :userId AND date >= :dayStart AND date < :dayEnd
    """)
    suspend fun countExpensesOnDay(userId: Long, dayStart: Long, dayEnd: Long): Int

    /** Distinct days with expenses in a range (for streak logic). */
    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', date / 1000, 'unixepoch') AS day
        FROM expenses
        WHERE user_id = :userId AND date >= :fromDate
        ORDER BY day DESC
    """)
    suspend fun getDistinctDaysWithExpenses(userId: Long, fromDate: Long): List<String>

    /** Get all expenses for a specific period (for reports). */
    @Query("""
        SELECT e.*, c.name AS category_name, c.color_hex AS category_color, c.emoji AS category_emoji
        FROM expenses e
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = :userId AND e.date >= :fromDate AND e.date < :toDate
        ORDER BY e.date DESC
    """)
    fun observeExpensesForPeriod(userId: Long, fromDate: Long, toDate: Long): Flow<List<ExpenseWithCategory>>
}
