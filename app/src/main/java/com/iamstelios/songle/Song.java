package com.iamstelios.songle;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Class used to define the different songs in the game.
 * <p>All properties are set in the constructor and cannot be changed afterwards.</p>
 */
public class Song {
    private final static String TAG = Song.class.getSimpleName();
    private final String number;
    private final String artist;
    private final String title;
    private final String link;

    public Song(String number, String artist, String title, String link) {
        this.number = number;
        this.artist = artist;
        this.title = title;
        this.link = link;
    }

    //Getter methods for accessing the properties of a Song
    public String getNumber() {
        return number;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    /**
     * Used to redirect the user to the video of the song he found
     *
     * @param context Context
     * @param link    Link to video
     */
    public static void watchYoutubeVideo(Context context, String link) {
        Activity activity = (Activity) context;
        final String TAG = activity.getClass().getName();
        if (!link.contains("https://youtu.be/")) {
            Log.e(TAG, "Youtube link not in correct problem");
            Toast.makeText(context, "Sorry, there was a problem loading the video.", Toast.LENGTH_SHORT).show();
            return;
        }
        String id = link.replace("https://youtu.be/", "");
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            context.startActivity(appIntent);
            Log.i(TAG, "User redirected to Youtube app");
        } catch (ActivityNotFoundException ex) {
            Log.i(TAG, "User redirected to browser to watch video");
            context.startActivity(webIntent);
        }
    }

    /**
     * Return the Song instance given the song's number
     *
     * @param songNumber Number of song
     * @return Song instance (null if not present)
     */
    public static Song getSong(String songNumber, List<Song> songList) {
        try {
            for (Song song : songList) {
                if (song.getNumber().equals(songNumber)) return song;
            }
        } catch (Exception e) {
            Log.e(TAG, "Tried to access song when resources not loaded!");
        }
        return null;
    }

    /**
     * Receives the lyric and returns the actual word
     *
     * @param lyric Lyric in line:word form (e.g.15:3)
     * @return Actual word (e.g. "Mama")
     */
    public static String lyricToWord(String lyric, ArrayList<String[]> allWords) {
        // Split the lyric so we separate the line from the word
        // Position 0 has the line number and position 1 has the word number
        String[] lineWord = lyric.split(":");
        try {
            int lineNum = Integer.parseInt(lineWord[0]);
            int wordNum = Integer.parseInt(lineWord[1]);
            // The word is located in lineNum-1 because the index starts with 0
            // Word position in the line is wordNum+1 because position 1 is the
            // line number and 0 is an empty string
            return allWords.get(lineNum - 1)[wordNum + 1];
        } catch (IndexOutOfBoundsException e) {
            //Lyric values does not match the text
            Log.e(TAG, "Unexpected lyric found!");
            return "Lyric is corrupted";
        } catch (NullPointerException e) {
            // Normally this case shouldn't be reached because each action that
            // calls lyricToWord ensures allWords is populated
            Log.e(TAG, "allWords not initialized");
            return "Lyric cannot be loaded your check connection";
        }
    }

    /**
     * Finds the words from a list of lyrics in line:word form
     *
     * @param lyricsFound List of lyrics in line:word form
     * @return List of actual words of lyrics
     */
    public static ArrayList<String> findWordsFound(Set<String> lyricsFound, ArrayList<String[]> allWords) {
        if (lyricsFound == null) {
            Log.e(TAG, "Lyrics found null, returning EMPTY list.");
            return new ArrayList<>();
        }
        ArrayList<String> wordsFound = new ArrayList<>();
        for (String lyric : lyricsFound) {
            wordsFound.add(lyricToWord(lyric, allWords));
        }
        return wordsFound;
    }

    /**
     * Generate a new song number
     *
     * @param total     Total number of songs
     * @param songsUsed Set of the
     * @return
     */
    public static String generateNewSongNum(int total, Set<String> songsUsed) {
        String songNum;
        int min = 1;
        int max = total;
        if (total <= songsUsed.size()) {
            //Should never be the case as the program won't call this method if all songs used
            Log.e(TAG, "Tried to generated new song number white the game is complete");
            return "Game complete!";
        }
        do {
            int random = new Random().nextInt((max - min) + 1) + min;
            songNum = String.format(Locale.ENGLISH, "%02d", random);
        } while (songsUsed.contains(songNum));
        return songNum;
    }
}
