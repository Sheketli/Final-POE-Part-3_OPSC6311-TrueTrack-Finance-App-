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
class ReportsFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun navigateToReports_VerifyCharts() {
        // Navigate via Bottom Nav
        onView(withId(R.id.reportsFragment)).perform(click())
        
        // Verify Charts are present
        onView(withId(R.id.pieChart)).check(matches(isDisplayed()))
        onView(withId(R.id.barChart)).check(matches(isDisplayed()))
    }

    @Test
    fun switchPeriod_UpdatesUI() {
        onView(withId(R.id.reportsFragment)).perform(click())
        
        // Click "Last Month" chip
        onView(withId(R.id.chip_last_month)).perform(click())
        
        // Verify still on reports and cards are visible
        onView(withId(R.id.tvReportsTotalSpent)).check(matches(isDisplayed()))
    }
}
