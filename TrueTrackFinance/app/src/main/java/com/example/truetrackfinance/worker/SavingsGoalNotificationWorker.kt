package com.example.truetrackfinance.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.truetrackfinance.data.repository.SavingsRepository
import com.example.truetrackfinance.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SavingsGoalNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val savingsRepository: SavingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("SavingsWorker", "Checking savings milestones")
        return Result.success()
    }
}
