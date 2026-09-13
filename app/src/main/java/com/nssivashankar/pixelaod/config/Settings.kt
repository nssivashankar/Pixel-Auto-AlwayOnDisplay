package com.nssivashankar.pixelaod.config

import android.content.ContentResolver
import android.provider.Settings as AndroidSettings

// Keep in sync with
// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/Settings.java
object Settings {
    const val DOZE_ALWAYS_ON = "doze_always_on"
    const val CHARGE_OPTIMIZATION_MODE = "charge_optimization_mode"
    const val ADAPTIVE_CHARGING_ENABLED = "adaptive_charging_enabled"

    val OBSERVABLE_SECURE_SETTINGS = listOf(
        DOZE_ALWAYS_ON,
        CHARGE_OPTIMIZATION_MODE,
        ADAPTIVE_CHARGING_ENABLED
    )

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

    fun getChargeOptimizationMode(contentResolver: ContentResolver): Int {
        return try {
            AndroidSettings.Secure.getInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0)
        } catch (_: Exception) {
            0
        }
    }

    fun setChargeOptimizationMode(contentResolver: ContentResolver, mode: Int) {
        try {
            AndroidSettings.Secure.putInt(contentResolver, CHARGE_OPTIMIZATION_MODE, mode)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAdaptiveChargingEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            AndroidSettings.Secure.getInt(contentResolver, ADAPTIVE_CHARGING_ENABLED, 1) == 1
        } catch (_: Exception) {
            true
        }
    }

    fun setAdaptiveChargingEnabled(contentResolver: ContentResolver, enabled: Boolean) {
        try {
            AndroidSettings.Secure.putInt(contentResolver, ADAPTIVE_CHARGING_ENABLED, if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}