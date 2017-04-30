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

/**
 * Activity which needs to communicate to Firebase.
 * This class assures that the calls to Firebase are
 * made while connected
 * <p>
 * Created by The Sunshinator on 11/11/2016.
 */

public abstract class ConnectedActivity
    extends AppCompatActivity {

  private static final String LOG_TAG          = ConnectedActivity.class.getSimpleName();
  private static final String BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

  private static final int TIMEOUT_CONNECTION     = 10000; // millis
  private static final int TIMEOUT_AUTHENTICATION = 5000;  // millis

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- -- Instance Variable -- ----- //
  private FirebaseAuth mAuth;

  private boolean mbIsWaitingAuth       = false;
  private boolean mbIsWaitingConnection = false;
  private boolean mbIsConnected         = false;

  private Handler mAuthTimeoutHandler       = new Handler();
  private Handler mConnectionTimeoutHandler = new Handler();
  // endregion
  // ----- ----- ----- ----- ----- ----- //

  // ----- ----- -----  ----- ----- ----- //
  // region // ----- -- Overridden Methods -- ----- //
  @Override
  protected void onCreate( @Nullable Bundle savedInstanceState ) {

    super.onCreate( savedInstanceState );
    initVariables();
  }

  @Override
  protected void onStart() {

    super.onStart();

    startConnectionListener();
  }

  @Override
  protected void onResume() {

    super.onResume();

    mAuth.addAuthStateListener( l_AuthState );
    mAuthTimeoutHandler.removeCallbacks( l_AuthTimeout );
  }

  @Override
  protected void onPause() {

    super.onPause();

    mAuth.removeAuthStateListener( l_AuthState );
    mAuthTimeoutHandler.removeCallbacks( l_AuthTimeout );

    mbIsWaitingAuth = false;
    mbIsConnected = false;

  }

  @Override
  protected void onStop() {

    super.onStop();
    stopConnectionListener();
  }

  @Nullable
  public final String getUserUid() {

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    return currentUser != null? currentUser.getUid() : null;
  }

  @Nullable
  public final String getUserEmail() {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    return currentUser != null? currentUser.getEmail() : null;
  }

  @CallSuper
  public void initVariables() {

    mAuth = FirebaseAuth.getInstance();
  }
  //endregion
  // ----- ----- -----  ----- ----- ----- //

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- ----- - Methods - ----- ----- //

  /**
   * Starts listener for the connection state of the device to the internet
   */
  private void startConnectionListener() {

    Log.d( LOG_TAG, "Starting connection listener" );

    IntentFilter filter = new IntentFilter();
    filter.addAction( BROADCAST_ACTION );
    registerReceiver( l_ConnectionState, filter );
  }

  /**
   * Stops listener for the connection state of the device to the internet
   */
  private void stopConnectionListener() {

    unregisterReceiver( l_ConnectionState );
    mConnectionTimeoutHandler.removeCallbacks( l_ConnectionTimeout );
    mbIsWaitingConnection = false;
    mbIsConnected = false;
  }
  //endregion
  // ----- ----- ----- ----- ----- ----- //

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- -----  Listeners  ----- ----- //
  private final FirebaseAuth.AuthStateListener l_AuthState = new FirebaseAuth.AuthStateListener() {
    @Override
    public void onAuthStateChanged( @NonNull FirebaseAuth firebaseAuth ) {

      FirebaseUser user = firebaseAuth.getCurrentUser();

      if ( user != null ) {
        Log.d( LOG_TAG, "User logged in" );

        mAuthTimeoutHandler.removeCallbacks( l_AuthTimeout );

        if ( !mbIsWaitingConnection
             && ConnectionObserver.isConnected( ConnectedActivity.this )
            && !mbIsConnected ) {
          mbIsConnected = true;
          onConnect();
        }

        mbIsWaitingAuth = false;

      } else {
        Log.d( LOG_TAG, "User logged out" );

        if ( !mbIsWaitingAuth ) {
          mAuthTimeoutHandler.postDelayed( l_AuthTimeout, TIMEOUT_AUTHENTICATION );
          mbIsWaitingAuth = true;
        }

      }
    }
  };

  private final ConnectionObserver l_ConnectionState = new ConnectionObserver() {
    @Override
    public void onReceive( Context context, Intent intent ) {

      Log.d( LOG_TAG, "Update connection state" );

      if ( isConnected( ConnectedActivity.this ) ) {

        mConnectionTimeoutHandler.removeCallbacks( l_ConnectionTimeout );
        mbIsWaitingConnection = false;

        if ( getUserUid() != null && !mbIsWaitingAuth && !mbIsConnected ) {
          mbIsConnected = true;
          onConnect();
        }
      } else {

        if ( !mbIsWaitingConnection ) {
          mConnectionTimeoutHandler.postDelayed( l_ConnectionTimeout, TIMEOUT_CONNECTION );
          mbIsWaitingConnection = true;
        }
      }

    }
  };

  //endregion
  // ----- ----- ----- ----- ----- ----- //

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- -----  Runnables  ----- ----- //
  private final Runnable l_AuthTimeout = new Runnable() {
    @Override
    public void run() {

      Log.i( LOG_TAG, "Auth timeout" );
      mbIsWaitingAuth = false;

      if ( getUserUid() == null ) {
        mbIsConnected = false;
        onUnauthenticated();
      }

    }
  };

  private final Runnable l_ConnectionTimeout = new Runnable() {
    @Override
    public void run() {

      mbIsWaitingConnection = false;

      if ( !ConnectionObserver.isConnected( ConnectedActivity.this ) ) {
        mbIsConnected = false;
        onDisconnect();
      }
    }
  };
  //endregion
  // ----- ----- ----- ----- ----- ----- //

  // ----- ----- -----  ----- ----- ----- //
  // region // ----- --- Abstract Methods --- ----- //

  protected abstract void onConnect();

  protected abstract void onDisconnect();

  protected abstract void onUnauthenticated();

  // endregion
  // ----- ----- -----  ----- ----- ----- //
}
