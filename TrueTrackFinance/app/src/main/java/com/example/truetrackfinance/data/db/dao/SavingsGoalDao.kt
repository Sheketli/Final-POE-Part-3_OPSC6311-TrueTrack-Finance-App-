package com.example.truetrackfinance.data.db.dao

import androidx.room.*
import com.example.truetrackfinance.data.db.entity.AnnualEnvelope
import com.example.truetrackfinance.data.db.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    // ── Savings Goals ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals WHERE user_id = :userId ORDER BY deadline ASC")
    fun observeSavingsGoals(userId: Long): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getSavingsGoalById(id: Long): SavingsGoal?

    /** Add a contribution amount to a goal's current total. */
    @Query("UPDATE savings_goals SET current_amount = current_amount + :contribution WHERE id = :id")
    suspend fun addContribution(id: Long, contribution: Double)

    // ── Annual Envelopes ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnualEnvelope(envelope: AnnualEnvelope): Long

    @Update
    suspend fun updateAnnualEnvelope(envelope: AnnualEnvelope)

    @Delete
    suspend fun deleteAnnualEnvelope(envelope: AnnualEnvelope)

    @Query("SELECT * FROM annual_envelopes WHERE user_id = :userId ORDER BY due_month ASC")
    fun observeAnnualEnvelopes(userId: Long): Flow<List<AnnualEnvelope>>
}
