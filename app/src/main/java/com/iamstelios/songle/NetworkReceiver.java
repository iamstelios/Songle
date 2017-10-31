package com.iamstelios.songle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;


public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkReceiver.class.getSimpleName();

    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
        //If the user didn't choose any preference use any connection
        String networkPref = prefs.getString(MainActivity.NETWORK_PREF_KEY, ANY);

        if (networkPref.equals(WIFI) && networkInfo != null
                && networkInfo.getType() ==
                ConnectivityManager.TYPE_WIFI) {
            // WiFi is connected, so use WiFi
            Log.i(TAG, "Connected using WiFi");
            MapsActivity.getInstance().runDownloads();

        } else if (networkPref.equals(ANY) && networkInfo != null) {
            // Have a network connection and permission, so use data
            Log.i(TAG, "Connected using Data");
            MapsActivity.getInstance().runDownloads();
        } else {
            // No WiFi and no permission, or no network connection
            Log.i(TAG, "No connection");
        }
    }
}
