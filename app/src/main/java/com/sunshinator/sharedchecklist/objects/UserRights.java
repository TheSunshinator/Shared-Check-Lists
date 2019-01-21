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
@SuppressWarnings("WeakerAccess")
public class UserRights {

    public static final String FB_KEY_RIGHTS = "rights";

    @IntDef(value = {
            Constants.MASK_RIGHT_REMOVE_USERS,
            Constants.MASK_RIGHT_ADD_ITEM,
            Constants.MASK_RIGHT_ADD_USERS,
            Constants.MASK_RIGHT_ALL,
            Constants.MASK_RIGHT_CHECK,
            Constants.MASK_RIGHT_CLEAN,
            Constants.MASK_RIGHT_DELETE,
            Constants.MASK_RIGHT_SEE
    })
    public @interface Mask {
    }

    private int rights = 0;
    private String uid = null;

    public static UserRights parse(@NonNull DataSnapshot snapshot) {
        UserRights instance = new UserRights();
        //noinspection ConstantConditions
        instance.uid = snapshot.getKey().replaceAll(",", ".");

        if (snapshot.child(FB_KEY_RIGHTS).exists()) {
            //noinspection ConstantConditions
            instance.rights = snapshot.child(FB_KEY_RIGHTS).getValue(Integer.class);
        }

        return instance;
    }

    public UserRights() {}

    public UserRights(@NonNull String email, @Mask int rights) {
        setId(email);
        this.rights = rights;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserRights) {
            UserRights userRights = (UserRights) obj;

            return uid == null
                    ? userRights.uid == null
                    : uid.equals(userRights.uid);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return uid.replaceAll(",", ".");
    }

    public int getRights() {
        return rights;
    }

    public void setRights(int rights) {
        this.rights = rights;
    }

    @Exclude
    public String getId() {
        return uid;
    }

    @Exclude
    public void setId(String id) {
        uid = id.replaceAll("\\.", ",");
    }
}
