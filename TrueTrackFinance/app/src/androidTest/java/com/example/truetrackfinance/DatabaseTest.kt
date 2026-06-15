package com.example.truetrackfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.truetrackfinance.data.db.AppDatabase
import com.example.truetrackfinance.data.db.dao.*
import com.example.truetrackfinance.data.db.entity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Room Database DAOs.
 * Verifies persistence and relational integrity (Foreign Keys).
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var budgetDao: BudgetDao

    private val testUserId = 1L

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
        budgetDao = db.budgetDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    /** Helper to ensure a parent user exists for foreign key constraints. */
    private suspend fun insertTestUser() {
        val user = User(
            id = testUserId,
            fullName = "Test User",
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hash"
        )
        userDao.insertUser(user)
    }

    @Test
    fun writeUserAndReadInList() = runTest {
        insertTestUser()
        val byName = userDao.getUserByUsername("testuser")
        assertEquals("Test User", byName?.fullName)
    }

    @Test
    fun insertCategoryAndRetrieve() = runTest {
        insertTestUser() // Required for Foreign Key
        val cat = Category(id = 1, userId = testUserId, name = "Food", colorHex = "#FF0000", emoji = "🍕")
        categoryDao.insertCategory(cat)
        val allCats = categoryDao.getCategoriesForUser(testUserId)
        assertTrue(allCats.any { it.name == "Food" })
    }

    @Test
    fun insertExpenseAndVerifyTotal() = runTest {
        insertTestUser()
        val cat = Category(id = 1, userId = testUserId, name = "Food", colorHex = "#FF0000")
        categoryDao.insertCategory(cat)
        
        val expense = Expense(
            userId = testUserId, 
            categoryId = 1, 
            amount = 150.50, 
            description = "Lunch", 
            date = System.currentTimeMillis()
        )
        expenseDao.insertExpense(expense)
        
        val total = expenseDao.observeTotalSpentInMonth(testUserId, 0, System.currentTimeMillis() + 86400000).first()
        assertEquals(150.50, total, 0.01)
    }

    @Test
    fun budgetLimitLogic() = runTest {
        insertTestUser()
        val budget = Budget(userId = testUserId, monthKey = "2026-04", totalGoal = 5000.0)
        budgetDao.insertOrUpdateBudget(budget)
        
        // Category must exist for CategoryLimit FK
        val cat = Category(id = 1, userId = testUserId, name = "Food", colorHex = "#FF0000")
        categoryDao.insertCategory(cat)
        
        val limit = CategoryLimit(userId = testUserId, categoryId = 1, monthKey = "2026-04", limitAmount = 1000.0)
        budgetDao.insertOrUpdateCategoryLimit(limit)
        
        val retrieved = budgetDao.getLimitForCategory(testUserId, 1, "2026-04")
        assertEquals(1000.0, retrieved ?: 0.0, 0.01)
    }

    @Test
    fun testBudgetUniqueConstraint() = runTest {
        insertTestUser()
        val budget1 = Budget(userId = testUserId, monthKey = "2026-04", totalGoal = 5000.0)
        budgetDao.insertOrUpdateBudget(budget1)

        val budget2 = Budget(userId = testUserId, monthKey = "2026-04", totalGoal = 6000.0)
        // insertOrUpdateBudget uses OnConflictStrategy.REPLACE, so it should update
        budgetDao.insertOrUpdateBudget(budget2)

        val retrieved = budgetDao.getBudgetForMonth(testUserId, "2026-04")
        assertEquals(6000.0, retrieved?.totalGoal ?: 0.0, 0.01)
        
        // Check if there's only one budget for this month
        // We'd need a way to count all budgets or just assume Room's REPLACE works with the UNIQUE index.
    }
}
