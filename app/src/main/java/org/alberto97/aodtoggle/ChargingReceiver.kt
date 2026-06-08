package org.alberto97.aodtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Show toast regardless of prefs to confirm receiver is firing
        Toast.makeText(
            context,
            "Battery event received! action=${intent.action}",
            Toast.LENGTH_LONG
        ).show()

        val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("charging_mode", false)) {
            Toast.makeText(context, "Charging mode is OFF in settings", Toast.LENGTH_LONG).show()
            return
        }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val isCharging = batteryManager.isCharging
        Toast.makeText(context, "isCharging=$isCharging", Toast.LENGTH_LONG).show()

        Settings.Secure.putInt(
            context.contentResolver,
            "doze_always_on",
            if (isCharging) 1 else 0
        )
    }
}