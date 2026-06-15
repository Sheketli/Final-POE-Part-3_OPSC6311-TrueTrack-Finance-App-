package com.example.truetrackfinance.util

import android.util.Log
import com.example.truetrackfinance.data.model.BadgeKey
import com.example.truetrackfinance.data.repository.BadgeRepository
import com.example.truetrackfinance.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BadgeEngine"

/**
 * BadgeEngine evaluates financial gamification rules to award achievement badges.
 */
@Singleton
class BadgeEngine @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val badgeRepository: BadgeRepository
) {

    /**
     * Evaluate rules for a specific user and return newly earned badges.
     */
    suspend fun evaluate(userId: Long): List<BadgeKey> {
        Log.d(TAG, "Evaluating achievements for user ID: $userId")
        val newlyAwarded = mutableListOf<BadgeKey>()

        // 1. Streak Logic (7 and 30 days)
        val streak = calculateCurrentStreak(userId)
        if (streak >= 7) awardIfNew(userId, BadgeKey.STREAK_7, newlyAwarded)
        if (streak >= 30) awardIfNew(userId, BadgeKey.STREAK_30, newlyAwarded)

        Log.i(TAG, "Evaluation complete. ${newlyAwarded.size} new badges earned.")
        return newlyAwarded
    }

    private suspend fun awardIfNew(userId: Long, key: BadgeKey, list: MutableList<BadgeKey>) {
        val wasAwarded = badgeRepository.awardBadgeIfNew(userId, key)
        if (wasAwarded) {
            Log.i(TAG, "Badge Awarded: ${key.displayName}")
            list.add(key)
        }
    }

    private suspend fun calculateCurrentStreak(userId: Long): Int {
        val thirtyDaysAgo = DateUtil.daysAgoStart(30)
        val daysWithLogs = expenseRepository.getDistinctDaysWithExpenses(userId, thirtyDaysAgo)
        if (daysWithLogs.isEmpty()) return 0

        var streak = 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = DateUtil.todayStart()

        for (i in 0 until 31) {
            val checkDay = fmt.format(Date(today - (i * 86_400_000L)))
            if (daysWithLogs.contains(checkDay)) {
                streak++
            } else if (i == 0) {
                // If they haven't logged today yet, the streak is still active if they logged yesterday
                continue 
            } else {
                break
            }
        }
        return streak
    }
}
