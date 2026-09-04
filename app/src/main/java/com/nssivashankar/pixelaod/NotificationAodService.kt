package com.nssivashankar.pixelaod

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.LocusId
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import com.nssivashankar.pixelaod.config.Settings as AodSettings

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()
    private var isCharging = false
    private var isChargingForegroundStarted = false
    private var isChargingPaused = false
    private var isBatteryFull = false
    private var isDndActive = false
    private var plugInTime = 0L
    private var lastActiveWattageTime = 0L
    private var isScreenOffAodActive = false
    private var isLiftToWakeActive = false
    private val handler = Handler(Looper.getMainLooper())

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val pickUpSensor by lazy { sensorManager.getDefaultSensor(25) } // Sensor.TYPE_PICK_UP_GESTURE

    // Cached Reflection classes to eliminate Class.forName overhead on battery events
    private val progressStyleClass by lazy {
        try { Class.forName("android.app.Notification\$ProgressStyle") } catch (_: Throwable) { null }
    }
    private val pointClass by lazy {
        try { Class.forName("android.app.Notification\$ProgressStyle\$Point") } catch (_: Throwable) { null }
    }
    private val segmentClass by lazy {
        try { Class.forName("android.app.Notification\$ProgressStyle\$Segment") } catch (_: Throwable) { null }
    }

    private val tokenScreenOff = Any()
    private val tokenLift = Any()

    private val triggerEventListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            if (getPrefs().getBoolean("lift_to_wake_aod", false)) {
                isLiftToWakeActive = true
                updateAodState()
                
                // Re-use same 10s timer logic
                handler.removeCallbacksAndMessages(tokenLift)
                handler.postAtTime({
                    isLiftToWakeActive = false
                    updateAodState()
                }, tokenLift, SystemClock.uptimeMillis() + 10000)
                
                // Re-register if screen is still off
                val isScreenOn = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive
                if (!isScreenOn) {
                    sensorManager.requestTriggerSensor(this, pickUpSensor)
                }
            }
        }
    }

    companion object {
        private const val CHARGING_NOTIF_ID = 1001
        private const val COMPLETION_NOTIF_ID = 1002
        private const val CHARGING_CHANNEL_ID = "charging_live_v11_fix"
        private const val COMPLETION_CHANNEL_ID = "battery_completion_v1"
        
        private const val ACTION_OPT_OFF = "com.nssivashankar.pixelaod.ACTION_OPT_OFF"
        private const val ACTION_OPT_80 = "com.nssivashankar.pixelaod.ACTION_OPT_80"
        private const val ACTION_OPT_ADAPTIVE = "com.nssivashankar.pixelaod.ACTION_OPT_ADAPTIVE"
        private const val ACTION_FULL_CHARGE = "com.nssivashankar.pixelaod.ACTION_FULL_CHARGE"
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Channel for Ongoing Live Updates - MUST be HIGH for lockscreen visibility
        val liveChannel = NotificationChannel(
            CHARGING_CHANNEL_ID,
            "Live Charging Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
            description = "Shows real-time charging wattage and completion time on Lockscreen/AOD"
        }
        nm.createNotificationChannel(liveChannel)

        // Channel for Completion Alerts (Sound)
        val completionChannel = NotificationChannel(
            COMPLETION_CHANNEL_ID,
            "Battery Completion Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = "Alerts when battery reaches 80% or 100%"
        }
        nm.createNotificationChannel(completionChannel)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_OPT_OFF -> {
                    AodSettings.setChargeOptimizationMode(contentResolver, 0)
                    AodSettings.setAdaptiveChargingEnabled(contentResolver, false)
                    getPrefs().edit { 
                        putString("charge_optimization", "0")
                        putBoolean("custom_limit_enabled", false)
                    }
                    updateChargingNotification(null)
                }
                ACTION_OPT_80 -> {
                    AodSettings.setChargeOptimizationMode(contentResolver, 1)
                    getPrefs().edit { 
                        putString("charge_optimization", "1")
                        putBoolean("custom_limit_enabled", false)
                    }
                    updateChargingNotification(null)
                }
                ACTION_OPT_ADAPTIVE -> {
                    AodSettings.setChargeOptimizationMode(contentResolver, 2)
                    AodSettings.setAdaptiveChargingEnabled(contentResolver, true)
                    getPrefs().edit { 
                        putString("charge_optimization", "2")
                        putBoolean("custom_limit_enabled", false)
                    }
                    updateChargingNotification(null)
                }
                ACTION_FULL_CHARGE -> {
                    AodSettings.setChargeOptimizationMode(contentResolver, 0)
                    AodSettings.setAdaptiveChargingEnabled(contentResolver, false)
                    
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(COMPLETION_NOTIF_ID)
                    
                    getPrefs().edit { 
                        putBoolean("custom_limit_enabled", false)
                        putString("charge_optimization", "0") 
                    }
                    
                    updateChargingNotification(null)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    plugInTime = System.currentTimeMillis()
                    lastActiveWattageTime = System.currentTimeMillis()
                    updateAodState()
                    updateChargingNotification(null)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    isChargingForegroundStarted = false
                    plugInTime = 0L
                    lastActiveWattageTime = 0L
                    
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (_: Exception) {}
                    nm.cancel(CHARGING_NOTIF_ID)
                    nm.cancel(COMPLETION_NOTIF_ID)
                    
                    syncActiveNotifications()
                    updateAodState()
                    updateChargingNotification(null)
                    lastAlertedPct = -1
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level != -1 && scale != -1) (level * 100 / scale) else -1
                    
                    isCharging = plugged != 0
                    
                    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // mV
                    val currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) // uA
                    val currentWattage = (abs(currentNow).toDouble() / 1_000_000.0) * (voltage.toDouble() / 1000.0)

                    if (currentWattage > 0.5) {
                        lastActiveWattageTime = System.currentTimeMillis()
                    }

                    val prefs = getPrefs()
                    val optMode = AodSettings.getChargeOptimizationMode(contentResolver)
                    val isAdaptiveLegacy = AodSettings.isAdaptiveChargingEnabled(contentResolver)
                    val customLimitEnabled = prefs.getBoolean("custom_limit_enabled", false)
                    val customTarget = prefs.getInt("custom_charging_limit", 80)

                    val isAdaptiveActive = optMode == 2 || isAdaptiveLegacy
                    isChargingPaused = isCharging && isAdaptiveActive && pct >= 80 && pct < 98 && currentWattage < 0.7

                    if (customLimitEnabled && isCharging) {
                        if (pct >= customTarget && optMode != 1) {
                            AodSettings.setChargeOptimizationMode(contentResolver, 1)
                        } else if (pct < customTarget - 2 && optMode == 1) {
                            AodSettings.setChargeOptimizationMode(contentResolver, 0)
                        }
                    }

                    isBatteryFull = status == BatteryManager.BATTERY_STATUS_FULL || 
                                   (optMode == 1 && pct >= 80) || 
                                   (customLimitEnabled && pct >= customTarget) ||
                                   pct >= 100

                    if (isCharging && plugInTime == 0L) {
                        plugInTime = System.currentTimeMillis()
                    }
                    updateAodState()
                    updateChargingNotification(intent)
                    checkBatteryCompletion(pct, status, optMode, customLimitEnabled, customTarget)
                }
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    updateDndStatus()
                    updateAodState()
                }
                Intent.ACTION_TIME_TICK -> {
                    if (getPrefs().getBoolean("scheduled_dnd", false)) {
                        updateAodState()
                    }
                    if (isCharging) {
                        updateChargingNotification(null)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val prefs = getPrefs()
                    if (prefs.getBoolean("screen_off_aod", false)) {
                        isScreenOffAodActive = true
                        updateAodState()
                        handler.removeCallbacksAndMessages(tokenScreenOff)
                        handler.postAtTime({
                            isScreenOffAodActive = false
                            updateAodState()
                        }, tokenScreenOff, SystemClock.uptimeMillis() + 10000)
                    }
                    
                    if (prefs.getBoolean("lift_to_wake_aod", false) && pickUpSensor != null) {
                        sensorManager.requestTriggerSensor(triggerEventListener, pickUpSensor)
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOffAodActive = false
                    isLiftToWakeActive = false
                    handler.removeCallbacksAndMessages(tokenScreenOff)
                    handler.removeCallbacksAndMessages(tokenLift)
                    sensorManager.cancelTriggerSensor(triggerEventListener, pickUpSensor)
                    updateAodState()
                }
            }
        }
    }

    private var lastAlertedPct = -1

    private fun checkBatteryCompletion(pct: Int, status: Int, optMode: Int, customLimitEnabled: Boolean, customTarget: Int) {
        if (!isCharging || pct == -1) return
        
        val activeTarget = if (customLimitEnabled) customTarget else 80

        if (((optMode == 1 && !customLimitEnabled && pct >= 80) || (customLimitEnabled && pct >= customTarget)) && lastAlertedPct < activeTarget) {
            lastAlertedPct = activeTarget
            sendCompletionNotification(
                "$activeTarget% Charging Complete",
                "Battery has reached $activeTarget% limit. Want to continue to 100%?",
                true
            )
        } else if ((status == BatteryManager.BATTERY_STATUS_FULL || pct >= 100) && lastAlertedPct < 100) {
            lastAlertedPct = 100
            sendCompletionNotification(
                "Battery Fully Charged",
                "Your Pixel is now at 100%.",
                false
            )
        }
    }

    private fun sendCompletionNotification(title: String, text: String, offerFullCharge: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, SettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        
        val builder = Notification.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_round_aod_24)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        if (offerFullCharge) {
            val fullChargeIntent = PendingIntent.getBroadcast(this, 4, Intent(ACTION_FULL_CHARGE).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, "Charge to 100%", fullChargeIntent).build())
        }

        nm.notify(COMPLETION_NOTIF_ID, builder.build())
    }

    private fun updateDndStatus() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val filter = nm.currentInterruptionFilter
        isDndActive = filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                      filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                      filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    private fun getPrefs(): SharedPreferences {
        return getSharedPreferences("aod_prefs", MODE_PRIVATE)
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in listOf("master_switch", "charging_mode", "charging_info_notif", "dnd_mode", "scheduled_dnd", "scheduled_dnd_start", "scheduled_dnd_end", "live_notif_mode")) {
            syncActiveNotifications()
            updateChargingNotification(null)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        updateDndStatus()
        getPrefs().registerOnSharedPreferenceChangeListener(prefListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(ACTION_OPT_OFF)
            addAction(ACTION_OPT_80)
            addAction(ACTION_OPT_ADAPTIVE)
            addAction(ACTION_FULL_CHARGE)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
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
        val blockedLiveApps = prefs.getStringSet("live_notif_blocked_apps", emptySet()) ?: emptySet()
        val activeNotifs = try { activeNotifications } catch (e: Exception) { 
            Log.e("NotificationAodService", "Error getting active notifications", e)
            null 
        } ?: return

        activeNotifKeys.clear()
        activeNotifs.forEach { sbn ->
            if (shouldTrigger(sbn, watchedApps, liveMode, blockedLiveApps)) {
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
        val blockedLiveApps = prefs.getStringSet("live_notif_blocked_apps", emptySet()) ?: emptySet()

        if (shouldTrigger(sbn, watchedApps, liveMode, blockedLiveApps)) {
            activeNotifKeys.add(sbn.key)
        } else {
            activeNotifKeys.remove(sbn.key)
        }

        updateAodState()
    }

    private fun shouldTrigger(
        sbn: StatusBarNotification,
        watchedApps: Set<String>,
        liveMode: Boolean,
        blockedLiveApps: Set<String>
    ): Boolean {
        val packageName = sbn.packageName
        
        // 0. Own charging notification: Never include in activeNotifKeys.
        if (packageName == this.packageName && sbn.id == CHARGING_NOTIF_ID) {
            return false
        }

        // 1. Explicitly watched apps always trigger
        if (packageName in watchedApps) return true

        // 2. Ignore system-level noise
        if ((packageName == "android") || (packageName == "com.android.systemui")) return false

        // 3. Live Notification Mode detection
        if (liveMode && packageName !in blockedLiveApps) {
            val notification = sbn.notification
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            
            if (isOngoing) {
                val extras = notification.extras
                val category = notification.category
                val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
                val isLiveCategory = category in listOf("navigation", "service", "progress", "location_sharing", "transport")
                val appKeywords = listOf("uber", "ride", "delivery", "food", "track", "map", "grab", "rapido", "ola", "zomato", "swiggy")
                val hasKeyword = appKeywords.any { packageName.contains(it, ignoreCase = true) }

                if (isLiveCategory || hasProgress || hasKeyword) {
                    return true
                }
            }
        }

        return false
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeNotifKeys.remove(sbn.key)
        updateAodState()
    }

    private fun updateAodState() {
        val prefs = getPrefs()
        val masterEnabled = prefs.getBoolean("master_switch", false)
        
        if (!masterEnabled) return

        val chargingMode = prefs.getBoolean("charging_mode", false)
        val isWattageIdle = isCharging && lastActiveWattageTime != 0L && (System.currentTimeMillis() - lastActiveWattageTime > 10 * 60 * 1000)

        val chargingTrigger = chargingMode && isCharging && !isBatteryFull && !isChargingPaused && !isWattageIdle
        
        val respectDnd = prefs.getBoolean("dnd_mode", false)
        val systemNotifAllowed = if (respectDnd) !isDndActive else true
        
        val isQuietHours = if (prefs.getBoolean("scheduled_dnd", false)) {
            isInQuietHours(
                prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00",
                prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00"
            )
        } else { false }

        val notifTrigger = systemNotifAllowed && !isQuietHours && activeNotifKeys.isNotEmpty()
        val shouldBeOn = chargingTrigger || notifTrigger || isScreenOffAodActive || isLiftToWakeActive

        setAod(enable = shouldBeOn)
    }

    private fun isInQuietHours(startStr: String, endStr: String): Boolean {
        try {
            val now = Calendar.getInstance()
            val currentMinutes = now[Calendar.HOUR_OF_DAY] * 60 + now[Calendar.MINUTE]
            val startParts = startStr.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endParts = endStr.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (_: Exception) { return false }
    }

    private fun setAod(enable: Boolean) {
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return
        val currentState = AodSettings.isAodEnabled(contentResolver)
        if (currentState == enable) return
        try {
            AodSettings.setAodEnabled(contentResolver, enable)
        } catch (e: SecurityException) { Log.e("NotificationAodService", "Failed to set AOD state", e) }
    }

    private fun updateChargingNotification(intent: Intent?) {
        val prefs = getPrefs()
        val enabled = prefs.getBoolean("charging_info_notif", false)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val batteryIntent = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val isPlugged = isCharging && plugged != 0
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val optMode = AodSettings.getChargeOptimizationMode(contentResolver)
        val customLimitEnabled = prefs.getBoolean("custom_limit_enabled", false)
        val customTarget = prefs.getInt("custom_charging_limit", 80)
        
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || (optMode == 1 && batteryPct >= 80) || (customLimitEnabled && batteryPct >= customTarget) || batteryPct >= 100

        if (!enabled || !isCharging || !isPlugged || isFull) {
            if (isChargingForegroundStarted) {
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}
                isChargingForegroundStarted = false
            }
            nm.cancel(CHARGING_NOTIF_ID)
            updateAodState()
            return
        }

        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0 // mV
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) // uA
        val currentWattage = (abs(currentNow).toDouble() / 1_000_000.0) * (voltage.toDouble() / 1000.0)
        val systemTimeToFull = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) bm.computeChargeTimeRemaining() else -1L
        val currentMa = Math.abs(currentNow) / 1000.0
        val msPerPct = if (currentMa > 100) (50.0 / currentMa * 3600.0 * 1000.0).toLong() else 0L

        fun estimateTimeTo(targetPct: Int): Long {
            if (batteryPct >= targetPct) return 0L
            if (systemTimeToFull > 0 && targetPct == 100) return systemTimeToFull
            if (targetPct == 80 && optMode == 1 && systemTimeToFull > 0) return systemTimeToFull
            var estimatedMs = (targetPct - batteryPct) * msPerPct
            if (targetPct > 80 && batteryPct < 80) estimatedMs += ((targetPct - maxOf(80, batteryPct)) * msPerPct * 0.5).toLong() 
            return estimatedMs
        }

        val isLimitActive = optMode == 1 || customLimitEnabled
        val targetLimit = if (customLimitEnabled) customTarget else 80
        val msTo80 = estimateTimeTo(80)
        val msTo100 = estimateTimeTo(100)
        val msToTarget = if (isLimitActive) estimateTimeTo(targetLimit) else msTo80

        val currentTime = System.currentTimeMillis()
        val clockTimeTarget = if (msToTarget > 0) formatToClockTime(currentTime + msToTarget) else ""
        val clockTime80 = if (msTo80 > 0) formatToClockTime(currentTime + msTo80) else ""
        val clockTime100 = if (msTo100 > 0) formatToClockTime(currentTime + msTo100) else ""

        val tempUnit = prefs.getString("unit_system", "metric") ?: "metric"
        val tempStr = if (tempUnit == "imperial") String.format(Locale.US, "%.1f°F", (temperature * 9/5) + 32) else String.format(
            Locale.US, "%.1f°C", temperature)
        val wattageStr = String.format(Locale.US, "%.1fW", currentWattage)

        // Dynamic charging type title (Fast AC, AC, USB, Wireless)
        val plugTypeStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> if (currentWattage >= 15.0) "Fast AC Charging" else "AC Charging"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> if (currentWattage >= 10.0) "Fast Wireless Charging" else "Wireless Charging"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock Charging"
            else -> "Charging"
        }
        val notificationTitle = "$plugTypeStr: $batteryPct%"

        // Formatting body placement:
        // Line 1: 18.5W • 32.4°C
        // Line 2: "80% at 10:45 PM" (Extreme Left)                        "Full at 11:15 PM" (Extreme Right edge)
        val line1 = "$wattageStr \u2022 $tempStr"
        val line2 = when {
            isLimitActive -> {
                if (batteryPct < targetLimit && clockTimeTarget.isNotEmpty()) {
                    "$targetLimit% at $clockTimeTarget"
                } else if (batteryPct >= targetLimit) {
                    "Limit Reached ($targetLimit%)"
                } else ""
            }
            else -> { // Off or Adaptive Charging
                if (batteryPct < 80 && clockTime80.isNotEmpty()) {
                    val leftText = "80% at $clockTime80"
                    val rightText = if (clockTime100.isNotEmpty()) "Full at $clockTime100" else ""
                    if (rightText.isNotEmpty()) leftText + "\u00A0".repeat(26) + rightText else leftText
                } else if (clockTime100.isNotEmpty()) {
                    "Full at $clockTime100"
                } else ""
            }
        }

        val notificationContentText = if (line2.isNotEmpty()) "$line1\n$line2" else line1

        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, SettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // Dynamic percentage-based color scheme for charging progress
        val progressColor = when {
            batteryPct < 20 -> Color.parseColor("#E53935") // Red (0% - 19%)
            batteryPct < 35 -> Color.parseColor("#FB8C00") // Orange (20% - 34%)
            batteryPct < 60 -> Color.parseColor("#FDD835") // Gold / Amber (35% - 59%)
            batteryPct < 80 -> Color.parseColor("#4CAF50") // Light Green (60% - 79%)
            else -> Color.parseColor("#00E676")           // Vibrant Emerald Green (80% - 100%)
        }

        val liveUpdateIcon = if (isDark) R.drawable.ic_bolt_outlined_24 else R.drawable.ic_bolt_dark_24
        val targetTimeMs = if (isLimitActive && batteryPct < targetLimit) currentTime + msToTarget else currentTime + msTo100
        val pillEtaText = if (targetTimeMs > currentTime) formatToClockTime(targetTimeMs) else ""

        val notificationBuilder = Notification.Builder(this, CHARGING_CHANNEL_ID)
            .setSmallIcon(liveUpdateIcon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setColor(progressColor)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setProgress(100, batteryPct, false)

        try {
            val styleClass = progressStyleClass

            if (styleClass != null) {
                val style = styleClass.getConstructor().newInstance()
                
                // Explicitly set total max track to 100
                try {
                    styleClass.getMethod("setProgressMax", Int::class.javaPrimitiveType).invoke(style, 100)
                } catch (_: Exception) {}

                // Set current active progress
                styleClass.getMethod("setProgress", Int::class.javaPrimitiveType).invoke(style, batteryPct)
                
                // Enable M3 Expressive styled progress (thick active track, thin inactive track)
                try {
                    styleClass.getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType).invoke(style, true)
                } catch (_: Exception) {}

                // Define active colored segment + subtle inactive segment
                val sClass = segmentClass
                if (sClass != null) {
                    try {
                        val segConstructor = sClass.getConstructor(Int::class.javaPrimitiveType)
                        val segments = mutableListOf<Any>()

                        // Active segment with dynamic battery color
                        val segActive = segConstructor.newInstance(batteryPct)
                        sClass.getMethod("setColor", Int::class.javaPrimitiveType).invoke(segActive, progressColor)
                        segments.add(segActive)

                        // Inactive remaining track segment
                        val remaining = maxOf(0, 100 - batteryPct)
                        if (remaining > 0) {
                            val segInactive = segConstructor.newInstance(remaining)
                            sClass.getMethod("setColor", Int::class.javaPrimitiveType).invoke(segInactive, Color.argb(40, 200, 200, 200))
                            segments.add(segInactive)
                        }

                        styleClass.getMethod("setProgressSegments", List::class.java).invoke(style, segments)
                    } catch (e: Exception) {
                        Log.e("NotificationAodService", "Failed to set progress segments", e)
                    }
                }

                notificationBuilder.setStyle(style as Notification.Style)
                
                try {
                    notificationBuilder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(notificationBuilder, true)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("NotificationAodService", "Failed to construct ProgressStyle", e)
        }

        val extras = Bundle()
        extras.putBoolean("android.requestPromotedOngoing", true)
        extras.putCharSequence("android.substName", "Pixel AOD")
        if (pillEtaText.isNotEmpty()) {
            val pillText = if (batteryPct < targetLimit) "${targetLimit}% $pillEtaText" else "Full $pillEtaText"
            extras.putString("android.shortCriticalText", pillText)
        }
        notificationBuilder.addExtras(extras)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) notificationBuilder.setLocusId(LocusId("charging_activity"))

        val isAdaptiveLegacy = AodSettings.isAdaptiveChargingEnabled(contentResolver)
        val offIntent = PendingIntent.getBroadcast(this, 1, Intent(ACTION_OPT_OFF).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val opt80Intent = PendingIntent.getBroadcast(this, 2, Intent(ACTION_OPT_80).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val adaptiveIntent = PendingIntent.getBroadcast(this, 3, Intent(ACTION_OPT_ADAPTIVE).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val offLabel = if (optMode == 0 && !isAdaptiveLegacy && !customLimitEnabled) "● Off" else "Off"
        val opt80Label = if (optMode == 1 && !customLimitEnabled) "● 80%" else "80%"
        val adaptiveLabel = if ((optMode == 2 || (optMode == 0 && isAdaptiveLegacy)) && !customLimitEnabled) "● Adaptive" else "Adaptive"

        if (customLimitEnabled) {
            val fullChargeIntent = PendingIntent.getBroadcast(this, 4, Intent(ACTION_FULL_CHARGE).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            notificationBuilder.addAction(Notification.Action.Builder(null, "Full Charge", fullChargeIntent).build())
        } else {
            notificationBuilder.addAction(Notification.Action.Builder(null, offLabel, offIntent).build())
            notificationBuilder.addAction(Notification.Action.Builder(null, opt80Label, opt80Intent).build())
            notificationBuilder.addAction(Notification.Action.Builder(null, adaptiveLabel, adaptiveIntent).build())
        }

        val chargingNotif = notificationBuilder.build()
        if (!isChargingForegroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(CHARGING_NOTIF_ID, chargingNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(CHARGING_NOTIF_ID, chargingNotif)
            }
            isChargingForegroundStarted = true
        } else {
            nm.notify(CHARGING_NOTIF_ID, chargingNotif)
        }
        
        updateAodState()
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun formatToClockTime(targetMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = targetMillis
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        return DateFormat.format(pattern, calendar).toString()
    }
}
