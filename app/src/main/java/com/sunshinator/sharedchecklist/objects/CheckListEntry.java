package com.sunshinator.sharedchecklist.objects;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;

@SuppressWarnings( "WeakerAccess" )
public class CheckListEntry {

  public static final String FB_KEY_ENTRY   = "entry";
  public static final String FB_KEY_CHECKED = "checkedBy";

  private String mEntry     = null;
  private String mCheckedBy = null;
  private String mUid       = null;

  public static CheckListEntry parse( DataSnapshot snapshot ) {

    CheckListEntry instance = new CheckListEntry();

    instance.mUid = snapshot.getKey();
    instance.mEntry = snapshot.child( FB_KEY_ENTRY ).getValue( String.class );
    instance.mCheckedBy = snapshot.child( FB_KEY_CHECKED ).getValue( String.class );

    return instance;
  }

  public CheckListEntry() {}

  public CheckListEntry( String entry, String checkedBy ) {

    mEntry = entry;
    mCheckedBy = checkedBy;
  }

  public CheckListEntry( String entry ) {

    mEntry = entry;
  }

  @Override
  public boolean equals( Object obj ) {

    if ( obj instanceof CheckListEntry ) {

      CheckListEntry subject = (CheckListEntry) obj;
      return mEntry == null
             ? subject.mEntry == null
             : mEntry.equals( subject.mEntry );

    } else { return false; }
  }

  @Override
  public int hashCode() {

    return mEntry.hashCode();
  }

  @Override
  public String toString() {

    return ( mCheckedBy != null? "[x] " : "[ ] " ) + mEntry;
  }

  @Exclude
  public boolean isChecked() {

    return mCheckedBy != null;
  }

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- - Getters and Setters - ----- //
  public String getEntry() {

    return mEntry;
  }

  public void setEntry( String entry ) {

    this.mEntry = entry;
  }

  public String getCheckedBy() {

    return mCheckedBy;
  }

  public void setCheckedBy( String checkedBy ) {

    mCheckedBy = checkedBy;
  }

  @Exclude
  public String getUid() {

    return mUid;
  }

  @Exclude
  public void setUid( String uid ) {

    mUid = uid;
  }

  // endregion
  // ----- ----- ----- ----- ----- ----- //
}
