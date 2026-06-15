package com.example.truetrackfinance.data.repository

import android.util.Log
import com.example.truetrackfinance.data.db.dao.ExchangeRateDao
import com.example.truetrackfinance.data.db.entity.ExchangeRate
import com.example.truetrackfinance.data.network.FrankfurterApi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CurrencyRepository"

@Singleton
class CurrencyRepository @Inject constructor(
    private val api: FrankfurterApi,
    private val exchangeRateDao: ExchangeRateDao
) {
    /**
     * Fetch latest rates from Frankfurter and cache them in DB.
     * Frankfurter default base is EUR.
     */
    suspend fun syncRates(): Boolean {
        return try {
            Log.d(TAG, "Syncing exchange rates from Frankfurter API")
            val response = api.getLatestRates()
            val rates = response.rates.map { (code, rate) ->
                ExchangeRate(code, rate)
            } + ExchangeRate("EUR", 1.0) // Add base EUR itself
            
            exchangeRateDao.insertRates(rates)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync exchange rates", e)
            false
        }
    }

    /**
     * Get the conversion rate from [fromCurrency] to [toCurrency].
     * Uses EUR as the pivot.
     */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return 1.0
        
        val allRates = exchangeRateDao.observeAllRates().first().associateBy { it.currencyCode }
        
        val fromRateToEur = allRates[fromCurrency]?.rateToEur ?: return 1.0
        val toRateToEur = allRates[toCurrency]?.rateToEur ?: return 1.0
        
        // Example: from USD to ZAR
        // USD -> EUR is 1/usdRate
        // EUR -> ZAR is zarRate
        // USD -> ZAR is zarRate / usdRate
        return toRateToEur / fromRateToEur
    }

    fun observeAllRates() = exchangeRateDao.observeAllRates()
    
    /** Returns list of supported currency codes. */
    fun getSupportedCurrencies(): List<String> = listOf(
        "AUD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD",
        "HUF", "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NOK",
        "NZD", "PHP", "PLN", "RON", "SEK", "SGD", "THB", "TRY", "USD", "ZAR"
    )
}
