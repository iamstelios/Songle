package com.iamstelios.songle;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests that check the String comparison used in the submission of a song by the user
 */
public class StringComparisonTest {
    /**
     * Checks the distance function in MapsActivity
     */
    @Test
    public void testLevenshteinDistance(){
        //Correct song
        String a = "Another Brick In The Wall, Part Two";
        //Incorrect song
        String b = "Wrong song";
        //Close but not enough
        String c = "Another Brick In The Wall";
        //Correct songs with slightly different writing
        String d = "Another Brick In The Wall Part Two";
        String e = "Another Brick In The Wall Part 2";

        assertEquals("Wrong Levenshtein Distance on " + a + " with "+ b,MapsActivity.distance(a,b), 32);
        assertEquals("Wrong Levenshtein Distance on " + a + " with "+ c,MapsActivity.distance(a,c), 10);
        assertEquals("Wrong Levenshtein Distance on " + a + " with "+ d,MapsActivity.distance(a,d), 1);
        assertEquals("Wrong Levenshtein Distance on " + a + " with "+ e,MapsActivity.distance(a,e), 4);

    }

    /**
     * Checks the roughlyEquals function in MapsActivity
     */
    @Test
    public void testRoughlyEquals(){
        //Correct song
        String a = "Another Brick In The Wall, Part Two";
        //Incorrect song
        String b = "Wrong song";
        //Close but not enough
        String c = "Another Brick In The Wall";
        //Correct songs with slightly different writing
        String d = "Another Brick In The Wall Part Two";
        String e = "Another Brick In The Wall Part 2";

        assertFalse("Wrong comparison of " + a + " with "+ b,MapsActivity.roughlyEquals(a,b));
        assertFalse("Wrong comparison of " + a + " with "+ c,MapsActivity.roughlyEquals(a,c));
        assertTrue("Wrong comparison of " + a + " with "+ d,MapsActivity.roughlyEquals(a,d));
        assertTrue("Wrong comparison of " + a + " with "+ e,MapsActivity.roughlyEquals(a,e));
    }
}