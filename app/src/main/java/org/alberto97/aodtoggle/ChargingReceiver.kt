package org.alberto97.aodtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("charging_mode", false)) return

        val enable = intent.action == Intent.ACTION_POWER_CONNECTED
        Settings.Secure.putInt(
            context.contentResolver,
            "doze_always_on",
            if (enable) 1 else 0
        )
    }
}