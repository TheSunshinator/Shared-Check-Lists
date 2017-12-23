package com.sunshinator.sharedchecklist.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sunshinator.sharedchecklist.Constants;
import com.sunshinator.sharedchecklist.ListFragment;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.adapters.StringAdapter;
import com.sunshinator.sharedchecklist.adapters.StringAdapter.ItemClickCallback;
import com.sunshinator.sharedchecklist.objects.CheckList;

import java.util.HashMap;
import java.util.Map;

public class SharedCheckList
        extends ConnectedActivity {

    private static final String LOG_TAG = SharedCheckList.class.getSimpleName();

    private static final String PREF_LIST_UID = "uid";

    private static final String FB_KEY_USERS = "authorizedUsers";
    private static final String FB_KEY_RIGHTS = "rights";

    public static final String REGEX_DOT = "\\.";
    public static final String REGEX_COMMA = ",";

    private static final String FB_FORMAT_LIST_RIGHTS = FB_KEY_USERS + "/%s/" + FB_KEY_RIGHTS;

    private TextView mvMessage;
    private View mvLoad;
    private View mvRetry;
    private View mvFragment;
    private EditText mvName;
    private RecyclerView mvListList;

    private LinearLayout mDrawer;
    private DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;

    private Query mListsQuery;
    private ListFragment mFragment;

    private Dialog mNewListDialog;
    private Dialog mChangeListDialog;

    private StringAdapter<CheckList> mAdapter;

    private Map<String, CheckList> mCheckLists;

    private enum State {
        LOADING, ERROR, READY
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_check_list);

        initViews();
        initDialogs();

        mCheckLists = new HashMap<>();
        mFragment = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        initActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adjustViewsVisibility(State.LOADING);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mListsQuery.removeEventListener(l_Lists);
        mCheckLists.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onConnect() {
        Log.d(LOG_TAG, "onConnect");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            initQuery(user);
            mListsQuery.addValueEventListener(l_Lists);
            mAdapter = new StringAdapter<>(CheckList.class, mListsQuery, l_ListSelected);
            mvListList.setAdapter(mAdapter);
        } else {
            onUnauthenticated();
        }
    }

    @Override
    protected void onDisconnect() {
        mListsQuery.removeEventListener(l_Lists);
        mCheckLists.clear();

        mvMessage.setText(R.string.err_network);
        adjustViewsVisibility(State.ERROR);
    }

    @Override
    protected void onUnauthenticated() {

        mListsQuery.removeEventListener(l_Lists);
        mCheckLists.clear();

        mvMessage.setText(R.string.err_auth);
        adjustViewsVisibility(State.ERROR);
    }

    private void initViews() {
        mDrawerLayout = findViewById(R.id.root);
        mDrawer = findViewById(R.id.drawer);
        mvLoad = findViewById(R.id.load);
        mvRetry = findViewById(R.id.retry);
        mvFragment = findViewById(R.id.fragment);
        mvMessage = findViewById(R.id.message);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.desc_open_drawer,
                R.string.desc_close_drawer);

        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void initDialogs() {
        initNewListDialog();
        initChangeListDialog();
    }

    private void initNewListDialog() {
        mNewListDialog = new Dialog(this);
        mNewListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mNewListDialog.setContentView(R.layout.dialog_new_list);

        mvName = mNewListDialog.findViewById(R.id.name);

        View vSubmit = mNewListDialog.findViewById(R.id.submit);
        vSubmit.setOnClickListener(l_SubmitList);
    }

    private void initChangeListDialog() {
        mChangeListDialog = new Dialog(this);
        mChangeListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mChangeListDialog.setContentView(R.layout.dialog_select_list);

        mvListList = mChangeListDialog.findViewById(R.id.list);
    }

    private void initQuery(FirebaseUser user) {
        @SuppressWarnings("ConstantConditions")
        String email = user.getEmail().replaceAll(REGEX_DOT, REGEX_COMMA);

        mListsQuery = FirebaseDatabase.getInstance().getReference().child(Constants.FB_BASE_PATH);
        mListsQuery = mListsQuery.orderByChild(String.format(FB_FORMAT_LIST_RIGHTS, email))
                .startAt(Constants.MASK_RIGHT_SEE);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void adjustViewsVisibility(State state) {
        mvLoad.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        mvFragment.setVisibility(state == State.READY ? View.VISIBLE : View.GONE);
        mvMessage.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
        mvRetry.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("ApplySharedPref")
    private void saveDisplayedList(String uid) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_LIST_UID, uid);
        editor.commit();
    }

    private void setLayoutTo(CheckList list) {
        mFragment.setToList(list);
        setTitle(list == null ? getString(R.string.app_name) : list.getName());
        saveDisplayedList(list == null ? null : list.getUid());
    }

    public void onLogout(View v) {
        onUnauthenticated();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, SplashScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onChangeList(View v) {
        if (mAdapter.getItemCount() <= 1) {
            Toast.makeText(this, R.string.warn_empty_list_choice, Toast.LENGTH_LONG).show();
        } else {
            mDrawerLayout.closeDrawer(mDrawer);
            mChangeListDialog.show();
        }
    }

    public void onNewList(View v) {
        mDrawerLayout.closeDrawer(mDrawer);
        mNewListDialog.show();
    }

    private final OnClickListener l_SubmitList = new OnClickListener() {
        @Override
        public void onClick(View view) {
            String name = mvName.getText().toString();
            String email = getUserEmail();

            if (name.length() <= 0) {
                Toast.makeText(SharedCheckList.this, R.string.warn_empty_name, Toast.LENGTH_LONG).show();
                return;
            }

            if (email == null) {
                return;
            }

            CheckList newList = new CheckList(name, email);
            newList.setName(name);

            final DatabaseReference newRef = mListsQuery.getRef().push();
            saveDisplayedList(newRef.getKey());
            newRef.setValue(newList, l_Completion);
        }
    };

    private final CompletionListener l_Completion = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
            if (databaseError != null) {
                Log.e(LOG_TAG, "onComplete", databaseError.toException());
                Toast.makeText(SharedCheckList.this, R.string.err_network, Toast.LENGTH_LONG).show();
            } else {
                mNewListDialog.dismiss();
                mvName.setText(null);
            }
        }
    };

    private final ValueEventListener l_Lists = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            mCheckLists.clear();

            for (DataSnapshot list : dataSnapshot.getChildren()) {
                final CheckList parsed = CheckList.parse(list);
                mCheckLists.put(parsed.getUid(), parsed);
            }

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SharedCheckList.this);
            String listToShow = sp.getString(PREF_LIST_UID, null);

            CheckList shownList = mFragment.getCheckList();
            if (mCheckLists.containsKey(listToShow)) {
                setLayoutTo(mCheckLists.get(listToShow));
            } else if (shownList != null
                    && mCheckLists.containsKey(mFragment.getCheckList().getUid())) {
                setLayoutTo(mCheckLists.get(mFragment.getCheckList().getUid()));
            } else if (mCheckLists.keySet().size() <= 0) {
                setLayoutTo(null);
            } else {
                setLayoutTo(mCheckLists.get(mCheckLists.keySet().iterator().next()));
            }

            adjustViewsVisibility(State.READY);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

            Log.w(LOG_TAG, "onCancelled: Database error", databaseError.toException());
            onDisconnect();

        }
    };

    private final ItemClickCallback l_ListSelected = new ItemClickCallback() {
        @Override
        public void onItemClick(DatabaseReference ref) {

            String uid = ref.getKey();
            setLayoutTo(mCheckLists.get(uid));

            mChangeListDialog.dismiss();
        }
    };

}
