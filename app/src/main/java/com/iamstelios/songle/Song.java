package com.iamstelios.songle;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Class used to define the different songs in the game.
 * <p>All properties are set in the constructor and cannot be changed afterwards.</p>
 */
public class Song {
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
     * @param context Context
     * @param link Link to video
     */
    public static void watchYoutubeVideo(Context context, String link) {
        //TODO check if this works !
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
}
