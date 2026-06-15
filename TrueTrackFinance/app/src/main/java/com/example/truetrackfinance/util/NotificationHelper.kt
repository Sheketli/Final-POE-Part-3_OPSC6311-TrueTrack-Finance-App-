package com.example.truetrackfinance.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.truetrackfinance.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_BUDGET = "budget_alerts"
private const val CHANNEL_SAVINGS = "savings_milestones"
private const val CHANNEL_RECURRING = "recurring_transactions"

/**
 * NotificationHelper handles sending system notifications for budget alerts and milestones.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                context.getString(R.string.notification_channel_budget),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            
            val savingsChannel = NotificationChannel(
                CHANNEL_SAVINGS,
                context.getString(R.string.notification_channel_savings),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING,
                context.getString(R.string.notification_channel_recurring),
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannels(listOf(budgetChannel, savingsChannel, recurringChannel))
        }
    }

    /** Send an alert when a category reaches 90% or 100% of its limit. */
    fun sendBudgetAlert(categoryId: Long, categoryName: String, isOverLimit: Boolean) {
        val title = if (isOverLimit) "Budget Exceeded!" else "Budget Warning"
        val message = if (isOverLimit) 
            context.getString(R.string.notif_budget_100, categoryName)
        else 
            context.getString(R.string.notif_budget_90, categoryName)

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(categoryId.toInt(), builder.build())
    }

    /** Send a notification when a savings goal milestone is reached. */
    fun sendSavingsMilestone(goalName: String, percent: Int) {
        val title = "Savings Milestone!"
        val message = context.getString(R.string.notif_savings_milestone, goalName, percent)

        val builder = NotificationCompat.Builder(context, CHANNEL_SAVINGS)
            .setSmallIcon(R.drawable.ic_savings)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /** Send a reminder 24 hours before a recurring transaction is logged. */
    fun sendRecurringReminder(expense: com.example.truetrackfinance.data.db.entity.Expense) {
        val title = "Recurring Log Tomorrow"
        val message = context.getString(R.string.notif_recurring_due, expense.description, expense.amount)

        val builder = NotificationCompat.Builder(context, CHANNEL_RECURRING)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(expense.id.toInt(), builder.build())
    }
}
