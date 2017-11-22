package com.iamstelios.songle;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.round;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = MapsActivity.class.getSimpleName();
    //TODO Callibrate distance
    private static final float COLLECTION_DISTANCE_MAXIMUM = 25;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //TODO: CHECK WHAT HAPPENS WHEN I DONT HAVE PERMISSIONS AND CHANGE ACCORDINGLY
    //private boolean mLocationPermissionGranted = false;
    private Location mLastLocation;
    private final LatLng GEORGE_SQUARE_LATLNG = new LatLng(55.944251, -3.189111);

    private List<Song> songList;
    private List<Placemark> placemarkList;
    private Set<String> lyricsFound;
    private int points;
    private String difficulty;
    private String songNumber;
    private final String KML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/%s/map%s.kml";
    private ArrayList<String[]> allWords;
    private final String WORDS_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/%s/words.txt";
    public static final String WORDS_FOUND_KEY = "words_found_key";
    private int songsFound;
    private int songsSkipped;
    private float songDistance;
    private float totalDistance;

    //Used to redirect the user to the video of the song he found
    public static void watchYoutubeVideo(Context context, String link) {
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
            Log.i(TAG, "User redirected to broswer to watch video");
            context.startActivity(webIntent);
        }
    }

    //Used to calculate how many points to deduct when a lyric is revealed
    private static final Map<String, Integer> pointsToDeduct = new HashMap<String, Integer>() {
        {
            put("veryinteresting", 400);
            put("interesting", 200);
            put("notboring", 100);
            put("boring", 50);
            put("unclassified", 500);
        }
    };

    //Used to calculate how many points to add when a lyric is collected
    private static final Map<String, Integer> pointsToScore = new HashMap<String, Integer>() {
        {
            put("veryinteresting", 50);
            put("interesting", 40);
            put("notboring", 25);
            put("boring", 10);
            put("unclassified", 200);
        }
    };

    private static final Map<String, Integer> submittingPointsToScore = new HashMap<String, Integer>() {
        {
            put("1", 1000);
            put("2", 500);
            put("3", 400);
            put("4", 200);
            put("5", 100);
        }
    };

    private static final Map<String, Integer> submittingPointsToDeduct = new HashMap<String, Integer>() {
        {
            put("1", 200);
            put("2", 100);
            put("3", 80);
            put("4", 50);
            put("5", 20);
        }
    };

    //Receives the lyric in the line:word form (eg.15:3) and returns the actual word
    private String lyricToWord(String lyric) {
        String[] lineWord = lyric.split(":");
        try {
            int lineNum = Integer.parseInt(lineWord[0]);
            int wordNum = Integer.parseInt(lineWord[1]);
            // The word is located in lineNum-1 because the index starts with 0
            // Word position in the line is wordNum+1 because position 1 is the line Number
            return allWords.get(lineNum - 1)[wordNum + 1];
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unexpected lyric found!");
            return getString(R.string.unexpected_lyric);
        } catch (NullPointerException e) {
            Log.e(TAG, "allWords not initialized");
            return getString(R.string.lyric_text_not_loaded);
        }
    }

    //Return the actual words found, given their "coordinates" and all the words
    private ArrayList<String> findWordsFound(Set<String> lyricsFound) {
        if (lyricsFound == null) {
            Log.e(TAG, "Lyrics found null, returning EMPTY list.");
            return new ArrayList<>();
        }
        ArrayList<String> wordsFound = new ArrayList<>();
        for (String lyric : lyricsFound) {
            wordsFound.add(lyricToWord(lyric));
        }
        return wordsFound;
    }

    private void updateSongsFoundText() {
        TextView songsFoundText = findViewById(R.id.songsFoundText);
        songsFoundText.setText(String.valueOf(songsFound));
    }

    //Increments the number of songs found
    private void addSongsFound() {
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        songsFound++;
        editor.putInt(MainActivity.CURRENT_SONGS_FOUND_KEY, songsFound);
        editor.apply();
        updateSongsFoundText();
    }

    private void updateSongsSkippedText() {
        TextView songsSkippedText = findViewById(R.id.songsSkippedText);
        songsSkippedText.setText(String.valueOf(songsSkipped));
    }

    //Increments the number of songs skipped
    private void addSongsSkipped() {
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        songsSkipped++;
        editor.putInt(MainActivity.CURRENT_SONGS_SKIPPED_KEY, songsSkipped);
        editor.apply();
        updateSongsSkippedText();
    }


    private void updateDistanceText() {
        TextView songDistanceText = findViewById(R.id.songDistanceText);
        songDistanceText.setText(String.format(Locale.ENGLISH,"%s m",String.valueOf(round(songDistance))));
    }

    private void updateDistance(Location location) {
        if (mLastLocation == null) {
            return;
        }
        double distanceToLast = location.distanceTo(mLastLocation);
        // if less than 10 metres, do not record
        if (distanceToLast < 10.00) {
            Log.i(TAG, "updateDistance: Distance too close, so not used.");
        } else {
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
            songDistance += distanceToLast;
            editor.putFloat(MainActivity.SONG_DIST_KEY, songDistance);
            editor.apply();
            //Open editor for global prefs to change total distance traveled
            editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
            totalDistance += distanceToLast;
            editor.putFloat(MainActivity.TOTAL_DIST_KEY, totalDistance);
            editor.apply();
            updateDistanceText();
            Log.i(TAG, "Distances have been updated");
        }
    }

    //Return the song given the song's number
    private Song getSong(String songNumber) {
        try {
            for (Song song : songList) {
                if (song.getNumber().equals(songNumber)) return song;
            }
        } catch (Exception e) {
            Log.e(TAG, "Tried to access song list when not loaded!");
        }
        return null;
    }

    //Update the screen showing the score and save the score in the preferences
    private void updateScore(int points) {
        TextView scoreText = findViewById(R.id.scoreText);
        scoreText.setText(String.valueOf(points));
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        editor.putInt(MainActivity.POINTS_KEY, points);
        editor.apply();
        //Check if highscore
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        int highscore = prefs.getInt(MainActivity.HIGHSCORE_KEY, MainActivity.STARTING_POINTS);
        SharedPreferences.Editor global_editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
        if (highscore < points) {
            Log.i(TAG, "New Highscore:" + points);
            global_editor.putInt(MainActivity.HIGHSCORE_KEY, points);
        }
        global_editor.apply();
    }

    //Add a lyric and update the lyrics found in the preferences
    private void addToLyricsFound(String lyric) {
        lyricsFound.add(lyric);
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();
    }

    //Calculate bonus points from distance
    private int bonusPoints(float distance) {
        return round(distance / Integer.parseInt(difficulty));
    }

    //Checks if the game is completed by going through all the songs
    private boolean isComplete(int songsCount){
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE);
        Set<String> songsUsed = prefs.getStringSet(MainActivity.SONGS_USED_KEY, new HashSet<String>());
        return songsUsed.size() >= songsCount;
    }

    //Takes as a parameter the current song and progresses the game to the next song
    private void progressSong(final Song song, boolean skipped) {
        //int currentSongNum = Integer.parseInt(song.getNumber());
        //TODO: Make a test about this case
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();

        if (skipped) {
            //Toast.makeText(this, "You've skipped the song :( You've walked " +
            //        songDistance + " meters failing to guess the song.", Toast.LENGTH_LONG).show();
            Log.i(TAG, "User skipped song");
            //Remove the points to the player score
            points -= submittingPointsToScore.get(difficulty);
            updateScore(points);
            //Update the number of songs skipped
            addSongsSkipped();
        } else {
            //Toast.makeText(this, "Congrats! You've progressed to the next song! You've walked " +
            //        songDistance + " meters trying to guess the song.", Toast.LENGTH_LONG).show();
            Log.i(TAG, "User guessed right");
            //Add the points to the player score
            points += submittingPointsToScore.get(difficulty) + bonusPoints(songDistance);
            updateScore(points);
            //Update the number of songs found
            addSongsFound();

            //Increase Total Songs found statistic by 1
            SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
            int totalSongsFound = prefs.getInt(MainActivity.TOTAL_SONGS_FOUND_KEY, 0) + 1;
            SharedPreferences.Editor global_editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
            global_editor.putInt(MainActivity.TOTAL_SONGS_FOUND_KEY, totalSongsFound);
            global_editor.apply();
        }
        //TODO CHANGE for RANDOM
        //Check if all the songs have been guessed
        //TODO add a function for completition condition boolean
        //if (currentSongNum >= songList.size()) {
        if(isComplete(songList.size())){

            Log.i(TAG, "User completed the game!");

            editor.remove(MainActivity.LYRICS_FOUND_KEY);
            editor.remove(MainActivity.DIFFICULTY_KEY);
            editor.remove(MainActivity.POINTS_KEY);
            editor.remove(MainActivity.SONG_KEY);
            editor.remove(MainActivity.SONG_DIST_KEY);
            editor.remove(MainActivity.CURRENT_SONGS_FOUND_KEY);
            editor.remove(MainActivity.CURRENT_SONGS_SKIPPED_KEY);
            editor.remove(MainActivity.SONGS_USED_KEY);
            //Commit changes because we want to be sure the continue button
            // will be not be present in main activity
            editor.commit();
            //Clear map from placemarks
            mMap.clear();
            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("title", song.getTitle());
            intent.putExtra("link", song.getLink());
            intent.putExtra("points", points);
            intent.putExtra("songsFound", songsFound);
            intent.putExtra("songsSkipped", songsSkipped);
            intent.putExtra("skipped", skipped);
            startActivity(intent);
            //TODO CHECK IF RETURN NEEDED
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        // Message different when user skipping song
        // (doesn't get to see the name of the song and  the video)
        builder.setMessage(skipped ? "You've skipped the song. \nYou've walked " +
                round(songDistance) + "m failing to guess the song." :
                "You've progressed to the next song! \nYou've walked " +
                        round(songDistance) + "m earning you " + bonusPoints(songDistance) +
                        " bonus points trying to guess: " + song.getArtist() + " - " + song.getTitle() + ".")
                .setTitle(skipped ? "Skipped song :(" : "Congrats!");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                //Do Nothing
            }
        });
        if (!skipped) {
            builder.setNegativeButton("Listen on Youtube", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User watch to watch video youtube
                    watchYoutubeVideo(getInstance(), song.getLink());
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();

        //TODO CHANGE TO RANDOM
        //Change the song number
        //songNumber = String.format(Locale.ENGLISH, "%02d", currentSongNum + 1);
        songNumber = MainActivity.getInstance().getNewSongNum(songList.size());
        editor.putString(MainActivity.SONG_KEY, songNumber);
        //Reset the lyrics found
        lyricsFound = new HashSet<String>();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();

        songDistance = 0;
        updateDistanceText();

        //Clear map from placemarks
        mMap.clear();

        runDownloads();

    }

    //Return a placemark of the matching position otherwise null
    private Placemark placemarkFromLatLng(LatLng position) {
        for (Placemark pl : placemarkList) {
            if (pl.getLatitude() == position.latitude && pl.getLongitude() == position.longitude) {
                return pl;
            }
        }
        Log.e(TAG, "Position of marker doesn't match any placemarks");
        return null;
    }


    public void runDownloads() {
        Log.i(TAG, "Attempting to download resources");

        //TODO CHECK IF song list download NEEDED


        //Update the Placemark map
        Downloader updater = new Downloader();
        updater.downloadPlacemarks(String.format(KML_URL, songNumber, difficulty));
        Log.i(TAG, "Map to be updated with new Song");
        //Download the text for the song
        updater.downloadLyrics(String.format(WORDS_URL, songNumber));
    }

    private NetworkReceiver receiver;
    //Have the Maps Activity instance so that the Network Receiver can access it
    private static MapsActivity instance;

    public static MapsActivity getInstance() {
        return instance;
    }

    public static void setInstance(MapsActivity instance) {
        MapsActivity.instance = instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
            Log.i(TAG, "Google Api Client initialized");
        }
        Log.i(TAG, "Map Created");

        //songList will always be populated as the Maps Activity won't open otherwise
        songList = MainActivity.getInstance().getSongList();

        //Retrieve the user preferences and stats
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE);
        difficulty = prefs.getString(MainActivity.DIFFICULTY_KEY, getString(R.string.difficulty_easy));
        songNumber = prefs.getString(MainActivity.SONG_KEY, MainActivity.getInstance().getNewSongNum(songList.size()));
        lyricsFound = prefs.getStringSet(MainActivity.LYRICS_FOUND_KEY, new HashSet<String>());
        points = prefs.getInt(MainActivity.POINTS_KEY, MainActivity.STARTING_POINTS);
        updateScore(points);
        songsFound = prefs.getInt(MainActivity.CURRENT_SONGS_FOUND_KEY, 0);
        updateSongsFoundText();
        songsSkipped = prefs.getInt(MainActivity.CURRENT_SONGS_SKIPPED_KEY, 0);
        updateSongsSkippedText();
        songDistance = prefs.getFloat(MainActivity.SONG_DIST_KEY, 0);
        updateDistanceText();
        prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        totalDistance = prefs.getFloat(MainActivity.TOTAL_DIST_KEY, 0);



        //String difficulty = getIntent().getExtras().getString(MainActivity.DIFFICULTY_KEY);
        Log.i(TAG, "Song Number: " + songNumber + " Difficulty: " + difficulty);
        //Toast.makeText(this, "Difficulty: " +
        //        getResources().getStringArray(R.array.difficulties)[Integer.parseInt(difficulty)-1],
        //        Toast.LENGTH_SHORT).show();

        //TODO REMOVE
        //Download the Placemark map
        //new DownloadPlacemarksTask().execute(String.format(KML_URL, songNumber, difficulty));
        //Download the text of the song
        //new DownloadLyricsTask().execute(String.format(WORDS_URL, songNumber));
        //Download the song list
        //new DownloadSongsTask().execute(SONGS_XML_URL);

        setInstance(this);
        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver(NetworkReceiver.MAPS_KEY);
        this.registerReceiver(receiver, filter);

        FloatingActionButton submitButton = (FloatingActionButton) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Song song = getSong(songNumber);
                if (song == null) {
                    Toast.makeText(MapsActivity.this,
                            R.string.songs_not_loaded_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    //Show a dialog that asks the user if user wants to submit the song
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    //Set the message and title of the dialog
                    builder.setMessage(String.format(Locale.ENGLISH, "Enter the title of the song. \nCorrect: +%d points. \nWrong: -%d points.", submittingPointsToScore.get(difficulty), submittingPointsToDeduct.get(difficulty)))
                            .setTitle(R.string.dialog_submit_title);
                    // Set up the input
                    final EditText input = new EditText(MapsActivity.this);
                    // Setting up the input for the submission
                    input.setInputType(InputType.TYPE_CLASS_TEXT);

                    builder.setView(input);
                    // Setting up the submission and cancellation buttons
                    builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //String that saves the guess
                            String submission = input.getText().toString();
                            Log.i(TAG, "User submitted song: " + submission);
                            //Incrementing the guess attempts value
                            SharedPreferences global_prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
                            int totalGuessAttempts = global_prefs.getInt(MainActivity.TOTAL_GUESS_ATTEMPTS, 0)+1;
                            SharedPreferences.Editor global_editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
                            global_editor.putInt(MainActivity.TOTAL_GUESS_ATTEMPTS,totalGuessAttempts);
                            global_editor.apply();
                            //TODO: Add the string that ignores the parenthesis substring
                            if (submission.equalsIgnoreCase(song.getTitle())) {
                                //Correct guess
                                progressSong(song, false);
                            } else {
                                //Wrong guess
                                //Subtracting the points
                                int toDeduct  = submittingPointsToDeduct.get(difficulty);
                                if (points - toDeduct >= 0) {
                                    //Points fully deducted
                                    points -= toDeduct;
                                    Log.i(TAG, pointsToDeduct + " points deducted");
                                } else {
                                    //Score goes to 0 because it can't be negative
                                    //User can keep guessing (only skipping and revealing is not allowed if was to drop below zero)
                                    points = 0;
                                    Log.e(TAG, "Points gone to zero.");
                                    Toast.makeText(MapsActivity.this, "Your score dropped to zero, but you can still keep guessing the song.", Toast.LENGTH_LONG).show();
                                }
                                updateScore(points);
                                Toast.makeText(MapsActivity.this, "Wrong Song :( Try Again.", Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "User guessed the wrong song.");
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                            Log.i(TAG, "User cancelled song submission");
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        //Skip song floating action button
        FloatingActionButton skipSongButton = (FloatingActionButton) findViewById(R.id.skipSongButton);
        skipSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Song song = getSong(songNumber);
                if (song == null) {
                    Toast.makeText(MapsActivity.this,
                            R.string.songs_not_loaded_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    //Show a dialog that asks the user if user wants to skip the song
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    //Set the message and title of the dialog
                    builder.setMessage(String.format(Locale.ENGLISH, "Are you sure that you want to skip this song? (-%d points)", submittingPointsToScore.get(difficulty)))
                            .setTitle(R.string.dialog_skip_song_title);
                    // Setting up the submission and cancellation buttons
                    builder.setPositiveButton(R.string.skip, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //TODO add to all point reduction
                            if (points - submittingPointsToScore.get(difficulty) >= 0) {
                                progressSong(song, true);
                            } else {
                                Log.e(TAG, "Not enough points to skip song.");
                                Toast.makeText(MapsActivity.this, "Not enough points to complete action.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                            Log.i(TAG, "User cancelled skipping song");
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        //Go to lyrics floating action button
        FloatingActionButton lyrics_button = (FloatingActionButton) findViewById(R.id.lyricsButton);
        lyrics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (allWords == null) {
                    Toast.makeText(MapsActivity.this, R.string.lyrics_connection_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<String> wordsFound = findWordsFound(lyricsFound);
                Intent intent = new Intent(MapsActivity.this, LyricsActivity.class);
                intent.putStringArrayListExtra(WORDS_FOUND_KEY, wordsFound);
                startActivity(intent);
            }
        });

        FloatingActionButton location_button = findViewById(R.id.locationButton);
        location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation == null) {
                    Log.e(TAG, "User tried to zoom to his location with no last location");
                    Toast.makeText(MapsActivity.this, "Please open location services before trying to zoom to your location", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, "Zoommed map to user's location");
                    LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17.5f));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregister the network receiver
        if(receiver!=null) {
            unregisterReceiver(receiver);
        }
    }

    protected void createLocationRequest() {
        // Set the parameters for the location request
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // preferably every 5 seconds
        mLocationRequest.setFastestInterval(1000); // at most every second
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Can we access the user's current location?
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //mLocationPermissionGranted = true;
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.i(TAG, "Location requests initiated");
        } else {
            //mLocationPermissionGranted = false;
            Log.e(TAG, "Location requests not initiated");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            createLocationRequest();
        } catch (java.lang.IllegalStateException ise) {
            Log.e(TAG, "IllegalStateException thrown [onConnected]");
        }
        // Can we access the user's current location?
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            //TODO: be sure I check that the location returned is not null
            mLastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onLocationChanged(Location current) {
        Log.i(TAG,
                " [onLocationChanged] Lat/long now (" +
                        String.valueOf(current.getLatitude()) + "," +
                        String.valueOf(current.getLongitude()) + ")"
        );
        updateDistance(current);
        mLastLocation = current;

        //TODO: Do something with current location
        //Eg. follow the player
        /*
        //New location of player
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        //Move camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        */
    }

    public boolean onMarkerClick(final Marker marker) {

        LatLng markerPosition = marker.getPosition();
        float[] results = new float[3];
        //Check if last location is initialized
        if (mLastLocation == null) {
            Log.i(TAG, "Last Location is Null, can't show dialog now.");
            // Can we access the user's current location?
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Last Location null and permissions are granted");
                if (!mMap.isMyLocationEnabled()) {
                    Log.i(TAG, "Enabling MyLocation");
                    //My location wasn't enabled so we enable it
                    locationButtonInitializer();

                    //Try to track location
                    try {
                        createLocationRequest();
                    } catch (java.lang.IllegalStateException ise) {
                        Log.e(TAG, "IllegalStateException thrown [onConnected]");
                    }
                    mLastLocation =
                            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                }
                if (mLastLocation == null) {
                    Toast.makeText(this, "You must open location services and track your location first!", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Player must open track location before accessing markers");
                    return true;
                }
            } else {
                Log.i(TAG, "Player didn't granted permissions for location");
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                return true;
            }
        }
        Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                markerPosition.latitude, markerPosition.longitude, results);
        final float distance = results[0];
        final Placemark lyricSelected = placemarkFromLatLng(markerPosition);
        final String lyricName = lyricSelected.getName();
        final String lyricClassification = lyricSelected.getDescription();
        Log.i(TAG, "Distance between player and lyric " + lyricName + " selected is approximately"
                + distance + " meters");

        //Show a dialog that asks the user if user wants to collect or reveal the lyric
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        //Set the message and title of the dialog
        builder.setMessage(R.string.dialog_lyric_message)
                .setTitle(R.string.dialog_lyric_title);
        //Notice Neutral is clicking collect, negative is revealing and positive is canceling
        builder.setNeutralButton(getString(R.string.collect) + "(+" + pointsToScore.get(lyricClassification) + ")", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User wants to collect lyric
                if (distance <= COLLECTION_DISTANCE_MAXIMUM) {
                    addToLyricsFound(lyricName);
                    points += pointsToScore.get(lyricClassification);
                    updateScore(points);
                    Toast.makeText(MapsActivity.this, "Congratulations! You've collected: " + lyricToWord(lyricName), Toast.LENGTH_LONG).show();
                    Log.i(TAG, "User collected lyric " + lyricName + " and scored " +
                            pointsToScore.get(lyricClassification) + " points.");
                    marker.remove();
                } else {
                    Toast.makeText(MapsActivity.this,
                            "You are too far away! Get closer to the lyric.",
                            Toast.LENGTH_LONG).show();
                    Log.i(TAG, "User too far away to collect");
                }
            }
        });

        //String revealMessage = R.string.reveal + "(-" + pointsToDeduct.get(lyricClassification) + ")";
        String revealMessage = getString(R.string.reveal) + "(-" + pointsToDeduct.get(lyricClassification) + ")";
        builder.setNegativeButton(revealMessage, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User wants to reveal lyric
                int toDeduct = pointsToDeduct.get(lyricClassification);
                if(points - toDeduct >=0){
                    addToLyricsFound(lyricName);
                    points -= pointsToDeduct.get(lyricClassification);
                    updateScore(points);
                    //TODO add message with the actual lyric
                    Toast.makeText(MapsActivity.this, "Yay! You've revealed: " + lyricToWord(lyricName), Toast.LENGTH_LONG).show();
                    Log.i(TAG, "User revealed lyric " + lyricName + " and deducted " +
                            pointsToDeduct.get(lyricClassification) + " points.");
                    marker.remove();
                }else{
                    Log.e(TAG, "Not enough points to skip song.");
                    Toast.makeText(MapsActivity.this, "Not enough points to complete action.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                //DO NOTHING
                Log.i(TAG, "User canceled selecting the lyric");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        //TODO: CHECK difference of return true and false
        return true;
    }

    @Override
    public void onConnectionSuspended(int flag) {
        Log.i(TAG, " >>>> onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
        //TODO: Throw a message popup to the user
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
        Log.e(TAG, " >>>> onConnectionFailed");
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        //TODO: REMOVE the comments
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        //Map to load in George Square by default
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GEORGE_SQUARE_LATLNG, 17));
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationButtonInitializer();
        }
        mMap.setOnMarkerClickListener(this);


    }


    private void locationButtonInitializer() {
        try {
            // Visualise current position with a small blue circle
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            Log.e(TAG, se.getMessage());
            Log.e(TAG, "Security exception thrown [onMapReady]");
        }
        //TODO delete or enable?
        // Add ``My location'' button to the user interface
        //mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    /*
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    */




    public void updateMap(List<Placemark> result){
        if (result != null) {
            placemarkList = result;
            for (Placemark pl : result) {
                //skip if a placemark is null (normally never)
                if (pl == null) continue;
                int id = getResources().getIdentifier(pl.getDescription(), "drawable",
                        getPackageName());
                //If the lyric was already found don't place its marker on the map
                if (lyricsFound.contains(pl.getName())) {
                    Log.i(TAG, "Lyric with name " + pl.getName() + " already found.");
                    //TODO CHECK IF DELETION NEEDED (if results are going to be used again)
                    //placemarkList.remove(pl);
                    continue;
                }
                mMap.addMarker(new MarkerOptions().position(
                        new LatLng(pl.getLatitude(), pl.getLongitude())).title(pl.getDescription()).visible(true)
                        .icon(BitmapDescriptorFactory.fromResource(id)));
                //TODO: SAVE MARKERS FOR LATER USE??
            }
            //Toast.makeText(MapsActivity.this, "Map Loaded Successfully", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Placemarks successfully added to the map");
        } else {
            Log.e(TAG, "Placemarks not loaded!");
        }
    }

    public void setAllWords(ArrayList<String[]> allWords){
        this.allWords = allWords;
    }



}



