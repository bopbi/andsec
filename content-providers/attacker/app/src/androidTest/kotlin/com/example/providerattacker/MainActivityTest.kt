package com.example.providerattacker

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
  @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Test
  fun startupUiShowsLabControlsAndFixedStatus() {
    onView(withText(R.string.lab_title)).check(matches(isDisplayed()))
    onView(withId(R.id.read_vulnerable_button)).check(matches(isDisplayed()))
    onView(withId(R.id.read_hardened_button)).check(matches(isDisplayed()))
    onView(withId(R.id.status_text)).check(matches(withText(R.string.initial_status)))
  }
}
