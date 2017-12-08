package com.iamstelios.songle;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.support.annotation.VisibleForTesting;
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

import static java.lang.Math.max;
import static java.lang.Math.round;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = MapsActivity.class.getSimpleName();
    /**
     * Distance from where the player can collect a lyric
     */
    private static final float COLLECTION_DISTANCE_MAXIMUM = 25;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    /**
     * Last Location received from the GPS
     */
    private Location mLastLocation;
    /**
     * Latitude and longitude of George Square
     */
    private final LatLng GEORGE_SQUARE_LATLNG = new LatLng(55.944251, -3.189111);
    /**
     * List of the songs in the Songle as given by the MainActivity
     */
    private List<Song> songList;
    /**
     * List of the placemarks on the map
     */
    private List<Placemark> placemarkList;
    /**
     * Set of the lyrics found by the player (current song)
     * <p>In form line:word e.g. "3:5"</p>
     */
    private Set<String> lyricsFound;

    /**
     * The points accumulated by the player
     * <p>Normally should be always positive</p>
     */
    private int points;

    /**
     * Getter for points - Used for testing
     * @return points
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public int getPoints() {
        return points;
    }

    /**
     * Level of difficulty in form "1"
     * Difficulty can range from 1 to 5, with 1 the hardest and 5 the easiest.
     */
    private String difficulty;

    /**
     * Number of the current song to be found
     */
    private String songNumber;
    /**
     * Getter for current song number - Used for testing
     * @return Current song number
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public String getSongNumber() {
        return songNumber;
    }

    //URLs used for retrieving the remote data
    private final String KML_URL =
            "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/%s/map%s.kml";
    private final String WORDS_URL =
            "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/%s/words.txt";
    /**
     * List of all the words (lyrics) found by the player
     */
    private ArrayList<String[]> allWords;
    /**
     * Used for the insertion and retrieval of the words found intent extra
     */
    public static final String WORDS_FOUND_KEY = "words_found_key";
    /**
     * Number of songs found in the current session
     */
    private int songsFound;
    /**
     * Number of songs skipped in the current session
     */
    private int songsSkipped;
    /**
     * Distance covered by the player during current song
     */
    private float songDistance;

    /**
     * Used to calculate how many points to deduct when a lyric is revealed,
     * depending on the rarity.
     */
    private static final Map<String, Integer> pointsToDeduct = new HashMap<String, Integer>() {
        {
            put("veryinteresting", 300);
            put("interesting", 180);
            put("notboring", 120);
            put("boring", 60);
            put("unclassified", 30);
        }
    };

    /**
     * Used to calculate how many points to add when a lyric is collected,
     * depending on the rarity.
     */
    private static final Map<String, Integer> pointsToScore = new HashMap<String, Integer>() {
        {
            put("veryinteresting", 100);
            put("interesting", 60);
            put("notboring", 40);
            put("boring", 20);
            put("unclassified", 10);
        }
    };

    /**
     * Used to calculate how many points to add when a song is guessed correctly,
     * depending on the difficulty.
     */
    private static final Map<String, Integer> submittingPointsToScore =
            new HashMap<String, Integer>() {
                {
                    put("1", 1000);
                    put("2", 500);
                    put("3", 400);
                    put("4", 200);
                    put("5", 100);
                }
            };

    /**
     * Used to calculate how many points to deduct when a song is not guessed correctly,
     * depending on the difficulty.
     */
    private static final Map<String, Integer> submittingPointsToDeduct =
            new HashMap<String, Integer>() {
                {
                    put("1", 200);
                    put("2", 100);
                    put("3", 80);
                    put("4", 50);
                    put("5", 20);
                }
            };

    /**
     * Receiver used to check the connection
     */
    private NetworkReceiver receiver;
    //Used to store the current instance created on onCreate
    private static MapsActivity instance;

    //Getter for instance
    public static MapsActivity getInstance() {
        return instance;
    }

    //Setter for instance
    public static void setInstance(MapsActivity instance) {
        MapsActivity.instance = instance;
    }

    /**
     * Setter method for allWords (used by the downloader)
     * @param allWords Value to be assigned
     */
    public void setAllWords(ArrayList<String[]> allWords) {
        this.allWords = allWords;
    }

    /**
     * Updates the TextView that shows the number of songs found
     */
    private void updateSongsFoundText() {
        TextView songsFoundText = findViewById(R.id.songsFoundText);
        songsFoundText.setText(String.valueOf(songsFound));
    }

    /**
     * Increments the number of songs found
     */
    private void addSongsFound() {
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
        songsFound++;
        editor.putInt(MainActivity.CURRENT_SONGS_FOUND_KEY, songsFound);
        editor.apply();
        //Need to update the text view after the changing the value
        updateSongsFoundText();
    }

    /**
     * Updates the TextView that shows the number of songs skipped
     */
    private void updateSongsSkippedText() {
        TextView songsSkippedText = findViewById(R.id.songsSkippedText);
        songsSkippedText.setText(String.valueOf(songsSkipped));
    }

    /**
     * Increments the number of songs skipped
     */
    private void addSongsSkipped() {
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
        songsSkipped++;
        editor.putInt(MainActivity.CURRENT_SONGS_SKIPPED_KEY, songsSkipped);
        editor.apply();
        //Need to update the text view after the changing the value
        updateSongsSkippedText();
    }

    /**
     * Updates the TextView that shows the distance travelled
     */
    private void updateDistanceText() {
        TextView songDistanceText = findViewById(R.id.songDistanceText);
        songDistanceText.setText(
                String.format(Locale.ENGLISH, "%s m", String.valueOf(round(songDistance))));
    }

    /**
     * Update the distance travelled
     *
     * @param location Current location
     */
    private void updateDistance(Location location) {
        if (mLastLocation == null) {
            return;
        }
        double distanceToLast = location.distanceTo(mLastLocation);
        // if less than 10 metres, do not record
        if (distanceToLast < 10.00) {
            Log.i(TAG, "updateDistance: Distance too close, so not used.");
        } else {
            SharedPreferences.Editor editor =
                    getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
            songDistance += distanceToLast;
            editor.putFloat(MainActivity.SONG_DIST_KEY, songDistance);
            editor.apply();
            //Retrieve the total distance covered
            SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
            float totalDistance = prefs.getFloat(MainActivity.TOTAL_DIST_KEY, 0);
            //Open editor for global prefs to change total distance traveled
            editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
            totalDistance += distanceToLast;
            editor.putFloat(MainActivity.TOTAL_DIST_KEY, totalDistance);
            editor.apply();
            //Update the Text that show the distance
            updateDistanceText();
            Log.i(TAG, "Distances have been updated");
        }
    }

    /**
     * Change the score value and update relevant properties
     *
     * @param points Updated score
     */
    private void updateScore(int points) {
        //Update the score text
        TextView scoreText = findViewById(R.id.scoreText);
        scoreText.setText(String.valueOf(points));
        //Save the score
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
        editor.putInt(MainActivity.POINTS_KEY, points);
        editor.apply();
        //Check if highscore
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        int highscore = prefs.getInt(MainActivity.HIGHSCORE_KEY, MainActivity.STARTING_POINTS);
        if (highscore < points) {
            //Update highscore
            Log.i(TAG, "New Highscore:" + points);
            SharedPreferences.Editor global_editor =
                    getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
            global_editor.putInt(MainActivity.HIGHSCORE_KEY, points);
            global_editor.apply();
        }
    }

    /**
     * Add a lyric to the lyrics found update the preferences
     *
     * @param lyric Lyric found
     */
    private void addToLyricsFound(String lyric) {
        lyricsFound.add(lyric);
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();
    }

    /**
     * Calculate bonus points from distance travelled in current song
     *
     * @param distance Distance travelled
     * @return Bonus points
     */
    private int bonusPoints(float distance) {
        //The easier the game, the less bonus points earned
        return round(distance / Integer.parseInt(difficulty));
    }

    /**
     * Checks if the game is completed (gone through all the songs)
     *
     * @param songsCount Number of songs gone through so far
     * @return true if game completed, false otherwise
     */
    private boolean isComplete(int songsCount) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE);
        Set<String> songsUsed =
                prefs.getStringSet(MainActivity.SONGS_USED_KEY, new HashSet<String>());
        return songsUsed.size() >= songsCount;
    }

    /**
     * Progresses the game to the next song
     *
     * @param song    Current song
     * @param skipped Was the song skipped? (T/F)
     */
    private void progressSong(final Song song, boolean skipped) {
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE).edit();
        //Changing the score
        if (skipped) {
            Log.i(TAG, "User skipped song");
            //Remove the points to the player score
            points -= submittingPointsToScore.get(difficulty);
            updateScore(points);
            //Update the number of songs skipped
            addSongsSkipped();
        } else {
            Log.i(TAG, "User guessed right");
            //Add the points to the player score
            points += submittingPointsToScore.get(difficulty) + bonusPoints(songDistance);
            updateScore(points);
            //Update the number of songs found
            addSongsFound();

            //Increment Total Songs found statistic
            SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
            int totalSongsFound = prefs.getInt(MainActivity.TOTAL_SONGS_FOUND_KEY, 0) + 1;
            SharedPreferences.Editor global_editor = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
            global_editor.putInt(MainActivity.TOTAL_SONGS_FOUND_KEY, totalSongsFound);
            global_editor.apply();
        }
        //Check if all the songs have been guessed
        if (isComplete(songList.size())) {

            Log.i(TAG, "User completed the game!");
            //Remove all the values from the current session in the preferences
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
            //Put the extras to fill the game finishing dialog in the MainActivity
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("title", song.getTitle());
            intent.putExtra("link", song.getLink());
            intent.putExtra("points", points);
            intent.putExtra("songsFound", songsFound);
            intent.putExtra("songsSkipped", songsSkipped);
            intent.putExtra("skipped", skipped);
            startActivity(intent);
            return;
        }
        //Dialog shown when progressing song
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        // Message different when user skipping song
        // (doesn't get to see the name of the song and  the video)
        String skippedMessage = String.format(Locale.ENGLISH,
                getString(R.string.dialog_skipped_msg), round(songDistance));
        String correctMessage = String.format(Locale.ENGLISH,
                getString(R.string.dialog_correct_msg),
                round(songDistance), bonusPoints(songDistance), song.getArtist(), song.getTitle());
        builder.setMessage(skipped ? skippedMessage : correctMessage)
                .setTitle(skipped ? getString(R.string.dialog_title_skipped) :
                        getString(R.string.dialog_title_correct));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                //Do Nothing
            }
        });
        if (!skipped) {
            builder.setNegativeButton(R.string.listen_youtube, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User chooses to watch video on youtube
                    Song.watchYoutubeVideo(getInstance(), song.getLink());
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();

        //Change the song number (chosen randomly from the songs not used yet)
        songNumber = MainActivity.getInstance().getNewSongNum(songList.size());
        editor.putString(MainActivity.SONG_KEY, songNumber);
        //Reset the lyrics found
        lyricsFound = new HashSet<>();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();

        songDistance = 0;
        updateDistanceText();

        //Clear map from placemarks
        mMap.clear();

        runDownloads();

    }

    /**
     * Find a Placemark from latitude and longitude
     *
     * @param position Position of placemark to be found
     * @return Placemark of the matching position (if no match then null)
     */
    private Placemark placemarkFromLatLng(LatLng position) {
        for (Placemark pl : placemarkList) {
            if (pl.getLatitude() == position.latitude && pl.getLongitude() == position.longitude) {
                return pl;
            }
        }
        Log.e(TAG, "Position of marker doesn't match any placemarks");
        return null;
    }

    /**
     * Downloads the placemarks and lyrics resources
     */
    public void runDownloads() {
        Log.i(TAG, "Attempting to download resources");

        //Update the Placemark map
        Downloader downloader = new Downloader();
        downloader.downloadPlacemarks(String.format(KML_URL, songNumber, difficulty));
        Log.i(TAG, "Map to be updated with new Song");
        //Download the text for the song
        downloader.downloadLyrics(String.format(WORDS_URL, songNumber));
    }

    /**
     * Calculates the Levenshtein distance between 2 strings
     * <p>(Used to compare similarity of 2 Strings)</p>
     *
     * @param a First String
     * @param b Second String
     * @return Levenshtein distance
     */
    public static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Checks if 2 Strings are roughly equals
     *
     * @param a First String
     * @param b Second String
     * @return True if similarity above threshold
     */
    public static boolean roughlyEquals(String a, String b) {
        // This function was made to allow very similar submissions to be accepted.
        // Minor grammatical mistakes can now pass.
        float threshold = 0.85f;
        //Comparison is case insensitive
        a = a.toLowerCase(Locale.ENGLISH);
        b = b.toLowerCase(Locale.ENGLISH);
        // The two string have to be 90% similar to be accepted
        int sizeLongest = max(a.length(), b.length());
        float percentage = (sizeLongest - distance(a, b)) / (float) sizeLongest;
        return percentage >= threshold;
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
        songList = MainActivity.getSongList();

        //Retrieve the user preferences and stats
        SharedPreferences prefs = getSharedPreferences(MainActivity.SESSION_PREFS, MODE_PRIVATE);
        difficulty =
                prefs.getString(MainActivity.DIFFICULTY_KEY, getString(R.string.difficulty_easy));
        songNumber =
                prefs.getString(MainActivity.SONG_KEY,
                        MainActivity.getInstance().getNewSongNum(songList.size()));
        lyricsFound = prefs.getStringSet(MainActivity.LYRICS_FOUND_KEY, new HashSet<String>());
        points = prefs.getInt(MainActivity.POINTS_KEY, MainActivity.STARTING_POINTS);
        updateScore(points);
        songsFound = prefs.getInt(MainActivity.CURRENT_SONGS_FOUND_KEY, 0);
        updateSongsFoundText();
        songsSkipped = prefs.getInt(MainActivity.CURRENT_SONGS_SKIPPED_KEY, 0);
        updateSongsSkippedText();
        songDistance = prefs.getFloat(MainActivity.SONG_DIST_KEY, 0);
        updateDistanceText();
        //Set the Total Songs textview
        TextView songsSkippedText = findViewById(R.id.songsTotalText);
        songsSkippedText.setText(String.valueOf(songList.size()));

        Log.i(TAG, "Song Number: " + songNumber + " Difficulty: " + difficulty);

        setInstance(this);
        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver(NetworkReceiver.MAPS_KEY);
        this.registerReceiver(receiver, filter);

        //Submit button listener
        FloatingActionButton submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Song song = Song.getSong(songNumber, songList);
                if (song == null) {
                    //Songs haven't been loaded
                    Toast.makeText(MapsActivity.this,
                            R.string.songs_not_loaded_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    //Show a dialog that asks the user if user wants to submit the song
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    //Set the message and title of the dialog
                    builder.setMessage(String.format(Locale.ENGLISH,
                            getString(R.string.dialog_submit),
                            submittingPointsToScore.get(difficulty),
                            submittingPointsToDeduct.get(difficulty)))
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
                            SharedPreferences global_prefs =
                                    getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
                            int totalGuessAttempts =
                                    global_prefs.getInt(MainActivity.TOTAL_GUESS_ATTEMPTS, 0) + 1;
                            SharedPreferences.Editor global_editor =
                                    getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE).edit();
                            global_editor.putInt(MainActivity.TOTAL_GUESS_ATTEMPTS, totalGuessAttempts);
                            global_editor.apply();
                            // Everything in a song title that's after a parenthesis can be ignored
                            // This is to simplify titles that might have longer versions
                            String titleSimplified = song.getTitle().replaceAll("[\\[{(].*", "");
                            if (roughlyEquals(submission, titleSimplified) ||
                                    roughlyEquals(submission, song.getTitle())) {
                                //Correct guess
                                progressSong(song, false);
                            } else {
                                //Wrong guess
                                //Subtracting the points
                                int toDeduct = submittingPointsToDeduct.get(difficulty);
                                if (points - toDeduct >= 0) {
                                    // Points fully deducted
                                    points -= toDeduct;
                                    Log.i(TAG, pointsToDeduct + " points deducted");
                                } else {
                                    // Score goes to 0 because it can't be negative
                                    // User can keep guessing (only skipping and revealing is not
                                    // allowed if was to drop below zero)
                                    points = 0;
                                    Log.e(TAG, "Points gone to zero.");
                                    Toast.makeText(MapsActivity.this,
                                            R.string.toast_score_zero, Toast.LENGTH_LONG).show();
                                }
                                updateScore(points);
                                Toast.makeText(MapsActivity.this, R.string.toast_wrong_song, Toast.LENGTH_SHORT).show();
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

        //Skip song floating action button listener
        FloatingActionButton skipSongButton = findViewById(R.id.skipSongButton);
        skipSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Song song = Song.getSong(songNumber, songList);
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
                            if (points - submittingPointsToScore.get(difficulty) >= 0) {
                                progressSong(song, true);
                            } else {
                                Log.e(TAG, "Not enough points to skip song.");
                                Toast.makeText(MapsActivity.this, R.string.toast_no_points, Toast.LENGTH_SHORT).show();
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

        //Go to lyrics floating action button listener
        FloatingActionButton lyrics_button = findViewById(R.id.lyricsButton);
        lyrics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (allWords == null) {
                    Toast.makeText(MapsActivity.this, R.string.lyrics_connection_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<String> wordsFound = Song.findWordsFound(lyricsFound,allWords);
                //Go to LyricsActivity
                Intent intent = new Intent(MapsActivity.this, LyricsActivity.class);
                //Put the words found as extra to populate the list
                intent.putStringArrayListExtra(WORDS_FOUND_KEY, wordsFound);
                startActivity(intent);
            }
        });

        //Zoom on current location button listener
        FloatingActionButton location_button = findViewById(R.id.locationButton);
        location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation == null) {
                    Log.e(TAG, "User tried to zoom to his location with no last location");
                    Toast.makeText(MapsActivity.this, R.string.toast_location_error, Toast.LENGTH_SHORT).show();
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
        //Connect the Google API Client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //disconnect the Google API Client
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregister the network receiver
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    /**
     * Creates a location request using the Google API
     */
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
            //Permissions already granted
            mLastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            //Request permission to access location
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
        //Update the distance travelled and change the last location to the current location
        updateDistance(current);
        mLastLocation = current;
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
                    locationInitializer();

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
                    Toast.makeText(this, R.string.toast_error_location_track, Toast.LENGTH_LONG).show();
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
        String collectMessage = String.format(Locale.ENGLISH, "Collect(+%d)", pointsToScore.get(lyricClassification));
        builder.setNeutralButton(collectMessage, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User wants to collect lyric
                if (distance <= COLLECTION_DISTANCE_MAXIMUM) {
                    //User is close enough to collect
                    addToLyricsFound(lyricName);
                    points += pointsToScore.get(lyricClassification);
                    updateScore(points);
                    Toast.makeText(MapsActivity.this, getString(R.string.toast_collected) + Song.lyricToWord(lyricName,allWords), Toast.LENGTH_LONG).show();
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

        String revealMessage = String.format(Locale.ENGLISH, "Reveal(-%d)", pointsToDeduct.get(lyricClassification));
        builder.setNegativeButton(revealMessage, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User wants to reveal lyric
                int toDeduct = pointsToDeduct.get(lyricClassification);
                if (points - toDeduct >= 0) {
                    //User has enough points to reveal the lyric
                    addToLyricsFound(lyricName);
                    points -= pointsToDeduct.get(lyricClassification);
                    updateScore(points);
                    Toast.makeText(MapsActivity.this, getString(R.string.toast_revealed) + Song.lyricToWord(lyricName,allWords), Toast.LENGTH_LONG).show();
                    Log.i(TAG, "User revealed lyric " + lyricName + " and deducted " +
                            pointsToDeduct.get(lyricClassification) + " points.");
                    marker.remove();
                } else {
                    Log.e(TAG, "Not enough points to reveal lyric.");
                    Toast.makeText(MapsActivity.this, R.string.toast_no_points, Toast.LENGTH_SHORT).show();
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

        //Map to zoom in George Square by default
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GEORGE_SQUARE_LATLNG, 17));
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationInitializer();
        }
        mMap.setOnMarkerClickListener(this);
    }

    /**
     * Visualise current position using Google API
     */
    private void locationInitializer() {
        try {
            // Visualise current position with a small blue circle
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            Log.e(TAG, se.getMessage());
            Log.e(TAG, "Security exception thrown [onMapReady]");
        }
        //Hide the default location button, as I use a custom zoom to location button
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    /**
     * Update the Map with new Placemarks
     * @param result List of Placemarks to be added to the map
     */
    public void updateMap(List<Placemark> result) {
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
                    continue;
                }
                mMap.addMarker(new MarkerOptions().position(
                        new LatLng(pl.getLatitude(), pl.getLongitude())).title(pl.getDescription()).visible(true)
                        .icon(BitmapDescriptorFactory.fromResource(id)));
            }
            Log.i(TAG, "Placemarks successfully added to the map");
        } else {
            Log.e(TAG, "Placemarks not loaded!");
        }
    }

}



