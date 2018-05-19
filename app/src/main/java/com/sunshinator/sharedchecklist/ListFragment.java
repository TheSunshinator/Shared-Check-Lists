package com.sunshinator.sharedchecklist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;
import com.sunshinator.sharedchecklist.adapters.CheckListAdapter;
import com.sunshinator.sharedchecklist.adapters.CheckListAdapter.ItemClickCallback;
import com.sunshinator.sharedchecklist.adapters.UserAdapter;
import com.sunshinator.sharedchecklist.objects.CheckList;
import com.sunshinator.sharedchecklist.objects.CheckListEntry;
import com.sunshinator.sharedchecklist.objects.UserRights;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ListFragment extends Fragment {

    private static final String LOG_TAG = ListFragment.class.getSimpleName();

    private static final String FB_KEY_APP = "SharedCheckList";
    private static final String FB_KEY_LIST = "lists";
    private static final String FB_KEY_ITEMS = "items";
    private static final String FB_KEY_USERS = "authorizedUsers";

    private Dialog mAddUserDialog;
    private Dialog mRemoveUserDialog;
    private AlertDialog mConfirmationDialog;
    private Dialog mNewEntryDialog;

    private View mvDeleteList;
    private View mvRemoveUsers;
    private View mvAddUsers;
    private View mvClearList;
    private View mvDeleteChecked;
    private View mvAddItems;
    private View mvMenu;
    private View mvRemoveUsersSubmit;
    private View mvRemoveUsersLoad;
    private RecyclerView mvUserList;
    private RecyclerView mvList;

    private EditText mvNewUser;
    private EditText mvNewEntry;

    private CheckBox mvPermAddItems;
    private CheckBox mvPermCheck;
    private CheckBox mvPermClean;
    private CheckBox mvPermAddUsers;
    private CheckBox mvPermRemoveUsers;
    private CheckBox mvPermDelete;

    private DatabaseReference mReference;
    private CheckList mCheckList;

    private CheckListAdapter mAdapter;
    private UserAdapter mUserAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initDialogs();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {

        View vRoot = inflater.inflate(R.layout.fragment_list, container);

        initViews(vRoot);

        return vRoot;

    }

    @Override
    public void onResume() {

        super.onResume();

        mvList.setAdapter(mAdapter);

    }

    @Override
    public void onPause() {
        super.onPause();

        closeAllDialogs();

        if(mAdapter != null) mAdapter.stopListening();
        if(mUserAdapter != null) mUserAdapter.stopListening();

        mCheckList = null;
    }

    private void initDialogs() {
        initAddUserDialog();
        initRemoveUserDialog();
        initNewEntryDialog();
    }

    private void initRemoveUserDialog() {

        mRemoveUserDialog = new Dialog(getContext());
        mRemoveUserDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mRemoveUserDialog.setContentView(R.layout.dialog_remove_user);

        mvRemoveUsersSubmit = mRemoveUserDialog.findViewById(R.id.submit);
        mvRemoveUsersLoad = mRemoveUserDialog.findViewById(R.id.load);
        mvUserList = mRemoveUserDialog.findViewById(R.id.list);

        mvRemoveUsersSubmit.setOnClickListener(l_OpenRemoveUserConfirmation);
    }

    private void initAddUserDialog() {

        mAddUserDialog = new Dialog(getContext());
        mAddUserDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAddUserDialog.setContentView(R.layout.dialog_new_user);

        mvNewUser = mAddUserDialog.findViewById(R.id.email_new);
        mvPermAddItems = mAddUserDialog.findViewById(R.id.perm_add_item);
        mvPermCheck = mAddUserDialog.findViewById(R.id.perm_check);
        mvPermAddUsers = mAddUserDialog.findViewById(R.id.perm_users_add);
        mvPermRemoveUsers = mAddUserDialog.findViewById(R.id.perm_users_remove);
        mvPermClean = mAddUserDialog.findViewById(R.id.perm_clean);
        mvPermDelete = mAddUserDialog.findViewById(R.id.perm_delete);

        View vSubmit = mAddUserDialog.findViewById(R.id.submit_user);
        vSubmit.setOnClickListener(l_SubmitNewUser);
    }

    private void initNewEntryDialog() {

        mNewEntryDialog = new Dialog(getContext());
        mNewEntryDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mNewEntryDialog.setContentView(R.layout.dialog_text_entry);

        mvNewEntry = mNewEntryDialog.findViewById(R.id.entry);

        Window window = mNewEntryDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        mNewEntryDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                mvNewEntry.setText(null);

            }
        });

        mvNewEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                boolean handled = false;

                if (i == EditorInfo.IME_ACTION_DONE) {
                    onSubmitNewEntry();
                    handled = true;
                }

                return handled;
            }
        });

        View vSubmit = mNewEntryDialog.findViewById(R.id.submit);
        vSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onSubmitNewEntry();
            }
        });
    }

    private void initViews(View vRoot) {

        mvDeleteList = vRoot.findViewById(R.id.delete_list);
        mvRemoveUsers = vRoot.findViewById(R.id.user_remove);
        mvAddUsers = vRoot.findViewById(R.id.user_add);
        mvClearList = vRoot.findViewById(R.id.clear);
        mvDeleteChecked = vRoot.findViewById(R.id.clear_checked);
        mvAddItems = vRoot.findViewById(R.id.add_item);
        mvMenu = vRoot.findViewById(R.id.menu);
        mvList = vRoot.findViewById(R.id.list);

        mvAddItems.setOnClickListener(l_OpenAddItemDialog);
        mvDeleteChecked.setOnClickListener(l_OpenDeleteCheckConfirmation);
        mvClearList.setOnClickListener(l_OpenClearConfirmation);
        mvAddUsers.setOnClickListener(l_OpenNewUserDialog);
        mvDeleteList.setOnClickListener(l_OpenDeleteListConfirmation);
        mvRemoveUsers.setOnClickListener(l_OpenRemoveUserDialog);
    }

    public void setToList(@Nullable CheckList list) {

        Log.d(LOG_TAG, "setToList: " + list);

        if (list != null && !list.equals(mCheckList)) {
            mReference = FirebaseDatabase.getInstance().getReference()
                    .child(FB_KEY_APP).child(FB_KEY_LIST).child(list.getUid());

            mAdapter = new CheckListAdapter(mReference.child(FB_KEY_ITEMS), l_ItemClicked);
            mAdapter.startListening();
            mvList.setAdapter(mAdapter);

            mUserAdapter = new UserAdapter(mReference.child(FB_KEY_USERS));
            mUserAdapter.startListening();
            mvUserList.setAdapter(mUserAdapter);
        }

        mCheckList = list;

        adjustRightsViewsVisibility();
    }

    private void adjustRightsViewsVisibility() {

        int rights = getUsersRights();

        mvAddItems
                .setVisibility((rights & Constants.MASK_RIGHT_ADD_ITEM) == Constants.MASK_RIGHT_ADD_ITEM
                        ? View.VISIBLE
                        : View.GONE);
        mvClearList.setVisibility((rights & Constants.MASK_RIGHT_CLEAN) == Constants.MASK_RIGHT_CLEAN
                ? View.VISIBLE
                : View.GONE);
        mvDeleteList
                .setVisibility((rights & Constants.MASK_RIGHT_DELETE) == Constants.MASK_RIGHT_DELETE
                        ? View.VISIBLE
                        : View.GONE);
        mvDeleteChecked
                .setVisibility((rights & Constants.MASK_RIGHT_CLEAN) == Constants.MASK_RIGHT_CLEAN
                        ? View.VISIBLE
                        : View.GONE);

        mvAddUsers.setVisibility(
                (rights & Constants.MASK_RIGHT_ADD_USERS) == Constants.MASK_RIGHT_ADD_USERS
                        ? View.VISIBLE
                        : View.GONE);
        mvRemoveUsers.setVisibility(
                (rights & Constants.MASK_RIGHT_REMOVE_USERS) == Constants.MASK_RIGHT_REMOVE_USERS
                        ? View.VISIBLE
                        : View.GONE);

        mvMenu.setVisibility(rights >= Constants.MASK_RIGHT_SEE ? View.VISIBLE : View.GONE);
    }

    private int getUsersRights() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || mCheckList == null) {
            return 0;
        }

        String email = user.getEmail();
        UserRights rights = mCheckList.getAuthorizedUsers().get(email);

        if (rights == null) {
            return 0;
        }

        return rights.getRights();
    }

    public CheckList getCheckList() {

        return mCheckList;
    }

    private void onSubmitNewEntry() {

        final String entry = mvNewEntry.getText().toString();

        if (entry.length() > 0) {
            mReference.getRef().child(FB_KEY_ITEMS).push()
                    .setValue(new CheckListEntry(entry), l_UpdateCompletion);
        } else {
            Toast.makeText(getContext(), R.string.warn_empty_item, Toast.LENGTH_LONG).show();
        }
    }

    private void closeAllDialogs() {

        if (mConfirmationDialog != null && mConfirmationDialog.isShowing()) {
            mConfirmationDialog.dismiss();
        }

        if (mAddUserDialog.isShowing()) {
            mAddUserDialog.dismiss();
        }

        if (mNewEntryDialog.isShowing()) {
            mNewEntryDialog.dismiss();
        }

        if (mRemoveUserDialog != null && mRemoveUserDialog.isShowing()) {
            mRemoveUserDialog.dismiss();
        }
    }

    private final View.OnClickListener l_SubmitNewUser = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String user = mvNewUser.getText().toString();

            if (user.length() <= 0) {
                Toast.makeText(getContext(), R.string.warn_empty_user, Toast.LENGTH_LONG).show();
                return;
            }

            UserRights rights = new UserRights();
            rights.setId(user);
            rights.setRights(computeRightsCode());

            mReference.child(FB_KEY_USERS).child(rights.getId())
                    .setValue(rights, l_UpdateCompletion);
        }
    };

    private int computeRightsCode() {

        int rightsCode = Constants.MASK_RIGHT_SEE;

        if (mvPermClean.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_CLEAN;
        }
        if (mvPermAddItems.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_ADD_ITEM;
        }
        if (mvPermAddUsers.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_ADD_USERS;
        }
        if (mvPermCheck.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_CHECK;
        }
        if (mvPermDelete.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_DELETE;
        }
        if (mvPermRemoveUsers.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_REMOVE_USERS;
        }

        return rightsCode;
    }

    private String enumerate(Collection<UserRights> list) {
        StringBuilder result = new StringBuilder();

        Iterator<UserRights> iterator = list.iterator();

        if (!iterator.hasNext()) return result.toString();

        result.append(iterator.next());

        while (iterator.hasNext()) {

            final UserRights next = iterator.next();

            if (iterator.hasNext()) {
                result.append(", ");
            } else {
                result.append(" and ");
            }

            result.append(next.toString());
        }

        return result.toString();
    }

    private final View.OnClickListener l_OpenAddItemDialog = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mNewEntryDialog.show();
        }
    };

    private final ItemClickCallback l_ItemClicked = new ItemClickCallback() {
        @Override
        public void onItemClick(DatabaseReference ref, CheckListEntry entry) {

            Log.d(LOG_TAG, "onItemClick: Entry: " + entry + " Ref: " + ref);

            final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) {
                return;
            }

            if ((mCheckList.getRightsOf(currentUser.getEmail()) & Constants.MASK_RIGHT_CHECK)
                    != Constants.MASK_RIGHT_CHECK) {
                return;
            }

            String checker = null;
            if (!entry.isChecked()) {
                checker = currentUser.getEmail();
            }

            ref.child(CheckListEntry.FB_KEY_CHECKED).setValue(checker, l_UpdateCompletion);

        }
    };

    private final CompletionListener l_UpdateCompletion = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

            if (databaseError != null) {
                Log.e(LOG_TAG, "onComplete", databaseError.toException());
                Toast.makeText(getContext(), R.string.err_network, Toast.LENGTH_LONG).show();
            } else {
                mvNewEntry.setText(null);
                mvNewUser.setText(null);

                if (mConfirmationDialog != null && mConfirmationDialog.isShowing()) {
                    mConfirmationDialog.dismiss();
                }
            }

        }
    };

    private final View.OnClickListener l_OpenDeleteCheckConfirmation = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_checked), mCheckList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, l_Clean)
                    .setNegativeButton(R.string.answer_negative, l_Dismiss);

            mConfirmationDialog = builder.create();
            mConfirmationDialog.show();
        }
    };

    private final View.OnClickListener l_OpenDeleteListConfirmation = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_list), mCheckList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, l_DeleteList)
                    .setNegativeButton(R.string.answer_negative, l_Dismiss);

            mConfirmationDialog = builder.create();
            mConfirmationDialog.show();
        }
    };

    private final DialogInterface.OnClickListener l_DeleteList = new DialogInterface
            .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

            mReference.setValue(null, l_UpdateCompletion);
        }
    };

    private final View.OnClickListener l_OpenClearConfirmation = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_all), mCheckList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, l_Clear)
                    .setNegativeButton(R.string.answer_negative, l_Dismiss);

            mConfirmationDialog = builder.create();
            mConfirmationDialog.show();
        }
    };

    private final DialogInterface.OnClickListener l_Clean = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

            List<String> checked = mAdapter.getCheckedEntries();

            Map<String, Object> updateMap = new HashMap<>();
            for (String entry : checked) {
                updateMap.put(entry, null);
            }

            mReference.child(FB_KEY_ITEMS).updateChildren(updateMap, l_UpdateCompletion);
        }
    };

    private final DialogInterface.OnClickListener l_Clear = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

            mReference.child(FB_KEY_ITEMS).setValue(null, l_UpdateCompletion);
        }
    };

    private final DialogInterface.OnClickListener l_Dismiss = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

            dialogInterface.dismiss();

        }
    };

    private final View.OnClickListener l_OpenNewUserDialog = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            closeAllDialogs();

            mAddUserDialog.show();
        }
    };

    private final View.OnClickListener l_OpenRemoveUserDialog = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();
            mRemoveUserDialog.show();
        }
    };

    private final View.OnClickListener l_OpenRemoveUserConfirmation = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            List<UserRights> toDelete = mUserAdapter.getCheckedEntries();

            Log.d(LOG_TAG, "onClick: To remove: " + toDelete.toString());

            if (toDelete.size() <= 0) {
                Toast.makeText(getContext(), R.string.warn_empty_user_set, Toast.LENGTH_LONG).show();
            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder = builder.setMessage(getString(R.string.message_confirm_delete_users,
                        enumerate(mUserAdapter.getCheckedEntries())))
                        .setPositiveButton(R.string.answer_affirmative, l_RemoveUsers)
                        .setNegativeButton(R.string.answer_negative, l_Dismiss);

                mConfirmationDialog = builder.create();
                mConfirmationDialog.show();
            }
        }
    };

    private final DialogInterface.OnClickListener l_RemoveUsers = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int yo) {
            mConfirmationDialog.dismiss();

            List<UserRights> toDelete = mUserAdapter.getCheckedEntries();

            mvRemoveUsersSubmit.setVisibility(View.GONE);
            mvRemoveUsersLoad.setVisibility(View.VISIBLE);

            Map<String, Object> updateMap = new HashMap<>();

            for (int i = 0; i < toDelete.size(); i++) {
                UserRights user = toDelete.get(i);
                updateMap.put(user.getId(), null);
            }

            mReference.child(FB_KEY_USERS).updateChildren(updateMap, l_UserDeleted);
        }
    };

    private final CompletionListener l_UserDeleted = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

            if (databaseError != null) {
                Toast.makeText(getContext(), R.string.err_network, Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG, "onComplete: ", databaseError.toException());
            } else {
                mRemoveUserDialog.dismiss();
            }

            mvRemoveUsersSubmit.setVisibility(View.GONE);
            mvRemoveUsersLoad.setVisibility(View.VISIBLE);
        }
    };
}
