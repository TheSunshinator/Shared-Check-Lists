package com.sunshinator.sharedchecklist.adapters;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.sunshinator.sharedchecklist.Constants;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.adapters.UserAdapter.ViewHolder;
import com.sunshinator.sharedchecklist.objects.UserRights;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends FirebaseRecyclerAdapter<UserRights, ViewHolder> {

    private static final String LOG_TAG = CheckListAdapter.class.getSimpleName();

    private List<UserRights> mSelected = new ArrayList<>();

    public UserAdapter(Query ref) {
        super(new FirebaseRecyclerOptions.Builder<UserRights>()
                .setQuery(ref, UserRights.class)
                .build());

        Log.d(LOG_TAG, "CheckListAdapter: Ref: " + ref);
    }

    public List<UserRights> getCheckedEntries() {
        return mSelected;
    }

    private final ItemClickCallback l_ItemClicked = new ItemClickCallback() {
        @Override
        public void onItemClick(UserRights entry) {
            if (!mSelected.remove(entry)) {
                mSelected.add(entry);
            }
        }
    };

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull UserRights model) {
        DatabaseReference ref = getRef(position);
        model.setId(ref.getKey());
        Log.d(LOG_TAG, "populateViewHolder: with entry: " + model.toString());
        holder.setToEntry(model, ref, l_ItemClicked);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_entry, parent, false);

        return new ViewHolder(view);
    }

    @SuppressWarnings("WeakerAccess") // Class is needed public static for Firebase
    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        private TextView mvRoot;
        private UserRights mEntry;
        private DatabaseReference mReference;
        private ItemClickCallback mCallback;

        public ViewHolder(View v) {

            super(v);

            mvRoot = (TextView) v;
            mvRoot.setOnClickListener(l_OnClick);
        }

        private void setToEntry(UserRights entry, DatabaseReference ref, ItemClickCallback callback) {

            mEntry = entry;

            if (mEntry != null) {
                if ((mEntry.getRights() & Constants.MASK_RIGHT_SEE) != Constants.MASK_RIGHT_SEE) {
                    mvRoot.setPaintFlags(mvRoot.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mvRoot.setPaintFlags(mvRoot.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                mvRoot.setText(mEntry.toString());
            } else {
                mvRoot.setText(null);
            }

            mReference = ref;
            mCallback = callback;
        }

        private final View.OnClickListener l_OnClick = new OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseUser auth = FirebaseAuth.getInstance().getCurrentUser();

                if (mEntry.getId() == null
                        || auth == null
                        || auth.getEmail() == null
                        || mEntry.toString().equals(auth.getEmail())) {
                    Toast.makeText(mvRoot.getContext(), R.string.warn_delete_self, Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                mEntry.setRights(
                        (mEntry.getRights() & Constants.MASK_RIGHT_SEE) ^ Constants.MASK_RIGHT_SEE);
                setToEntry(mEntry, mReference, mCallback);

                mCallback.onItemClick(mEntry);
            }
        };
    }

    private interface ItemClickCallback {
        void onItemClick(UserRights entry);
    }
}
