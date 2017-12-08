package com.iamstelios.songle;


import android.content.Context;
import android.content.SharedPreferences;
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
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SkipSongTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Checks when skipping a song:
     * <p>Text view of skipped songs updated</p>
     * <p>Song is changed</p>
     * <p>Points are deducted</p>
     */
    @Test
    public void skipSongTest() {
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()));
        appCompatImageButton.perform(click());

        if(MainActivity.getInstance().hasProgress()){
            onView(allOf(withId(android.R.id.button1), withText("OK")))
                .perform(click());
        }

        ViewInteraction appCompatTextView = onView(
                allOf(withId(android.R.id.text1), withText("Medium"),
                        childAtPosition(
                                allOf(withId(R.id.select_dialog_listview),
                                        withParent(withId(R.id.contentPanel))),
                                2),
                        isDisplayed()));
        appCompatTextView.perform(click());

        // Added a sleep statement to match the app's execution delay.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Retrieve the current song number saved to be compared with after skipping
        SharedPreferences prefs = mActivityTestRule.getActivity()
                .getSharedPreferences(MainActivity.SESSION_PREFS, Context.MODE_PRIVATE);
        String previousSongNum = prefs.getString(MainActivity.SONG_KEY, "ERROR404");
        //Save the Score to be compared afterwards
        int previousScore = prefs.getInt(MainActivity.POINTS_KEY,0);

        //Check that the number of songs skipped in a new game is correct
        ViewInteraction songsSkippedText = onView(
                allOf(withId(R.id.songsSkippedText),
                        isDisplayed()));
        songsSkippedText.check(matches(withText("0")));

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.skipSongButton), isDisplayed()));
        floatingActionButton.perform(click());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Skip")));
        appCompatButton.perform(scrollTo(), click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        appCompatButton2.perform(scrollTo(), click());

        //Check that the number of songs skipped is incremented to 1 from 0
        songsSkippedText.check(matches(withText("1")));

        String afterSkipSongNum = prefs.getString(MainActivity.SONG_KEY, "ERROR404");
        assertNotEquals("Song is the same after skipping!",previousSongNum,afterSkipSongNum);

        int afterSkipScore = prefs.getInt(MainActivity.POINTS_KEY,0);
        assertTrue("Points not deducted from score after skipping song",afterSkipScore<previousScore);

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
