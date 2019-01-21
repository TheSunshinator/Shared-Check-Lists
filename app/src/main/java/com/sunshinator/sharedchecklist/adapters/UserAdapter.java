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

    private List<UserRights> selectedRights = new ArrayList<>();

    public UserAdapter(Query ref) {
        super(new FirebaseRecyclerOptions.Builder<UserRights>()
                .setQuery(ref, UserRights.class)
                .build());

        Log.d(LOG_TAG, "CheckListAdapter: Ref: " + ref);
    }

    public List<UserRights> getCheckedEntries() {
        return selectedRights;
    }

    private final ItemClickCallback onItemClickedListener = new ItemClickCallback() {
        @Override
        public void onItemClick(UserRights entry) {
            if (!selectedRights.remove(entry)) {
                selectedRights.add(entry);
            }
        }
    };

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull UserRights model) {
        DatabaseReference ref = getRef(position);
        model.setId(ref.getKey());
        Log.d(LOG_TAG, "populateViewHolder: with entry: " + model.toString());
        holder.setToEntry(model, ref, onItemClickedListener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_entry, parent, false);
        return new ViewHolder(view);
    }

    @SuppressWarnings("WeakerAccess") // Class is needed public static for Firebase
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private UserRights entry;
        private DatabaseReference reference;
        private ItemClickCallback callback;

        public ViewHolder(View v) {
            super(v);

            textView = (TextView) v;
            textView.setOnClickListener(onClickListener);
        }

        private void setToEntry(UserRights entry, DatabaseReference ref, ItemClickCallback callback) {
            this.entry = entry;

            if (this.entry != null) {
                if ((this.entry.getRights() & Constants.MASK_RIGHT_SEE) != Constants.MASK_RIGHT_SEE) {
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                textView.setText(this.entry.toString());
            } else {
                textView.setText(null);
            }

            reference = ref;
            this.callback = callback;
        }

        private final View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser auth = FirebaseAuth.getInstance().getCurrentUser();

                if (entry.getId() == null
                        || auth == null
                        || auth.getEmail() == null
                        || entry.toString().equals(auth.getEmail())) {
                    Toast.makeText(textView.getContext(), R.string.warn_delete_self, Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                entry.setRights((entry.getRights() & Constants.MASK_RIGHT_SEE) ^ Constants.MASK_RIGHT_SEE);
                setToEntry(entry, reference, callback);

                callback.onItemClick(entry);
            }
        };
    }

    private interface ItemClickCallback {
        void onItemClick(UserRights entry);
    }
}
