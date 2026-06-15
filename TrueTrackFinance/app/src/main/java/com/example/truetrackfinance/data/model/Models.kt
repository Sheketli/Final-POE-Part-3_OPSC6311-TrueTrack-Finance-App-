package com.example.truetrackfinance.data.model

import androidx.room.ColumnInfo
import androidx.room.Ignore

/**
 * Result of a JOIN between expenses and categories.
 */
data class ExpenseWithCategory(
    val id: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val amount: Double,
    val description: String,
    val date: Long,
    @ColumnInfo(name = "receipt_photo_path") val receiptPhotoPath: String?,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "recurring_frequency") val recurringFrequency: String?,
    @ColumnInfo(name = "next_recurrence_date") val nextRecurrenceDate: Long?,
    val currency: String,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_color") val categoryColor: String?,
    @ColumnInfo(name = "category_emoji") val categoryEmoji: String?
)

/**
 * Per-category spending aggregate for the Reports / Dashboard.
 */
data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val totalSpent: Double
)

/**
 * Daily spending aggregate for the bar chart.
 */
data class DailySpending @JvmOverloads constructor(
    val day: String,       // "yyyy-MM-dd"
    val totalSpent: Double,
    @Ignore val categoryBreakdown: List<CategorySpending> = emptyList()
)

/**
 * UI state model for category progress.
 */
data class CategoryProgress(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val emoji: String?,
    val limitAmount: Double,
    val spentAmount: Double
) {
    val percentage: Float get() = if (limitAmount <= 0) 0f else (spentAmount / limitAmount * 100f).toFloat()
    val isOverLimit: Boolean get() = spentAmount > limitAmount
    val isNearLimit: Boolean get() = percentage >= 90f && !isOverLimit
}

/** Enum for badge keys. */
enum class BadgeKey(val key: String, val displayName: String, val description: String) {
    FIRST_LOG("FIRST_LOG", "First Log", "Log your very first expense"),
    STREAK_7("7_DAY_STREAK", "7-Day Streak", "Log expenses 7 days in a row"),
    STREAK_30("30_DAY_STREAK", "30-Day Streak", "Log expenses 30 days in a row"),
    BUDGET_HERO("BUDGET_HERO", "Budget Hero", "Close a month under your total budget"),
    CATEGORY_MASTER("CATEGORY_MASTER", "Category Master", "All categories within limits for a full month"),
    SAVER("SAVER", "Saver", "Reach a savings goal in full"),
    CONSISTENT_PLANNER("CONSISTENT_PLANNER", "Consistent Planner", "Set budget goals for 3 consecutive months")
}
