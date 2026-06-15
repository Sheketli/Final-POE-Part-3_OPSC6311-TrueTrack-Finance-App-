package com.example.truetrackfinance.data.repository

import android.util.Log
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.truetrackfinance.data.db.dao.UserDao
import com.example.truetrackfinance.data.db.entity.User
import com.example.truetrackfinance.util.SessionManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"
private const val LOCK_DURATION_MS = 60_000L  // 60 seconds

/**
 * Result types for login attempts to ensure clean UI state handling.
 */
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Failed(val attemptsRemaining: Int) : LoginResult()
    data class Locked(val lockedUntilMs: Long) : LoginResult()
    object UserNotFound : LoginResult()
}

/**
 * Result types for registration to ensure specific error feedback.
 */
sealed class RegisterResult {
    data class Success(val userId: Long) : RegisterResult()
    object UsernameTaken : RegisterResult()
    object EmailTaken : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

/**
 * UserRepository coordinates authentication and user profile operations.
 * Implements security requirements: bcrypt hashing and failure-based account lockout.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    /**
     * Requirement: User Registration.
     * Securely registers a new user after verifying unique constraints.
     */
    suspend fun register(fullName: String, username: String, email: String, password: String): RegisterResult {
        Log.d(TAG, "Attempting to register user: $username ($fullName)")

        // 1. Check for existing credentials to prevent duplicates
        if (userDao.getUserByUsername(username) != null) {
            Log.w(TAG, "Registration aborted: Username '$username' already exists in Room DB")
            return RegisterResult.UsernameTaken
        }
        if (userDao.getUserByEmail(email) != null) {
            Log.w(TAG, "Registration aborted: Email '$email' already exists in Room DB")
            return RegisterResult.EmailTaken
        }

        // 2. Security: Hash password with salt using bcrypt before DB insert
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        Log.v(TAG, "Password successfully hashed with bcrypt (cost factor 12)")
        
        val user = User(fullName = fullName, username = username, email = email, passwordHash = hash)

        return try {
            val id = userDao.insertUser(user)
            Log.i(TAG, "DB Success: New user created with ID $id")
            RegisterResult.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Critical DB error during registration", e)
            RegisterResult.Error(e.message ?: "Database insertion failed")
        }
    }

    /**
     * Requirement: User Login.
     * Authenticates credentials and enforces the 3-attempt lockout security policy.
     */
    suspend fun login(username: String, password: String): LoginResult {
        Log.d(TAG, "Audit: Login attempt started for $username")
        
        // 1. Dual-identifier lookup (Username or Email supported)
        val user = userDao.getUserByUsername(username) 
            ?: userDao.getUserByEmail(username)
            ?: run {
                Log.w(TAG, "Audit: User '$username' not found in database")
                return LoginResult.UserNotFound
            }

        // 2. Security Check: Is account currently locked?
        val now = System.currentTimeMillis()
        if (user.lockedUntil > now) {
            val wait = (user.lockedUntil - now) / 1000
            Log.w(TAG, "Security Gate: Account locked. Remaining: $wait seconds")
            return LoginResult.Locked(user.lockedUntil)
        }

        // 3. Password Verification
        val passwordMatches = BCrypt.verifyer()
            .verify(password.toCharArray(), user.passwordHash).verified

        if (!passwordMatches) {
            // Failure logic: increment count and potentially lock
            val nextFailed = user.failedAttempts + 1
            val lockUntil = if (nextFailed >= 3) now + LOCK_DURATION_MS else 0L
            
            userDao.incrementFailedAttempts(user.username, lockUntil)
            
            val remaining = (3 - nextFailed).coerceAtLeast(0)
            Log.w(TAG, "Security Event: Incorrect password. Failure #$nextFailed. Remaining attempts: $remaining")
            return LoginResult.Failed(remaining)
        }

        // 4. Success logic: Reset security counters and persist session
        userDao.resetFailedAttempts(user.username, now)
        sessionManager.saveSession(user.id, user.fullName)
        Log.i(TAG, "Audit: Authentication successful for user ID ${user.id}")
        return LoginResult.Success(user)
    }

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)

    /** Observe user changes (Hilt + Flow) for live UI updates like the Dashboard greeting. */
    fun observeUser(userId: Long): Flow<User?> = userDao.observeUser(userId)

    suspend fun updateUser(user: User) {
        Log.v(TAG, "Action: Updating user profile in database")
        userDao.updateUser(user)
    }

    fun logout() {
        sessionManager.clearSession()
        Log.i(TAG, "Action: User logged out - clearing session tokens")
    }

    suspend fun updateLastActive(userId: Long) {
        // Essential for the 60-second inactivity biometric gate
        userDao.updateLastActive(userId, System.currentTimeMillis())
    }
}
