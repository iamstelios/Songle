package com.iamstelios.songle;


import android.app.Activity;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HelpActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);
    /**
     * Checks that the user is redirected to the HelpActivity and
     * when he presses the navigation up button goes back to main activity
     */
    @Test
    public void helpActivityTest() {
        onView(
                allOf(withId(R.id.HelpButton), withContentDescription("Help"), isDisplayed()))
                .perform(click());
        //Check if the instructions are shown
        onView(
                allOf(withId(R.id.relativeLayoutInstructions), isDisplayed()))
                .check(matches(isDisplayed()));

        onView(
                allOf(withContentDescription("Navigate up"),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()))
                .perform(click());
        // Check if pressing Navigate up goes back to the MainActivity
        // by checking if the New Game button is displayed)
        onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game")))
                .check(matches(isDisplayed()));

    }

}
