package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SettingsScreen(
    onPermissionRequest: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMasterSwitchChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    val lazyListState = rememberLazyListState()
    
    // Global Master Switch State (Read from Prefs, but we need to track it for disabling items)
    var masterSwitch by remember { mutableStateOf(prefs.getBoolean("master_switch", false)) }
    
    // We need to listen to preference changes for the master switch specifically
    // to ensure the UI items disable/enable correctly when the title toggle changes.
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "master_switch") {
                val newState = p.getBoolean(key, false)
                masterSwitch = newState
                onMasterSwitchChange(newState)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Dialog States
    var showAppListDialog by remember { mutableStateOf(false) }
    var showBlockListDialog by remember { mutableStateOf(false) }
    var showChargingModeDialog by remember { mutableStateOf(false) }

    // Toggle States (For Reactive UI Refresh)
    var isScheduledDnd by remember { mutableStateOf(prefs.getBoolean("scheduled_dnd", false)) }
    var customLimit by remember { mutableStateOf(prefs.getInt("custom_charging_limit", 80)) }
    
    if (showChargingModeDialog) {
        // We use a local state for the dialog selection to ensure the UI is snappy
        var selectedMode by remember { 
            mutableStateOf(if (prefs.getBoolean("custom_limit_enabled", false)) 3 else AodSettings.getChargeOptimizationMode(context.contentResolver)) 
        }
        
        AlertDialog(
            onDismissRequest = { showChargingModeDialog = false },
            title = { Text("Charging Optimization") },
            text = {
                Column {
                    listOf("Off" to 0, "Limit to 80%" to 1, "Adaptive Charging" to 2, "Custom Limit" to 3).forEach { (label, mode) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val previousMode = selectedMode
                                selectedMode = mode
                                
                                // Offload blocking IPC calls to IO thread to prevent UI lag
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        if (mode != 3) {
                                            prefs.edit().putBoolean("custom_limit_enabled", false).apply()
                                            AodSettings.setChargeOptimizationMode(context.contentResolver, mode)
                                            if (mode == 0) AodSettings.setAdaptiveChargingEnabled(context.contentResolver, false)
                                            
                                            withContext(Dispatchers.Main) {
                                                showChargingModeDialog = false
                                            }
                                        } else {
                                            prefs.edit().putBoolean("custom_limit_enabled", true).apply()
                                            AodSettings.setChargeOptimizationMode(context.contentResolver, 0)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            selectedMode = previousMode
                                        }
                                    }
                                }
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(label)
                        }
                    }

                    if (selectedMode == 3) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Limit: ${customLimit}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = customLimit.toFloat(),
                            onValueChange = { 
                                val rounded = (it.toInt() / 5) * 5
                                if (rounded != customLimit) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    customLimit = rounded
                                    scope.launch(Dispatchers.IO) {
                                        prefs.edit().putInt("custom_charging_limit", rounded).apply()
                                    }
                                }
                            },
                            valueRange = 80f..100f,
                            steps = 3, // 80, 85, 90, 95, 100
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showChargingModeDialog = false }) { 
                    Text(if (selectedMode == 3) "Done" else "Cancel") 
                } 
            }
        )
    }

    if (showAppListDialog) {
        val watchedPackages = remember { prefs.getStringSet("watched_apps", emptySet()) ?: emptySet() }
        AppListDialog(
            title = "Per-App Notifications",
            selectedPackages = watchedPackages,
            onDismiss = { showAppListDialog = false },
            onConfirm = { packages ->
                prefs.edit().putStringSet("watched_apps", packages).apply()
                showAppListDialog = false
            }
        )
    }

    if (showBlockListDialog) {
        val blockedPackages = remember { prefs.getStringSet("live_notif_blocklist", emptySet()) ?: emptySet() }
        AppListDialog(
            title = "Manage Block List",
            selectedPackages = blockedPackages,
            onDismiss = { showBlockListDialog = false },
            onConfirm = { packages ->
                prefs.edit().putStringSet("live_notif_blocklist", packages).apply()
                showBlockListDialog = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding
        ) {
            item { PreferenceCategory(title = "Automation & Triggers") }
            
            item {
                PreferenceSwitch(
                    title = "Charging Mode",
                    summary = "Turn on AOD automatically when charger is connected",
                    icon = Icons.Default.BatteryChargingFull,
                    checked = prefs.getBoolean("charging_mode", false),
                    enabled = masterSwitch,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        prefs.edit().putBoolean("charging_mode", it).apply() 
                    }
                )
            }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.charging_info_title),
                    summary = stringResource(R.string.charging_info_summary),
                    icon = Icons.Default.Info,
                    checked = prefs.getBoolean("charging_info_notif", false),
                    enabled = masterSwitch,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        prefs.edit().putBoolean("charging_info_notif", it).apply()
                    }
                )
            }

            item {
                val isCustomLimit = prefs.getBoolean("custom_limit_enabled", false)
                val currentMode = AodSettings.getChargeOptimizationMode(context.contentResolver)
                val modeSummary = when {
                    isCustomLimit -> "Custom Limit: ${customLimit}%"
                    currentMode == 1 -> "Limit to 80%"
                    currentMode == 2 -> "Adaptive Charging"
                    else -> "Off"
                }
                
                PreferenceItem(
                    title = "Charging Optimization",
                    summary = modeSummary,
                    icon = Icons.Default.BatterySaver,
                    enabled = masterSwitch,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showChargingModeDialog = true 
                    }
                )
            }

            item {
                PreferenceItem(
                    title = "Per-App Notifications",
                    summary = "Always trigger AOD for these apps",
                    icon = Icons.Default.Notifications,
                    enabled = masterSwitch,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAppListDialog = true 
                    }
                )
            }

            item {
                PreferenceSwitch(
                    title = "Live Notification Mode",
                    summary = "AOD for Maps, Uber etc.",
                    icon = Icons.Default.Map,
                    checked = prefs.getBoolean("live_notif_mode", false),
                    enabled = masterSwitch,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        prefs.edit().putBoolean("live_notif_mode", it).apply() 
                    },
                    showSecondaryAction = true,
                    onSecondaryActionClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showBlockListDialog = true 
                    }
                )
            }

            item { PreferenceCategory(title = "Restrictions") }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.dnd_mode_title),
                    summary = stringResource(R.string.dnd_mode_summary),
                    checked = prefs.getBoolean("dnd_mode", false),
                    enabled = masterSwitch,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        prefs.edit().putBoolean("dnd_mode", it).apply() 
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.scheduled_dnd_title),
                    summary = stringResource(R.string.scheduled_dnd_summary),
                    checked = isScheduledDnd,
                    enabled = masterSwitch,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isScheduledDnd = it
                        prefs.edit().putBoolean("scheduled_dnd", it).apply() 
                    }
                )
            }

            if (isScheduledDnd) {
                item {
                    var startTime by remember { mutableStateOf(prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00") }
                    PreferenceItem(
                        title = "Start Time",
                        summary = startTime,
                        enabled = masterSwitch,
                        onClick = {
                            val parts = startTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val time = String.format(Locale.US, "%02d:%02d", h, m)
                                prefs.edit().putString("scheduled_dnd_start", time).apply()
                                startTime = time
                            }, parts[0].toInt(), parts[1].toInt(), true).show()
                        }
                    )
                }
                item {
                    var endTime by remember { mutableStateOf(prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00") }
                    PreferenceItem(
                        title = "End Time",
                        summary = endTime,
                        enabled = masterSwitch,
                        onClick = {
                            val parts = endTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val time = String.format(Locale.US, "%02d:%02d", h, m)
                                prefs.edit().putString("scheduled_dnd_end", time).apply()
                                endTime = time
                            }, parts[0].toInt(), parts[1].toInt(), true).show()
                        }
                    )
                }
            }

            item { PreferenceCategory(title = "Service Status") }

            item {
                val hasWriteSecure = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                PreferenceItem(
                    title = "Write Secure Settings",
                    summary = if (hasWriteSecure) "Granted" else "Missing - Tap to grant via Shizuku",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPermissionRequest()
                    }
                )
            }
            
            item {
                val enabledListeners = AndroidSettings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                val hasNotifyAccess = enabledListeners?.contains(context.packageName) == true
                PreferenceItem(
                    title = "Notification Access",
                    summary = if (hasNotifyAccess) "Granted" else "Missing - Required for app detection",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) 
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    val hasPostNotif = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    PreferenceItem(
                        title = "Notification Permission",
                        summary = if (hasPostNotif) "Granted" else "Missing - Required for charging info",
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(48.dp))
                MadeWithLoveFooter(haptic)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MadeWithLoveFooter(haptic: HapticFeedback) {
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
                    .scale(scale)
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
fun PreferenceCategory(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
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
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) } },
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }
    )
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
    var isChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        isChecked = checked
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSecondaryAction) {
                    IconButton(onClick = onSecondaryActionClick, enabled = enabled) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Manage",
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                Switch(
                    checked = isChecked,
                    enabled = enabled,
                    onCheckedChange = {
                        isChecked = it
                        onCheckedChange(it)
                    }
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
