package com.example.truetrackfinance

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.truetrackfinance.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun openAddExpense_NavigateBack() {
        // FAB is global in MainActivity
        onView(withId(R.id.fabAddGlobal)).perform(click())
        
        // Verify we are on Add Expense screen
        onView(withId(R.id.etAmount)).check(matches(isDisplayed()))
        
        // Go back
        pressBack()
        
        // Verify we are back on Dashboard
        onView(withId(R.id.circular_progress_budget)).check(matches(isDisplayed()))
    }

    @Test
    fun addExpense_Validation() {
        onView(withId(R.id.fabAddGlobal)).perform(click())
        
        // Try to save empty
        onView(withId(R.id.btnSaveExpense)).perform(click())
        
        // Should still be on the same screen (validation prevented navigation)
        onView(withId(R.id.etAmount)).check(matches(isDisplayed()))
    }
}
