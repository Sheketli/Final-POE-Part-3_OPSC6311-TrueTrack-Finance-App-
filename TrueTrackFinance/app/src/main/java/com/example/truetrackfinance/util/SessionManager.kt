package com.example.truetrackfinance.util

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "ttf_session_secure_prefs"
private const val KEY_USER_ID = "active_user_id"
private const val KEY_USER_NAME = "active_user_name"
private const val KEY_DB_PASSPHRASE = "db_passphrase_key"

/**
 * SessionManager handles active user sessions and secure credential storage.
 * Uses AES-256 via EncryptedSharedPreferences for hardware-backed security.
 */
@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Store the session for a successfully authenticated user. */
    fun saveSession(userId: Long, fullName: String) {
        Log.i("SessionManager", "Saving session for user ID: $userId")
        sharedPreferences.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, fullName)
            .apply()
    }

    /** Returns true if a user is currently logged in. */
    fun isLoggedIn(): Boolean = sharedPreferences.getLong(KEY_USER_ID, -1L) != -1L

    /** Retrieve the current active user ID. */
    fun getActiveUserId(): Long = sharedPreferences.getLong(KEY_USER_ID, -1L)

    /** Retrieve the display name (Full Name) of the active user. */
    fun getActiveUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)

    /** Clears session data on logout. */
    fun clearSession() {
        Log.d("SessionManager", "Clearing active session")
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .apply()
    }

    /**
     * Retrieves or generates a high-entropy 256-bit key for SQLCipher encryption.
     * This key is stored in the encrypted shared preferences vault.
     */
    fun getOrCreateDbPassphrase(): ByteArray {
        val stored = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        return if (stored != null) {
            Base64.decode(stored, Base64.DEFAULT)
        } else {
            val entropy = ByteArray(32)
            SecureRandom().nextBytes(entropy)
            val encoded = Base64.encodeToString(entropy, Base64.DEFAULT)
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
            Log.i("SessionManager", "Generated new DB encryption passphrase")
            entropy
        }
    }
}
