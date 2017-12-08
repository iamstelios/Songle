package com.iamstelios.songle;


import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that check the functionality of the generateNewSongNum function
 */
public class GenerateNewSongNumTest {
    private HashSet<String> populateSongsUsed(){
        HashSet<String> songsUsed = new HashSet<>();
        songsUsed.add("01");
        songsUsed.add("10");
        songsUsed.add("7");
        songsUsed.add("5");
        songsUsed.add("3");
        return songsUsed;
    }

    /**
     * Check if generated song number is correct
     */
    @Test
    public void generateNewCheck(){
        HashSet<String> songsUsed = populateSongsUsed();
        int max = 15;
        String newSongNum = Song.generateNewSongNum(max,songsUsed);
        int newSongNumInt = Integer.parseInt(newSongNum);
        assertTrue("newSongNumber is not in the correct range",newSongNumInt >0 && newSongNumInt<=max);
        assertTrue("newSongNumber already in use",!songsUsed.contains(newSongNum));
    }

    /**
     * Test the behaviour when the total is less than the songsUsed size
     */
    @Test
    public void completeCheck(){
        HashSet<String> songsUsed = populateSongsUsed();
        int max = songsUsed.size()-2;
        String newSongNum = Song.generateNewSongNum(max,songsUsed);
        assertEquals("generateNewSongNum doesn't handle complete games correctly",newSongNum,"Game complete!");
    }
}
