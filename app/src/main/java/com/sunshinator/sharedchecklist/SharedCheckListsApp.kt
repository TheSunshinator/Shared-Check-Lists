package com.sunshinator.sharedchecklist

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class SharedCheckListsApp : Application(){

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }
}