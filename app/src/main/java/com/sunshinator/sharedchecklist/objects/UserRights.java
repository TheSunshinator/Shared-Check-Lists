package com.sunshinator.sharedchecklist.objects;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;
import com.sunshinator.sharedchecklist.Constants;

/**
 * Identifies a user that has access to a checklist with certain permissions
 * <p>
 * Created by The Sunshinator on 19/04/2017.
 */
public class UserRights {

  public static final String FB_KEY_RIGHTS = "rights";

  @IntDef( value = {
          Constants.INSTANCE.getMASK_RIGHT_REMOVE_USERS(),
          Constants.INSTANCE.getMASK_RIGHT_ADD_ITEM(),
          Constants.INSTANCE.getMASK_RIGHT_ADD_USERS(),
          Constants.INSTANCE.getMASK_RIGHT_ALL(),
          Constants.INSTANCE.getMASK_RIGHT_CHECK(),
          Constants.INSTANCE.getMASK_RIGHT_CLEAN(),
          Constants.INSTANCE.getMASK_RIGHT_DELETE(),
          Constants.INSTANCE.getMASK_RIGHT_SEE()
  })
  public @interface Mask{}

  private int    mRights = 0;
  private String mId     = null;

  public static UserRights parse( @NonNull DataSnapshot snapshot ) {

    UserRights instance = new UserRights();
    instance.mId = snapshot.getKey().replaceAll( ",", "." );

    if ( snapshot.child( FB_KEY_RIGHTS ).exists() ) {

      instance.mRights = snapshot.child( FB_KEY_RIGHTS ).getValue( Integer.class );

    }

    return instance;
  }

  public UserRights() {}


  public UserRights( @NonNull String email, @Mask int rights ) {
    setId( email );
    mRights = rights;
  }

  @Override
  public boolean equals( Object obj ) {

    if ( obj instanceof UserRights ) {
      UserRights userRights = (UserRights) obj;

      return mId == null
             ? userRights.mId == null
             : mId.equals( userRights.mId );
    } else {
      return false;
    }

  }

  @Override
  public int hashCode() {

    return mId.hashCode();

  }

  @Override
  public String toString() {

    return mId.replaceAll( ",", "." );
  }

  // ----- ----- ----- ----- ----- ----- //
  // region // ----- - Getters and Setters - ----- //
  public int getRights() {

    return mRights;
  }

  public void setRights( int rights ) {

    mRights = rights;
  }

  @Exclude
  public String getId() {

    return mId;
  }

  @Exclude
  public void setId( String id ) {

    mId = id.replaceAll( "\\.", "," );
  }
  // endregion
  // ----- ----- ----- ----- ----- ----- //

}
