package com.iamstelios.songle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/*
public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkReceiver.class.getSimpleName();

    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkPref.equals(WIFI) && networkInfo != null
                && networkInfo.getType() ==
                ConnectivityManager.TYPE_WIFI){
                Log.i(TAG,"Connected using WiFi");
            //TODO: Add a variable to download XML otherwise remove this
            // WiFi is connected, so use WiFi
        } else if (networkPref.equals(ANY) && networkInfo != null) {
            // Have a network connection and permission, so use data
            Log.i(TAG,"Connected using Data");
        } else {
            // No WiFi and no permission, or no network connection
            Log.i(TAG,"No connection");
        }
    }
}
*/