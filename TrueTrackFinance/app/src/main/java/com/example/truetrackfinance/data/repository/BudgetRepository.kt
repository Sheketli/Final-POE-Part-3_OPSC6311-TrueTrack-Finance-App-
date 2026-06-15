package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.BudgetDao
import com.example.truetrackfinance.data.db.entity.Budget
import com.example.truetrackfinance.data.db.entity.CategoryLimit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BudgetRepository"

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    suspend fun setBudgetForMonth(
        userId: Long, 
        monthKey: String, 
        totalGoal: Double, 
        totalIncome: Double = 0.0,
        minSpent: Double = 0.0,
        maxSpent: Double = 0.0
    ) {
        Log.d(TAG, "Setting budget for $monthKey: goal=$totalGoal, income=$totalIncome, min=$minSpent, max=$maxSpent")
        budgetDao.insertOrUpdateBudget(
            Budget(
                userId = userId, 
                monthKey = monthKey, 
                totalGoal = totalGoal, 
                totalIncome = totalIncome,
                minSpentGoal = minSpent,
                maxSpentGoal = maxSpent
            )
        )
    }

    fun observeBudgetForMonth(userId: Long, monthKey: String): Flow<Budget?> =
        budgetDao.observeBudgetForMonth(userId, monthKey)

    suspend fun getBudgetForMonth(userId: Long, monthKey: String): Budget? =
        budgetDao.getBudgetForMonth(userId, monthKey)

    suspend fun setCategoryLimit(userId: Long, categoryId: Long, monthKey: String, limit: Double) {
        Log.d(TAG, "Setting limit for category $categoryId in $monthKey: R$limit")
        budgetDao.insertOrUpdateCategoryLimit(
            CategoryLimit(userId = userId, categoryId = categoryId, monthKey = monthKey, limitAmount = limit)
        )
    }

    fun observeCategoryLimitsForMonth(userId: Long, monthKey: String): Flow<List<CategoryLimit>> =
        budgetDao.observeCategoryLimitsForMonth(userId, monthKey)

    suspend fun getCategoryLimitsForMonth(userId: Long, monthKey: String): List<CategoryLimit> =
        budgetDao.getCategoryLimitsForMonth(userId, monthKey)

    suspend fun getLimitForCategory(userId: Long, categoryId: Long, monthKey: String): Double? =
        budgetDao.getLimitForCategory(userId, categoryId, monthKey)
}
