package com.iamstelios.songle;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Xml;
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
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    //TODO: ADD other TAGs and Logs or remove from here
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private Location mLastLocation;

    private final String SONGS_XML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/songs.xml";
    private List<Song> songList;
    private final String KML_URL = "http://www.inf.ed.ac.uk/teaching/courses/selp/data/songs/%s/map%s.kml";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Download the song list
        new DownloadSongsTask().execute(SONGS_XML_URL);

        //Retrieve the user preferences
        SharedPreferences prefs = getSharedPreferences(MainActivity.USER_PREFS,MODE_PRIVATE);
        String difficulty = prefs.getString(MainActivity.DIFFICULTY_KEY,"1");
        String songNumber = prefs.getString(MainActivity.SONG_KEY,"01");
        //String difficulty = getIntent().getExtras().getString(MainActivity.DIFFICULTY_KEY);
        Log.i(TAG,"Song Number: "+songNumber+" Difficulty: "+difficulty);
        //Toast.makeText(this, "Difficulty: " +
        //        getResources().getStringArray(R.array.difficulties)[Integer.parseInt(difficulty)-1],
        //        Toast.LENGTH_SHORT).show();

        //Download the Placemark map
        new DownloadPlacemarksTask().execute(String.format(KML_URL,songNumber,difficulty));

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }
        Log.i(TAG,"Map Created");
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
        //TODO: Do something with current location
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
        LatLng georgeSquare = new LatLng(55.944251, -3.189111);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(georgeSquare, 17));

        try {
            // Visualise current position with a small blue circle
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            Log.e(TAG, "Security exception thrown [onMapReady]");
        }
        // Add ``My location'' button to the user interface
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

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
            Log.i(TAG,"DownloadSongsTask finished");
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

    private List<Placemark> readDocument(XmlPullParser parser)throws
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
                }catch (NullPointerException e){
                    Log.e(TAG,"The xml provided is incorrectly structured");
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
            }else {
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
                for (Placemark pl : result) {
                    int id = getResources().getIdentifier(pl.description, "drawable",
                            getPackageName());
                    mMap.addMarker(new MarkerOptions().position(
                            new LatLng(pl.latitude, pl.longitude)).title(pl.description).visible(true)
                                    .icon(BitmapDescriptorFactory.fromResource(id)));
                    //TODO: SAVE MARKERS FOR LATER USE
                }
                //Toast.makeText(MapsActivity.this, "Map Loaded Successfully", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Placemarks successfully added to the map");
            }else{
                Log.e(TAG,"Placemarks not loaded!");
            }
        }
    }

}



