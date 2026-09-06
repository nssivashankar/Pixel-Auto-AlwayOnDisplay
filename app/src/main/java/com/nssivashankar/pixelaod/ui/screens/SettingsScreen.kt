package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import com.nssivashankar.pixelaod.ui.theme.AppHaptics
import com.nssivashankar.pixelaod.ui.theme.iosTouchFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// --- High-Performance Settings State Holder ---
@Stable
class SettingsState(private val context: Context, private val scope: CoroutineScope) {
    val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
    private val resolver = context.contentResolver

    var masterSwitch by mutableStateOf(prefs.getBoolean("master_switch", false))
    var chargingMode by mutableStateOf(prefs.getBoolean("charging_mode", false))
    var chargingInfoNotif by mutableStateOf(prefs.getBoolean("charging_info_notif", false))
    var liveNotifMode by mutableStateOf(prefs.getBoolean("live_notif_mode", false))
    var dndMode by mutableStateOf(prefs.getBoolean("dnd_mode", false))
    var scheduledDnd by mutableStateOf(prefs.getBoolean("scheduled_dnd", false))
    var scheduledDndStart by mutableStateOf(prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00")
    var scheduledDndEnd by mutableStateOf(prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00")
    var watchedApps by mutableStateOf(prefs.getStringSet("watched_apps", emptySet()) ?: emptySet())
    var liveNotifBlocklist by mutableStateOf(prefs.getStringSet("live_notif_blocklist", emptySet()) ?: emptySet())
    var customLimitEnabled by mutableStateOf(prefs.getBoolean("custom_limit_enabled", false))
    var customLimit by mutableIntStateOf(prefs.getInt("custom_charging_limit", 80))
    var screenOffAod by mutableStateOf(prefs.getBoolean("screen_off_aod", false))
    var liftToWakeAod by mutableStateOf(prefs.getBoolean("lift_to_wake_aod", false))
    var unitSystem by mutableStateOf(prefs.getString("unit_system", "metric") ?: "metric")
    var currentOptimizationMode by mutableIntStateOf(AodSettings.getChargeOptimizationMode(resolver))

    var hasWriteSecurePermission by mutableStateOf(
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    )
    var hasNotificationAccessPermission by mutableStateOf(
        AndroidSettings.Secure.getString(resolver, "enabled_notification_listeners")?.contains(context.packageName) == true
    )

    fun refreshPermissions(context: Context) {
        hasWriteSecurePermission = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        val enabledListeners = AndroidSettings.Secure.getString(resolver, "enabled_notification_listeners")
        hasNotificationAccessPermission = enabledListeners?.contains(context.packageName) == true
    }

    init {
        // Migration: Move v1.1.4 'temp_unit' to v1.1.5 'unit_system'
        if (prefs.contains("temp_unit")) {
            val oldVal = prefs.getString("temp_unit", "C")
            val newVal = if (oldVal == "F") "imperial" else "metric"
            prefs.edit().putString("unit_system", newVal).remove("temp_unit").apply()
            unitSystem = newVal
        }
    }

    private fun syncSystemSettings() {
        val sysMode = AodSettings.getChargeOptimizationMode(resolver)
        val customTarget = prefs.getInt("custom_charging_limit", 80)
        
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = try { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1 } catch (_: Exception) { -1 }
        
        val isPlugged = try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            plugged != 0
        } catch (_: Exception) { false }

        if (customLimitEnabled) {
            val isExpectingLimit = isPlugged && pct >= customTarget && pct != -1
            val expectedSysMode = if (isExpectingLimit) 1 else 0
            if (sysMode != expectedSysMode) {
                customLimitEnabled = false
                prefs.edit().putBoolean("custom_limit_enabled", false).apply()
            }
        }

        currentOptimizationMode = sysMode
    }

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scope.launch(Dispatchers.IO) {
                syncSystemSettings()
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            "master_switch" -> masterSwitch = p.getBoolean(key, false)
            "charging_mode" -> chargingMode = p.getBoolean(key, false)
            "charging_info_notif" -> chargingInfoNotif = p.getBoolean(key, false)
            "live_notif_mode" -> liveNotifMode = p.getBoolean(key, false)
            "dnd_mode" -> dndMode = p.getBoolean(key, false)
            "scheduled_dnd" -> scheduledDnd = p.getBoolean(key, false)
            "scheduled_dnd_start" -> scheduledDndStart = p.getString(key, "22:00") ?: "22:00"
            "scheduled_dnd_end" -> scheduledDndEnd = p.getString(key, "07:00") ?: "07:00"
            "watched_apps" -> watchedApps = p.getStringSet(key, emptySet()) ?: emptySet()
            "live_notif_blocklist" -> liveNotifBlocklist = p.getStringSet(key, emptySet()) ?: emptySet()
            "custom_limit_enabled" -> customLimitEnabled = p.getBoolean(key, false)
            "custom_charging_limit" -> customLimit = p.getInt(key, 80)
            "screen_off_aod" -> screenOffAod = p.getBoolean(key, false)
            "lift_to_wake_aod" -> liftToWakeAod = p.getBoolean(key, false)
            "unit_system" -> unitSystem = p.getString(key, "metric") ?: "metric"
        }
    }

