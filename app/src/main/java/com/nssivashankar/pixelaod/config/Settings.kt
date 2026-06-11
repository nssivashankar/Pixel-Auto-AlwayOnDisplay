package com.nssivashankar.pixelaod.config

import android.content.ContentResolver
import android.provider.Settings as AndroidSettings

// Keep in sync with
// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/Settings.java
object Settings {
    const val DOZE_ALWAYS_ON = "doze_always_on"

    fun isAodEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            AndroidSettings.Secure.getInt(contentResolver, DOZE_ALWAYS_ON) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun setAodEnabled(contentResolver: ContentResolver, enabled: Boolean) {
        try {
            AndroidSettings.Secure.putInt(contentResolver, DOZE_ALWAYS_ON, if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}