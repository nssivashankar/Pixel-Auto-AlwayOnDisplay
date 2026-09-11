package com.nssivashankar.pixelaod

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action.contains("POWER_CONNECTED", ignoreCase = true) || action.contains("POWER_DISCONNECTED", ignoreCase = true)) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationAodService::class.java)
                )
            } catch (_: Exception) {}
        }
    }
}
