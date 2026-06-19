package com.nssivashankar.pixelaod

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.LocusId
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.Calendar
import com.nssivashankar.pixelaod.config.Settings as AodSettings

class NotificationAodService : NotificationListenerService() {

    private val activeNotifKeys = mutableSetOf<String>()
    private var isCharging = false
    private var isBatteryFull = false
    private var isDndActive = false
    private var plugInTime = 0L

    companion object {
        private const val CHARGING_NOTIF_ID = 1001
        private const val CHARGING_CHANNEL_ID = "charging_live_v10"
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHARGING_CHANNEL_ID,
            "Live Charging Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = "Shows real-time charging wattage and completion time"
        }
        nm.createNotificationChannel(channel)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    plugInTime = System.currentTimeMillis()
                    updateAodState()
                    updateChargingNotification(null)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    plugInTime = 0L
                    syncActiveNotifications()
                    updateAodState()
                    updateChargingNotification(null)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level != -1 && scale != -1) (level * 100 / scale) else -1
                    
                    isCharging = plugged != 0
                    isBatteryFull = status == BatteryManager.BATTERY_STATUS_FULL || pct >= 100

                    if (isCharging && plugInTime == 0L) {
                        plugInTime = System.currentTimeMillis()
                    }
                    updateAodState()
                    updateChargingNotification(intent)
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
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "master_switch",
            "watched_apps",
            "live_notif_mode",
            "live_notif_blocked_apps",
            "charging_mode",
            "dnd_mode",
            "scheduled_dnd",
            "scheduled_dnd_start",
            "scheduled_dnd_end",
            "charging_info_notif",
            -> {
                syncActiveNotifications()
                updateChargingNotification(null)
            }
        }
    }

    private fun getPrefs() = getSharedPreferences("aod_prefs", MODE_PRIVATE)

        override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        getPrefs().registerOnSharedPreferenceChangeListener(prefListener)
        
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else -1

        isCharging = plugged != 0
        isBatteryFull = status == BatteryManager.BATTERY_STATUS_FULL || pct >= 100

        if (isCharging) {
            plugInTime = System.currentTimeMillis()
        }

        updateDndStatus()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        registerReceiver(receiver, filter)
    }

    private fun updateDndStatus() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        isDndActive = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
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
        
        // 0. Own charging notification should always be treated as a live update if enabled
        if (packageName == this.packageName && sbn.id == CHARGING_NOTIF_ID) {
            return getPrefs().getBoolean("charging_info_notif", false)
        }

        // 1. Explicitly watched apps always trigger

        // 2. Ignore system-level noise
        if ((packageName == "android") || (packageName == "com.android.systemui")) return false

        // 3. Live Notification Mode detection
        if (liveMode && packageName !in blockedLiveApps) {
            val notification = sbn.notification
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            
            if (isOngoing) {
                val extras = notification.extras
                val category = notification.category
                
                // Exclude media playback notifications (Spotify, YouTube, etc.)
                val isMedia = category == Notification.CATEGORY_TRANSPORT || 
                             extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                             extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                
                if (!isMedia) {
                    // Detect high-value ongoing updates (Navigation, Delivery, Rides, Progress)
                    val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
                    
                    // Category-based detection (broadened)
                    val isLiveCategory = category in listOf(
                        "navigation",      // Notification.CATEGORY_NAVIGATION (API 28)
                        "service",         // Notification.CATEGORY_SERVICE
                        "progress",        // Notification.CATEGORY_PROGRESS
                        "location_sharing", // Notification.CATEGORY_LOCATION_SHARING (API 31)
                        "transport"
                    )

                    // Keyword-based fallback for apps that don't set categories correctly (Zomato, Uber, etc.)
                    val appKeywords = listOf("uber", "ride", "delivery", "food", "track", "map", "grab", "rapido", "ola", "zomato", "swiggy")
                    val hasKeyword = appKeywords.any { packageName.contains(it, ignoreCase = true) }

                    if (isLiveCategory || hasProgress || hasKeyword) {
                        Log.d("AodService", "Live Update detected: $packageName")
                        return true
                    }
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
        
        if (!masterEnabled) {
            setAod(enable = false)
            return
        }

        val chargingMode = prefs.getBoolean("charging_mode", false)
        val chargingInfoMode = prefs.getBoolean("charging_info_notif", false)
        
        // Charging trigger only stays active if NOT full
        val chargingTrigger = (chargingMode || chargingInfoMode) && isCharging && !isBatteryFull
        
        // System DND check
        val respectDnd = prefs.getBoolean("dnd_mode", false)
        val systemNotifAllowed = if (respectDnd) !isDndActive else true
        
        // Scheduled DND check
        val isQuietHours = if (prefs.getBoolean("scheduled_dnd", false)) {
            isInQuietHours(
                prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00",
                prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00"
            )
        } else {
            false
        }

        val notifTrigger = systemNotifAllowed && !isQuietHours && activeNotifKeys.isNotEmpty()
        val shouldBeOn = chargingTrigger || notifTrigger

        Log.d("AodService", "State Update: Charging=$isCharging, Notifs=${activeNotifKeys.size}, ShouldBeOn=$shouldBeOn")

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
                // Overnight period (e.g., 22:00 to 07:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (_: Exception) {
            return false
        }
    }

    private fun setAod(enable: Boolean) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val currentState = AodSettings.isAodEnabled(contentResolver)
        if (currentState == enable) return // Avoid redundant writes

        try {
            AodSettings.setAodEnabled(contentResolver, enable)
            
            // Fix for Android 15/16/17: Force display state refresh when turning OFF.
            // When unplugged or notifications cleared, we send a "Ghost" notification
            // to kick the system out of the stale AOD state.
            if (!enable) {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "aod_refresh_channel"
                
                val channel = NotificationChannel(channelId, "AOD Sync", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)

                val ghostNotif = Notification.Builder(this, channelId)
                    .setSmallIcon(android.R.color.transparent)
                    .setContentTitle("")
                    .setGroup("aod_sync_group")
                    .setGroupAlertBehavior(Notification.GROUP_ALERT_ALL)
                    .build()

                // Post and immediately cancel to trigger a "wake" and settings re-read
                notificationManager.notify(999, ghostNotif)
                notificationManager.cancel(999)
                
                // Fallback: system-level pulse intent
                sendBroadcast(Intent("com.android.systemui.doze.pulse"))
            }
        } catch (e: SecurityException) {
            Log.e("NotificationAodService", "Failed to set AOD state", e)
        }
    }

    private fun updateChargingNotification(intent: Intent?) {
        val prefs = getPrefs()
        val enabled = prefs.getBoolean("charging_info_notif", false)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val batteryIntent = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val isPlugged = plugged != 0

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else -1
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || batteryPct >= 100

        if (!enabled || !isPlugged || isFull) {
            activeNotifKeys.remove(this.packageName + "|" + CHARGING_NOTIF_ID)
            stopForeground(STOP_FOREGROUND_REMOVE)
            nm.cancel(CHARGING_NOTIF_ID)
            updateAodState()
            return
        }

        val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // mV
        
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) // uA
        val currentWattage = (Math.abs(currentNow).toDouble() / 1_000_000.0) * (voltage.toDouble() / 1000.0)
        
        val wattageStr = if (batteryPct >= 95 && currentWattage < 3.0) {
            "Trickle Charging"
        } else {
            String.format(java.util.Locale.US, "%.1fW", currentWattage)
        }

        var timeToFull = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            bm.computeChargeTimeRemaining()
        } else {
            -1L
        }

        // Manual fallback: If system fails to calculate (heat/low power), we do the math
        if (timeToFull <= 0 && currentNow > 0 && batteryPct != -1 && batteryPct < 100) {
            val currentMa = Math.abs(currentNow) / 1000.0
            if (currentMa > 50) { // Need at least some current to estimate
                val pctRemaining = (100 - batteryPct)
                // Assuming average Pixel battery (approx 48mAh per 1%)
                val mAhRemaining = pctRemaining * 48.0
                timeToFull = ((mAhRemaining / currentMa) * 3600.0 * 1000.0).toLong()
            }
        }

        val targetTimeMillis = if (timeToFull > 0) {
            val now = System.currentTimeMillis()
            if (batteryPct != -1 && batteryPct < 80) {
                val timeTo80 = (timeToFull * (80 - batteryPct) / (100 - batteryPct).coerceAtLeast(1))
                now + timeTo80
            } else {
                now + timeToFull
            }
        } else {
            0L
        }

        val timeStr = if (targetTimeMillis > 0) {
            if (batteryPct != -1 && batteryPct < 80) {
                getString(R.string.charging_info_time_remaining_80, formatToClockTime(targetTimeMillis))
            } else {
                getString(R.string.charging_info_time_remaining_100, formatToClockTime(targetTimeMillis))
            }
        } else {
            "Calculating..."
        }

        val wattageStrFinal = wattageStr
        val tempStr = String.format(java.util.Locale.US, "%.1f°C", temperature)

        val contentIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, SettingsActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "$timeStr \u2022 $wattageStrFinal \u2022 $tempStr"
        
        val notificationBuilder = Notification.Builder(this, CHARGING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24)
            .setLargeIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_bolt_24))
            .setContentTitle(getString(R.string.charging_info_notification_title, batteryPct))
            .setContentText(contentText)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false) 
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setColor(getColor(android.R.color.holo_blue_dark))
            .setShortcutId("charging_status")

        val extras = android.os.Bundle()
        
        // Android 15 "Live Update" promotion request
        extras.putBoolean("android.requestPromotedOngoing", true)

        if (targetTimeMillis > 0) {
            // Promote to Live Notification only when time is calculated
            notificationBuilder.setCategory("progress")
            notificationBuilder.setWhen(targetTimeMillis)
            
            // Short text for the status bar pill/chip (must be short to fit)
            val timeOnly = formatToClockTime(targetTimeMillis)
            val pillText = if (batteryPct != -1 && batteryPct < 80) "80% $timeOnly" else "Full $timeOnly"
            extras.putString("android.shortCriticalText", pillText)
        } else {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE)
        }

        // Essential for Pixel status bar chip and At a Glance integration
        extras.putBoolean("android.substName", true)
        notificationBuilder.addExtras(extras)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            notificationBuilder.setLocusId(LocusId("charging_activity"))
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification = notificationBuilder.build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(CHARGING_NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(CHARGING_NOTIF_ID, notification)
        }
        
        // Force sync local tracking for own notification
        if (activeNotifKeys.add(this.packageName + "|" + CHARGING_NOTIF_ID)) {
            updateAodState()
        }
    }

    private fun formatToClockTime(targetMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = targetMillis
        val is24Hour = android.text.format.DateFormat.is24HourFormat(this)
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return android.text.format.DateFormat.format(pattern, calendar).toString()
    }
}