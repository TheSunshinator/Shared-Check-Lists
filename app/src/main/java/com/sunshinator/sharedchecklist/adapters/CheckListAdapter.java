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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.adapters.CheckListAdapter.ViewHolder;
import com.sunshinator.sharedchecklist.objects.CheckListEntry;

import java.util.ArrayList;
import java.util.List;

public class CheckListAdapter extends FirebaseRecyclerAdapter<CheckListEntry, ViewHolder> {

    private static final String LOG_TAG = CheckListAdapter.class.getSimpleName();

    private ItemClickCallback callback;

    public CheckListAdapter(Query ref, ItemClickCallback callback) {
        super(new FirebaseRecyclerOptions.Builder<CheckListEntry>()
                .setQuery(ref, CheckListEntry.class)
                .build());
        Log.d(LOG_TAG, "CheckListAdapter: Ref: " + ref);

        this.callback = callback;
    }

    public List<String> getCheckedEntries() {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < getItemCount(); i++) {
            final CheckListEntry item = getItem(i);
            if (item.isChecked()) {
                list.add(getRef(i).getKey());
            }
        }

        return list;
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull CheckListEntry model) {
        Log.d(LOG_TAG, "populateViewHolder: with entry: " + model.toString());
        DatabaseReference ref = getRef(position);
        holder.setToEntry(model, ref, callback);
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
        private CheckListEntry entryView;
        private ItemClickCallback callback;
        private DatabaseReference reference;

        public ViewHolder(View v) {
            super(v);

            textView = (TextView) v;
            textView.setOnClickListener(onClickListener);
        }

        private void setToEntry(CheckListEntry entry, DatabaseReference ref, ItemClickCallback callback) {
            entryView = entry;

            if (entryView != null) {
                if (entryView.isChecked()) {
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                textView.setText(entryView.getEntry());
            } else {
                textView.setText(null);
            }

            reference = ref;
            this.callback = callback;
        }

        private final View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null) {
                    callback.onItemClick(reference, entryView);
                }
            }
        };
    }

    public interface ItemClickCallback {
        void onItemClick(DatabaseReference ref, CheckListEntry entry);
    }
}
