package com.nssivashankar.pixelaod.config

import androidx.compose.ui.graphics.Color

object Constants {
    // --- Notification Channels & IDs ---
    const val CHARGING_CHANNEL_ID = "charging_live_v11_fix"
    const val COMPLETION_CHANNEL_ID = "battery_completion_v1"
    const val AOD_REFRESH_CHANNEL_ID = "aod_refresh_channel"

    const val CHARGING_NOTIF_ID = 1001
    const val COMPLETION_NOTIF_ID = 1002
    const val GHOST_NOTIF_ID = 999

    // --- Intent Actions ---
    const val ACTION_OPT_OFF = "com.nssivashankar.pixelaod.ACTION_OPT_OFF"
    const val ACTION_OPT_80 = "com.nssivashankar.pixelaod.ACTION_OPT_80"
    const val ACTION_OPT_ADAPTIVE = "com.nssivashankar.pixelaod.ACTION_OPT_ADAPTIVE"
    const val ACTION_FULL_CHARGE = "com.nssivashankar.pixelaod.ACTION_FULL_CHARGE"
    const val ACTION_TIMEOUT_SCREEN_OFF = "com.nssivashankar.pixelaod.ACTION_TIMEOUT_SCREEN_OFF"
    const val ACTION_TIMEOUT_LIFT = "com.nssivashankar.pixelaod.ACTION_TIMEOUT_LIFT"

    // --- Timeouts & Delays ---
    const val CHARGING_UPDATE_INTERVAL_MS = 15_000L
    const val DEFAULT_PREVIEW_TIMEOUT_MS = 10_000L
    const val DEFAULT_PREVIEW_TIMEOUT_SECONDS = 10
    val PREVIEW_DURATION_OPTIONS = listOf(5, 10, 20, 30, 40, 50, 60)
    const val WAKELOCK_TIMEOUT_MS = 12_000L
    const val DOZE_SETTLE_DELAY_MS = 250L

    // --- Sensor Constants ---
    const val SENSOR_TYPE_PICK_UP = 25 // Sensor.TYPE_PICK_UP_GESTURE on Pixel devices

    // --- System Package & Keyword Exclusions ---
    val SYSTEM_NOISE_PACKAGES = setOf("android", "com.android.systemui")
    
    val DEFAULT_LIVE_APP_KEYWORDS = listOf(
        "uber", "ride", "delivery", "food", "track", 
        "map", "grab", "rapido", "ola", "zomato", "swiggy"
    )

    // --- Battery Level Progress Colors ---
    fun getBatteryProgressColor(batteryPct: Int): Int {
        return when {
            batteryPct < 20 -> android.graphics.Color.parseColor("#E53935") // Red
            batteryPct < 35 -> android.graphics.Color.parseColor("#FB8C00") // Orange
            batteryPct < 60 -> android.graphics.Color.parseColor("#FDD835") // Gold
            batteryPct < 80 -> android.graphics.Color.parseColor("#4CAF50") // Light Green
            else -> android.graphics.Color.parseColor("#00E676")           // Emerald Green
        }
    }
}
