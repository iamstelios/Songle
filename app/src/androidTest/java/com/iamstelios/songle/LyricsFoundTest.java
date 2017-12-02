package com.iamstelios.songle;


import android.content.Intent;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ListView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Checks the correctness of the LyricsActivity
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class LyricsFoundTest {

    @Rule
    public ActivityTestRule<LyricsActivity> mActivityTestRule = new ActivityTestRule<>(LyricsActivity.class,false,false);

    @Before
    public void setup(){
        Intent intent = new Intent();
        //Initialize an ArrayList with some sample lyrics
        ArrayList<String> wordsFound = new ArrayList<>();
        wordsFound.add("This");
        wordsFound.add("is");
        wordsFound.add("a");
        wordsFound.add("test!");
        //Put the words found as extra to populate the list in the LyricsActivity
        intent.putStringArrayListExtra(MapsActivity.WORDS_FOUND_KEY, wordsFound);
        mActivityTestRule.launchActivity(intent);
    }

    /**
     * Checks if all 4 lyrics are shown in their correct positions
     */
    @Test
    public void lyricsFoundTest() {

        ListView listview = (ListView) mActivityTestRule.getActivity().findViewById(android.R.id.list);

        assertThat("List has a wrong number of lyrics",listview.getCount(), is(4));
        assertThat("Lyrics are not correctly positioned",(String)listview.getItemAtPosition(0), is("This"));
        assertThat("Lyrics are not correctly positioned",(String)listview.getItemAtPosition(1), is("is"));
        assertThat("Lyrics are not correctly positioned",(String)listview.getItemAtPosition(2), is("a"));
        assertThat("Lyrics are not correctly positioned",(String)listview.getItemAtPosition(3), is("test!"));

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
