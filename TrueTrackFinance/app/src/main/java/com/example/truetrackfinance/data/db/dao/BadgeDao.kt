package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.Badge
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: Badge): Long

    @Query("SELECT * FROM badges WHERE user_id = :userId ORDER BY earned_at DESC")
    fun observeBadges(userId: Long): Flow<List<Badge>>

    @Query("SELECT * FROM badges WHERE user_id = :userId")
    suspend fun getBadgesForUser(userId: Long): List<Badge>

    @Query("SELECT * FROM badges WHERE user_id = :userId AND badge_key = :key LIMIT 1")
    suspend fun getBadgeByKey(userId: Long, key: String): Badge?

    /** Award a badge: sets earned_at to current time. */
    @Query("UPDATE badges SET earned_at = :timestamp WHERE user_id = :userId AND badge_key = :key")
    suspend fun awardBadge(userId: Long, key: String, timestamp: Long)

    /** Pre-seed all badge slots (locked) for a new user. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<Badge>)
}
