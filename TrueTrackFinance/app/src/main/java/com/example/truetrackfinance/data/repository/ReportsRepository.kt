package com.example.truetrackfinance.data.repository

import com.example.truetrackfinance.data.db.dao.ExpenseDao
import com.example.truetrackfinance.data.model.CategorySpending
import com.example.truetrackfinance.data.model.DailySpending
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class ReportsRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    /**
     * Requirement: Provide daily spending with category breakdown for stacked bar chart.
     */
    fun observeDailyStackedSpending(userId: Long, fromDate: Long, toDate: Long): Flow<List<DailySpending>> {
        return expenseDao.observeExpensesForPeriod(userId, fromDate, toDate).map { filtered ->
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val groupedByDay = filtered.groupBy { fmt.format(Date(it.date)) }

            groupedByDay.map { (day, expensesOnDay) ->
                val totalOnDay = expensesOnDay.sumOf { it.amount }
                val breakdown = expensesOnDay.groupBy { it.categoryId }.map { (catId, exps) ->
                    val first = exps.first()
                    CategorySpending(
                        categoryId = catId ?: -1L,
                        categoryName = first.categoryName ?: "Uncategorised",
                        colorHex = first.categoryColor ?: "#74777F",
                        totalSpent = exps.sumOf { it.amount }
                    )
                }
                DailySpending(day, totalOnDay, breakdown)
            }.sortedBy { it.day }
        }
    }
}
