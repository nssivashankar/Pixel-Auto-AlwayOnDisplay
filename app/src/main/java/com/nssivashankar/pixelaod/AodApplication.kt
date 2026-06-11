package com.nssivashankar.pixelaod

import android.app.Application
import com.google.android.material.color.DynamicColors

class AodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}