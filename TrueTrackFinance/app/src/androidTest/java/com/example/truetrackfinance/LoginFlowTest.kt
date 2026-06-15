package com.example.truetrackfinance

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.truetrackfinance.ui.AuthActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Login and Registration flows.
 * Ensures that navigation between tabs and basic input logic is stable.
 */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AuthActivity::class.java)

    @Test
    fun loginFlow_InitialState_IsLoginTab() {
        // Verify that the login tab elements are displayed by default
        onView(withId(R.id.et_username)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()))
    }

    @Test
    fun navigateToRegister_AndBack_Success() {
        // 1. Switch to Register tab
        onView(withId(R.id.btn_tab_register)).perform(click())
        
        // 2. Verify registration fields are visible
        onView(withId(R.id.et_full_name)).check(matches(isDisplayed()))
        onView(withId(R.id.et_email)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_register)).check(matches(isDisplayed()))
        
        // 3. Switch back to Sign In
        onView(withId(R.id.btn_tab_signin)).perform(click())
        
        // 4. Verify back on Login tab
        onView(withId(R.id.et_username)).check(matches(isDisplayed()))
    }

    @Test
    fun loginWithEmptyFields_StayOnScreen() {
        // Perform click without entering data
        onView(withId(R.id.btn_login)).perform(click())
        
        // Should still be on the same screen (not crashed, and text field still visible)
        onView(withId(R.id.et_username)).check(matches(isDisplayed()))
    }
}
