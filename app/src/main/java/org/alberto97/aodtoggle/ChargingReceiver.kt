package org.alberto97.aodtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.Settings

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("charging_mode", false)) return

        val isCharging = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> true
            Intent.ACTION_POWER_DISCONNECTED -> false
            Intent.ACTION_BATTERY_CHANGED -> {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            }
            else -> return
        }

        Settings.Secure.putInt(
            context.contentResolver,
            "doze_always_on",
            if (isCharging) 1 else 0
        )
    }
}