package com.sunshinator.sharedchecklist.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.adapters.StringAdapter.ViewHolder;

/**
 * Firebase Recycler Adapter for the grocery list
 * <p>
 * Created by The Sunshinator on 11/04/2017.
 */
public class StringAdapter<T>
    extends FirebaseRecyclerAdapter<T, ViewHolder> {

  private static final String LOG_TAG = CheckListAdapter.class.getSimpleName();

  private ItemClickCallback mCallback;

  public StringAdapter( Class<T> modelClass, Query ref, ItemClickCallback callback ) {

    super( modelClass, R.layout.item_list_entry, ViewHolder.class, ref );

    Log.d( LOG_TAG, "CheckListAdapter: Ref: " + ref );

    mCallback = callback;
  }

  @Override
  protected void populateViewHolder( ViewHolder viewHolder, T entry, int position ) {

    Log.d( LOG_TAG, "populateViewHolder: with entry: " + entry.toString() );
    DatabaseReference ref = getRef( position );
    viewHolder.setToEntry( entry, ref, mCallback );
  }

  @SuppressWarnings( "WeakerAccess" ) // Class is needed public static for Firebase
  public static class ViewHolder
      extends RecyclerView.ViewHolder {

    private TextView          mvRoot;
    private Object            mEntry;
    private ItemClickCallback mCallback;
    private DatabaseReference mReference;

    public ViewHolder( View v ) {

      super( v );

      mvRoot = (TextView) v;
      mvRoot.setOnClickListener( l_OnClick );
    }

    private void setToEntry( Object entry, DatabaseReference ref, ItemClickCallback callback ) {

      mEntry = entry;

      mvRoot.setText( entry.toString() );

      mReference = ref;
      mCallback = callback;
    }

    private final View.OnClickListener l_OnClick = new OnClickListener() {
      @Override
      public void onClick( View view ) {

        if ( mCallback != null ) {
          mCallback.onItemClick( mReference );
        }

      }
    };
  }

  public interface ItemClickCallback {

    void onItemClick( DatabaseReference ref );
  }
}
