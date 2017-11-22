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

    //private static final int REQUEST_PERMISSION_WRITE = 1001;
    //private boolean permissionGranted;
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
    private final String SONGS_XML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/songs.xml";

    public static int STARTING_POINTS = 500;

    //Used to determine the difficulty when starting a new game
    private String difficulty;

    private List<Song> songList;

    private NetworkReceiver receiver;

    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    public static void setInstance(MainActivity instance) {
        MainActivity.instance = instance;
    }

    private void showCompletedGameDialog(String artist, String title, final String link, int points, int songsFound, int songsSkipped, boolean skipped) {
        int totalSongs = songsFound + songsSkipped;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
        //Message different when user skipped last song
        builder.setMessage(skipped ? getString(R.string.dialog_complete_message_skipped) +
                "\nFinal score: " + points + "\n You've guessed " + songsFound + " out of " +
                totalSongs + " songs." :
                getString(R.string.dialog_complete_message) + " " + artist + " - " + title +
                        "\n Final score: " + points + "\n You've guessed " + songsFound +
                        " out of " + totalSongs + " songs.")
                .setTitle(R.string.dialog_complete_title);
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
                    MapsActivity.watchYoutubeVideo(getInstance(), link);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setSongList(List<Song> songList) {
        this.songList = songList;
    }

    public List<Song> getSongList() {
        return songList;
    }

    //Chooses a new song number from the songs not used yet
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


    //Dialog for choosing difficulty
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
                        SharedPreferences.Editor editor = getSharedPreferences(USER_PREFS, MODE_PRIVATE).edit();
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
                        Toast.makeText(MainActivity.this, R.string.new_game_toast, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Started a new game");
                        //Load the Maps Activity
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        //intent.putExtra(DIFFICULTY_KEY, difficulty);
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

    //Check if user has progress stored by checking if difficulty is stored
    private boolean hasProgress() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE);
        return prefs.contains(DIFFICULTY_KEY);
    }

    //Used to for binding the music service
    private boolean mIsBound = false;
    private MusicService mServ;
    private ServiceConnection Scon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mServ = ((MusicService.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, MusicService.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    //Prepares the Music
    private void prepareMusic() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        boolean isMusicOn = prefs.getBoolean(IS_MUSIC_ON_KEY, true);
        ImageButton MusicButton = (ImageButton) findViewById(R.id.MusicButton);
        if (isMusicOn) {
            MusicButton.setImageResource(R.drawable.music_on);
            if(mServ!=null){
                mServ.resumeMusic();
            }else{
                Intent music = new Intent();
                music.setClass(this,MusicService.class);
                startService(music);
            }
        } else {
            MusicButton.setImageResource(R.drawable.music_off);
            if(mServ!=null){
                mServ.pauseMusic();
            }
        }
    }

    //Flips the music preference on/off
    private void flipMusic() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        boolean isMusicOn = prefs.getBoolean(IS_MUSIC_ON_KEY, true);
        SharedPreferences.Editor editor = getSharedPreferences(GLOBAL_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(IS_MUSIC_ON_KEY, !isMusicOn);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Main Activity started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        setInstance(this);

        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver(NetworkReceiver.MAIN_KEY);
        this.registerReceiver(receiver, filter);

        //Hide continue button if no progress
        View continueButton = findViewById(R.id.ContinueButton);
        if (!hasProgress()) {
            continueButton.setVisibility(View.GONE);
        } else {
            continueButton.setVisibility(View.VISIBLE);
        }

        //Bind the music service
        doBindService();

        prepareMusic();


        Intent intent = this.getIntent();
        /* Obtain String from Intent  */
        //TODO check case where I finish game so I have intent, then start new game, then press back
        if (intent.hasExtra("artist")) {
            //Assumming that if the intent has the artist extra
            // then it has the other extras too
            String artist = intent.getExtras().getString("artist");
            String title = intent.getExtras().getString("title");
            String link = intent.getExtras().getString("link");
            int points = intent.getExtras().getInt("points");
            int songsFound = intent.getExtras().getInt("songsFound");
            int songsSkipped = intent.getExtras().getInt("songsSkipped");
            boolean skipped = intent.getExtras().getBoolean("skipped");

            showCompletedGameDialog(artist, title, link, points, songsFound, songsSkipped, skipped);

            intent.removeExtra("artist");
            intent.removeExtra("title");
            intent.removeExtra("link");
            intent.removeExtra("points");
            intent.removeExtra("songsFound");
            intent.removeExtra("songsSkipped");
            intent.removeExtra("skipped");
        }

        /*
        FloatingActionButton start_button = (FloatingActionButton) findViewById(R.id.start);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        ImageButton newGameButton = (ImageButton) findViewById(R.id.NewGameButton);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songList == null) {
                    Toast.makeText(MainActivity.this, R.string.internet_connection_toast, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "User tried to start a new game without internet connection");
                    return;
                }
                if (hasProgress()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.dialog_new_game_message)
                            .setTitle(R.string.dialog_new_game_title);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
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
                    startActivity(intent);
                } else {
                    Log.e(TAG, "User pressed continue button while having no progress");
                }
            }
        });

        //Connection button shanges the data connection preference of the user
        ImageButton connectionButton = (ImageButton) findViewById(R.id.ConnectionButton);
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences.Editor editor = getSharedPreferences(GLOBAL_PREFS, MODE_PRIVATE).edit();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose data connection settings");
                builder.setItems(R.array.connection_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        String networkPref;
                        if (which == 0) {
                            networkPref = NetworkReceiver.WIFI;
                            Toast.makeText(MainActivity.this,
                                    "Downloads are only over WiFi now!",
                                    Toast.LENGTH_LONG).show();
                        } else if (which == 1) {
                            networkPref = NetworkReceiver.ANY;
                            Toast.makeText(MainActivity.this, "Downloads are allowed with both WiFi and Data now!", Toast.LENGTH_LONG).show();
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

        ImageButton statsButton = (ImageButton) findViewById(R.id.StatsButton);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
                int highscore = prefs.getInt(HIGHSCORE_KEY, 0);
                int totalSongsFound = prefs.getInt(TOTAL_SONGS_FOUND_KEY, 0);
                float totalDistance = prefs.getFloat(TOTAL_DIST_KEY, 0);
                int totalGuessAttempts = prefs.getInt(TOTAL_GUESS_ATTEMPTS, 1);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Your stats");
                builder.setMessage("Total distance travelled: " + round(totalDistance) +
                        "m \n\nHighest score: " + highscore + "\n\nTotal number of songs found: " +
                        totalSongsFound + "\n\nGuessing Accuracy: " +
                        round(((double) totalSongsFound / totalGuessAttempts) * 100) + "%");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        ImageButton helpButton = (ImageButton) findViewById(R.id.HelpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });

        ImageButton musicButton = (ImageButton) findViewById(R.id.MusicButton);
        musicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipMusic();
                prepareMusic();
            }
        });

        /*
        //Ensuring permissions are granted
        if(!permissionGranted){
            checkPermissions();
            return;
        }
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO MAKE THIS A METHOD AND REMOVE DUPLICATE CODE
        //Update the continue button visibility if progress has started
        View continueButton = findViewById(R.id.ContinueButton);
        if (!hasProgress()) {
            continueButton.setVisibility(View.GONE);
        } else {
            continueButton.setVisibility(View.VISIBLE);
        }
        prepareMusic();
    }

    /*
    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
        mServ.onDestroy();
    }*/

    /*
    @Override
    protected void onPause() {
        super.onPause();
        mServ.pauseMusic();
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregister the network receiver
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        doUnbindService();
        mServ.onDestroy();
        //TODO REMOVE?
        //Unbind the background music service
        //doUnbindService();
        //mServ.onDestroy();
    }

    public void downloadSongList() {
        //Download the song list
        new Downloader().downloadSongs(SONGS_XML_URL);
    }

    /* Settings stuff TODO DELETE!

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Open Settings
            Log.i(TAG, "Accessing Settings");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */


    /*
    //TODO: CHECK IF STORAGE ACCESS IS NEEDED
    //Code below borrowed by https://gist.github.com/davidgassner/e92184e5c50daf59ffad8eb447cafde9
    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Initiate request for permissions.
    private boolean checkPermissions() {

        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "This app only works on devices with usable external storage",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE);
            return false;
        } else {
            return true;
        }
    }

    // Handle permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    Toast.makeText(this, "External storage permission granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You must grant permission!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    */
}
