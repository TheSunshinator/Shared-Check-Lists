package com.sunshinator.sharedchecklist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.sunshinator.sharedchecklist.BuildConfig;
import com.sunshinator.sharedchecklist.R;

public class SplashScreen extends AppCompatActivity {

    private static final String LOG_TAG = SplashScreen.class.getSimpleName();

    public static final int RC_SIGN_IN = 1;

    private TextView mvErrorMessage;
    private View mvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mvErrorMessage = findViewById(R.id.error);
        mvRetry = findViewById(R.id.retry);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            onLoggedIn();
        } else {
            Log.d(LOG_TAG, "onCreate: No user logged in");
            onLoginRetry(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            onLoggedIn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            Log.d(LOG_TAG, "onActivityResult: Login response: "
                    + (response == null ? null : response.toString()));

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                onLoggedIn();
            } else if (response == null) {
                finish();
            } else if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                mvErrorMessage.setText(R.string.message_login_network);
                mvRetry.setVisibility(View.VISIBLE);
            } else {
                mvErrorMessage.setText(R.string.message_login_unknown);
                mvRetry.setVisibility(View.VISIBLE);

                if (!BuildConfig.DEBUG) {
                    FirebaseCrash.report(new IllegalStateException(
                            "Could not login. Login result code: " + response.getErrorCode()));
                }
            }
        }
    }

    private void onLoggedIn() {
        Log.d(LOG_TAG, "onCreate: User logged in");
        Intent intent = new Intent(this, SharedCheckList.class);
        startActivity(intent);
        finish();
    }

    public void onLoginRetry(View v) {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .build(), RC_SIGN_IN);
    }

    // TODO Logout button
    // TODO Change Firebase's rules
    // TODO: 20/04/2017 Adjust layouts to landscape mode
}
