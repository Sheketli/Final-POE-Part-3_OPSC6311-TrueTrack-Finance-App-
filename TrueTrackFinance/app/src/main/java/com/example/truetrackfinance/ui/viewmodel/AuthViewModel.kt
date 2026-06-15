package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.User
import com.example.truetrackfinance.data.repository.*
import com.example.truetrackfinance.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthViewModel"

/** UI state for the Auth screen. */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoginSuccess(val userId: Long) : AuthState()
    data class LoginFailed(val attemptsRemaining: Int) : AuthState()
    data class AccountLocked(val lockedUntilMs: Long) : AuthState()
    object UserNotFound : AuthState()
    data class RegisterSuccess(val userId: Long) : AuthState()
    object UsernameTaken : AuthState()
    object EmailTaken : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val badgeRepository: BadgeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState.asLiveData()

    /** Check if there is already an active session. */
    fun hasActiveSession(): Boolean = sessionManager.isLoggedIn()

    fun getActiveUserId(): Long = sessionManager.getActiveUserId()

    /** Get current active user details as LiveData for personalized UI. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActiveUser(): LiveData<User?> {
        val userId = sessionManager.getActiveUserId()
        Log.v(TAG, "Observing data for active user: $userId")
        return userRepository.observeUser(userId).asLiveData()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "login() called for: $username")
            _authState.value = when (val result = userRepository.login(username.trim(), password)) {
                is LoginResult.Success -> AuthState.LoginSuccess(result.user.id)
                is LoginResult.Failed  -> AuthState.LoginFailed(result.attemptsRemaining)
                is LoginResult.Locked  -> AuthState.AccountLocked(result.lockedUntilMs)
                is LoginResult.UserNotFound -> AuthState.UserNotFound
            }
        }
    }

    /** Register with expanded parameters to support Full Name and Username/Email distinction. */
    fun register(fullName: String, username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "register() called for name: $fullName, handle: $username")
            val result = userRepository.register(fullName, username.trim(), email.trim(), password)
            when (result) {
                is RegisterResult.Success -> {
                    // Seed default categories and badge slots for the new user
                    categoryRepository.seedDefaultCategories(result.userId)
                    badgeRepository.seedBadgesForUser(result.userId)
                    // Auto-login after registration
                    sessionManager.saveSession(result.userId, fullName)
                    _authState.value = AuthState.RegisterSuccess(result.userId)
                }
                is RegisterResult.UsernameTaken -> _authState.value = AuthState.UsernameTaken
                is RegisterResult.EmailTaken    -> _authState.value = AuthState.EmailTaken
                is RegisterResult.Error         -> _authState.value = AuthState.Error(result.message)
            }
        }
    }

    fun logout() {
        userRepository.logout()
        _authState.value = AuthState.Idle
    }

    fun updateActivityTimestamp(userId: Long) {
        viewModelScope.launch {
            userRepository.updateLastActive(userId)
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }
}
