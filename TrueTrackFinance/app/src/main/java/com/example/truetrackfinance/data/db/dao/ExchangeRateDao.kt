package com.example.truetrackfinance.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.truetrackfinance.data.db.entity.ExchangeRate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRate>)

    @Query("SELECT * FROM exchange_rates")
    fun observeAllRates(): Flow<List<ExchangeRate>>

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getRateByCode(code: String): ExchangeRate?

    @Query("DELETE FROM exchange_rates")
    suspend fun clearRates()
}
