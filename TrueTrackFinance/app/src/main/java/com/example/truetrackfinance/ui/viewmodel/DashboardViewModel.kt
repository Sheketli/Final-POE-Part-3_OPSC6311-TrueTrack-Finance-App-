package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.data.repository.BudgetRepository
import com.example.truetrackfinance.data.repository.CategoryRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

/**
 * UI State representing all dynamic data on the Home screen.
 */
data class DashboardUiState(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val dailyAllowance: Double = 0.0,
    val currentStreak: Int = 0,
    val categoryProgress: List<CategoryProgress> = emptyList(),
    val hasUnallocatedFunds: Boolean = false,
    val unallocatedAmount: Double = 0.0,
    val isLoading: Boolean = true
) {
    /** Fraction for the circular budget ring (0.0 to 1.0). */
    val progressFraction: Float
        get() = if (totalBudget <= 0) 0f else (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)
    
    val remaining: Double get() = totalBudget - totalSpent
}

/**
 * DashboardViewModel manages the core financial logic for the Home screen.
 * Implements: Budget Ring, Daily Allowance, Zero-Based Unallocated Banner, and Streak counter.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: LiveData<DashboardUiState> = _uiState.asLiveData()

    /**
     * Initialise the dashboard for a specific user ID.
     */
    fun initialise(userId: Long) {
        if (_userId.value == userId) return
        _userId.value = userId
        observeDashboardData(userId)
        Log.d(TAG, "Dashboard Logic: Initialising data streams for user ID $userId")
    }

    /**
     * Requirement: Progress Home Dashboard (Updates in real time).
     * Combines multiple database flows into a single unified UI state.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDashboardData(userId: Long) {
        val monthKey = DateUtil.currentMonthKey()
        val monthStart = DateUtil.monthStart(monthKey)
        val monthEnd = DateUtil.monthEnd(monthKey)

        viewModelScope.launch {
            // Requirement: Updates in real time as expenses are logged.
            // Using Flow.combine to react to any change in any of the 5 sources.
            combine(
                expenseRepository.observeTotalSpentInMonth(userId, monthStart, monthEnd),
                budgetRepository.observeBudgetForMonth(userId, monthKey),
                expenseRepository.observeCategorySpending(userId, monthStart, monthEnd),
                budgetRepository.observeCategoryLimitsForMonth(userId, monthKey),
                categoryRepository.observeCategories(userId)
            ) { totalSpent, budget, categorySpending, limits, categories ->

                Log.v(TAG, "Dashboard Analytics: Recalculating metrics for $monthKey")

                val totalBudget = budget?.totalGoal ?: 0.0
                val totalIncome = budget?.totalIncome ?: 0.0
                
                // Requirement: Daily Allowance Indicator logic
                val remainingDays = DateUtil.remainingDaysInMonth()
                val dailyAllowance = if (remainingDays > 0)
                    (totalBudget - totalSpent) / remainingDays else 0.0
                Log.v(TAG, "Metric: Daily Allowance calculated as R$dailyAllowance")

                // Map raw data to UI progress models
                val spendingMap = categorySpending.associate { it.categoryId to it.totalSpent }
                val limitMap = limits.associate { it.categoryId to it.limitAmount }
                
                val progressList = categories.map { cat ->
                    CategoryProgress(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        colorHex = cat.colorHex,
                        emoji = cat.emoji,
                        limitAmount = limitMap[cat.id] ?: 0.0,
                        spentAmount = spendingMap[cat.id] ?: 0.0
                    )
                }.sortedByDescending { it.spentAmount }

                // Requirement: Zero-Based Budgeting Framework unallocated funds check.
                val totalAllocated = limitMap.values.sum()
                val unallocated = totalIncome - totalAllocated

                // Requirement: Streak Logic integration
                val streak = calculateStreak(userId)

                DashboardUiState(
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                    totalIncome = totalIncome,
                    dailyAllowance = dailyAllowance,
                    currentStreak = streak,
                    categoryProgress = progressList,
                    hasUnallocatedFunds = Math.abs(unallocated) > 0.01,
                    unallocatedAmount = unallocated,
                    isLoading = false
                )
            }.catch { e ->
                Log.e(TAG, "Critical Fault: Error combining dashboard data flows", e)
            }.collect { state ->
                _uiState.value = state
                Log.i(TAG, "UI Update: Home dashboard refreshed successfully")
            }
        }
    }

    /**
     * Requirement: Gamification Streaks logic.
     * Checks how many consecutive days have transactions logged.
     */
    private suspend fun calculateStreak(userId: Long): Int {
        val thirtyDaysAgo = DateUtil.daysAgoStart(30)
        val days = expenseRepository.getDistinctDaysWithExpenses(userId, thirtyDaysAgo)
        
        if (days.isEmpty()) {
            Log.v(TAG, "Streak Logic: No recent logs found")
            return 0
        }

        var streak = 0
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        for (i in 0 until 30) {
            val expectedDay = fmt.format(java.util.Date(DateUtil.todayStart() - i * 86_400_000L))
            if (days.contains(expectedDay)) {
                streak++
            } else if (i == 0) {
                // If they haven't logged today yet, the streak remains from yesterday
                continue 
            } else {
                break // Sequence broken
            }
        }
        
        Log.d(TAG, "Streak Analytics: User has a $streak day logging streak")
        return streak
    }
}
