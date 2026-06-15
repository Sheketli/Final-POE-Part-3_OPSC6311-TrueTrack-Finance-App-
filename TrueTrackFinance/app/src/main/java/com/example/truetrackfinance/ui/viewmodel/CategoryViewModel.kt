package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.Budget
import com.example.truetrackfinance.data.db.entity.Category
import com.example.truetrackfinance.data.db.entity.CategoryLimit
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

private const val TAG = "CategoryViewModel"

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: LiveData<List<Category>> = _userId
        .filter { it > 0 }
        .flatMapLatest { categoryRepository.observeCategories(it) }
        .asLiveData(viewModelScope.coroutineContext)

    /**
     * Requirement: Categories with real-time progress (Spent vs Limit) for the management UI.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithProgress: LiveData<List<CategoryProgress>> = _userId
        .filter { it > 0 }
        .flatMapLatest { userId ->
            val monthKey = DateUtil.currentMonthKey()
            val start = DateUtil.monthStart(monthKey)
            val end = DateUtil.monthEnd(monthKey)
            
            combine(
                categoryRepository.observeCategories(userId),
                budgetRepository.observeCategoryLimitsForMonth(userId, monthKey),
                expenseRepository.observeCategorySpending(userId, start, end)
            ) { cats, limits, spending ->
                val limitMap = limits.associate { it.categoryId to it.limitAmount }
                val spendingMap = spending.associate { it.categoryId to it.totalSpent }
                
                cats.map { cat ->
                    CategoryProgress(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        colorHex = cat.colorHex,
                        emoji = cat.emoji,
                        limitAmount = limitMap[cat.id] ?: 0.0,
                        spentAmount = spendingMap[cat.id] ?: 0.0
                    )
                }
            }
        }.asLiveData(viewModelScope.coroutineContext)

    /** Set when the delete confirmation dialog needs the affected expense count. */
    private val _deletePreview = MutableStateFlow<Pair<Category, Int>?>(null)
    val deletePreview: LiveData<Pair<Category, Int>?> = _deletePreview.asLiveData()

    /** Current month's budget data for allocation. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBudget: LiveData<Budget?> = _userId
        .filter { it > 0 }
        .flatMapLatest { budgetRepository.observeBudgetForMonth(it, DateUtil.currentMonthKey()) }
        .asLiveData(viewModelScope.coroutineContext)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryLimits: LiveData<List<CategoryLimit>> = _userId
        .filter { it > 0 }
        .flatMapLatest { budgetRepository.observeCategoryLimitsForMonth(it, DateUtil.currentMonthKey()) }
        .asLiveData(viewModelScope.coroutineContext)

    fun initialise(userId: Long) { _userId.value = userId }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.addCategory(category)
            Log.i(TAG, "Added category: ${category.name}")
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            Log.d(TAG, "Updated category ID ${category.id}: ${category.name}")
        }
    }

    /** Prepare delete: load expense count for confirmation dialog. */
    fun prepareDelete(catProgress: CategoryProgress) {
        viewModelScope.launch {
            // Fetch the underlying Category entity
            val categoriesList = categoryRepository.observeCategories(_userId.value).first()
            val category = categoriesList.find { it.id == catProgress.categoryId } ?: return@launch

            val count = categoryRepository.countExpensesForCategory(category.id)
            _deletePreview.value = Pair(category, count)
        }
    }

    fun confirmDelete(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
            _deletePreview.value = null
            Log.w(TAG, "Deleted category: ${category.name}")
        }
    }

    fun cancelDelete() { _deletePreview.value = null }

    fun updateSortOrder(orderedIds: List<Long>) {
        viewModelScope.launch {
            categoryRepository.updateSortOrders(orderedIds)
            Log.d(TAG, "Updated sort order for ${orderedIds.size} categories")
        }
    }

    /**
     * Set the expected income and spending goals for the current month.
     */
    fun setMonthlyGoals(income: Double, minSpent: Double, maxSpent: Double) {
        viewModelScope.launch {
            val userId = _userId.value
            val monthKey = DateUtil.currentMonthKey()
            budgetRepository.setBudgetForMonth(
                userId = userId, 
                monthKey = monthKey, 
                totalGoal = maxSpent, // Use max as totalGoal for gauge
                totalIncome = income,
                minSpent = minSpent,
                maxSpent = maxSpent
            )
            Log.i(TAG, "Monthly goals saved: Income=$income, Min=$minSpent, Max=$maxSpent")
        }
    }

    /**
     * Save the spending limits for all categories.
     */
    fun saveCategoryLimits(limits: Map<Long, Double>) {
        viewModelScope.launch {
            val userId = _userId.value
            val monthKey = DateUtil.currentMonthKey()
            limits.forEach { (catId, limit) ->
                budgetRepository.setCategoryLimit(userId, catId, monthKey, limit)
            }
            Log.i(TAG, "Saved limits for ${limits.size} categories")
        }
    }
}
