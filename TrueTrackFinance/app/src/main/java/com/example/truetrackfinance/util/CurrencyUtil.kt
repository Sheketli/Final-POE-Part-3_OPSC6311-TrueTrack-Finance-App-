package com.example.truetrackfinance.util

import android.util.Log
import java.text.NumberFormat
import java.util.*

/**
 * CurrencyUtil handles robust numeric parsing and formatting for multiple currencies.
 * Essential for "Zero Bug" stability when handling diverse user inputs and regional settings.
 */
object CurrencyUtil {

    private val TAG = "CurrencyUtil"

    /** 
     * Format a Double with its corresponding currency symbol/code. 
     * Uses Locale.US as default for deterministic formatting across regions.
     */
    @JvmOverloads
    fun format(amount: Double, currencyCode: String = "ZAR"): String {
        return try {
            val format = when (currencyCode) {
                "ZAR" -> NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                else -> {
                    val fmt = NumberFormat.getCurrencyInstance(Locale.US)
                    fmt.currency = Currency.getInstance(currencyCode)
                    fmt
                }
            }
            format.format(amount)
        } catch (e: Exception) {
            Log.e(TAG, "Formatting fault for $currencyCode: falling back to basic format", e)
            String.format(Locale.US, "%s %.2f", currencyCode, amount)
        }
    }

    /** 
     * Robustly parse a user-entered amount string to a Double. 
     * Requirement: Handle invalid inputs without crashing.
     */
    fun parseAmount(input: String): Double? {
        if (input.isBlank()) return null
        
        try {
            // Replace comma with dot to standardize decimal logic
            val standardized = input.replace(",", ".")
            
            // Complex Regex: Remove everything except digits and the LAST dot 
            val parts = standardized.split(".")
            val cleaned = if (parts.size > 1) {
                val integerPart = parts.subList(0, parts.size - 1).joinToString("").replace(Regex("[^0-9]"), "")
                val decimalPart = parts.last().replace(Regex("[^0-9]"), "")
                "$integerPart.$decimalPart"
            } else {
                standardized.replace(Regex("[^0-9]"), "")
            }
            
            return cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Critical Parsing Fault: unexpected format in '$input'", e)
            return null
        }
    }
}
