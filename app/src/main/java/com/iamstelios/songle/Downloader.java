package com.iamstelios.songle;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Downloader {

    private static final String TAG = Downloader.class.getSimpleName();

    //Instantiating the parser for extracting Songs from xml
    private static final String ns = null;

    public void downloadPlacemarks(String URL){
        new DownloadPlacemarksTask().execute(URL);
    }
    public void downloadLyrics(String URL){
        new DownloadLyricsTask().execute(URL);
    }
    public void downloadSongs(String URL){
        new DownloadSongsTask().execute(URL);
    }

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
            MainActivity.getInstance().setSongList(result);
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
            Log.i(TAG, "DownloadPlacemarksTask finished");
            MapsActivity.getInstance().updateMap(result);
        }
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
            MapsActivity.getInstance().setAllWords(result);
        }
    }
}
