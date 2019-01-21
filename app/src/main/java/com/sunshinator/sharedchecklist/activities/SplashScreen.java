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
import com.sunshinator.sharedchecklist.BuildConfig;
import com.sunshinator.sharedchecklist.R;

public class SplashScreen extends AppCompatActivity {

    private static final String LOG_TAG = SplashScreen.class.getSimpleName();

    public static final int RC_SIGN_IN = 1;

    private TextView errorMessageView;
    private View retryCta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        errorMessageView = findViewById(R.id.error);
        retryCta = findViewById(R.id.retry);

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

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            Log.d(LOG_TAG, "onActivityResult: Login response: " + (response == null ? null : response.toString()));

            if (resultCode == RESULT_OK) {
                onLoggedIn();
            } else if (response == null) {
                finish();
            } else if (response.getError() != null && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                errorMessageView.setText(R.string.message_login_network);
                retryCta.setVisibility(View.VISIBLE);
            } else {
                errorMessageView.setText(R.string.message_login_unknown);
                retryCta.setVisibility(View.VISIBLE);
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
}
