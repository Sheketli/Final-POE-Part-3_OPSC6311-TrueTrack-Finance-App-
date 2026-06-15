package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import com.example.truetrackfinance.data.repository.BadgeRepository
import com.example.truetrackfinance.data.repository.CategoryRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.data.model.BadgeKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExpenseViewModel"

data class ExpenseFilter(
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val search: String? = null,
    val categoryIds: Set<Long> = emptySet()
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)
    private val _filter = MutableStateFlow(ExpenseFilter())
    val filter: LiveData<ExpenseFilter> = _filter.asLiveData()

    /** Newly awarded badge key (consumed by the UI to trigger confetti animation). */
    private val _newBadge = MutableSharedFlow<BadgeKey>(extraBufferCapacity = 1)
    val newBadge: SharedFlow<BadgeKey> = _newBadge.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: LiveData<List<ExpenseWithCategory>> = _userId
        .filter { it > 0 }
        .flatMapLatest { userId ->
            _filter.flatMapLatest { f ->
                expenseRepository.observeFilteredExpenses(
                    userId, f.fromDate, f.toDate, f.minAmount, f.maxAmount, f.search
                )
            }
        }
        .map { list ->
            val filterCats = _filter.value.categoryIds
            if (filterCats.isEmpty()) list
            else list.filter { it.categoryId in filterCats }
        }
        .asLiveData(viewModelScope.coroutineContext)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: LiveData<List<com.example.truetrackfinance.data.db.entity.Category>> = _userId
        .filter { it > 0 }
        .flatMapLatest { userId ->
            categoryRepository.observeCategories(userId)
        }
        .asLiveData(viewModelScope.coroutineContext)

    fun initialise(userId: Long) {
        _userId.value = userId
    }

    fun updateFilter(filter: ExpenseFilter) {
        _filter.value = filter
        Log.d(TAG, "Filter updated: $filter")
    }

    fun clearFilter() { _filter.value = ExpenseFilter() }

    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            val userId = _userId.value
            val isNew = expense.id == 0L
            if (isNew) {
                val id = expenseRepository.addExpense(expense)
                Log.d(TAG, "Saved new expense ID $id")
                checkAndAwardFirstLogBadge(userId)
            } else {
                expenseRepository.updateExpense(expense)
                Log.d(TAG, "Updated expense ID ${expense.id}")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            Log.d(TAG, "Deleted expense ID ${expense.id}")
        }
    }

    fun getExpenseById(id: Long): LiveData<Expense?> = liveData {
        emit(expenseRepository.getExpenseById(id))
    }

    private suspend fun checkAndAwardFirstLogBadge(userId: Long) {
        val awarded = badgeRepository.awardBadgeIfNew(userId, BadgeKey.FIRST_LOG)
        if (awarded) {
            Log.d(TAG, "First Log badge awarded to user $userId")
            _newBadge.emit(BadgeKey.FIRST_LOG)
        }
    }
}
