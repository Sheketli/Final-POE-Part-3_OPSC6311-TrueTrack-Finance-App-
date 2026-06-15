package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.ExpenseDao
import com.example.truetrackfinance.data.db.dao.CategoryDao
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.data.model.CategorySpending
import com.example.truetrackfinance.data.model.DailySpending
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import com.example.truetrackfinance.util.DateUtil
import com.example.truetrackfinance.util.ImageUtil
import com.example.truetrackfinance.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ExpenseRepository"

/**
 * ExpenseRepository manages the core financial transactions lifecycle.
 * Handles Database CRUD, receipt photo deletion, and budget limit monitoring.
 */
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val budgetRepository: BudgetRepository,
    private val notificationHelper: NotificationHelper
) {
    // In-memory cache to prevent notification spam within a single session/month.
    // Format: "userId_categoryId_monthKey_threshold"
    private val notifiedThresholds = mutableSetOf<String>()

    /** Observe expenses with filter criteria (reverse-chronological). */
    fun observeFilteredExpenses(userId: Long, from: Long?, to: Long?, min: Double?, max: Double?, search: String?): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeFilteredExpenses(userId, from, to, min, max, search)

    fun observeAllExpenses(userId: Long): Flow<List<ExpenseWithCategory>> = expenseDao.observeAllExpenses(userId)

    /**
     * Requirement: Save Expense.
     * Inserts into Room DB and immediately triggers a budget audit.
     */
    suspend fun addExpense(expense: Expense): Long {
        Log.d(TAG, "Action: Adding new expense [${expense.description}] R${expense.amount}")
        val id = expenseDao.insertExpense(expense)
        
        // --- Logic: Automated budget auditing ---
        expense.categoryId?.let { checkBudgetAlerts(expense.userId, it) }
        
        return id
    }

    /** Updates an existing expense and re-audits the associated category limit. */
    suspend fun updateExpense(expense: Expense) {
        Log.i(TAG, "Action: Updating expense ID ${expense.id}")
        expenseDao.updateExpense(expense)
        expense.categoryId?.let { checkBudgetAlerts(expense.userId, it) }
    }

    /**
     * Requirement: Delete Expense + Photo.
     * Ensures private storage is cleaned up when an entry is removed.
     */
    suspend fun deleteExpense(expense: Expense) {
        Log.w(TAG, "Action: Deleting expense ID ${expense.id} - ${expense.description}")
        
        // Requirement: Image cleanup
        expense.receiptPhotoPath?.let { 
            Log.v(TAG, "Storage Audit: Deleting linked receipt photo file")
            ImageUtil.deleteReceiptImage(it) 
        }
        
        expenseDao.deleteExpense(expense)
    }

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    fun observeTotalSpentInMonth(userId: Long, start: Long, end: Long): Flow<Double> = 
        expenseDao.observeTotalSpentInMonth(userId, start, end)

    fun observeCategorySpending(userId: Long, from: Long, to: Long): Flow<List<CategorySpending>> =
        expenseDao.observeCategorySpending(userId, from, to)

    fun observeDailySpending(userId: Long, from: Long, to: Long): Flow<List<DailySpending>> =
        expenseDao.observeDailySpending(userId, from, to)

    suspend fun getDistinctDaysWithExpenses(userId: Long, from: Long): List<String> =
        expenseDao.getDistinctDaysWithExpenses(userId, from)

    /**
     * Requirement: Push notifications at 90% and 100% of each category limit.
     * Evaluates current spending against monthly plan and triggers system alerts if necessary.
     */
    private suspend fun checkBudgetAlerts(userId: Long, categoryId: Long) {
        val monthKey = DateUtil.currentMonthKey()
        val limit = budgetRepository.getLimitForCategory(userId, categoryId, monthKey) ?: return
        
        Log.v(TAG, "Security: Auditing budget for category $categoryId")
        
        // Aggregate total spent in this specific category for the current calendar month
        val spending = expenseDao.observeCategorySpending(
            userId, DateUtil.monthStart(monthKey), DateUtil.monthEnd(monthKey)
        ).first().find { it.categoryId == categoryId }?.totalSpent ?: 0.0

        val ratio = spending / limit
        val category = categoryDao.getCategoryById(categoryId)
        val categoryName = category?.name ?: "Unknown Category"

        // Requirement: Notifications at 90% and 100%
        // We use notifiedThresholds to ensure we don't spam the user every time they log an expense
        // after they've already crossed the threshold.
        if (ratio >= 1.0) {
            val key = "${userId}_${categoryId}_${monthKey}_100"
            if (!notifiedThresholds.contains(key)) {
                Log.w(TAG, "Budget Alert: $categoryName exceeded (Ratio: $ratio)")
                notificationHelper.sendBudgetAlert(categoryId, categoryName, isOverLimit = true)
                notifiedThresholds.add(key)
            }
        } else if (ratio >= 0.9) {
            val key = "${userId}_${categoryId}_${monthKey}_90"
            if (!notifiedThresholds.contains(key)) {
                Log.i(TAG, "Budget Alert: $categoryName near limit (Ratio: $ratio)")
                notificationHelper.sendBudgetAlert(categoryId, categoryName, isOverLimit = false)
                notifiedThresholds.add(key)
            }
        }
    }
}
