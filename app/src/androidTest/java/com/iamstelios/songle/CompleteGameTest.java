package com.iamstelios.songle;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.espresso.NoMatchingViewException;
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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CompleteGameTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Checks that the user is redirected to the MainActivity with no continue option
     * and the global stats are updated correctly
     *
     * For the test to work correctly, these are the requirements:
     * - active internet connection
     * - location enabled
     * - permissions for location enabled
     */
    @Test
    public void correctSubmissionTest() {
        SharedPreferences prefs = mActivityTestRule.getActivity()
                .getSharedPreferences(MainActivity.GLOBAL_PREFS, Context.MODE_PRIVATE);
        int previousTotalSongsFound = prefs.getInt(MainActivity.TOTAL_SONGS_FOUND_KEY, 0);
        int previousGuessAttempts = prefs.getInt(MainActivity.TOTAL_GUESS_ATTEMPTS, 0);

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

        //Keep submitting correct songs until submitButton is not displayed
        try {
            while (true) {
                onView(
                        allOf(withId(R.id.submitButton), isDisplayed()))
                        .perform(click());

                onView(
                        allOf(withClassName(is("android.widget.EditText")),
                                withParent(allOf(withId(R.id.custom),
                                        withParent(withId(R.id.customPanel)))),
                                isDisplayed()))
                        .perform(click());

                String previousSongNumber = MapsActivity.getInstance().getSongNumber();
                //Get the correct Song so we can submit the correct title
                Song correctSong = Song.getSong(previousSongNumber, MainActivity.getSongList());

                onView(
                        allOf(withClassName(is("android.widget.EditText")),
                                withParent(allOf(withId(R.id.custom),
                                        withParent(withId(R.id.customPanel)))),
                                isDisplayed()))
                        .perform(replaceText(correctSong.getTitle()), closeSoftKeyboard());

                onView(
                        allOf(withId(android.R.id.button1), withText("Submit")))
                        .perform(scrollTo(), click());
                //Check if dialog shown is of the correct form with the Listen on Youtube button
                onView(allOf(withId(android.R.id.button2), withText("Listen on Youtube")))
                        .check(matches(isDisplayed()));

                onView(allOf(withId(android.R.id.button1), withText("OK")))
                        .perform(click());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoMatchingViewException e) {
            //Do nothing - Used to exit the loop
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Check if continue button is not visible (game completed)
        onView(
                allOf(withId(R.id.ContinueButton), withContentDescription("Continue")))
                .check(matches(not(isDisplayed())));
        //Check the saved stats
        int newTotalSongsFound = prefs.getInt(MainActivity.TOTAL_SONGS_FOUND_KEY, 0);
        int newGuessAttempts = prefs.getInt(MainActivity.TOTAL_GUESS_ATTEMPTS, 0);
        assertTrue("Total songs found didn't increase",newTotalSongsFound>previousTotalSongsFound);
        assertTrue("Total guess attempts didn't increase",newGuessAttempts>previousGuessAttempts);
        assertTrue("Songs found and guess attempts didn't increase by the same amount",
                (newTotalSongsFound-previousTotalSongsFound)==(newGuessAttempts-previousGuessAttempts));
        //Check if all the songs have been passed
        int totalNumberOfSongs = MainActivity.getSongList().size();
        assertTrue("Not all songs have been completed!",
                (newTotalSongsFound-previousTotalSongsFound)==totalNumberOfSongs);

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
