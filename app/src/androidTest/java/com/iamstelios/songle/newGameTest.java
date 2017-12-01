package com.iamstelios.songle;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
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
import static org.junit.Assert.assertTrue;

/**
 * Creates a new game from a fresh start.
 * For the test to work correcty, these are the requirements:
 * - active internet connection
 * - location enabled
 * - permissions for location enabled
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class newGameTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);

    private Context getMainActivityContext() {
        return mActivityTestRule.getActivity();
    }

    /**
     * Clear all the Preferences so that the tests run on a clean environment
     */
    @Before
    public void clearPrefs() {
        File root = InstrumentationRegistry.getTargetContext().getFilesDir().getParentFile();
        String[] sharedPreferencesFileNames = new File(root, "shared_prefs").list();
        if (sharedPreferencesFileNames != null) {
            for (String fileName : sharedPreferencesFileNames) {
                InstrumentationRegistry.getTargetContext().getSharedPreferences(fileName.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
            }
        }
        //MainActivity a = mActivityTestRule.getActivity().get;
        mActivityTestRule.launchActivity(null);
    }

    @Test
    public void newGameTest() {
        //Check if continue button is not visible at the start (no progress)
        ViewInteraction continueButton = onView(
                allOf(withId(R.id.ContinueButton), withContentDescription("Continue")));
        continueButton.check(matches(not(isDisplayed())));

        //Start a new Game
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(android.R.id.text1), withText("Extremely Easy"),
                        childAtPosition(
                                allOf(withId(R.id.select_dialog_listview),
                                        withParent(withId(R.id.contentPanel))),
                                4),
                        isDisplayed()));
        appCompatTextView.perform(click());

        // Added a sleep statement to match the app's execution delay.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Check that the starting score in a new game is correct
        ViewInteraction scoreText = onView(
                allOf(withId(R.id.scoreText),
                        isDisplayed()));
        scoreText.check(matches(withText("500")));
        //Check that the starting distance in a new game is correct
        ViewInteraction songDistanceText = onView(
                allOf(withId(R.id.songDistanceText),
                        isDisplayed()));
        songDistanceText.check(matches(withText("0 m")));
        //Check that the number of songs found in a new game is correct
        ViewInteraction songsFoundText = onView(
                allOf(withId(R.id.songsFoundText),
                        isDisplayed()));
        songsFoundText.check(matches(withText("0")));

        //Check that the number of songs skipped in a new game is correct
        ViewInteraction songsSkippedText = onView(
                allOf(withId(R.id.songsSkippedText),
                        isDisplayed()));
        songsSkippedText.check(matches(withText("0")));
        //Submit a wrong song
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.submitButton), isDisplayed()));
        floatingActionButton.perform(click());

        ViewInteraction editText = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(R.id.custom),
                                withParent(withId(R.id.customPanel)))),
                        isDisplayed()));
        editText.perform(click());

        ViewInteraction editText2 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(R.id.custom),
                                withParent(withId(R.id.customPanel)))),
                        isDisplayed()));
        editText2.perform(replaceText("wrong song"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Submit")));
        appCompatButton.perform(scrollTo(), click());

        //Check if points are deducted for wrong submission
        scoreText.check(matches(withText("480")));

        ViewInteraction skipSongButton = onView(
                allOf(withId(R.id.skipSongButton), isDisplayed()));
        skipSongButton.perform(click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("Skip")));
        appCompatButton2.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        appCompatButton3.perform(scrollTo(), click());

        //Check if songs skipped number changed
        songsSkippedText.check(matches(withText("1")));
        //Check if points reduced from skipping
        scoreText.check(matches(withText("380")));
        //Go back to MainActivity
        pressBack();

        //Check if continue button appears now that we have some progress
        ViewInteraction imageButton = onView(
                allOf(withId(R.id.ContinueButton), withContentDescription("Continue")));
        imageButton.check(matches(isDisplayed()));

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
