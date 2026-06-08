package org.alberto97.aodtoggle

import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()
    private var chargingReceiver: ChargingReceiver? = null

    private fun getPrefs() = getSharedPreferences("aod_prefs", MODE_PRIVATE)

    override fun onCreate() {
        super.onCreate()
        // Register charging receiver at runtime
        chargingReceiver = ChargingReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(chargingReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        chargingReceiver?.let { unregisterReceiver(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getPrefs()
        val watchedApps = prefs.getStringSet("watched_apps", emptySet()) ?: emptySet()
        val liveMode = prefs.getBoolean("live_notif_mode", false)

        val shouldTrigger = (sbn.packageName in watchedApps) ||
                (liveMode && sbn.isOngoing)

        if (shouldTrigger) {
            activeNotifKeys.add(sbn.key)
            setAod(true)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeNotifKeys.remove(sbn.key)
        if (activeNotifKeys.isEmpty()) {
            setAod(false)
        }
    }

    private fun setAod(enable: Boolean) {
        Settings.Secure.putInt(
            contentResolver,
            "doze_always_on",
            if (enable) 1 else 0
        )
    }
}