package com.example.truetrackfinance.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.truetrackfinance.data.repository.CurrencyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ExchangeRateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currencyRepository: CurrencyRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val success = currencyRepository.syncRates()
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "ExchangeRateSyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ExchangeRateWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
