package com.iamstelios.songle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Tests that check the functionality of findWordsFound
 */
public class findWordsFoundTest {
    /**
     * Returns a sample word list
     * @return Sample word list
     */
    private ArrayList<String[]> populateAllWords(){
        ArrayList<String[]> allWords = new ArrayList<>();
        //Matching the style of allWords in MapsActivity
        String[] line1={"","1","This","is","a","test"};
        String[] line2={"","2","And","this","is","line","2"};
        allWords.add(line1);
        allWords.add(line2);
        return allWords;
    }

    /**
     * Test if the correct lyric list is returned
     */
    @Test
    public void correctListTest(){
        ArrayList<String[]> allWords = populateAllWords();
        Set<String> lyricsFound = new HashSet<>();
        lyricsFound.add("1:1");
        lyricsFound.add("2:2");
        lyricsFound.add("1:4");
        lyricsFound.add("2:3");
        ArrayList<String> result = Song.findWordsFound(lyricsFound,allWords);
        assertTrue("allWords din't find the lyric: This",result.contains("This"));
        assertTrue("allWords din't find the lyric: this",result.contains("this"));
        assertTrue("allWords din't find the lyric: test",result.contains("test"));
        assertTrue("allWords din't find the lyric: is",result.contains("is"));
    }

    /**
     * Test null lyricsFound behaviour
     */
    @Test
    public void nullLyricsFoundTest(){
        ArrayList<String[]> allWords = populateAllWords();
        Set<String> lyricsFound = null;
        ArrayList<String> result = Song.findWordsFound(lyricsFound,allWords);
        assertTrue("null Lyrics found not handled correctly",result.isEmpty());
    }

    /**
     * Test null allWords behaviour
     */
    @Test
    public void nullAllWordsTest(){
        ArrayList<String[]> allWords = null;
        Set<String> lyricsFound = new HashSet<>();
        lyricsFound.add("1:2");
        lyricsFound.add("5:5");
        ArrayList<String> result = Song.findWordsFound(lyricsFound,allWords);
        ArrayList<String> expected = new ArrayList<>();
        expected.add("Lyric cannot be loaded your check connection");
        expected.add("Lyric cannot be loaded your check connection");
        assertTrue("null allWords not handled correctly",result.equals(expected));
    }
}
