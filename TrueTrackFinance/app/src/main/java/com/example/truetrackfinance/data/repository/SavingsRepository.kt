package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.SavingsGoalDao
import com.example.truetrackfinance.data.db.entity.AnnualEnvelope
import com.example.truetrackfinance.data.db.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SavingsRepository"

@Singleton
class SavingsRepository @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) {
    // ── Savings Goals ────────────────────────────────────────────────────────

    fun observeSavingsGoals(userId: Long): Flow<List<SavingsGoal>> =
        savingsGoalDao.observeSavingsGoals(userId)

    suspend fun addSavingsGoal(goal: SavingsGoal): Long {
        Log.d(TAG, "Creating savings goal: ${goal.name}, target: ${goal.targetAmount}")
        return savingsGoalDao.insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) = savingsGoalDao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoal) {
        Log.d(TAG, "Deleting savings goal ID ${goal.id}")
        savingsGoalDao.deleteSavingsGoal(goal)
    }

    suspend fun addContribution(goalId: Long, amount: Double) {
        Log.d(TAG, "Adding R$amount contribution to goal $goalId")
        savingsGoalDao.addContribution(goalId, amount)
    }

    // ── Annual Envelopes ─────────────────────────────────────────────────────

    fun observeAnnualEnvelopes(userId: Long): Flow<List<AnnualEnvelope>> =
        savingsGoalDao.observeAnnualEnvelopes(userId)

    suspend fun addAnnualEnvelope(envelope: AnnualEnvelope): Long {
        Log.d(TAG, "Creating annual envelope: ${envelope.name}")
        return savingsGoalDao.insertAnnualEnvelope(envelope)
    }

    suspend fun updateAnnualEnvelope(envelope: AnnualEnvelope) =
        savingsGoalDao.updateAnnualEnvelope(envelope)

    suspend fun deleteAnnualEnvelope(envelope: AnnualEnvelope) {
        Log.d(TAG, "Deleting annual envelope ID ${envelope.id}")
        savingsGoalDao.deleteAnnualEnvelope(envelope)
    }
}
