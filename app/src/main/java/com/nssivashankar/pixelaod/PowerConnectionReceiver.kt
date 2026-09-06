package com.nssivashankar.pixelaod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
            val serviceIntent = Intent(context, NotificationAodService::class.java).apply {
                this.action = action
            }
            try {
                context.startForegroundService(serviceIntent)
            } catch (_: Exception) {
                // If background service start is restricted by system, NotificationListenerService lifecycle handles binding
            }
        }
    }
}
