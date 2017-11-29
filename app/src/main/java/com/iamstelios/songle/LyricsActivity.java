package com.iamstelios.songle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Activity that is used to list all the lyrics found to the user.
 * <p>The lyrics found should be added as an <b>ArrayListExtra</b> using
 * <b>MapsActivity.WORDS_FOUND_KEY</b> as key.</p>
 */
public class LyricsActivity extends AppCompatActivity {

    private static final String TAG = LyricsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);
        Log.i(TAG, "Lyrics presented to the user");

        Intent intent = getIntent();
        // The words found are given as an extra from the maps activity intent
        // calling lyrics activity
        ArrayList<String> wordsFound = intent.getStringArrayListExtra(MapsActivity.WORDS_FOUND_KEY);

        // Array adapter for the list view
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, wordsFound);
        //List the lyrics in a ListView
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(adapter);
    }

}
