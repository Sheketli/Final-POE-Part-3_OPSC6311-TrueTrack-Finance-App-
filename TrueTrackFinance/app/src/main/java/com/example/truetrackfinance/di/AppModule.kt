package com.example.truetrackfinance.di

import android.content.Context
import com.example.truetrackfinance.data.db.AppDatabase
import com.example.truetrackfinance.data.db.dao.*
import com.example.truetrackfinance.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAOs as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): AppDatabase {
        val passphrase = sessionManager.getOrCreateDbPassphrase()
        return AppDatabase.create(context, passphrase)
    }

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    @Singleton
    fun provideBadgeDao(db: AppDatabase): BadgeDao = db.badgeDao()

    @Provides
    @Singleton
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()
}
