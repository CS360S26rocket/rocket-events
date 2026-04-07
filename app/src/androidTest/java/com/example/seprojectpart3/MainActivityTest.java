package com.example.seprojectpart3;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testRegisterButtonDisplaysResult() {
        // Enter email
        Espresso.onView(withId(R.id.etEmail))
                .perform(ViewActions.typeText("test@lums.edu.pk"),
                        ViewActions.closeSoftKeyboard());

        // Enter password
        Espresso.onView(withId(R.id.etPassword))
                .perform(ViewActions.typeText("password123"),
                        ViewActions.closeSoftKeyboard());

        // Enter name
        Espresso.onView(withId(R.id.etName))
                .perform(ViewActions.typeText("Test User"),
                        ViewActions.closeSoftKeyboard());

        // Click Register button
        Espresso.onView(withId(R.id.btnRegister))
                .perform(ViewActions.click());

        // Verify result text
        Espresso.onView(withId(R.id.tvResult))
                .check(ViewAssertions.matches(
                        withText(containsString("Registered"))
                ));
    }
}