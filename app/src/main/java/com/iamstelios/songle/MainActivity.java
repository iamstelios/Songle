package com.iamstelios.songle;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    //Used as the keys to the user preferences
    public static final String USER_PREFS = "user_preferences";
    public static final String SONG_KEY = "song_key";
    public static final String DIFFICULTY_KEY = "difficulty_key";
    public static final String LYRICS_FOUND_KEY = "lyrics_found_key";
    public static final String POINTS_KEY = "points_key";
    public static final String GLOBAL_PREFS = "global_preferences";
    public static final String NETWORK_PREF_KEY = "network_pref_key";
    public static final String SONG_DIST_KEY = "song_distance_key";
    public static final String TOTAL_DIST_KEY = "total_distance_key";
    public static final String HIGHSCORE_KEY = "highscore_key";
    public static final String TOTAL_SONGS_FOUND_KEY = "total_songs_found_key";
    public static final String TOTAL_GUESS_ATTEMPTS = "total_guess_attempts";
    public static final String CURRENT_SONGS_FOUND_KEY = "current_songs_found";
    public static final String CURRENT_SONGS_SKIPPED_KEY = "current_songs_skipped";
    public static final String IS_MUSIC_ON_KEY = "is_music_on";
    public static final String SONGS_USED_KEY = "songs_used_key";
    /**
     * URL that the songs xml is located
     */
    private final String SONGS_XML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/songs.xml";

    /**
     * The score that a new game has initially
     */
    public static int STARTING_POINTS = 500;

    /**
     * Difficulty values (should be between "1" and "5")
     */
    private String difficulty;
    /**
     * List of all the Songs in the game
     */
    private static List<Song> songList;

    /**
     * Receiver used to check the connection
     */
    private NetworkReceiver receiver;
    //Used to store the current instance created on onCreate
    private static MainActivity instance;

    //Getter for the instance
    public static MainActivity getInstance() {
        return instance;
    }

    //Setter for the instance
    public static void setInstance(MainActivity instance) {
        MainActivity.instance = instance;
    }

    /**
     * Shows the dialog when the user finisher the game
     *
     * @param artist       Artist of the last song
     * @param title        Title of the last song
     * @param link         Link of the last song
     * @param points       Points earned
     * @param songsFound   Songs found in the session
     * @param songsSkipped Songs skipped in the session
     * @param skipped      Was the last song skipped?
     */
    private void showCompletedGameDialog(String artist, String title, final String link, int points,
                                         int songsFound, int songsSkipped, boolean skipped) {
        int totalSongs = songsFound + songsSkipped;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
        //Message different when user skipped last song
        String skippedMessage =
                String.format(Locale.ENGLISH, getString(R.string.dialog_completed_skipped_message),
                        points, songsFound, totalSongs);
        String correctMessage =
                String.format(Locale.ENGLISH,
                        getString(R.string.dialog_completed_correct_message),
                        artist, title, points, songsFound, totalSongs);
        builder.setMessage(skipped ? skippedMessage : correctMessage)
                .setTitle(R.string.dialog_complete_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                //Do Nothing
            }
        });
        if (!skipped) {
            builder.setNegativeButton(R.string.listen_youtube, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User wants to listen on youtube
                    Song.watchYoutubeVideo(getInstance(), link);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Setter method for songList
     * @param songList List to be assigned
     */
    public void setSongList(List<Song> songList) {
        this.songList = songList;
    }

    /**
     * Getter method for songList
     * @return songList
     */
    public static List<Song> getSongList() {
        return songList;
    }

    /**
     * Chooses a new song number from the songs not used yet
     * @param size Number of total songs
     * @return New song number
     */
    public String getNewSongNum(int size) {

        SharedPreferences prefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        Set<String> songsUsed = prefs.getStringSet(SONGS_USED_KEY, new HashSet<String>());

        String songNum;
        int min = 1;
        int max = size;
        do {
            //No need to check for completion as the program won't call this method if all songs used
            int random = new Random().nextInt((max - min) + 1) + min;
            songNum = String.format(Locale.ENGLISH, "%02d", random);
        } while (songsUsed.contains(songNum));
        songsUsed.add(songNum);
        //Update the songs used set with the new song
        SharedPreferences.Editor editor = getSharedPreferences(USER_PREFS, MODE_PRIVATE).edit();
        editor.putStringSet(SONGS_USED_KEY, songsUsed);
        editor.apply();
        return songNum;
    }

    /**
     * Dialog for choosing difficulty when starting a new game
     */
    private void chooseDifficulty() {
        //Using an Alert Dialog to choose difficulty
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //Set the dialog characteristics
        builder.setTitle(R.string.dialog_difficulty)
                .setItems(R.array.difficulties, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        difficulty = String.valueOf(which + 1);
                        //Resetting the values of the game
                        SharedPreferences.Editor editor =
                                getSharedPreferences(USER_PREFS, MODE_PRIVATE).edit();
                        editor.putString(DIFFICULTY_KEY, difficulty);
                        //Starting for the first song if it's a new game
                        editor.putString(SONG_KEY, getNewSongNum(songList.size()));
                        //Set the lyrics found to empty
                        editor.putStringSet(LYRICS_FOUND_KEY, new HashSet<String>());
                        editor.putFloat(SONG_DIST_KEY, 0);
                        editor.putInt(POINTS_KEY, STARTING_POINTS);
                        editor.putInt(CURRENT_SONGS_FOUND_KEY, 0);
                        editor.putInt(CURRENT_SONGS_SKIPPED_KEY, 0);
                        editor.putStringSet(SONGS_USED_KEY, new HashSet<String>());
                        editor.apply();
                        Toast.makeText(MainActivity.this, R.string.new_game_toast,
                                Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Started a new game");
                        //Load the Maps Activity
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //DO NOTHING
                        Log.i(TAG, "User canceled selecting the difficulty");
                    }
                });
        //Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Check if user has progress stored
     * @return True if user has progress
     */
    private boolean hasProgress() {
        //Check if user has progress by checking if difficulty is stored
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE);
        return prefs.contains(DIFFICULTY_KEY);
    }

    //Used to for binding the music service
    private boolean mIsBound = false;
    private MusicService mServ;
    //Service connection for the music service
    private ServiceConnection Scon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mServ = ((MusicService.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    /**
     * Bind the music service to the context
     */
    void doBindService() {
        bindService(new Intent(this, MusicService.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbind the music service from the context
     */
    void doUnbindService() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    /**
     * Prepares the background music and assigns the proper image to the music button
     */
    private void prepareMusic() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        //Music is on on the first run
        boolean isMusicOn = prefs.getBoolean(IS_MUSIC_ON_KEY, true);
        ImageButton MusicButton = (ImageButton) findViewById(R.id.MusicButton);
        if (isMusicOn) {
            //Music button should show that the music is on
            MusicButton.setImageResource(R.drawable.music_on);
            if (mServ != null) {
                //Resume the music if it was started before
                mServ.resumeMusic();
            } else {
                //Start the music if it's the first time playing it this run
                Intent music = new Intent();
                music.setClass(this, MusicService.class);
                startService(music);
            }
        } else {
            //Music button should show that the music is off
            MusicButton.setImageResource(R.drawable.music_off);
            if (mServ != null) {
                //Pause the music as with the player choice
                mServ.pauseMusic();
            }
        }
    }

    /**
     * Flips the music preference on/off
     */
    private void flipMusic() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        boolean isMusicOn = prefs.getBoolean(IS_MUSIC_ON_KEY, true);
        SharedPreferences.Editor editor = getSharedPreferences(GLOBAL_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(IS_MUSIC_ON_KEY, !isMusicOn);
        editor.apply();
    }

    /**
     * Show continue button if there is existing progress
     * <p>Hide continue button if no progress</p>
     */
    private void prepareContinue(){
        View continueButton = findViewById(R.id.ContinueButton);
        if (!hasProgress()) {
            continueButton.setVisibility(View.GONE);
        } else {
            continueButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Main Activity started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Save the instance created here, so it can be reference by other activities
        setInstance(this);

        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver(NetworkReceiver.MAIN_KEY);
        this.registerReceiver(receiver, filter);
        // Make sure continue button is visible when there is progress
        prepareContinue();

        //Bind the music service
        doBindService();
        //Play the background music if it's turned on
        prepareMusic();

        Intent intent = this.getIntent();
        // Obtain Extras from Intent if this MainActivity started
        // after a game was completed in MapsActivity
        if (intent.hasExtra("artist")) {
            //Assuming that if the intent has the artist extra
            // then it has the other extras too
            String artist = intent.getExtras().getString("artist");
            String title = intent.getExtras().getString("title");
            String link = intent.getExtras().getString("link");
            int points = intent.getExtras().getInt("points");
            int songsFound = intent.getExtras().getInt("songsFound");
            int songsSkipped = intent.getExtras().getInt("songsSkipped");
            boolean skipped = intent.getExtras().getBoolean("skipped");

            showCompletedGameDialog(artist, title, link, points, songsFound, songsSkipped, skipped);
            //Clean the extras
            intent.removeExtra("artist");
            intent.removeExtra("title");
            intent.removeExtra("link");
            intent.removeExtra("points");
            intent.removeExtra("songsFound");
            intent.removeExtra("songsSkipped");
            intent.removeExtra("skipped");
        }
        //New Game button listener
        ImageButton newGameButton = (ImageButton) findViewById(R.id.NewGameButton);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songList == null) {
                    Toast.makeText(MainActivity.this, R.string.internet_connection_toast,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "User tried to start a new game without internet connection");
                    return;
                }
                if (hasProgress()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.dialog_new_game_message)
                            .setTitle(R.string.dialog_new_game_title);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button, proceed
                            chooseDifficulty();
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog and doesn't want to lose progress
                            Log.i(TAG, "User canceled selecting new game");
                        }
                    });
                    //Get the AlertDialog from create()
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    //No other progress so starting a new game won't cause problems
                    chooseDifficulty();
                }

            }
        });
        View continueButton = findViewById(R.id.ContinueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songList == null) {
                    Toast.makeText(MainActivity.this, R.string.internet_connection_toast, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "User tried to continue a game without an internet connection to download the songList");
                    return;
                }
                //Check if the user already has progress by checking if he has a difficulty set
                if (hasProgress()) {
                    Toast.makeText(MainActivity.this, R.string.continue_toast, Toast.LENGTH_SHORT).show();
                    //Load the Maps Activity
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    //No need to pass extras, all properties are stored in the preferences
                    startActivity(intent);
                } else {
                    Log.e(TAG, "User pressed continue button while having no progress");
                }
            }
        });

        //Connection button changes the data connection preference of the user
        ImageButton connectionButton = (ImageButton) findViewById(R.id.ConnectionButton);
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences.Editor editor =
                        getSharedPreferences(GLOBAL_PREFS, MODE_PRIVATE).edit();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialog_connection_title);
                builder.setItems(R.array.connection_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        String networkPref;
                        if (which == 0) {
                            //Only over wifi
                            networkPref = NetworkReceiver.WIFI;
                            Toast.makeText(MainActivity.this, R.string.toast_wifi_only,
                                    Toast.LENGTH_LONG).show();
                        } else if (which == 1) {
                            //Use data connection too
                            networkPref = NetworkReceiver.ANY;
                            Toast.makeText(MainActivity.this, R.string.toast_wifi_data,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            return;
                        }
                        editor.putString(NETWORK_PREF_KEY, networkPref);
                        editor.apply();
                        Log.i(TAG, "Data connection preferences changed to " + networkPref);

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });
        //Stats Button shows some statistics of the user
        ImageButton statsButton = (ImageButton) findViewById(R.id.StatsButton);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs =
                        getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
                int highscore = prefs.getInt(HIGHSCORE_KEY, 0);
                int totalSongsFound = prefs.getInt(TOTAL_SONGS_FOUND_KEY, 0);
                float totalDistance = prefs.getFloat(TOTAL_DIST_KEY, 0);
                int totalGuessAttempts = prefs.getInt(TOTAL_GUESS_ATTEMPTS, 1);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.stats);
                String message =
                        String.format(Locale.ENGLISH,getString(R.string.dialog_stats_message),
                        round(totalDistance),highscore,totalSongsFound,
                        round(((double) totalSongsFound / totalGuessAttempts) * 100));
                builder.setMessage(message);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        //Help button send the user to the HelpActivity that  provides instructions to play the game
        ImageButton helpButton = (ImageButton) findViewById(R.id.HelpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });
        //Used to switch the background music on and off
        ImageButton musicButton = (ImageButton) findViewById(R.id.MusicButton);
        musicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipMusic();
                prepareMusic();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        //Update the continue button visibility if progress has started
        prepareContinue();
        //Resume music if was playing
        prepareMusic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregister the network receiver
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        //Unbind the music service
        doUnbindService();
        //Stop the service
        Intent music = new Intent();
        music.setClass(this, MusicService.class);
        stopService(music);
        mServ.onDestroy();
    }

    /**
     * Downloads the SongList from the given URL
     */
    public void downloadSongList() {
        //Download the song list
        new Downloader().downloadSongs(SONGS_XML_URL);
    }

}
