package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import java.util.Locale

@Composable
fun SettingsScreen(
    onPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    
    // The Mirror Engine Layer
    val contentLayer = rememberGraphicsLayer()

    var masterSwitch by remember { mutableStateOf(prefs.getBoolean("master_switch", false)) }
    
    // Dialog States
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAppListDialog by remember { mutableStateOf(false) }
    
    val systemTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerContentHeight = 64.dp
    val headerHeight = headerContentHeight + systemTopPadding
    val isDark = isSystemInDarkTheme()

    // --- Mirror Engine Alignment ---
    // Remove the buffer to allow the blur to reach the top of the header
    val topBufferPx = 0f

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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. The Real Content (Sharp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Record content AFTER drawing so we capture exactly what's on screen
                    contentLayer.record {
                        this@drawWithContent.drawContent()
                    }
                },
            contentPadding = PaddingValues(top = headerHeight, bottom = 48.dp),
            state = lazyListState
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
                    summary = "AoD for Maps, Uber etc. \u2022 Tap to manage block list",
                    icon = Icons.Default.Map,
                    checked = prefs.getBoolean("live_notif_mode", false),
                    onCheckedChange = { prefs.edit().putBoolean("live_notif_mode", it).apply() }
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
                    checked = prefs.getBoolean("scheduled_dnd", false),
                    onCheckedChange = { prefs.edit().putBoolean("scheduled_dnd", it).apply() }
                )
            }

            if (prefs.getBoolean("scheduled_dnd", false)) {
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
                        onClick = { /* Handled in Activity normally, but can trigger system dialog */ }
                    )
                }
            }
        }

        // 2. The Glass Header Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            // iOS Style Ultra-Frosted Lens (Mirror)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                80f, 80f, android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
                    .drawBehind {
                        // Suppress glows by clipping edges in the mirror view
                        val width = size.width
                        val height = size.height
                        
                        clipRect(
                            left = 0f,
                            top = topBufferPx,
                            right = width,
                            bottom = height
                        ) {
                            // We don't need translate here if the record { } captures the whole screen 
                            // BUT we need to ensure the LazyColumn is recorded CORRECTLY.
                            drawLayer(contentLayer)
                        }
                    }
            )

            // iOS Translucent Tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isDark) 
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                        else 
                            Color.White.copy(alpha = 0.6f)
                    )
            )

            // Toolbar Content
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(systemTopPadding))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerContentHeight)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pixel AOD",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = masterSwitch,
                        onCheckedChange = {
                            masterSwitch = it
                            prefs.edit().putBoolean("master_switch", it).apply()
                            AodSettings.setAodEnabled(context.contentResolver, it)
                        }
                    )
                }
                
                // Visual Separator
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
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
    onCheckedChange: (Boolean) -> Unit
) {
    var isChecked by remember { mutableStateOf(checked) }
    // Update local state if the preference changes externally
    LaunchedEffect(checked) {
        isChecked = checked
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onCheckedChange(it)
                }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}
