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
    private static final String FB_KEY_NAME = "name";
    private static final String FB_KEY_USERS = "authorizedUsers";

    private String name = null;
    private String uid = null;
    private Map<String, UserRights> users = null;
    private Map<String, CheckListEntry> entries = null;

    public static CheckList parse(@NonNull DataSnapshot snapshot) {
        CheckList instance = new CheckList();
        instance.uid = snapshot.getKey();
        instance.name = snapshot.child(FB_KEY_NAME).getValue(String.class);

        if (snapshot.child(FB_KEY_USERS).exists()) {
            instance.users = new HashMap<>();

            for (DataSnapshot user : snapshot.child(FB_KEY_USERS).getChildren()) {
                final UserRights parsed = UserRights.parse(user);
                instance.users.put(parsed.getId(), parsed);
            }
        }

        if (snapshot.child(FB_KEY_ITEMS).exists()) {
            instance.entries = new HashMap<>();

            for (DataSnapshot item : snapshot.child(FB_KEY_ITEMS).getChildren()) {
                final CheckListEntry parsed = CheckListEntry.parse(item);
                instance.entries.put(parsed.getUid(), parsed);
            }
        }

        return instance;
    }

    @SuppressWarnings("WeakerAccess")
    public CheckList() {
    }

    public CheckList(@NonNull String name, @NonNull String email) {
        this.name = name;

        users = new HashMap<>();
        final UserRights adminRights = new UserRights(email, Constants.MASK_RIGHT_ALL);
        users.put(adminRights.getId(), adminRights);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj instanceof CheckList) {
            CheckList checkList = (CheckList) obj;

            return uid == null
                    ? checkList.uid == null
                    : uid.equals(checkList.uid);
        } else if (obj instanceof String) {
            String uid = (String) obj;
            return uid.equals(this.uid);
        }

        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Exclude
    public int getRightsOf(String email) {
        if (email == null) return 0;
        if (users == null) return 0;
        UserRights userRights = users.get(email);
        return userRights == null ? 0 : userRights.getRights();
    }

    @Exclude
    public String getUid() {
        return uid;
    }

    @Exclude
    public void setUid(String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, UserRights> getAuthorizedUsers() {
        return users;
    }

    public void setAuthorizedUsers(Map<String, UserRights> users) {
        this.users = users;
    }

    public Map<String, CheckListEntry> getItems() {
        return entries;
    }

    public void setItems(Map<String, CheckListEntry> entries) {
        this.entries = entries;
    }
}
