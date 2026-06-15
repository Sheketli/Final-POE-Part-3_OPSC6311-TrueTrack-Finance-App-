package com.example.truetrackfinance

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.truetrackfinance.data.repository.BudgetRepository
import com.example.truetrackfinance.data.repository.CategoryRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.ui.viewmodel.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { expenseRepository.observeTotalSpentInMonth(any(), any(), any()) } returns flowOf(2500.0)
        every { budgetRepository.observeBudgetForMonth(any(), any()) } returns flowOf(mockk(relaxed = true) {
            every { totalGoal } returns 8000.0
        })
        
        viewModel = DashboardViewModel(expenseRepository, budgetRepository, categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard initialization updates UI state`() = runTest {
        viewModel.initialise(1L)
        
        val state = viewModel.uiState.getOrAwaitValue()
        assertEquals(8000.0, state.totalBudget, 0.01)
        assertEquals(2500.0, state.totalSpent, 0.01)
        assertEquals(5500.0, state.remaining, 0.01)
    }
}
