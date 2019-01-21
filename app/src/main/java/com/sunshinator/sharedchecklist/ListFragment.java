package com.sunshinator.sharedchecklist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    private Dialog addUserDialog;
    private Dialog removeUserDialog;
    private AlertDialog confirmationDialog;
    private Dialog newEntryDialog;

    private View deleteListCta;
    private View removeUsersCta;
    private View addUsersCta;
    private View clearListCta;
    private View deleteCheckedCta;
    private View addItemsCta;
    private View menuLayout;
    private View removeUsersSubmitCta;
    private View removeUsersLoadView;
    private RecyclerView userList;
    private RecyclerView listContent;

    private EditText newUserEntry;
    private EditText newListEntry;

    private CheckBox addItemsPermissionCheckBox;
    private CheckBox checkPermissionCheckBox;
    private CheckBox cleanPermissionCheckBox;
    private CheckBox addUsersPermissionCheckBox;
    private CheckBox removeUsersPermissionCheckBox;
    private CheckBox deletePermissionCheckBox;

    private DatabaseReference dbReference;
    private CheckList checkList;

    private CheckListAdapter checkListAdapter;
    private UserAdapter userAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDialogs();
    }

    private void initDialogs() {
        initAddUserDialog();
        initRemoveUserDialog();
        initNewEntryDialog();
    }

    private void initAddUserDialog() {
        //noinspection ConstantConditions
        addUserDialog = new Dialog(getContext());
        addUserDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addUserDialog.setContentView(R.layout.dialog_new_user);

        newUserEntry = addUserDialog.findViewById(R.id.email_new);
        addItemsPermissionCheckBox = addUserDialog.findViewById(R.id.perm_add_item);
        checkPermissionCheckBox = addUserDialog.findViewById(R.id.perm_check);
        addUsersPermissionCheckBox = addUserDialog.findViewById(R.id.perm_users_add);
        removeUsersPermissionCheckBox = addUserDialog.findViewById(R.id.perm_users_remove);
        cleanPermissionCheckBox = addUserDialog.findViewById(R.id.perm_clean);
        deletePermissionCheckBox = addUserDialog.findViewById(R.id.perm_delete);

        View vSubmit = addUserDialog.findViewById(R.id.submit_user);
        vSubmit.setOnClickListener(newUserSubmittedListener);
    }

    private void initRemoveUserDialog() {
        //noinspection ConstantConditions
        removeUserDialog = new Dialog(getContext());
        removeUserDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        removeUserDialog.setContentView(R.layout.dialog_remove_user);

        removeUsersSubmitCta = removeUserDialog.findViewById(R.id.submit);
        removeUsersLoadView = removeUserDialog.findViewById(R.id.load);
        userList = removeUserDialog.findViewById(R.id.list);

        removeUsersSubmitCta.setOnClickListener(removeUserClickListener);
    }

    private void initNewEntryDialog() {
        //noinspection ConstantConditions
        newEntryDialog = new Dialog(getContext());
        newEntryDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        newEntryDialog.setContentView(R.layout.dialog_text_entry);

        newListEntry = newEntryDialog.findViewById(R.id.entry);

        Window window = newEntryDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        newEntryDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                newListEntry.setText(null);
            }
        });

        newListEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        View vSubmit = newEntryDialog.findViewById(R.id.submit);
        vSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmitNewEntry();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View vRoot = inflater.inflate(R.layout.fragment_list, container);
        initViews(vRoot);
        return vRoot;
    }

    private void initViews(View vRoot) {
        deleteListCta = vRoot.findViewById(R.id.delete_list);
        removeUsersCta = vRoot.findViewById(R.id.user_remove);
        addUsersCta = vRoot.findViewById(R.id.user_add);
        clearListCta = vRoot.findViewById(R.id.clear);
        deleteCheckedCta = vRoot.findViewById(R.id.clear_checked);
        addItemsCta = vRoot.findViewById(R.id.add_item);
        menuLayout = vRoot.findViewById(R.id.menu);
        listContent = vRoot.findViewById(R.id.list);

        addItemsCta.setOnClickListener(openAddItemDialogListener);
        deleteCheckedCta.setOnClickListener(openDeleteCheckConfirmationListener);
        clearListCta.setOnClickListener(openClearConfirmationListener);
        addUsersCta.setOnClickListener(openNewUserDialogListener);
        deleteListCta.setOnClickListener(openDeleteListConfirmationListener);
        removeUsersCta.setOnClickListener(openRemoveUserDialogListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        listContent.setAdapter(checkListAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        closeAllDialogs();

        if (checkListAdapter != null) checkListAdapter.stopListening();
        if (userAdapter != null) userAdapter.stopListening();

        checkList = null;
    }

    private void closeAllDialogs() {
        if (confirmationDialog != null && confirmationDialog.isShowing()) {
            confirmationDialog.dismiss();
        }

        if (addUserDialog.isShowing()) {
            addUserDialog.dismiss();
        }

        if (newEntryDialog.isShowing()) {
            newEntryDialog.dismiss();
        }

        if (removeUserDialog != null && removeUserDialog.isShowing()) {
            removeUserDialog.dismiss();
        }
    }

    public void setToList(@Nullable CheckList list) {
        Log.d(LOG_TAG, "setToList: " + list);

        if (list != null && !list.equals(checkList)) {
            dbReference = FirebaseDatabase.getInstance().getReference()
                    .child(FB_KEY_APP)
                    .child(FB_KEY_LIST)
                    .child(list.getUid());

            checkListAdapter = new CheckListAdapter(dbReference.child(FB_KEY_ITEMS), itemClickedListener);
            checkListAdapter.startListening();
            listContent.setAdapter(checkListAdapter);

            userAdapter = new UserAdapter(dbReference.child(FB_KEY_USERS));
            userAdapter.startListening();
            userList.setAdapter(userAdapter);
        }

        checkList = list;

        adjustRightsViewsVisibility();
    }

    private void adjustRightsViewsVisibility() {
        int rights = getUsersRights();

        addItemsCta.setVisibility((rights & Constants.MASK_RIGHT_ADD_ITEM) == Constants.MASK_RIGHT_ADD_ITEM
                ? View.VISIBLE
                : View.GONE);
        clearListCta.setVisibility((rights & Constants.MASK_RIGHT_CLEAN) == Constants.MASK_RIGHT_CLEAN
                ? View.VISIBLE
                : View.GONE);
        deleteListCta.setVisibility((rights & Constants.MASK_RIGHT_DELETE) == Constants.MASK_RIGHT_DELETE
                ? View.VISIBLE
                : View.GONE);
        deleteCheckedCta.setVisibility((rights & Constants.MASK_RIGHT_CLEAN) == Constants.MASK_RIGHT_CLEAN
                ? View.VISIBLE
                : View.GONE);

        addUsersCta.setVisibility((rights & Constants.MASK_RIGHT_ADD_USERS) == Constants.MASK_RIGHT_ADD_USERS
                ? View.VISIBLE
                : View.GONE);
        removeUsersCta.setVisibility((rights & Constants.MASK_RIGHT_REMOVE_USERS) == Constants.MASK_RIGHT_REMOVE_USERS
                ? View.VISIBLE
                : View.GONE);

        menuLayout.setVisibility(rights >= Constants.MASK_RIGHT_SEE ? View.VISIBLE : View.GONE);
    }

    private int getUsersRights() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || checkList == null) {
            return 0;
        }

        String email = user.getEmail();
        UserRights rights = checkList.getAuthorizedUsers().get(email);

        return rights == null ? 0 : rights.getRights();
    }

    public CheckList getCheckList() {
        return checkList;
    }

    private void onSubmitNewEntry() {
        final String entry = newListEntry.getText().toString();

        if (entry.length() > 0) {
            dbReference.getRef().child(FB_KEY_ITEMS).push()
                    .setValue(new CheckListEntry(entry), dbUpdateCompletionListener);
        } else {
            Toast.makeText(getContext(), R.string.warn_empty_item, Toast.LENGTH_LONG).show();
        }
    }

    private final View.OnClickListener newUserSubmittedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String user = newUserEntry.getText().toString();

            if (user.length() <= 0) {
                Toast.makeText(getContext(), R.string.warn_empty_user, Toast.LENGTH_LONG).show();
                return;
            }

            UserRights rights = new UserRights();
            rights.setId(user);
            rights.setRights(computeRightsCode());

            dbReference.child(FB_KEY_USERS).child(rights.getId())
                    .setValue(rights, dbUpdateCompletionListener);
        }
    };

    private int computeRightsCode() {

        int rightsCode = Constants.MASK_RIGHT_SEE;

        if (cleanPermissionCheckBox.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_CLEAN;
        }
        if (addItemsPermissionCheckBox.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_ADD_ITEM;
        }
        if (addUsersPermissionCheckBox.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_ADD_USERS;
        }
        if (checkPermissionCheckBox.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_CHECK;
        }
        if (deletePermissionCheckBox.isChecked()) {
            rightsCode |= Constants.MASK_RIGHT_DELETE;
        }
        if (removeUsersPermissionCheckBox.isChecked()) {
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

    private final View.OnClickListener openAddItemDialogListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            newEntryDialog.show();
        }
    };

    private final ItemClickCallback itemClickedListener = new ItemClickCallback() {
        @Override
        public void onItemClick(DatabaseReference ref, CheckListEntry entry) {
            Log.d(LOG_TAG, "onItemClick: Entry: " + entry + " Ref: " + ref);

            final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) {
                return;
            }

            if ((checkList.getRightsOf(currentUser.getEmail()) & Constants.MASK_RIGHT_CHECK) != Constants.MASK_RIGHT_CHECK) {
                return;
            }

            String checker = null;
            if (!entry.isChecked()) {
                checker = currentUser.getEmail();
            }

            ref.child(CheckListEntry.FB_KEY_CHECKED).setValue(checker, dbUpdateCompletionListener);
        }
    };

    private final CompletionListener dbUpdateCompletionListener = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
            if (databaseError != null) {
                Log.e(LOG_TAG, "onComplete", databaseError.toException());
                Toast.makeText(getContext(), R.string.err_network, Toast.LENGTH_LONG).show();
            } else {
                newListEntry.setText(null);
                newUserEntry.setText(null);

                if (confirmationDialog != null && confirmationDialog.isShowing()) {
                    confirmationDialog.dismiss();
                }
            }

        }
    };

    private final View.OnClickListener openDeleteCheckConfirmationListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_checked), checkList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, cleanClickedListener)
                    .setNegativeButton(R.string.answer_negative, dismissClickedListener);

            confirmationDialog = builder.create();
            confirmationDialog.show();
        }
    };

    private final View.OnClickListener openDeleteListConfirmationListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_list), checkList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, deleteListListener)
                    .setNegativeButton(R.string.answer_negative, dismissClickedListener);

            confirmationDialog = builder.create();
            confirmationDialog.show();
        }
    };

    private final DialogInterface.OnClickListener deleteListListener = new DialogInterface
            .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dbReference.setValue(null, dbUpdateCompletionListener);
        }
    };

    private final View.OnClickListener openClearConfirmationListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder = builder.setMessage(String.format(
                    getString(R.string.message_confirm_delete_all), checkList.getName()))
                    .setPositiveButton(R.string.answer_affirmative, clearClickListener)
                    .setNegativeButton(R.string.answer_negative, dismissClickedListener);

            confirmationDialog = builder.create();
            confirmationDialog.show();
        }
    };

    private final DialogInterface.OnClickListener cleanClickedListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            List<String> checked = checkListAdapter.getCheckedEntries();

            Map<String, Object> updateMap = new HashMap<>();
            for (String entry : checked) {
                updateMap.put(entry, null);
            }

            dbReference.child(FB_KEY_ITEMS).updateChildren(updateMap, dbUpdateCompletionListener);
        }
    };

    private final DialogInterface.OnClickListener clearClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dbReference.child(FB_KEY_ITEMS).setValue(null, dbUpdateCompletionListener);
        }
    };

    private final DialogInterface.OnClickListener dismissClickedListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    };

    private final View.OnClickListener openNewUserDialogListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();
            addUserDialog.show();
        }
    };

    private final View.OnClickListener openRemoveUserDialogListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeAllDialogs();
            removeUserDialog.show();
        }
    };

    private final View.OnClickListener removeUserClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            List<UserRights> toDelete = userAdapter.getCheckedEntries();

            Log.d(LOG_TAG, "onClick: To remove: " + toDelete.toString());

            if (toDelete.size() <= 0) {
                Toast.makeText(getContext(), R.string.warn_empty_user_set, Toast.LENGTH_LONG).show();
            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder = builder.setMessage(getString(R.string.message_confirm_delete_users,
                        enumerate(userAdapter.getCheckedEntries())))
                        .setPositiveButton(R.string.answer_affirmative, removeUsersListener)
                        .setNegativeButton(R.string.answer_negative, dismissClickedListener);

                confirmationDialog = builder.create();
                confirmationDialog.show();
            }
        }
    };

    private final DialogInterface.OnClickListener removeUsersListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int yo) {
            confirmationDialog.dismiss();

            List<UserRights> toDelete = userAdapter.getCheckedEntries();

            removeUsersSubmitCta.setVisibility(View.GONE);
            removeUsersLoadView.setVisibility(View.VISIBLE);

            Map<String, Object> updateMap = new HashMap<>();

            for (int i = 0; i < toDelete.size(); i++) {
                UserRights user = toDelete.get(i);
                updateMap.put(user.getId(), null);
            }

            dbReference.child(FB_KEY_USERS).updateChildren(updateMap, userDeletedListener);
        }
    };

    private final CompletionListener userDeletedListener = new CompletionListener() {
        @Override
        public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
            if (databaseError != null) {
                Toast.makeText(getContext(), R.string.err_network, Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG, "onComplete: ", databaseError.toException());
            } else {
                removeUserDialog.dismiss();
            }

            removeUsersSubmitCta.setVisibility(View.GONE);
            removeUsersLoadView.setVisibility(View.VISIBLE);
        }
    };
}
