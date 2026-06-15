package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.data.model.CategorySpending
import com.example.truetrackfinance.data.model.DailySpending
import com.example.truetrackfinance.data.repository.BudgetRepository
import com.example.truetrackfinance.data.repository.CategoryRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.data.repository.ReportsRepository
import com.example.truetrackfinance.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ReportsViewModel"

enum class ReportPeriod { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, THIS_YEAR, CUSTOM }

data class ReportsUiState(
    val categorySpending: List<CategorySpending> = emptyList(),
    val dailySpending: List<DailySpending> = emptyList(),
    val categoryProgress: List<CategoryProgress> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalTransactionCount: Int = 0,
    val dailyBudgetTarget: Double = 0.0,
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val fromDate: Long = DateUtil.monthStart(DateUtil.currentMonthKey()),
    val toDate: Long = DateUtil.monthEnd(DateUtil.currentMonthKey()),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)
    private val _period = MutableStateFlow(
        Triple(ReportPeriod.THIS_MONTH,
            DateUtil.monthStart(DateUtil.currentMonthKey()),
            DateUtil.monthEnd(DateUtil.currentMonthKey()))
    )

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: LiveData<ReportsUiState> = _uiState.asLiveData()

    fun initialise(userId: Long) {
        if (_userId.value == userId) return
        _userId.value = userId
        observeReportsData()
    }

    fun setPeriod(period: ReportPeriod, from: Long? = null, to: Long? = null) {
        val cal = java.util.Calendar.getInstance()
        val (fromDate, toDate) = when (period) {
            ReportPeriod.THIS_MONTH -> {
                val mk = DateUtil.currentMonthKey()
                DateUtil.monthStart(mk) to DateUtil.monthEnd(mk)
            }
            ReportPeriod.LAST_MONTH -> {
                cal.add(java.util.Calendar.MONTH, -1)
                val fmt = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
                val mk = fmt.format(cal.time)
                DateUtil.monthStart(mk) to DateUtil.monthEnd(mk)
            }
            ReportPeriod.LAST_3_MONTHS -> {
                DateUtil.daysAgoStart(90) to DateUtil.todayEnd()
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.timeInMillis to DateUtil.todayEnd()
            }
            ReportPeriod.CUSTOM -> (from ?: DateUtil.daysAgoStart(30)) to (to ?: DateUtil.todayEnd())
        }
        _period.value = Triple(period, fromDate, toDate)
        Log.d(TAG, "Period set to $period: $fromDate .. $toDate")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeReportsData() {
        viewModelScope.launch {
            _period.flatMapLatest { (period, fromDate, toDate) ->
                val userId = _userId.value
                val monthKey = DateUtil.currentMonthKey()
                
                combine(
                    expenseRepository.observeCategorySpending(userId, fromDate, toDate),
                    reportsRepository.observeDailyStackedSpending(userId, fromDate, toDate),
                    budgetRepository.observeCategoryLimitsForMonth(userId, monthKey),
                    categoryRepository.observeCategories(userId),
                    budgetRepository.observeBudgetForMonth(userId, monthKey),
                    expenseRepository.observeAllExpenses(userId)
                ) { flows ->
                    @Suppress("UNCHECKED_CAST")
                    val cats = flows[0] as List<CategorySpending>
                    @Suppress("UNCHECKED_CAST")
                    val daily = flows[1] as List<DailySpending>
                    @Suppress("UNCHECKED_CAST")
                    val limits = flows[2] as List<com.example.truetrackfinance.data.db.entity.CategoryLimit>
                    @Suppress("UNCHECKED_CAST")
                    val allCats = flows[3] as List<com.example.truetrackfinance.data.db.entity.Category>
                    @Suppress("UNCHECKED_CAST")
                    val budget = flows[4] as com.example.truetrackfinance.data.db.entity.Budget?
                    @Suppress("UNCHECKED_CAST")
                    val expenses = flows[5] as List<com.example.truetrackfinance.data.model.ExpenseWithCategory>
                    
                    val filteredExpenses = expenses.filter { it.date in fromDate..toDate }
                    val limitMap = limits.associate { it.categoryId to it.limitAmount }
                    val spendingMap = cats.associate { it.categoryId to it.totalSpent }
                    
                    val progress = allCats.map { cat ->
                        CategoryProgress(
                            categoryId = cat.id,
                            categoryName = cat.name,
                            colorHex = cat.colorHex,
                            emoji = cat.emoji,
                            limitAmount = limitMap[cat.id] ?: 0.0,
                            spentAmount = spendingMap[cat.id] ?: 0.0
                        )
                    }

                    val totalBudget = budget?.totalGoal ?: 0.0
                    val daysInMonth = DateUtil.daysInMonth(monthKey)
                    val dailyTarget = if (daysInMonth > 0) totalBudget / daysInMonth else 0.0

                    ReportsUiState(
                        categorySpending = cats,
                        dailySpending = daily,
                        categoryProgress = progress,
                        totalSpent = cats.sumOf { it.totalSpent },
                        totalTransactionCount = filteredExpenses.size,
                        dailyBudgetTarget = dailyTarget,
                        period = period,
                        fromDate = fromDate,
                        toDate = toDate,
                        isLoading = false
                    )
                }
            }.catch { e -> Log.e(TAG, "Reports flow error", e) }
             .collect { _uiState.value = it }
        }
    }
}
