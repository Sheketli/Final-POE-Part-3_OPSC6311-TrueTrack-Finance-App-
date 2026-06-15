package com.example.truetrackfinance.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralised date/time utility functions used across ViewModels and Workers.
 */
object DateUtil {

    private val MONTH_KEY_FORMAT = SimpleDateFormat("yyyy-MM", Locale.US)
    private val DISPLAY_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val SHORT_DATE_FORMAT = SimpleDateFormat("dd MMM", Locale.getDefault())

    /** Returns the current month key, e.g. "2026-04". */
    fun currentMonthKey(): String = MONTH_KEY_FORMAT.format(Date())

    /** Returns the month key for a given epoch millisecond value. */
    fun monthKeyFor(epochMs: Long): String = MONTH_KEY_FORMAT.format(Date(epochMs))

    /** Epoch ms of the first millisecond of the month for the given key. */
    fun monthStart(monthKey: String): Long {
        val cal = Calendar.getInstance()
        cal.time = MONTH_KEY_FORMAT.parse(monthKey)!!
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Epoch ms of the first millisecond of the NEXT month (exclusive upper bound). */
    fun monthEnd(monthKey: String): Long {
        val cal = Calendar.getInstance()
        cal.time = MONTH_KEY_FORMAT.parse(monthKey)!!
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Number of days remaining in the current month (inclusive of today). */
    fun remainingDaysInMonth(): Int {
        val cal = Calendar.getInstance()
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return lastDay - cal.get(Calendar.DAY_OF_MONTH) + 1
    }

    /** Human-readable date string, e.g. "25 Apr 2026". */
    fun formatDisplay(epochMs: Long): String = DISPLAY_DATE_FORMAT.format(Date(epochMs))

    /** Short date, e.g. "25 Apr". */
    fun formatShort(epochMs: Long): String = SHORT_DATE_FORMAT.format(Date(epochMs))

    /** Formats a timestamp into a time string, e.g. "14:30". */
    fun formatTime(epochMs: Long?): String {
        if (epochMs == null) return "--:--"
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return fmt.format(Date(epochMs))
    }

    /** Start of today in epoch ms. */
    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** End of today (start of tomorrow) in epoch ms. */
    fun todayEnd(): Long = todayStart() + 86_400_000L

    /** Start of the day N days ago. */
    fun daysAgoStart(days: Int): Long = todayStart() - (days.toLong() * 86_400_000L)

    /** Number of months between two epoch timestamps. */
    fun monthsBetween(start: Long, end: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        val diffYears = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        return diffYears * 12 + endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
    }

    /** Number of days in a given month key. */
    fun daysInMonth(monthKey: String): Int {
        val cal = Calendar.getInstance()
        cal.time = MONTH_KEY_FORMAT.parse(monthKey)!!
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Calculates the next scheduled date based on a frequency string. */
    fun calculateNextDate(from: Long, frequency: String?): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (frequency) {
            "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Fortnightly" -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            "Monthly" -> cal.add(Calendar.MONTH, 1)
            "Annually" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}
