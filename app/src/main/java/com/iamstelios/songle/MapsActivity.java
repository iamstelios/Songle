package com.iamstelios.songle;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = MapsActivity.class.getSimpleName();
    //TODO Callibrate distance
    private static final float COLLECTION_DISTANCE_MAXIMUM = 20;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //TODO: CHECK WHAT HAPPENS WHEN I DONT HAVE PERMISSIONS AND CHANGE ACCORDINGLY
    private boolean mLocationPermissionGranted = false;
    private Location mLastLocation;
    private final LatLng GEORGE_SQUARE_LATLNG = new LatLng(55.944251, -3.189111);

    private final String SONGS_XML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/songs.xml";
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
            put("boring", 50);
            put("unclassified", 200);
        }
    };

    //Return the song given the song's number
    private Song getSong(String songNumber) {
        try {
            for (Song song : songList) {
                if (song.number.equals(songNumber)) return song;
            }
        } catch (Exception e) {
            Log.e(TAG, "Tried to access song list when not loaded!");
        }
        return null;
    }

    //Update the screen showing the score and save the score in the preferences
    private void updateScore(int points) {
        TextView scoreText = (TextView) findViewById(R.id.scoreText);
        scoreText.setText(String.valueOf(points));
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        editor.putInt(MainActivity.POINTS_KEY, points);
        editor.apply();
    }

    //Add a lyric and update the lyrics found in the preferences
    private void addToLyricsFound(String lyric) {
        lyricsFound.add(lyric);
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();
    }


    //Takes as a parameter the current song and progresses the game to the next song
    private void progressSong(Song song) {
        int currentSongNum = Integer.parseInt(song.number);
        //TODO: Make a test about this case
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE).edit();
        //Check if all the songs have been guesses
        if (currentSongNum >= songList.size()) {
            Log.i(TAG, "User completed the game!");
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setMessage(R.string.dialog_complete_message)
                    //TODO Add calories and distance walked
                    .setTitle(R.string.dialog_complete_title);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    //Do Nothing
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            editor.remove(MainActivity.LYRICS_FOUND_KEY);
            editor.remove(MainActivity.DIFFICULTY_KEY);
            editor.remove(MainActivity.POINTS_KEY);
            editor.remove(MainActivity.SONG_KEY);
            //Commit changes because we want to be sure the continue button
            // will be not be present in main activity
            editor.commit();

            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            startActivity(intent);
            //TODO CHECK IF RETURN NEEDED
            return;
        }
        Toast.makeText(this, "Congrats! You've progressed to the next song!", Toast.LENGTH_LONG).show();
        Log.i(TAG, "User guess right and goes to the next song");
        //Change the song number
        songNumber = String.format(Locale.ENGLISH, "%02d", currentSongNum + 1);
        editor.putString(MainActivity.SONG_KEY, songNumber);
        //Reset the lyrics found
        lyricsFound = new HashSet<String>();
        editor.putStringSet(MainActivity.LYRICS_FOUND_KEY, lyricsFound);
        editor.apply();
        //Add the points to the player score
        points += 500;
        updateScore(points);

        //Update the Placemark map for the new song
        new DownloadPlacemarksTask().execute(String.format(KML_URL, songNumber, difficulty));
        Log.i(TAG, "Map Updated with new Song");
        //Download the text for the new song
        new DownloadLyricsTask().execute(String.format(WORDS_URL, songNumber));
    }

    //Return a placemark of the matching position otherwise null
    private Placemark placemarkFromLatLng(LatLng position) {
        for (Placemark pl : placemarkList) {
            if (pl.latitude == position.latitude && pl.longitude == position.longitude) {
                return pl;
            }
        }
        Log.e(TAG, "Position of marker doesn't match any placemarks");
        return null;
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
        }
        Log.i(TAG, "Map Created");
        //Download the song list
        new DownloadSongsTask().execute(SONGS_XML_URL);
        //Retrieve the user preferences and stats
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS, MODE_PRIVATE);
        difficulty = prefs.getString(MainActivity.DIFFICULTY_KEY, getString(R.string.difficulty_easy));
        songNumber = prefs.getString(MainActivity.SONG_KEY, getString(R.string.first_song_number));
        lyricsFound = prefs.getStringSet(MainActivity.LYRICS_FOUND_KEY, new HashSet<String>());
        points = prefs.getInt(MainActivity.POINTS_KEY, MainActivity.STARTING_POINTS);
        updateScore(points);
        //String difficulty = getIntent().getExtras().getString(MainActivity.DIFFICULTY_KEY);
        Log.i(TAG, "Song Number: " + songNumber + " Difficulty: " + difficulty);
        //Toast.makeText(this, "Difficulty: " +
        //        getResources().getStringArray(R.array.difficulties)[Integer.parseInt(difficulty)-1],
        //        Toast.LENGTH_SHORT).show();

        //Download the Placemark map
        new DownloadPlacemarksTask().execute(String.format(KML_URL, songNumber, difficulty));
        //Download the text of the song
        new DownloadLyricsTask().execute(String.format(WORDS_URL, songNumber));

        FloatingActionButton submit_button = (FloatingActionButton) findViewById(R.id.submitButton);
        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Song song = getSong(songNumber);
                if (song == null) {
                    Toast.makeText(MapsActivity.this,
                            "Songs not loaded. Go back and check your connection.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    //Show a dialog that asks the user if he wants to submit the song
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    //Set the message and title of the dialog
                    builder.setMessage(R.string.dialog_submit_message)
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
                            if (submission.equalsIgnoreCase(song.title)) {
                                //Correct guess
                                progressSong(song);
                            } else {
                                //Wrong guess
                                points -= 10;
                                updateScore(points);
                                Toast.makeText(MapsActivity.this, "Wrong Song! Try Again :)", Toast.LENGTH_SHORT).show();
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

        //Go to lyrics floating action button
        FloatingActionButton lyrics_button = (FloatingActionButton) findViewById(R.id.lyricsButton);
        lyrics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> wordsFound = findWordsFound(lyricsFound);
                Intent intent = new Intent(MapsActivity.this, LyricsActivity.class);
                intent.putStringArrayListExtra(WORDS_FOUND_KEY, wordsFound);
                startActivity(intent);
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
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
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
        if (mLastLocation == null) {
            Toast.makeText(this, "You must open location services and track your location first! (Upper right corner)", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Player must open track location before accessing markers");
            return true;
        }
        Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                markerPosition.latitude, markerPosition.longitude, results);
        final float distance = results[0];
        final Placemark lyricSelected = placemarkFromLatLng(markerPosition);
        final String lyricName = lyricSelected.name;
        final String lyricClassification = lyricSelected.description;
        Log.i(TAG, "Distance between player and lyric " + lyricName + " selected is approximately"
                + distance + " meters");

        //Show a dialog that asks the user if he wants to collect or reveal the lyric
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        //Set the message and title of the dialog
        builder.setMessage(R.string.dialog_lyric_message)
                .setTitle(R.string.dialog_lyric_title);
        //Notice Neutral is clicking collect, negative is revealing and positive is canceling
        builder.setNeutralButton(R.string.collect, new DialogInterface.OnClickListener() {
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

        //TODO CHECK WHY IT R.string.reveal DOESNT WORK
        //String revealMessage = R.string.reveal + "(-" + pointsToDeduct.get(lyricClassification) + ")";
        String revealMessage = "Reveal(-" + pointsToDeduct.get(lyricClassification) + ")";
        builder.setNegativeButton(revealMessage, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User wants to reveal lyric
                addToLyricsFound(lyricName);
                points -= pointsToDeduct.get(lyricClassification);
                updateScore(points);
                //TODO add message with the actual lyric
                Toast.makeText(MapsActivity.this, "Yay! You've revealed: " + lyricToWord(lyricName), Toast.LENGTH_LONG).show();
                Log.i(TAG, "User reveal lyric " + lyricName + " and deducted " +
                        pointsToDeduct.get(lyricClassification) + " points.");
                marker.remove();
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
                            this, R.raw.style_json));

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

        try {
            // Visualise current position with a small blue circle
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            Log.e(TAG, "Security exception thrown [onMapReady]");
        }
        // Add ``My location'' button to the user interface
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMarkerClickListener(this);

    }

    /*
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    */
    //Class for the Songs
    public static class Song {
        public final String number;
        public final String artist;
        public final String title;
        public final String link;

        private Song(String number, String artist, String title, String link) {
            this.number = number;
            this.artist = artist;
            this.title = title;
            this.link = link;
        }
    }

    //Instantiating the parser for extracting Songs from xml
    private static final String ns = null;

    List<Song> parseSongs(InputStream in) throws XmlPullParserException,
            IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                    false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    //Reading the XML feed
    private List<Song> readFeed(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        List<Song> entries = new ArrayList<Song>();
        parser.require(XmlPullParser.START_TAG, ns, "Songs");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the song tag
            if (name.equals("Song")) {
                entries.add(readSong(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    //Reading a song from the XML feed
    private Song readSong(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Song");
        String number = null;
        String artist = null;
        String title = null;
        String link = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("Number")) {
                number = readNumber(parser);
            } else if (name.equals("Artist")) {
                artist = readArtist(parser);
            } else if (name.equals("Title")) {
                title = readTitle(parser);
            } else if (name.equals("Link")) {
                link = readLink(parser);
            } else {
                skip(parser);
            }
        }
        return new Song(number, artist, title, link);
    }

    //Reading a number of a song
    private String readNumber(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Number");
        String number = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Number");
        return number;
    }

    //Reading an artist
    private String readArtist(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Artist");
        String artist = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Artist");
        return artist;
    }

    //Reading a title of a song
    private String readTitle(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Title");
        return title;
    }

    //Reading a link
    private String readLink(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Link");
        String link = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Link");
        return link;
    }

    //Reading text
    private String readText(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    //Skipping uninteresting tags in case they exist
    private void skip(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // Also available: HttpsURLConnection
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private List<Song> loadSongsFromNetwork(String urlString) throws
            XmlPullParserException, IOException {
        List<Song> songs;
        try (InputStream stream = downloadUrl(urlString)) {
            songs = parseSongs(stream);
        }
        return songs;
    }

    private class DownloadSongsTask extends AsyncTask<String, Void, List<Song>> {
        private static final String error_load = "Unable to load content. Check your network connection.";
        private static final String error_xml = "Error parsing XML song list.";

        @Override
        protected List<Song> doInBackground(String... urls) {
            try {
                return loadSongsFromNetwork(urls[0]);
            } catch (IOException e) {
                //Toast.makeText(MapsActivity.this, error_load, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "DownloadSongsTask: " + error_load + " RETURNING NULL!");
                return null;
            } catch (XmlPullParserException e) {
                //Toast.makeText(MapsActivity.this, error_xml, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "DownloadSongsTask: " + error_xml + " RETURNING NULL!");
                return null;
            }//TODO: CHECK ALL USES OF THE SONGS LIST ARE CHECKING NULL
        }

        @Override
        protected void onPostExecute(List<Song> result) {
            Log.i(TAG, "DownloadSongsTask finished");
            songList = result;
        }
    }


    public static class Placemark {
        public final String name;
        public final String description;
        public final double latitude;
        public final double longitude;

        public Placemark(String name, String description, double latitude, double longitude) {
            this.name = name;
            this.description = description;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    List<Placemark> parsePlacemarkers(InputStream in) throws XmlPullParserException,
            IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                    false);
            parser.setInput(in, null);
            parser.nextTag();
            return readKmlFeed(parser);
        } finally {
            in.close();
        }
    }

    //Reading the XML feed
    private List<Placemark> readKmlFeed(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        List<Placemark> entries = new ArrayList<Placemark>();
        parser.require(XmlPullParser.START_TAG, ns, "kml");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the Document tag
            if (name.equals("Document")) {
                return readDocument(parser);
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    private List<Placemark> readDocument(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Document");
        List<Placemark> entries = new ArrayList<Placemark>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the Placemark tag
            if (name.equals("Placemark")) {
                entries.add(readPlacemark(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    //Reading a Placemark from the XML feed
    private Placemark readPlacemark(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Placemark");
        String name = null;
        String description = null;
        double latitude = 0;
        double longitude = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String tag = parser.getName();
            if (tag.equals("name")) {
                name = readName(parser);
            } else if (tag.equals("description")) {
                description = readDescription(parser);
            } else if (tag.equals("Point")) {
                String point = readPoint(parser);
                try {
                    String[] coordinates = point.split(",");
                    latitude = Double.parseDouble(coordinates[1]);
                    longitude = Double.parseDouble(coordinates[0]);
                } catch (NullPointerException e) {
                    Log.e(TAG, "The xml provided is incorrectly structured");
                }
            } else {
                skip(parser);
            }
        }
        return new Placemark(name, description, latitude, longitude);
    }

    //Reading the point of a placemark
    private String readPoint(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Point");
        String coordinates = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("coordinates")) {
                coordinates = readCoordinates(parser);
            } else {
                skip(parser);
            }
        }
        return coordinates;
    }

    //Reading a name of a placemark
    private String readName(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return name;
    }

    //Reading the description
    private String readDescription(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String description = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");
        return description;
    }

    //Reading a coordinates of the placemark
    private String readCoordinates(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "coordinates");
        String coordinates = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "coordinates");
        return coordinates;
    }

    //Load the Kml file with all the Placemarks
    private List<Placemark> loadKmlFromNetwork(String urlString) throws
            XmlPullParserException, IOException {
        List<Placemark> placemarks;
        try (InputStream stream = downloadUrl(urlString)) {
            placemarks = parsePlacemarkers(stream);
        }
        return placemarks;
    }

    private class DownloadPlacemarksTask extends AsyncTask<String, Void, List<Placemark>> {
        private static final String error_load = "Unable to load content. Check your network connection.";
        private static final String error_xml = "Error parsing placemarkers list.";

        @Override
        protected List<Placemark> doInBackground(String... urls) {
            try {
                return loadKmlFromNetwork(urls[0]);
            } catch (IOException e) {
                //Toast.makeText(MapsActivity.this, error_load, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "DownloadPlacemarksTask: " + error_load + " RETURNING NULL!");
                return null;
            } catch (XmlPullParserException e) {
                //Toast.makeText(MapsActivity.this, error_xml, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "DownloadPlacemarksTask: " + error_xml + " RETURNING NULL!");
                return null;
            }//TODO: CHECK ALL USES OF THE PLACEMARKS LIST ARE CHECKING NULL
        }

        @Override
        protected void onPostExecute(List<Placemark> result) {
            if (result != null) {
                placemarkList = result;
                for (Placemark pl : result) {
                    //skip if a placemark is null (normally never)
                    if (pl == null) continue;
                    int id = getResources().getIdentifier(pl.description, "drawable",
                            getPackageName());
                    //If the lyric was already found don't place its marker on the map
                    if (lyricsFound.contains(pl.name)) {
                        Log.i(TAG, "Lyric with name " + pl.name + " already found.");
                        //TODO CHECK IF DELETION NEEDED (if results are going to be used again)
                        //placemarkList.remove(pl);
                        continue;
                    }
                    mMap.addMarker(new MarkerOptions().position(
                            new LatLng(pl.latitude, pl.longitude)).title(pl.description).visible(true)
                            .icon(BitmapDescriptorFactory.fromResource(id)));
                    //TODO: SAVE MARKERS FOR LATER USE??
                }
                //Toast.makeText(MapsActivity.this, "Map Loaded Successfully", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Placemarks successfully added to the map");
            } else {
                Log.e(TAG, "Placemarks not loaded!");
            }
        }
    }


    //Receives the lyric in the line:word form (eg.15:3) and returns the actual word
    private String lyricToWord(String lyric) {
        String[] lineWord = lyric.split(":");
        try {
            int lineNum = Integer.parseInt(lineWord[0]);
            int wordNum = Integer.parseInt(lineWord[1]);
            // The word is located in lineNum-1 because the index starts with 0
            // Word position in the line is wordNum+1 because position 1 is the line Number
            return allWords.get(lineNum - 1)[wordNum+1];
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unexpected lyric found!");
            return null;
        } catch (NullPointerException e) {
            Log.e(TAG, "allWords not initialized");
            return null;
        }
    }

    //Return the actual words found, given their "coordinates" and all the words
    private ArrayList<String> findWordsFound(Set<String> lyricsFound) {
        if (lyricsFound == null) {
            Log.e(TAG, "Input contains null, returning EMPTY list.");
            return new ArrayList<>();
        }
        ArrayList<String> wordsFound = new ArrayList<>();
        for (String lyric : lyricsFound) {
            wordsFound.add(lyricToWord(lyric));
        }
        return wordsFound;
    }

    //Manipulate the input file and return the parsed lyrics
    private ArrayList<String[]> parseLyrics(InputStream stream) {
        Log.i(TAG, "Parsing the text file");
        ArrayList<String[]> words = new ArrayList<>();
        try (Scanner scanner = new Scanner(stream)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //Splits and returns only words
                //splitting on not word characters, [-] and [']
                //Note: first string is empty, and second is the line number
                //I will leave it as it, not to use much processing power
                words.add(line.split("[^\\w'-]+"));
            }
            scanner.close();
            return words;
        }
    }

    private ArrayList<String[]> loadLyricsFromNetwork(String urlString)
            throws IOException {
        ArrayList<String[]> lyrics;
        try (InputStream stream = downloadUrl(urlString)) {
            lyrics = parseLyrics(stream);
        }
        return lyrics;
    }

    private class DownloadLyricsTask extends AsyncTask<String, Void, ArrayList<String[]>> {
        private static final String error_load = "Unable to load content. Check your network connection.";

        @Override
        protected ArrayList<String[]> doInBackground(String... urls) {
            try {
                return loadLyricsFromNetwork(urls[0]);
            } catch (IOException e) {
                Log.e(TAG, "DownloadLyricsTask: " + error_load + " RETURNING NULL!");
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String[]> result) {
            Log.i(TAG, "DownloadLyricsTask finished");
            allWords = result;
        }
    }


}



