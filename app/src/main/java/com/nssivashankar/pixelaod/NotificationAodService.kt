package com.nssivashankar.pixelaod

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import java.util.Calendar

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()
    private var isCharging = false
    private var isDndActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    updateAodState()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    syncActiveNotifications()
                    updateAodState()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    // Charging is active only when actually charging, not when full
                    isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING)
                    updateAodState()
                }
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    updateDndStatus()
                    updateAodState()
                }
                Intent.ACTION_TIME_TICK -> {
                    if (getPrefs().getBoolean("scheduled_dnd", false)) {
                        updateAodState()
                    }
                }
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "master_switch", "watched_apps", "live_notif_mode", "charging_mode", "dnd_mode",
            "scheduled_dnd", "scheduled_dnd_start", "scheduled_dnd_end" -> {
                syncActiveNotifications()
            }
        }
    }

    private fun getPrefs() = getSharedPreferences("aod_prefs", MODE_PRIVATE)

    override fun onCreate() {
        super.onCreate()
        getPrefs().registerOnSharedPreferenceChangeListener(prefListener)
        
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING)

        updateDndStatus()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        registerReceiver(receiver, filter)
    }

    private fun updateDndStatus() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        isDndActive = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    override fun onDestroy() {
        super.onDestroy()
        getPrefs().unregisterOnSharedPreferenceChangeListener(prefListener)
        unregisterReceiver(receiver)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        syncActiveNotifications()
    }

    private fun syncActiveNotifications() {
        val prefs = getPrefs()
        if (!prefs.getBoolean("master_switch", false)) {
            activeNotifKeys.clear()
            updateAodState()
            return
        }

        val watchedApps = prefs.getStringSet("watched_apps", emptySet()) ?: emptySet()
        val liveMode = prefs.getBoolean("live_notif_mode", false)
        val activeNotifs = try { activeNotifications } catch (e: Exception) { 
            Log.e("NotificationAodService", "Error getting active notifications", e)
            null 
        } ?: return

        activeNotifKeys.clear()
        activeNotifs.forEach { sbn ->
            if (shouldTrigger(sbn, watchedApps, liveMode)) {
                activeNotifKeys.add(sbn.key)
            }
        }
        updateAodState()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getPrefs()
        if (!prefs.getBoolean("master_switch", false)) return

        val watchedApps = prefs.getStringSet("watched_apps", emptySet()) ?: emptySet()
        val liveMode = prefs.getBoolean("live_notif_mode", false)

        if (shouldTrigger(sbn, watchedApps, liveMode)) {
            activeNotifKeys.add(sbn.key)
        } else {
            activeNotifKeys.remove(sbn.key)
        }
        updateAodState()
    }

    private fun shouldTrigger(sbn: StatusBarNotification, watchedApps: Set<String>, liveMode: Boolean): Boolean {
        // Ignore system notifications for "Live" mode unless specifically watched
        val isSystem = sbn.packageName == "android" || sbn.packageName == "com.android.systemui"
        
        val ranking = Ranking()
        val isSilent = if (currentRanking.getRanking(sbn.key, ranking)) {
            ranking.importance < NotificationManager.IMPORTANCE_DEFAULT
        } else {
            false
        }

        if (isSilent) return false

        val isWatchedApp = sbn.packageName in watchedApps
        
        // Live mode: ongoing notifications from non-system apps
        val category = sbn.notification.category
        val isLiveNotif = liveMode && sbn.isOngoing && !isSystem && (
            category == "navigation" ||
            category == Notification.CATEGORY_SERVICE ||
            category == Notification.CATEGORY_TRANSPORT ||
            category == Notification.CATEGORY_PROGRESS ||
            sbn.packageName.contains("uber", ignoreCase = true) ||
            sbn.packageName.contains("grab", ignoreCase = true) ||
            sbn.packageName.contains("lyft", ignoreCase = true)
        )

        return isWatchedApp || isLiveNotif
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeNotifKeys.remove(sbn.key)
        updateAodState()
    }

    private fun updateAodState() {
        val prefs = getPrefs()
        val masterEnabled = prefs.getBoolean("master_switch", false)
        
        if (!masterEnabled) {
            setAod(enable = false)
            return
        }

        val chargingMode = prefs.getBoolean("charging_mode", false)
        val chargingTrigger = chargingMode && isCharging
        
        // System DND check
        val respectDnd = prefs.getBoolean("dnd_mode", false)
        val systemNotifAllowed = if (respectDnd) !isDndActive else true
        
        // Scheduled DND check
        val isQuietHours = if (prefs.getBoolean("scheduled_dnd", false)) {
            isInQuietHours(
                prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00",
                prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00"
            )
        } else {
            false
        }

        val notifTrigger = systemNotifAllowed && !isQuietHours && activeNotifKeys.isNotEmpty()
        val shouldBeOn = chargingTrigger || notifTrigger

        Log.d("AodService", "State Update: Charging=$isCharging, Notifs=${activeNotifKeys.size}, ShouldBeOn=$shouldBeOn")

        setAod(enable = shouldBeOn)
    }

    private fun isInQuietHours(startStr: String, endStr: String): Boolean {
        try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val startParts = startStr.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()

            val endParts = endStr.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Overnight period (e.g., 22:00 to 07:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun setAod(enable: Boolean) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val currentState = AodSettings.isAodEnabled(contentResolver)
        if (currentState == enable) return // Avoid redundant writes

        try {
            AodSettings.setAodEnabled(contentResolver, enable)
            
            // Workaround for newer Android versions (15, 16, 17) where the screen 
            // might not reflect the Secure.Settings change immediately when on AOD/Lockscreen.
            // We force a display state refresh by sending a broadcast that SystemUI listens to.
            if (!enable) {
                val intent = Intent("com.android.systemui.doze.pulse")
                sendBroadcast(intent)
            }
        } catch (e: SecurityException) {
            Log.e("NotificationAodService", "Failed to set AOD state", e)
        }
    }
}