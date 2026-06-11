package com.nssivashankar.pixelaod

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nssivashankar.pixelaod.config.Settings as AodSettings

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()
    private var isCharging = false

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                          (status == BatteryManager.BATTERY_STATUS_FULL)
            updateAodState()
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "master_switch" || key == "watched_apps" || key == "live_notif_mode" || key == "charging_mode") {
            syncActiveNotifications()
        }
    }

    private fun getPrefs() = getSharedPreferences("aod_prefs", MODE_PRIVATE)

    override fun onCreate() {
        super.onCreate()
        getPrefs().registerOnSharedPreferenceChangeListener(prefListener)
        
        // Initialize charging state
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                      (status == BatteryManager.BATTERY_STATUS_FULL)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(chargingReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        getPrefs().unregisterOnSharedPreferenceChangeListener(prefListener)
        unregisterReceiver(chargingReceiver)
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
        val activeNotifs = try { activeNotifications } catch (_: Exception) { null } ?: return

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
            ranking.importance < 3 // 3 is IMPORTANCE_DEFAULT
        } else {
            false
        }

        if (isSilent) return false

        val isWatchedApp = sbn.packageName in watchedApps
        
        // Live mode: ongoing notifications from non-system apps
        val isLiveNotif = liveMode && sbn.isOngoing && !isSystem && (
            sbn.notification.category == "navigation" ||
            sbn.notification.category == Notification.CATEGORY_SERVICE ||
            sbn.notification.category == Notification.CATEGORY_TRANSPORT ||
            sbn.notification.category == Notification.CATEGORY_PROGRESS ||
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

        val chargingTrigger = isCharging && prefs.getBoolean("charging_mode", false)
        val notifTrigger = activeNotifKeys.isNotEmpty()

        setAod(enable = chargingTrigger || notifTrigger)
    }

    private fun setAod(enable: Boolean) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            Settings.Secure.putInt(
                contentResolver,
                AodSettings.DOZE_ALWAYS_ON,
                if (enable) 1 else 0,
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}