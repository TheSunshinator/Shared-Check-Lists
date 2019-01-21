package com.sunshinator.sharedchecklist.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

public class SharedCheckList extends ConnectedActivity {

    private static final String LOG_TAG = SharedCheckList.class.getSimpleName();

    private static final String PREF_LIST_UID = "uid";

    private static final String FB_KEY_USERS = "authorizedUsers";
    private static final String FB_KEY_RIGHTS = "rights";

    public static final String REGEX_DOT = "\\.";
    public static final String REGEX_COMMA = ",";

    private static final String FB_FORMAT_LIST_RIGHTS = FB_KEY_USERS + "/%s/" + FB_KEY_RIGHTS;

    private TextView message;
    private View loadingView;
    private View retryCta;
    private View contentFragment;
    private EditText nameEntry;
    private RecyclerView listRecyclerView;

    private LinearLayout drawerView;
    private DrawerLayout drawerLayout;

    private ActionBarDrawerToggle drawerToggle;

    private Query listsQuery;
    private ListFragment listFragment;

    private Dialog newListDialog;
    private Dialog changeListDialog;

    private StringAdapter<CheckList> adapter;

    private Map<String, CheckList> checkLists;

    private enum State {
        LOADING, ERROR, READY
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_check_list);

        initViews();
        initDialogs();

        checkLists = new HashMap<>();
        listFragment = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        initActionBar();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.root);
        drawerView = findViewById(R.id.drawer);
        loadingView = findViewById(R.id.load);
        retryCta = findViewById(R.id.retry);
        contentFragment = findViewById(R.id.fragment);
        message = findViewById(R.id.message);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.desc_open_drawer, R.string.desc_close_drawer);

        drawerLayout.addDrawerListener(drawerToggle);
    }

    private void initDialogs() {
        initNewListDialog();
        initChangeListDialog();
    }

    private void initNewListDialog() {
        newListDialog = new Dialog(this);
        newListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        newListDialog.setContentView(R.layout.dialog_new_list);

        nameEntry = newListDialog.findViewById(R.id.name);

        View vSubmit = newListDialog.findViewById(R.id.submit);
        vSubmit.setOnClickListener(submitListClicked);
    }

    private void initChangeListDialog() {
        changeListDialog = new Dialog(this);
        changeListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        changeListDialog.setContentView(R.layout.dialog_select_list);

        listRecyclerView = changeListDialog.findViewById(R.id.list);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adjustViewsVisibility(State.LOADING);
    }

    private void adjustViewsVisibility(State state) {
        loadingView.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        contentFragment.setVisibility(state == State.READY ? View.VISIBLE : View.GONE);
        message.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
        retryCta.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        listsQuery.removeEventListener(listsFetchedListener);

        if (adapter != null) adapter.stopListening();

        checkLists.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onConnect() {
        Log.d(LOG_TAG, "onConnect");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            initQuery(user);
            listsQuery.addValueEventListener(listsFetchedListener);
            adapter = new StringAdapter<>(CheckList.class, listsQuery, l_ListSelected);
            adapter.startListening();
            listRecyclerView.setAdapter(adapter);
        } else {
            onUnauthenticated();
        }
    }

    @Override
    protected void onDisconnect() {
        listsQuery.removeEventListener(listsFetchedListener);
        checkLists.clear();

        message.setText(R.string.err_network);
        adjustViewsVisibility(State.ERROR);
    }

    @Override
    protected void onUnauthenticated() {
        listsQuery.removeEventListener(listsFetchedListener);
        checkLists.clear();

        message.setText(R.string.err_auth);
        adjustViewsVisibility(State.ERROR);
    }

    private void initQuery(FirebaseUser user) {
        @SuppressWarnings("ConstantConditions")
        String email = user.getEmail().replaceAll(REGEX_DOT, REGEX_COMMA);

        listsQuery = FirebaseDatabase.getInstance().getReference().child(Constants.FB_BASE_PATH)
                .orderByChild(String.format(FB_FORMAT_LIST_RIGHTS, email))
                .startAt(Constants.MASK_RIGHT_SEE);
    }

    @SuppressLint("ApplySharedPref")
    private void saveDisplayedList(String uid) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_LIST_UID, uid);
        editor.commit();
    }

    private void setLayoutTo(CheckList list) {
        listFragment.setToList(list);
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
        if (adapter.getItemCount() <= 1) {
            Toast.makeText(this, R.string.warn_empty_list_choice, Toast.LENGTH_LONG).show();
        } else {
            drawerLayout.closeDrawer(drawerView);
            changeListDialog.show();
        }
    }

    public void onNewList(View v) {
        drawerLayout.closeDrawer(drawerView);
        newListDialog.show();
    }

    private final OnClickListener submitListClicked = new OnClickListener() {
        @Override
        public void onClick(View view) {
            String name = nameEntry.getText().toString();
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

            final DatabaseReference newRef = listsQuery.getRef().push();
            saveDisplayedList(newRef.getKey());
            newRef.setValue(newList, fbValueSetCompletedListener);
        }
    };

    private final CompletionListener fbValueSetCompletedListener = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
            if (databaseError != null) {
                Log.e(LOG_TAG, "onComplete", databaseError.toException());
                Toast.makeText(SharedCheckList.this, R.string.err_network, Toast.LENGTH_LONG).show();
            } else {
                newListDialog.dismiss();
                nameEntry.setText(null);
            }
        }
    };

    private final ValueEventListener listsFetchedListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            checkLists.clear();

            for (DataSnapshot list : dataSnapshot.getChildren()) {
                final CheckList parsed = CheckList.parse(list);
                checkLists.put(parsed.getUid(), parsed);
            }

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SharedCheckList.this);
            String listToShow = sp.getString(PREF_LIST_UID, null);

            CheckList shownList = listFragment.getCheckList();
            if (checkLists.containsKey(listToShow)) {
                setLayoutTo(checkLists.get(listToShow));
            } else if (shownList != null && checkLists.containsKey(listFragment.getCheckList().getUid())) {
                setLayoutTo(checkLists.get(listFragment.getCheckList().getUid()));
            } else if (checkLists.keySet().size() <= 0) {
                setLayoutTo(null);
            } else {
                setLayoutTo(checkLists.get(checkLists.keySet().iterator().next()));
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
            setLayoutTo(checkLists.get(uid));

            changeListDialog.dismiss();
        }
    };
}
