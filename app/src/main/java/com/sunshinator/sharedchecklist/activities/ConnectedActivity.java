package com.sunshinator.sharedchecklist.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sunshinator.sharedchecklist.ConnectionObserver;

public abstract class ConnectedActivity extends AppCompatActivity {

    private static final String LOG_TAG = ConnectedActivity.class.getSimpleName();
    private static final String BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    private static final int TIMEOUT_CONNECTION = 10000; // millis
    private static final int TIMEOUT_AUTHENTICATION = 5000;  // millis

    private FirebaseAuth auth;

    private boolean isWaitingAuth = false;
    private boolean isWaitingConnection = false;
    private boolean isConnected = false;

    private Handler authTimeoutHandler = new Handler();
    private Handler connectionTimeoutHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVariables();
    }

    @CallSuper
    public void initVariables() {
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startConnectionListener();
    }

    private void startConnectionListener() {
        Log.d(LOG_TAG, "Starting connection listener");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION);
        registerReceiver(connectionStateListener, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        auth.addAuthStateListener(authStateListener);
        authTimeoutHandler.removeCallbacks(authTimeoutListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        auth.removeAuthStateListener(authStateListener);
        authTimeoutHandler.removeCallbacks(authTimeoutListener);

        isWaitingAuth = false;
        isConnected = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConnectionListener();
    }

    private void stopConnectionListener() {
        unregisterReceiver(connectionStateListener);
        connectionTimeoutHandler.removeCallbacks(connectionTimeoutListener);

        isWaitingConnection = false;
        isConnected = false;
    }

    @Nullable
    public final String getUserEmail() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getEmail() : null;
    }

    private final FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null) {
                Log.d(LOG_TAG, "User logged in");

                authTimeoutHandler.removeCallbacks(authTimeoutListener);

                if (!isWaitingConnection
                        && !isConnected
                        && ConnectionObserver.isConnected(ConnectedActivity.this)) {
                    isConnected = true;
                    onConnect();
                }

                isWaitingAuth = false;
            } else {
                Log.d(LOG_TAG, "User logged out");

                if (!isWaitingAuth) {
                    authTimeoutHandler.postDelayed(authTimeoutListener, TIMEOUT_AUTHENTICATION);
                    isWaitingAuth = true;
                }
            }
        }
    };

    private final ConnectionObserver connectionStateListener = new ConnectionObserver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "Update connection state");

            if (isConnected(ConnectedActivity.this)) {
                connectionTimeoutHandler.removeCallbacks(connectionTimeoutListener);
                isWaitingConnection = false;

                if (getUserUid() != null && !isWaitingAuth && !isConnected) {
                    isConnected = true;
                    onConnect();
                }
            } else if (!isWaitingConnection) {
                connectionTimeoutHandler.postDelayed(connectionTimeoutListener, TIMEOUT_CONNECTION);
                isWaitingConnection = true;
            }
        }
    };

    @Nullable
    public final String getUserUid() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private final Runnable authTimeoutListener = new Runnable() {
        @Override
        public void run() {
            Log.i(LOG_TAG, "Auth timeout");
            isWaitingAuth = false;

            if (getUserUid() == null) {
                isConnected = false;
                onUnauthenticated();
            }
        }
    };

    private final Runnable connectionTimeoutListener = new Runnable() {
        @Override
        public void run() {
            isWaitingConnection = false;

            if (!ConnectionObserver.isConnected(ConnectedActivity.this)) {
                isConnected = false;
                onDisconnect();
            }
        }
    };

    protected abstract void onConnect();
    protected abstract void onDisconnect();
    protected abstract void onUnauthenticated();
}
