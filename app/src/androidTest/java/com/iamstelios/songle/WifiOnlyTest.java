package com.iamstelios.songle;


import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WifiOnlyTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    /**
     * Checks that the app doesn't start a new game when there is no WiFi
     * and the data connection is enabled, but in the connection settings
     * allows only connections over WiFi
     *
     * For the test to work correctly, these are the requirements:
     * - active data connection
     * - WiFi disabled
     * - permissions for location enabled
     */
    @Test
    public void wifiOnlyTest() {
        mActivityTestRule.launchActivity(null);
        onView(
                allOf(withId(R.id.ConnectionButton), withContentDescription("Connection"), isDisplayed()))
                .perform(click());

        onView(
                allOf(withId(android.R.id.text1), withText("Use only WiFi"),
                        childAtPosition(
                                allOf(withId(R.id.select_dialog_listview),
                                        withParent(withId(R.id.contentPanel))),
                                0),
                        isDisplayed()))
                .perform(click());
        // Restart app to get rid of the songList that was downloaded before the
        mActivityTestRule.getActivity().finish();
        mActivityTestRule.launchActivity(null);
        // Try to start a new game
        onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()))
                .perform(click());

        // Check if it still on the MainActivity and no dialog is shown
        // because songList is not downloaded
        if(MainActivity.getInstance().hasProgress()){
            onView(allOf(withId(android.R.id.button1), withText("OK")))
                    .check(doesNotExist());
        }else{
            onView(
                    allOf(withId(android.R.id.text1), withText("Medium")))
                    .check(doesNotExist());
        }
        // Change the connection back to using data too, so that the songList is downloaded
        onView(
                allOf(withId(R.id.ConnectionButton), withContentDescription("Connection"), isDisplayed()))
                .perform(click());

        onView(
                allOf(withId(android.R.id.text1), withText("Use Data & WiFi"),
                        childAtPosition(
                                allOf(withId(R.id.select_dialog_listview),
                                        withParent(withId(R.id.contentPanel))),
                                1),
                        isDisplayed()))
                .perform(click());

        // Restart app
        mActivityTestRule.getActivity().finish();
        mActivityTestRule.launchActivity(null);

        // The songList should have been downloaded by now
        // NOTE the if songlist is not downloaded then test is going to fail
        onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()))
                .perform(click());

        // Check if a dialog is shown to start a new game now that the songList is loaded
        if(MainActivity.getInstance().hasProgress()){
            onView(allOf(withId(android.R.id.button1), withText("OK")))
                    .check(matches(isDisplayed()));
        }else{
            onView(
                    allOf(withId(android.R.id.text1), withText("Medium")))
                    .check(matches(isDisplayed()));
        }
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
