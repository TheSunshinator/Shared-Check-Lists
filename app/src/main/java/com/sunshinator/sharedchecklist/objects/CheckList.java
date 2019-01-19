package com.sunshinator.sharedchecklist.objects;

import android.support.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;
import com.sunshinator.sharedchecklist.Constants;
import java.util.HashMap;
import java.util.Map;

/**
 * Object representation of a checklist in Firebase
 * <p>
 * Created by The Sunshinator on 19/04/2017.
 */
public class CheckList {

  private static final String FB_KEY_ITEMS = "items";
  private static final String FB_KEY_NAME  = "name";
  private static final String FB_KEY_USERS = "authorizedUsers";

  private String                      mName    = null;
  private String                      mUid     = null;
  private Map<String, UserRights>     mUsers   = null;
  private Map<String, CheckListEntry> mEntries = null;

  public static CheckList parse( @NonNull DataSnapshot snapshot ) {

    CheckList instance = new CheckList();
    instance.mUid = snapshot.getKey();
    instance.mName = snapshot.child( FB_KEY_NAME ).getValue( String.class );

    if ( snapshot.child( FB_KEY_USERS ).exists() ) {
      instance.mUsers = new HashMap<>();

      for ( DataSnapshot user : snapshot.child( FB_KEY_USERS ).getChildren() ) {
        final UserRights parsed = UserRights.parse( user );
        instance.mUsers.put( parsed.getId(), parsed );
      }

    }

    if ( snapshot.child( FB_KEY_ITEMS ).exists() ) {
      instance.mEntries = new HashMap<>();

      for ( DataSnapshot item : snapshot.child( FB_KEY_ITEMS ).getChildren() ) {
        final CheckListEntry parsed = CheckListEntry.parse( item );
        instance.mEntries.put( parsed.getUid(), parsed );
      }

    }

    return instance;
  }

  public CheckList() {}
  public CheckList( @NonNull String name, @NonNull String email ) {
    mName = name;

    mUsers = new HashMap<>();
    final UserRights adminRights = new UserRights( email, Constants.INSTANCE.getMASK_RIGHT_ALL());
    mUsers.put( adminRights.getId(), adminRights );
  }

  @Override
  public boolean equals( Object obj ) {

    if ( obj == null ) {
      return false;
    }

    if ( obj instanceof CheckList ) {
      CheckList checkList = (CheckList) obj;

      return mUid == null
             ? checkList.mUid == null
             : mUid.equals( checkList.mUid );
    } else if ( obj instanceof String ) {
      String uid = (String) obj;

      return uid.equals( mUid );
    }

    return false;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Exclude
  public int getRightsOf( String email ){
    if( email == null ) return 0;
    if( mUsers == null ) return 0;
    if( !mUsers.containsKey( email ) ) return 0;
    return mUsers.get( email ).getRights();
  }

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- - Getters and Setters - ----- //
  @Exclude
  public String getUid() {

    return mUid;
  }

  @Exclude
  public void setUid( String uid ) {

    mUid = uid;
  }

  @NonNull
  public String getName() {

    return mName;
  }

  public void setName( String name ) {

    mName = name;
  }

  public Map<String, UserRights> getAuthorizedUsers() {

    return mUsers;
  }

  public void setAuthorizedUsers( Map<String, UserRights> users ) {

    mUsers = users;
  }

  public Map<String, CheckListEntry> getItems() {

    return mEntries;
  }

  public void setItems( Map<String, CheckListEntry> entries ) {

    mEntries = entries;
  }

  // endregion
  // ----- ----- ----- ----- ----- ----- //

}
