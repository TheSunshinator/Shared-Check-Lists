package com.sunshinator.sharedchecklist.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.adapters.StringAdapter.ViewHolder;
import com.sunshinator.sharedchecklist.objects.UserRights;

public class StringAdapter<T> extends FirebaseRecyclerAdapter<T, ViewHolder> {

    private static final String LOG_TAG = CheckListAdapter.class.getSimpleName();

    private ItemClickCallback mCallback;

    public StringAdapter(Class<T> modelClass, Query ref, ItemClickCallback callback) {
        super(new FirebaseRecyclerOptions.Builder<T>()
                .setQuery(ref, modelClass)
                .build());

        Log.d(LOG_TAG, "CheckListAdapter: Ref: " + ref);

        mCallback = callback;
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull T model) {
        Log.d(LOG_TAG, "populateViewHolder: with entry: " + model.toString());
        DatabaseReference ref = getRef(position);
        holder.setToEntry(model, ref, mCallback);
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
        private ItemClickCallback mCallback;
        private DatabaseReference mReference;

        public ViewHolder(View v) {
            super(v);

            mvRoot = (TextView) v;
            mvRoot.setOnClickListener(l_OnClick);
        }

        private void setToEntry(Object entry, DatabaseReference ref, ItemClickCallback callback) {
            mvRoot.setText(entry.toString());
            mReference = ref;
            mCallback = callback;
        }

        private final View.OnClickListener l_OnClick = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallback != null) {
                    mCallback.onItemClick(mReference);
                }
            }
        };
    }

    public interface ItemClickCallback {
        void onItemClick(DatabaseReference ref);
    }
}
