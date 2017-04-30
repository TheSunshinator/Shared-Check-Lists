package com.sunshinator.sharedchecklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Checks the current network state of the device
 *
 * Created by The Sunshinator on 12/11/2016.
 */
public abstract class ConnectionObserver extends BroadcastReceiver {

    private static final String LOG_TAG = "ConnectionObserver";

    public static boolean isConnected(Context context) {
        ConnectivityManager cm
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getState() == NetworkInfo.State.CONNECTED;
    }
}