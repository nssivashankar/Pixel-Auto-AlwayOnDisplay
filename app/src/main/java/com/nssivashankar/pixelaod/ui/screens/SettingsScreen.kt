package com.nssivashankar.pixelaod.ui.screens

import android.content.Context
import android.os.Build
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings

@Composable
fun SettingsScreen(
    onPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    val lazyListState = rememberLazyListState()
    
    // The Mirror Engine Layer
    val contentLayer = rememberGraphicsLayer()

    var masterSwitch by remember { mutableStateOf(prefs.getBoolean("master_switch", false)) }
    
    // Dialog States
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAppListDialog by remember { mutableStateOf(false) }
    
    val headerHeight = 64.dp + WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val isDark = isSystemInDarkTheme()

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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. The Real Content (Sharp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // Record content for the mirror
                    contentLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
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

            item { PreferenceCategory(title = "Service Status") }

            item {
                PreferenceItem(
                    title = "Write Secure Settings",
                    summary = "Tap to grant via Shizuku",
                    onClick = onPermissionRequest
                )
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
                        drawLayer(contentLayer)
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
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding()))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
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