    fun startObserving() {
        scope.launch(Dispatchers.IO) {
            syncSystemSettings()
        }
        AodSettings.OBSERVABLE_SECURE_SETTINGS.forEach { setting ->
            resolver.registerContentObserver(
                AndroidSettings.Secure.getUriFor(setting),
                false,
                settingsObserver
            )
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    fun stopObserving() {
        resolver.unregisterContentObserver(settingsObserver)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    fun updateMasterSwitch(enabled: Boolean) {
        masterSwitch = enabled
        prefs.edit().putBoolean("master_switch", enabled).apply()
    }

    fun updateChargingMode(enabled: Boolean) {
        chargingMode = enabled
        prefs.edit().putBoolean("charging_mode", enabled).apply()
    }

    fun updateChargingInfoNotif(enabled: Boolean) {
        chargingInfoNotif = enabled
        prefs.edit().putBoolean("charging_info_notif", enabled).apply()
    }

    fun updateLiveNotifMode(enabled: Boolean) {
        liveNotifMode = enabled
        prefs.edit().putBoolean("live_notif_mode", enabled).apply()
    }

    fun updateDndMode(enabled: Boolean) {
        dndMode = enabled
        prefs.edit().putBoolean("dnd_mode", enabled).apply()
    }

    fun updateScheduledDnd(enabled: Boolean) {
        scheduledDnd = enabled
        prefs.edit().putBoolean("scheduled_dnd", enabled).apply()
    }

    fun updateScheduledDndStart(time: String) {
        scheduledDndStart = time
        prefs.edit().putString("scheduled_dnd_start", time).apply()
    }

    fun updateScheduledDndEnd(time: String) {
        scheduledDndEnd = time
        prefs.edit().putString("scheduled_dnd_end", time).apply()
    }

    fun updateWatchedApps(apps: Set<String>) {
        watchedApps = apps
        prefs.edit().putStringSet("watched_apps", apps).apply()
    }

    fun updateLiveNotifBlocklist(apps: Set<String>) {
        liveNotifBlocklist = apps
        prefs.edit().putStringSet("live_notif_blocklist", apps).apply()
    }

    fun updateScreenOffAod(enabled: Boolean) {
        screenOffAod = enabled
        prefs.edit().putBoolean("screen_off_aod", enabled).apply()
    }

    fun updateLiftToWakeAod(enabled: Boolean) {
        liftToWakeAod = enabled
        prefs.edit().putBoolean("lift_to_wake_aod", enabled).apply()
    }

    fun updateCustomLimit(limit: Int) {
        customLimit = limit
        prefs.edit().putInt("custom_charging_limit", limit).apply()
    }

    fun updateUnitSystem(system: String) {
        unitSystem = system
        prefs.edit().putString("unit_system", system).apply()
    }

    fun setOptimization(mode: Int, custom: Boolean) {
        scope.launch(Dispatchers.IO) {
            customLimitEnabled = custom
            prefs.edit().putBoolean("custom_limit_enabled", custom).apply()
            
            if (!custom) {
                AodSettings.setChargeOptimizationMode(resolver, mode)
                when (mode) {
                    0 -> AodSettings.setAdaptiveChargingEnabled(resolver, false)
                    1 -> AodSettings.setAdaptiveChargingEnabled(resolver, false)
                    2 -> AodSettings.setAdaptiveChargingEnabled(resolver, true)
                }
                withContext(Dispatchers.Main) {
                    currentOptimizationMode = mode
                }
            } else {
                // When Custom Limit is active, we disable system-level Adaptive Charging
                // to prevent conflicting '0.0W' holds and maintain consistent charging.
                AodSettings.setChargeOptimizationMode(resolver, 0)
                AodSettings.setAdaptiveChargingEnabled(resolver, false)
                withContext(Dispatchers.Main) {
                    currentOptimizationMode = 0
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    masterSwitchEnabled: Boolean,
    onPermissionRequest: () -> Unit,
    currentTab: Int,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMasterSwitchChange: (Boolean) -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val state = remember { SettingsState(context, scope) }

    DisposableEffect(state, context) {
        state.startObserving()
        state.refreshPermissions(context)
        onDispose { state.stopObserving() }
    }

    // Sync external master switch state to state holder
    LaunchedEffect(masterSwitchEnabled) {
        state.masterSwitch = masterSwitchEnabled
    }

    // Sync state changes back to Activity
    LaunchedEffect(state.masterSwitch) {
        onMasterSwitchChange(state.masterSwitch)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut(tween(220)))
                }
            },
            label = "tabTransition"
        ) { page ->
            if (page == 0) {
                MainSettingsList(
                    state = state, 
                    contentPadding = contentPadding, 
                    onPermissionRequest = onPermissionRequest
                )
            } else {
                AboutScreen(
                    contentPadding = contentPadding,
                    onPermissionRequest = onPermissionRequest
                )
            }
        }
    }
}

@Composable
fun NavigationPill(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Home Pill Button
        val isHome = currentTab == 0
        val homeBg by animateColorAsState(
            targetValue = if (isHome) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            animationSpec = tween(durationMillis = 200),
            label = "homeBg"
        )
        val homeTint by animateColorAsState(
            targetValue = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 200),
            label = "homeTint"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(homeBg)
                .clickable {
                    if (!isHome) {
                        AppHaptics.performTabSelect(haptic)
                        onTabSelected(0)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .animateContentSize(tween(durationMillis = 200))
                    .padding(horizontal = 10.dp)
            ) {
                if (isHome) {
                    Icon(
                        imageVector = Icons.Default.Home, 
                        contentDescription = "Home",
                        tint = homeTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                    color = homeTint
                )
            }
        }

        // About Pill Button
        val isAbout = currentTab == 1
        val aboutBg by animateColorAsState(
            targetValue = if (isAbout) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            animationSpec = tween(durationMillis = 200),
            label = "aboutBg"
        )
        val aboutTint by animateColorAsState(
            targetValue = if (isAbout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 200),
            label = "aboutTint"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(aboutBg)
                .clickable {
                    if (!isAbout) {
                        AppHaptics.performTabSelect(haptic)
                        onTabSelected(1)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .animateContentSize(tween(durationMillis = 200))
                    .padding(horizontal = 10.dp)
            ) {
                if (isAbout) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "About",
                        tint = aboutTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isAbout) FontWeight.Bold else FontWeight.Medium,
                    color = aboutTint
                )
            }
        }
    }
}

@Composable
fun MainSettingsList(
    state: SettingsState,
    contentPadding: PaddingValues,
    onPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    
    var showAppListDialog by remember { mutableStateOf(false) }
    var showBlockListDialog by remember { mutableStateOf(false) }
    var showChargingModeDialog by remember { mutableStateOf(false) }
    var showTempUnitDialog by remember { mutableStateOf(false) }

    val onChargingModeChange = remember(state) { { enabled: Boolean -> state.updateChargingMode(enabled) } }
    val onChargingInfoChange = remember(state) { { enabled: Boolean -> state.updateChargingInfoNotif(enabled) } }
    val onLiveNotifChange = remember(state) { { enabled: Boolean -> state.updateLiveNotifMode(enabled) } }
    val onDndModeChange = remember(state) { { enabled: Boolean -> state.updateDndMode(enabled) } }
    val onScheduledDndChange = remember(state) { { enabled: Boolean -> state.updateScheduledDnd(enabled) } }
    val onScreenOffAodChange = remember(state) { { enabled: Boolean -> state.updateScreenOffAod(enabled) } }
    val onLiftToWakeAodChange = remember(state) { { enabled: Boolean -> state.updateLiftToWakeAod(enabled) } }

    val onShowChargingModeDialog = remember { { showChargingModeDialog = true } }
    val onShowTempUnitDialog = remember { { showTempUnitDialog = true } }
    val onShowAppListDialog = remember { { showAppListDialog = true } }
    val onShowBlockListDialog = remember { { showBlockListDialog = true } }

    if (showTempUnitDialog) {
        AlertDialog(
            onDismissRequest = { showTempUnitDialog = false },
            title = { Text(stringResource(R.string.temp_unit_title)) },
            text = {
                Column {
                    listOf(
                        stringResource(R.string.temp_unit_celsius) to "metric",
                        stringResource(R.string.temp_unit_fahrenheit) to "imperial"
                    ).forEach { (label, value) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.updateUnitSystem(value)
                                showTempUnitDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.unitSystem == value,
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTempUnitDialog = false }) { Text("Done") } }
        )
    }

    if (showChargingModeDialog) {
        AlertDialog(
            onDismissRequest = { showChargingModeDialog = false },
            title = { Text("Charging Optimization") },
            text = {
                Column {
                    listOf("Off" to 0, "Limit to 80%" to 1, "Adaptive Charging" to 2, "Custom Limit" to 3).forEach { (label, mode) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.setOptimization(mode, mode == 3)
                                if (mode != 3) showChargingModeDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = if (mode == 3) state.customLimitEnabled else (!state.customLimitEnabled && state.currentOptimizationMode == mode),
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(label)
                        }
                    }

                    if (state.customLimitEnabled) {
                        Spacer(Modifier.height(16.dp))
                        Text(text = "Limit: ${state.customLimit}%", fontWeight = FontWeight.Bold)
                        Slider(
                            value = state.customLimit.toFloat(),
                            onValueChange = { 
                                val rounded = (it.toInt() / 5) * 5
                                if (rounded != state.customLimit) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    state.updateCustomLimit(rounded)
                                }
                            },
                            valueRange = 80f..100f,
                            steps = 3
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showChargingModeDialog = false }) { Text("Done") } }
        )
    }

    if (showAppListDialog) {
        AppListDialog(
            title = "Per-App Notifications",
            selectedPackages = state.watchedApps,
            onDismiss = { showAppListDialog = false },
            onConfirm = { packages ->
                state.updateWatchedApps(packages)
                showAppListDialog = false
            }
        )
    }

    if (showBlockListDialog) {
        AppListDialog(
            title = "Manage Block List",
            selectedPackages = state.liveNotifBlocklist,
            onDismiss = { showBlockListDialog = false },
            onConfirm = { packages ->
                state.updateLiveNotifBlocklist(packages)
                showBlockListDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = lazyListState
    ) {
        if (!state.hasWriteSecurePermission) {
            item(key = "permission_warning_card", contentType = "warning_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onPermissionRequest() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AOD Automation Permission Required",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Secure settings permission is required for AOD automation features. Tap to grant via Shizuku or ADB. Live Charging Details works standalone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // --- Category 1: CHARGING ---
        item(key = "cat_charging", contentType = "header") { 
            PreferenceCategory(title = "CHARGING", isFirst = state.hasWriteSecurePermission) 
        }
        
        item(key = "pref_charging", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Charging Mode",
                summary = "Turn on AOD automatically when charger is connected",
                icon = Icons.Default.BatteryChargingFull,
                checked = state.chargingMode,
                enabled = state.masterSwitch,
                onCheckedChange = onChargingModeChange
            )
        }

        item(key = "pref_info", contentType = "preference_switch") {
            PreferenceSwitch(
                title = stringResource(R.string.charging_info_title),
                summary = stringResource(R.string.charging_info_summary) + " (Standalone Feature)",
                icon = Icons.Default.Info,
                checked = state.chargingInfoNotif,
                enabled = true,
                onCheckedChange = onChargingInfoChange,
                showSecondaryAction = true,
                onSecondaryActionClick = onShowTempUnitDialog
            )
        }

        item(key = "pref_opt", contentType = "preference_item") {
            val modeSummary = when {
                state.customLimitEnabled -> "Custom Limit: ${state.customLimit}%"
                state.currentOptimizationMode == 1 -> "Limit to 80%"
                state.currentOptimizationMode == 2 -> "Adaptive Charging"
                else -> "Off"
            }
            PreferenceItem(
                title = "Charging Optimization",
                summary = modeSummary,
                icon = Icons.Default.BatterySaver,
                enabled = state.masterSwitch,
                onClick = onShowChargingModeDialog
            )
        }

        // --- Category 2: NOTIFICATION ---
        item(key = "cat_notif", contentType = "header") { 
            PreferenceCategory(title = "NOTIFICATION") 
        }

        item(key = "pref_live", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Live Notification Mode",
                summary = "AOD for Maps, Uber etc.",
                icon = Icons.Default.Map,
                checked = state.liveNotifMode,
                enabled = state.masterSwitch,
                onCheckedChange = onLiveNotifChange,
                showSecondaryAction = true,
                onSecondaryActionClick = onShowBlockListDialog
            )
        }

        item(key = "pref_apps", contentType = "preference_item") {
            PreferenceItem(
                title = "Per-App Notifications",
                summary = "Always trigger AOD for these apps",
                icon = Icons.Default.Notifications,
                enabled = state.masterSwitch,
                onClick = onShowAppListDialog
            )
        }

        // --- Category 3: LOCKSCREEN ---
        item(key = "cat_display", contentType = "header") { 
            PreferenceCategory(title = "LOCKSCREEN") 
        }

        item(key = "pref_screen_off", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Lock Screen AOD",
                summary = "Show AOD for 10 seconds after locking",
                icon = Icons.Default.LockClock,
                checked = state.screenOffAod,
                enabled = state.masterSwitch,
                onCheckedChange = onScreenOffAodChange
            )
        }

        item(key = "pref_lift_to_wake", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Lift to Wake AOD",
                summary = "Show AOD for 10 seconds when you pick up your phone",
                icon = Icons.Default.VerticalAlignTop,
                checked = state.liftToWakeAod,
                enabled = state.masterSwitch,
                onCheckedChange = onLiftToWakeAodChange
            )
        }

        // --- Category 4: Quiet Hours ---
        item(key = "cat_restrict", contentType = "header") { 
            PreferenceCategory(title = "Quiet Hours") 
        }

        item(key = "pref_dnd", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Respect System DND",
                summary = stringResource(R.string.dnd_mode_summary),
                icon = Icons.Default.DoNotDisturbOn,
                checked = state.dndMode,
                enabled = state.masterSwitch,
                onCheckedChange = onDndModeChange
            )
        }
        
        item(key = "pref_scheduled", contentType = "preference_switch") {
            PreferenceSwitch(
                title = "Scheduled Sleep",
                summary = "Disable AOD during specific hours",
                icon = Icons.Default.Schedule,
                checked = state.scheduledDnd,
                enabled = state.masterSwitch,
                onCheckedChange = onScheduledDndChange
            )
        }

        if (state.scheduledDnd) {
            item(key = "pref_start", contentType = "preference_item") {
                val currentStart = state.scheduledDndStart
                val parts = currentStart.split(":")
                val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 22
                val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                PreferenceItem(
                    title = "Start Time",
                    summary = currentStart,
                    icon = Icons.Default.VerticalAlignTop,
                    enabled = state.masterSwitch,
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val time = String.format(Locale.US, "%02d:%02d", h, m)
                            state.updateScheduledDndStart(time)
                        }, startHour, startMin, true).show()
                    }
                )
            }
            item(key = "pref_end", contentType = "preference_item") {
                val currentEnd = state.scheduledDndEnd
                val parts = currentEnd.split(":")
                val endHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val endMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                PreferenceItem(
                    title = "End Time",
                    summary = currentEnd,
                    icon = Icons.Default.VerticalAlignBottom,
                    enabled = state.masterSwitch,
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val time = String.format(Locale.US, "%02d:%02d", h, m)
                            state.updateScheduledDndEnd(time)
                        }, endHour, endMin, true).show()
                    }
                )
            }
        }

        item(key = "footer", contentType = "footer") {
            Spacer(Modifier.height(16.dp))
            MadeWithLoveFooter(haptic)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MadeWithLoveFooter(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    var isBeating by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isBeating) 1.4f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heartScale"
    )

    LaunchedEffect(isBeating) {
        if (isBeating) {
            kotlinx.coroutines.delay(300)
            isBeating = false
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isBeating = true
                }
                .padding(16.dp)
        ) {
            Text(
                "Made with ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Love",
                tint = Color.Red,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            Text(
                " for the Pixel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreferenceCategory(title: String, isFirst: Boolean = false) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(
            start = 16.dp, 
            top = if (isFirst) 8.dp else 24.dp, 
            bottom = 8.dp
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = shape
            )
            .clip(shape)
            .iosTouchFeedback(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                if (summary != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    showSecondaryAction: Boolean = false,
    onSecondaryActionClick: () -> Unit = {}
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = shape
            )
            .clip(shape)
            .iosTouchFeedback(enabled = enabled) {
                onCheckedChange(!checked)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                if (summary != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }

            if (showSecondaryAction) {
                IconButton(onClick = onSecondaryActionClick, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage",
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null
            )
        }
    }
}
