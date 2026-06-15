package com.example.truetrackfinance.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import net.sqlcipher.database.SupportFactory
import com.example.truetrackfinance.data.db.dao.*
import com.example.truetrackfinance.data.db.entity.*

/**
 * Main Room database for TrueTrackFinance.
 *
 * Encrypted with SQLCipher — the passphrase is derived at runtime from the
 * EncryptedSharedPreferences vault so it is never stored in plain text.
 *
 * Schema version history:
 *   1 → Initial schema (all core entities)
 */
@Database(
    entities = [
        User::class,
        Category::class,
        Expense::class,
        Budget::class,
        CategoryLimit::class,
        SavingsGoal::class,
        AnnualEnvelope::class,
        Badge::class,
        ExchangeRate::class,
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun badgeDao(): BadgeDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        private const val DATABASE_NAME = "truetrack_finance.db"

        /**
         * Build a SQLCipher-encrypted Room database.
         * [passphrase] should be a securely generated and stored AES-256 key.
         */
        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }

        // ── Migrations ───────────────────────────────────────────────────────
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN full_name TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN start_time INTEGER")
                db.execSQL("ALTER TABLE expenses ADD COLUMN end_time INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN min_spent_goal REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE budgets ADD COLUMN max_spent_goal REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN exchange_rate_to_base REAL NOT NULL DEFAULT 1.0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exchange_rates (
                        currencyCode TEXT NOT NULL PRIMARY KEY,
                        rate_to_eur REAL NOT NULL,
                        last_updated INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
