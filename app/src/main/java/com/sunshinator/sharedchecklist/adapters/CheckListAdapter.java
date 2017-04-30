package com.sunshinator.sharedchecklist.adapters;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.sunshinator.sharedchecklist.adapters.CheckListAdapter.ViewHolder;
import com.sunshinator.sharedchecklist.R;
import com.sunshinator.sharedchecklist.objects.CheckListEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * Firebase Recycler Adapter for the grocery list
 * <p>
 * Created by The Sunshinator on 11/04/2017.
 */
public class CheckListAdapter
    extends FirebaseRecyclerAdapter<CheckListEntry, ViewHolder> {

  private static final String LOG_TAG = CheckListAdapter.class.getSimpleName();

  private ItemClickCallback mCallback;

  public CheckListAdapter( Query ref, ItemClickCallback callback ) {

    super( CheckListEntry.class, R.layout.item_list_entry, ViewHolder.class, ref );

    Log.d( LOG_TAG, "CheckListAdapter: Ref: " + ref );

    mCallback = callback;
  }

  @Override
  protected void populateViewHolder( ViewHolder viewHolder, CheckListEntry entry, int position ) {

    Log.d( LOG_TAG, "populateViewHolder: with entry: " + entry.toString() );
    DatabaseReference ref = getRef( position );
    viewHolder.setToEntry( entry, ref, mCallback );
  }

  @Override
  protected void onCancelled( DatabaseError error ) {

    super.onCancelled( error );

    Log.e( LOG_TAG, "onCancelled: ", error.toException() );
  }

  public List<String> getCheckedEntries() {

    List<String> list = new ArrayList<>();

    for ( int i = 0; i < getItemCount(); i++ ) {
      final CheckListEntry item = getItem( i );
      if ( item.isChecked() ) {
        list.add( getRef( i ).getKey() );
      }
    }

    return list;
  }

  @SuppressWarnings( "WeakerAccess" ) // Class is needed public static for Firebase
  public static class ViewHolder
      extends RecyclerView.ViewHolder {

    private TextView          mvRoot;
    private CheckListEntry    mEntry;
    private ItemClickCallback mCallback;
    private DatabaseReference mReference;

    public ViewHolder( View v ) {

      super( v );

      mvRoot = (TextView) v;
      mvRoot.setOnClickListener( l_OnClick );
    }

    private void setToEntry( CheckListEntry entry, DatabaseReference ref,
                             ItemClickCallback callback ) {

      mEntry = entry;

      if ( mEntry != null ) {
        if ( mEntry.isChecked() ) {
          mvRoot.setPaintFlags( mvRoot.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG );
        } else {
          mvRoot.setPaintFlags( mvRoot.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG );
        }

        mvRoot.setText( mEntry.getEntry() );
      } else {
        mvRoot.setText( null );
      }

      mReference = ref;
      mCallback = callback;
    }

    private final View.OnClickListener l_OnClick = new OnClickListener() {
      @Override
      public void onClick( View view ) {

        if ( mCallback != null ) {
          mCallback.onItemClick( mReference, mEntry );
        }

      }
    };
  }

  public interface ItemClickCallback {

    void onItemClick( DatabaseReference ref, CheckListEntry entry );
  }
}
