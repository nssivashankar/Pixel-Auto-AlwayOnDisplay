package org.alberto97.aodtoggle

import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()

    private fun getPrefs() = getSharedPreferences("aod_prefs", MODE_PRIVATE)

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