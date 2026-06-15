package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    /** Increment failed login attempts and set lockout timestamp if limit reached. */
    @Query("""
        UPDATE users 
        SET failed_attempts = failed_attempts + 1,
            locked_until = CASE WHEN failed_attempts + 1 >= 3 
                           THEN :lockUntil ELSE locked_until END
        WHERE username = :username
    """)
    suspend fun incrementFailedAttempts(username: String, lockUntil: Long)

    /** Reset failed attempts on successful login and update last_active. */
    @Query("""
        UPDATE users 
        SET failed_attempts = 0, locked_until = 0, last_active = :timestamp
        WHERE username = :username
    """)
    suspend fun resetFailedAttempts(username: String, timestamp: Long)

    @Query("UPDATE users SET biometric_enabled = :enabled WHERE id = :userId")
    suspend fun setBiometricEnabled(userId: Long, enabled: Boolean)

    @Query("UPDATE users SET dark_mode = :enabled WHERE id = :userId")
    suspend fun setDarkMode(userId: Long, enabled: Boolean)

    @Query("UPDATE users SET notification_enabled = :enabled WHERE id = :userId")
    suspend fun setNotificationEnabled(userId: Long, enabled: Boolean)

    @Query("UPDATE users SET last_active = :timestamp WHERE id = :userId")
    suspend fun updateLastActive(userId: Long, timestamp: Long)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: Long): Flow<User?>

    @Query("SELECT id FROM users")
    suspend fun getAllUserIds(): List<Long>
}
