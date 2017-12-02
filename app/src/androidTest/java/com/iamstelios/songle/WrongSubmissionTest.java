package com.iamstelios.songle;


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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WrongSubmissionTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Checks when submitting a wrong song:
     * Text view of found songs remains the same
     * Song is same
     * Points are reduced
     *
     * For the test to work correctly, these are the requirements:
     * - active internet connection
     * - location enabled
     * - permissions for location enabled
     */
    @Test
    public void wrongSubmissionTest() {
        onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()))
                .perform(click());

        if (MainActivity.getInstance().hasProgress()) {
            onView(allOf(withId(android.R.id.button1), withText("OK")))
                    .perform(click());
        }

        onView(
                allOf(withId(android.R.id.text1), withText("Medium"),
                        childAtPosition(
                                allOf(withId(R.id.select_dialog_listview),
                                        withParent(withId(R.id.contentPanel))),
                                2),
                        isDisplayed()))
                .perform(click());

        // Added a sleep statement to match the app's execution delay.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(
                allOf(withId(R.id.submitButton), isDisplayed()))
                .perform(click());

        onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(R.id.custom),
                                withParent(withId(R.id.customPanel)))),
                        isDisplayed()))
                .perform(click());
        //Retrieve score and song number for later comparison
        int previousPoints = MapsActivity.getInstance().getPoints();
        String previousSongNumber = MapsActivity.getInstance().getSongNumber();

        onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(R.id.custom),
                                withParent(withId(R.id.customPanel)))),
                        isDisplayed()))
                .perform(replaceText("wrong song"), closeSoftKeyboard());

        onView(
                allOf(withId(android.R.id.button1), withText("Submit")))
                .perform(scrollTo(), click());

        //Check if SongsFound txt remains 0
        ViewInteraction songsFoundText = onView(
                allOf(withId(R.id.songsFoundText),
                        isDisplayed()));
        songsFoundText.check(matches(withText("0")));

        int afterSubmitPoints = MapsActivity.getInstance().getPoints();
        String afterSubmitSongNumber = MapsActivity.getInstance().getSongNumber();

        assertTrue("Points not deducted after wrong submission", afterSubmitPoints<previousPoints);
        assertEquals("Song number changed after wrong submission", afterSubmitSongNumber,previousSongNumber);
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
