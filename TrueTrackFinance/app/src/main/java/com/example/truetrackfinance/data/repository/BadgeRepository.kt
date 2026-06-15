package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.BadgeDao
import com.example.truetrackfinance.data.db.entity.Badge
import com.example.truetrackfinance.data.model.BadgeKey
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BadgeRepository"

@Singleton
class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao
) {
    fun observeBadges(userId: Long): Flow<List<Badge>> = badgeDao.observeBadges(userId)

    suspend fun getBadgesForUser(userId: Long): List<Badge> = badgeDao.getBadgesForUser(userId)

    /**
     * Award a badge if it has not already been earned.
     * Returns true if newly awarded, false if already held.
     */
    suspend fun awardBadgeIfNew(userId: Long, key: BadgeKey): Boolean {
        val existing = badgeDao.getBadgeByKey(userId, key.key)
        if (existing?.earnedAt != null) {
            Log.d(TAG, "Badge ${key.key} already earned by user $userId")
            return false
        }
        val now = System.currentTimeMillis()
        badgeDao.awardBadge(userId, key.key, now)
        Log.d(TAG, "Awarded badge ${key.key} to user $userId at $now")
        return true
    }

    /** Pre-seed all badge slots in locked state for a new user on registration. */
    suspend fun seedBadgesForUser(userId: Long) {
        val badges = BadgeKey.entries.map { key ->
            Badge(userId = userId, badgeKey = key.key, earnedAt = null)
        }
        badgeDao.insertBadges(badges)
        Log.d(TAG, "Seeded ${badges.size} locked badge slots for user $userId")
    }
}
