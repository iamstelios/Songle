package com.iamstelios.songle;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests that check the functionality of the getSong function
 */
public class GetSongTest {

    private List<Song> populateSongList(){
        Song a = new Song("1337","Johnny Cash","Folsom Prison Blues","https://www.youtube.com/watch?v=v7gV5C5mB7A");
        Song b = new Song("05","Someone","Something","www.dont.click.this");
        Song c = new Song("08","Test","Testing","thisisalink.com");
        List<Song> songList = new ArrayList<>();
        songList.add(a);
        songList.add(b);
        songList.add(c);
        return songList;
    }
    /**
     * Tests if the function returns the correct song
     */
    @Test
    public void correctSongTest(){
        List<Song> songList = populateSongList();
        Song result = Song.getSong("1337",songList);
        Song a = new Song("1337","Johnny Cash","Folsom Prison Blues","https://www.youtube.com/watch?v=v7gV5C5mB7A");
        assertEquals("getSong doesn't retrieve the correct song",result.getTitle(),a.getTitle());
    }

    /**
     * Check if null is returned when a song is not in the song list
     */
    @Test
    public void nonExistingSongTest(){
        List<Song> songList = populateSongList();
        Song result = Song.getSong("404",songList);
        assertNull("Handling unknown songs failure",result);
    }

    /**
     * Check cases where a null argument is given
     */
    @Test
    public void nullArgumentTest(){
        Song result = Song.getSong("404",null);
        assertNull("Handling null song list failure",result);
        result = Song.getSong(null,new ArrayList<Song>());
        assertNull("Handling null song failure",result);
    }
}
