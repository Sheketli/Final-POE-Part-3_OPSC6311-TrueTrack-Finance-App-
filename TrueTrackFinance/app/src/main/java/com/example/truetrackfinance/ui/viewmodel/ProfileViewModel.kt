package com.example.truetrackfinance.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.User
import com.example.truetrackfinance.data.repository.ExpenseRepository
import com.example.truetrackfinance.data.repository.UserRepository
import com.example.truetrackfinance.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val user: LiveData<User?> = _userId
        .filter { it > 0 }
        .flatMapLatest { userRepository.observeUser(it) }
        .asLiveData(viewModelScope.coroutineContext)

    fun initialise(userId: Long) {
        _userId.value = userId
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = _userId.value
            if (userId > 0) {
                userRepository.getUserById(userId)?.let {
                    userRepository.updateUser(it.copy(biometricEnabled = enabled))
                }
            }
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = _userId.value
            if (userId > 0) {
                userRepository.getUserById(userId)?.let {
                    userRepository.updateUser(it.copy(notificationEnabled = enabled))
                }
            }
        }
    }

    /**
     * Requirement: Export all user expenses to a CSV file for data portability.
     */
    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val userId = _userId.value
            if (userId <= 0) return@launch
            
            Log.d(TAG, "Fetching all expenses for CSV export (User: $userId)")
            val expenses = expenseRepository.observeAllExpenses(userId).first()
            
            CsvExporter.exportExpenses(context, expenses)
        }
    }
}
