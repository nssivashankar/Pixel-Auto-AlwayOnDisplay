package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import java.util.Locale

@Composable
fun SettingsScreen(
    onPermissionRequest: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    val lazyListState = rememberLazyListState()
    
    // Dialog States
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAppListDialog by remember { mutableStateOf(false) }
    var showBlockListDialog by remember { mutableStateOf(false) }

    // Toggle States (For Reactive UI Refresh)
    var isScheduledDnd by remember { mutableStateOf(prefs.getBoolean("scheduled_dnd", false)) }
    
    if (showBatteryDialog) {
        val currentMode = AodSettings.getChargeOptimizationMode(context.contentResolver)
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text(stringResource(R.string.charge_optimization_title)) },
            text = {
                Column {
                    listOf("Off" to 0, "Limit to 80%" to 1, "Adaptive Charging" to 2).forEach { (label, mode) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                AodSettings.setChargeOptimizationMode(context.contentResolver, mode)
                                if (mode == 0) AodSettings.setAdaptiveChargingEnabled(context.contentResolver, false)
                                showBatteryDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentMode == mode, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBatteryDialog = false }) { Text("Cancel") } }
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
                    summary = "Turn on AoD automatically when charger is connected",
                    icon = Icons.Default.BatteryChargingFull,
                    checked = prefs.getBoolean("charging_mode", false),
                    onCheckedChange = { prefs.edit().putBoolean("charging_mode", it).apply() }
                )
            }

            item {
                PreferenceItem(
                    title = "Per-App Notifications",
                    summary = "Always trigger AoD for these apps",
                    icon = Icons.Default.Notifications,
                    onClick = { showAppListDialog = true }
                )
            }

            item {
                PreferenceSwitch(
                    title = "Live Notification Mode",
                    summary = "AoD for Maps, Uber etc.",
                    icon = Icons.Default.Map,
                    checked = prefs.getBoolean("live_notif_mode", false),
                    onCheckedChange = { prefs.edit().putBoolean("live_notif_mode", it).apply() },
                    showSecondaryAction = true,
                    onSecondaryActionClick = { showBlockListDialog = true }
                )
            }

            item { PreferenceCategory(title = stringResource(R.string.battery_health_title)) }

            item {
                val currentMode = AodSettings.getChargeOptimizationMode(context.contentResolver)
                PreferenceItem(
                    title = stringResource(R.string.charge_optimization_title),
                    summary = when(currentMode) {
                        1 -> "Limit to 80%"
                        2 -> "Adaptive Charging"
                        else -> "Off"
                    },
                    icon = Icons.Default.BatterySaver,
                    onClick = { showBatteryDialog = true }
                )
            }

            item { PreferenceCategory(title = "UI & Appearance") }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.charging_info_title),
                    summary = stringResource(R.string.charging_info_summary),
                    icon = Icons.Default.Info,
                    checked = prefs.getBoolean("charging_info_notif", false),
                    onCheckedChange = { prefs.edit().putBoolean("charging_info_notif", it).apply() }
                )
            }

            item { PreferenceCategory(title = "Restrictions") }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.dnd_mode_title),
                    summary = stringResource(R.string.dnd_mode_summary),
                    checked = prefs.getBoolean("dnd_mode", false),
                    onCheckedChange = { prefs.edit().putBoolean("dnd_mode", it).apply() }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.scheduled_dnd_title),
                    summary = stringResource(R.string.scheduled_dnd_summary),
                    checked = isScheduledDnd,
                    onCheckedChange = { 
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
                        onClick = {
                            val parts = startTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
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
                        onClick = {
                            val parts = endTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
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
                    onClick = onPermissionRequest
                )
            }
            
            item {
                val enabledListeners = AndroidSettings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                val hasNotifyAccess = enabledListeners?.contains(context.packageName) == true
                PreferenceItem(
                    title = "Notification Access",
                    summary = if (hasNotifyAccess) "Granted" else "Missing - Required for app detection",
                    onClick = { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    val hasPostNotif = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    PreferenceItem(
                        title = "Notification Permission",
                        summary = if (hasPostNotif) "Granted" else "Missing - Required for charging info",
                        onClick = { /* Handled in Activity normally */ }
                    )
                }
            }
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    )
}

@Composable
fun PreferenceSwitch(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
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
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSecondaryAction) {
                    IconButton(onClick = onSecondaryActionClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Manage",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = isChecked,
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
