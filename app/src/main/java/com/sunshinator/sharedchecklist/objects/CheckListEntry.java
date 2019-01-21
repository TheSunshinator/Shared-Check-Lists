package com.sunshinator.sharedchecklist.objects;

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;

@SuppressWarnings("WeakerAccess")
public class CheckListEntry {

    public static final String FB_KEY_ENTRY = "entry";
    public static final String FB_KEY_CHECKED = "checkedBy";

    private String entry = null;
    private String checkerEmail = null;
    private String uid = null;

    public static CheckListEntry parse(DataSnapshot snapshot) {
        CheckListEntry instance = new CheckListEntry();

        instance.uid = snapshot.getKey();
        instance.entry = snapshot.child(FB_KEY_ENTRY).getValue(String.class);
        instance.checkerEmail = snapshot.child(FB_KEY_CHECKED).getValue(String.class);

        return instance;
    }

    public CheckListEntry() { }
    public CheckListEntry(String entry, String checkedBy) {
        this.entry = entry;
        checkerEmail = checkedBy;
    }

    public CheckListEntry(String entry) {
        this.entry = entry;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CheckListEntry) {

            CheckListEntry subject = (CheckListEntry) obj;
            return entry == null
                    ? subject.entry == null
                    : entry.equals(subject.entry);

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {

        return entry.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return (checkerEmail != null ? "[x] " : "[ ] ") + entry;
    }

    @Exclude
    public boolean isChecked() {
        return checkerEmail != null;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getCheckedBy() {
        return checkerEmail;
    }

    public void setCheckedBy(String checkedBy) {
        checkerEmail = checkedBy;
    }

    @Exclude
    public String getUid() {
        return uid;
    }

    @Exclude
    public void setUid(String uid) {
        this.uid = uid;
    }
}
