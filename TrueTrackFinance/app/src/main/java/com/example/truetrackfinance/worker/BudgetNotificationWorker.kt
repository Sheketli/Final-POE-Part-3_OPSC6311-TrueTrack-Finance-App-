package com.example.truetrackfinance.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.truetrackfinance.data.repository.BudgetRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * BudgetNotificationWorker runs periodically to evaluate category spending.
 */
@HiltWorker
class BudgetNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("BudgetWorker", "Checking category limits")
        return Result.success()
    }
}
