package com.iamstelios.songle;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OfflineTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);
    /**
     * Checks if a new game can be started while offline (it shouldn't)
     * For the test to work correctly, these are the requirements:
     * - internet connection DISABLED
     * (if connections only over wifi option enabled then data connection can be active)
     * If there is an internet connection the test should fail!!!
     */
    @Test
    public void offlineTest() {
        ViewInteraction NewGameButton = onView(
                allOf(withId(R.id.NewGameButton), withContentDescription("New Game"), isDisplayed()));
        NewGameButton.perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //if there still a new game button on the screen it means that it didn't change
        NewGameButton.check(matches(isDisplayed()));
    }

}
