package com.example.truetrackfinance.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── User ────────────────────────────────────────────────────────────────────
/**
 * Stores registered user credentials. Passwords are stored as bcrypt hashes.
 * EncryptedSharedPreferences holds the active session token.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "full_name") val fullName: String,       // user's real name
    @ColumnInfo(name = "username") val username: String,       // 6–20 chars, unique
    @ColumnInfo(name = "email") val email: String,             // valid email format
    @ColumnInfo(name = "password_hash") val passwordHash: String, // bcrypt hash
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "locked_until") val lockedUntil: Long = 0L, // epoch ms; 0 = not locked
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    @ColumnInfo(name = "pin_hash") val pinHash: String? = null,    // optional 4–6 digit PIN
    @ColumnInfo(name = "dark_mode") val darkMode: Boolean = false,
    @ColumnInfo(name = "base_currency") val baseCurrency: String = "ZAR",
    @ColumnInfo(name = "notification_enabled") val notificationEnabled: Boolean = true,
    @ColumnInfo(name = "last_active") val lastActive: Long = System.currentTimeMillis(),
)

// ─── Category ────────────────────────────────────────────────────────────────
/**
 * Budget categories created by the user. At least 5 are pre-loaded at first launch.
 * Each category has a colour (from 16 presets) and an optional emoji icon.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id")]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "name") val name: String,               // max 30 chars
    @ColumnInfo(name = "color_hex") val colorHex: String,      // e.g. "#0D9488"
    @ColumnInfo(name = "emoji") val emoji: String? = null,     // optional emoji icon
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,   // for drag-and-drop reordering
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── Expense ─────────────────────────────────────────────────────────────────
/**
 * Core transaction entity. Links to a Category and optionally stores a receipt photo path.
 * Recurring expenses generate child records via WorkManager.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("user_id"), Index("category_id")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long?,  // null = Uncategorised
    @ColumnInfo(name = "amount") val amount: Double,           // ZAR, 2 decimal places
    @ColumnInfo(name = "description") val description: String, // max 100 chars
    @ColumnInfo(name = "date") val date: Long,                 // epoch ms of transaction date
    @ColumnInfo(name = "start_time") val startTime: Long? = null, // epoch ms or time of day
    @ColumnInfo(name = "end_time") val endTime: Long? = null,
    @ColumnInfo(name = "receipt_photo_path") val receiptPhotoPath: String? = null, // private storage path
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurring_frequency") val recurringFrequency: String? = null, // DAILY/WEEKLY/FORTNIGHTLY/MONTHLY/ANNUALLY
    @ColumnInfo(name = "next_recurrence_date") val nextRecurrenceDate: Long? = null,
    @ColumnInfo(name = "currency") val currency: String = "ZAR",
    @ColumnInfo(name = "exchange_rate_to_base") val exchangeRateToBase: Double = 1.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── ExchangeRate ────────────────────────────────────────────────────────────
/**
 * Cached exchange rates from Frankfurter API. 
 * Rates are relative to EUR (API default), then converted to user's base currency.
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey val currencyCode: String,
    @ColumnInfo(name = "rate_to_eur") val rateToEur: Double,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

// ─── Budget ──────────────────────────────────────────────────────────────────
/**
 * Monthly budget goal. One record per user per month (month stored as "yyyy-MM").
 * Also holds expected monthly income for zero-based budgeting.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id", "month_key", unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "month_key") val monthKey: String,      // "yyyy-MM"
    @ColumnInfo(name = "total_goal") val totalGoal: Double,    // legacy field
    @ColumnInfo(name = "min_spent_goal") val minSpentGoal: Double = 0.0, // New field: Min monthly spending
    @ColumnInfo(name = "max_spent_goal") val maxSpentGoal: Double = 0.0, // New field: Max monthly spending
    @ColumnInfo(name = "total_income") val totalIncome: Double = 0.0, // for zero-based framework
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── CategoryLimit ────────────────────────────────────────────────────────────
/**
 * Per-category spending limit for a specific month. Separate from the Budget entity
 * so limits can be adjusted without touching the total budget record.
 */
@Entity(
    tableName = "category_limits",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("user_id", "category_id", "month_key", unique = true)]
)
data class CategoryLimit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "month_key") val monthKey: String,       // "yyyy-MM"
    @ColumnInfo(name = "limit_amount") val limitAmount: Double
)

// ─── SavingsGoal ─────────────────────────────────────────────────────────────
/**
 * A named savings goal with a target amount and deadline.
 * Contributions are logged manually; progress is derived from sum of contributions.
 */
@Entity(
    tableName = "savings_goals",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id")]
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "target_amount") val targetAmount: Double,
    @ColumnInfo(name = "current_amount") val currentAmount: Double = 0.0,
    @ColumnInfo(name = "deadline") val deadline: Long,          // epoch ms
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── AnnualEnvelope ───────────────────────────────────────────────────────────
/**
 * Large irregular annual expense planned month by month.
 * Monthly set-aside = annual_amount / months_remaining.
 */
@Entity(
    tableName = "annual_envelopes",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id")]
)
data class AnnualEnvelope(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "name") val name: String,               // e.g. "Vehicle Insurance"
    @ColumnInfo(name = "annual_amount") val annualAmount: Double,
    @ColumnInfo(name = "due_month") val dueMonth: Int,          // 1–12
    @ColumnInfo(name = "accumulated") val accumulated: Double = 0.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── Badge ────────────────────────────────────────────────────────────────────
/**
 * Gamification badge awarded to a user. 'earnedAt' is null for locked badges.
 */
@Entity(
    tableName = "badges",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id")]
)
data class Badge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "badge_key") val badgeKey: String,      // e.g. "FIRST_LOG", "7_DAY_STREAK"
    @ColumnInfo(name = "earned_at") val earnedAt: Long? = null // null = locked
)
