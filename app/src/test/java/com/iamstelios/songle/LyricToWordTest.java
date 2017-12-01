package com.iamstelios.songle;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Tests that check the functionality of the LyricToWord function
 */
public class LyricToWordTest {
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
     * Test if the correct lyric is returned
     */
    @Test
    public void correctLyricTest(){
        String firstWord = "1:1";
        ArrayList<String[]> allWords = populateAllWords();
        assertEquals("lyricToWord doesn't return the correct lyric",Song.lyricToWord(firstWord,allWords),"This");
    }
    /**
     * Test unexpected lyric behaviour
     */
    @Test
    public void unexpectedLyricTest(){
        //This lyric is not in allWords
        String unexpectedLyric = "1337:1337";
        ArrayList<String[]> allWords = populateAllWords();
        assertEquals("lyricToWord doesn't handle unexpected lyrics",Song.lyricToWord(unexpectedLyric,allWords),"Lyric is corrupted");
    }
    /**
     * Test null allWords behaviour
     */
    @Test
    public void nullAllWordsTest(){
        String firstWord = "1:1";
        ArrayList<String[]> allWords = null;
        assertEquals("lyricToWord doesn't handle null allWords",Song.lyricToWord(firstWord,allWords),"Lyric cannot be loaded your check connection");
    }
}
