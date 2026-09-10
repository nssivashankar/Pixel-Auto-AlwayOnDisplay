package com.nssivashankar.pixelaod

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationAodService::class.java)
                )
            } catch (_: Exception) {}
        }
    }
}
