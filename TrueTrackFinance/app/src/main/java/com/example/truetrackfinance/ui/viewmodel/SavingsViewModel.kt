package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.AnnualEnvelope
import com.example.truetrackfinance.data.db.entity.SavingsGoal
import com.example.truetrackfinance.data.repository.BadgeRepository
import com.example.truetrackfinance.data.model.BadgeKey
import com.example.truetrackfinance.data.repository.SavingsRepository
import com.example.truetrackfinance.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SavingsViewModel"

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val badgeRepository: BadgeRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val savingsGoals: LiveData<List<SavingsGoal>> = _userId
        .filter { it > 0 }
        .flatMapLatest { savingsRepository.observeSavingsGoals(it) }
        .asLiveData(viewModelScope.coroutineContext)

    @OptIn(ExperimentalCoroutinesApi::class)
    val annualEnvelopes: LiveData<List<AnnualEnvelope>> = _userId
        .filter { it > 0 }
        .flatMapLatest { savingsRepository.observeAnnualEnvelopes(it) }
        .asLiveData(viewModelScope.coroutineContext)

    private val _newBadge = MutableSharedFlow<BadgeKey>(extraBufferCapacity = 1)
    val newBadge: SharedFlow<BadgeKey> = _newBadge.asSharedFlow()

    fun initialise(userId: Long) { _userId.value = userId }

    fun addGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsRepository.addSavingsGoal(goal)
            Log.d(TAG, "Added savings goal: ${goal.name}")
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch { savingsRepository.updateSavingsGoal(goal) }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsRepository.deleteSavingsGoal(goal)
            Log.d(TAG, "Deleted savings goal ID ${goal.id}")
        }
    }

    /**
     * Log a manual contribution and check for milestone notifications.
     */
    fun addContribution(goalId: Long, amount: Double) {
        viewModelScope.launch {
            val goals = savingsGoals.value ?: return@launch
            val goal = goals.find { it.id == goalId } ?: return@launch
            
            val oldPercent = (goal.currentAmount / goal.targetAmount * 100).toInt()
            val newAmount = goal.currentAmount + amount
            val newPercent = (newAmount / goal.targetAmount * 100).toInt()
            
            Log.i(TAG, "Action: Contribution of R$amount to '${goal.name}'. Progress: $oldPercent% -> $newPercent%")
            
            savingsRepository.addContribution(goalId, amount)

            // --- Requirement: Notifications at 25%, 50%, 75%, and 100% ---
            checkAndSendMilestone(goal.name, oldPercent, newPercent)

            // Award Saver badge if 100% reached
            if (newPercent >= 100) {
                val awarded = badgeRepository.awardBadgeIfNew(_userId.value, BadgeKey.SAVER)
                if (awarded) {
                    Log.d(TAG, "Badge earned: SAVER")
                    _newBadge.emit(BadgeKey.SAVER)
                }
            }
        }
    }

    private fun checkAndSendMilestone(name: String, old: Int, new: Int) {
        val milestones = listOf(25, 50, 75, 100)
        milestones.forEach { m ->
            if (old < m && new >= m) {
                Log.d(TAG, "Security: milestone reached ($m%). Triggering notification.")
                notificationHelper.sendSavingsMilestone(name, m)
            }
        }
    }

    fun addEnvelope(envelope: AnnualEnvelope) {
        viewModelScope.launch {
            savingsRepository.addAnnualEnvelope(envelope)
            Log.d(TAG, "Added annual envelope: ${envelope.name}")
        }
    }

    fun deleteEnvelope(envelope: AnnualEnvelope) {
        viewModelScope.launch { savingsRepository.deleteAnnualEnvelope(envelope) }
    }
}
