package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// --- High-Performance Settings State Holder ---
class SettingsState(context: Context, private val scope: kotlinx.coroutines.CoroutineScope) {
    private val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
    private val resolver = context.contentResolver

    var masterSwitch by mutableStateOf(prefs.getBoolean("master_switch", false))
    var chargingMode by mutableStateOf(prefs.getBoolean("charging_mode", false))
    var chargingInfoNotif by mutableStateOf(prefs.getBoolean("charging_info_notif", false))
    var liveNotifMode by mutableStateOf(prefs.getBoolean("live_notif_mode", false))
    var dndMode by mutableStateOf(prefs.getBoolean("dnd_mode", false))
    var scheduledDnd by mutableStateOf(prefs.getBoolean("scheduled_dnd", false))
    var customLimitEnabled by mutableStateOf(prefs.getBoolean("custom_limit_enabled", false))
    var customLimit by mutableIntStateOf(prefs.getInt("custom_charging_limit", 80))
    var currentOptimizationMode by mutableIntStateOf(AodSettings.getChargeOptimizationMode(resolver))

    fun updateMasterSwitch(enabled: Boolean) {
        masterSwitch = enabled
        prefs.edit().putBoolean("master_switch", enabled).apply()
        AodSettings.setAodEnabled(resolver, enabled)
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

    fun updateCustomLimit(limit: Int) {
        customLimit = limit
        prefs.edit().putInt("custom_charging_limit", limit).apply()
    }

    fun setOptimization(mode: Int, custom: Boolean) {
        scope.launch(Dispatchers.IO) {
            customLimitEnabled = custom
            prefs.edit().putBoolean("custom_limit_enabled", custom).apply()
            
            if (!custom) {
                AodSettings.setChargeOptimizationMode(resolver, mode)
                if (mode == 2) AodSettings.setAdaptiveChargingEnabled(resolver, true)
                else if (mode == 0) AodSettings.setAdaptiveChargingEnabled(resolver, false)
                currentOptimizationMode = mode
            } else {
                AodSettings.setChargeOptimizationMode(resolver, 0)
                currentOptimizationMode = 0
            }
        }
    }
}

@Composable
fun SettingsScreen(onPermissionRequest: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val state = remember { SettingsState(context, scope) }
    val contentLayer = rememberGraphicsLayer()
    
    var currentTab by remember { mutableIntStateOf(0) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues(density).calculateTopPadding()
    val headerHeight = 64.dp + statusBarPadding
    val isDark = isSystemInDarkTheme()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Custom Material 3 Floating Bottom Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Button
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentTab = 0 
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        // About Button
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentTab = 1 
                            },
                            icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 1. Content Layer
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "navigation"
            ) { targetTab ->
                if (targetTab == 0) {
                    MainSettingsList(state, headerHeight, contentLayer, onPermissionRequest)
                } else {
                    AboutScreen(contentPadding = PaddingValues(top = headerHeight, bottom = 120.dp))
                }
            }

            // 2. Glass Header Layer (Persists across tabs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    30f, 30f, android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                        .drawBehind {
                            clipRect(0f, 0f, size.width, size.height) {
                                drawLayer(contentLayer)
                            }
                        }
                )

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

                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(statusBarPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (currentTab == 0) "Pixel AOD" else "About",
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 30.sp, 
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        if (currentTab == 0) {
                            Switch(
                                checked = state.masterSwitch,
                                onCheckedChange = { state.updateMasterSwitch(it) }
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun MainSettingsList(
    state: SettingsState,
    headerHeight: androidx.compose.ui.unit.Dp,
    contentLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    onPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    
    var showAppListDialog by remember { mutableStateOf(false) }
    var showBlockListDialog by remember { mutableStateOf(false) }
    var showChargingModeDialog by remember { mutableStateOf(false) }

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
        val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
        AppListDialog(
            title = "Per-App Notifications",
            selectedPackages = prefs.getStringSet("watched_apps", emptySet()) ?: emptySet(),
            onDismiss = { showAppListDialog = false },
            onConfirm = { packages ->
                prefs.edit().putStringSet("watched_apps", packages).apply()
                showAppListDialog = false
            }
        )
    }

    if (showBlockListDialog) {
        val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
        AppListDialog(
            title = "Manage Block List",
            selectedPackages = prefs.getStringSet("live_notif_blocklist", emptySet()) ?: emptySet(),
            onDismiss = { showBlockListDialog = false },
            onConfirm = { packages ->
                prefs.edit().putStringSet("live_notif_blocklist", packages).apply()
                showBlockListDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                contentLayer.record {
                    this@drawWithContent.drawContent()
                }
            },
        contentPadding = PaddingValues(top = headerHeight, bottom = 120.dp),
        state = lazyListState
    ) {
        item(key = "cat_auto") { PreferenceCategory(title = "Automation & Triggers") }
        
        item(key = "pref_charging") {
            PreferenceSwitch(
                title = "Charging Mode",
                summary = "Turn on AOD automatically when charger is connected",
                icon = Icons.Default.BatteryChargingFull,
                checked = state.chargingMode,
                enabled = state.masterSwitch,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.updateChargingMode(it)
                }
            )
        }

        item(key = "pref_info") {
            PreferenceSwitch(
                title = stringResource(R.string.charging_info_title),
                summary = stringResource(R.string.charging_info_summary),
                icon = Icons.Default.Info,
                checked = state.chargingInfoNotif,
                enabled = state.masterSwitch,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.updateChargingInfoNotif(it)
                }
            )
        }

        item(key = "pref_opt") {
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
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showChargingModeDialog = true 
                }
            )
        }

        item(key = "pref_apps") {
            PreferenceItem(
                title = "Per-App Notifications",
                summary = "Always trigger AOD for these apps",
                icon = Icons.Default.Notifications,
                enabled = state.masterSwitch,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAppListDialog = true 
                }
            )
        }

        item(key = "pref_live") {
            PreferenceSwitch(
                title = "Live Notification Mode",
                summary = "AOD for Maps, Uber etc.",
                icon = Icons.Default.Map,
                checked = state.liveNotifMode,
                enabled = state.masterSwitch,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.updateLiveNotifMode(it)
                },
                showSecondaryAction = true,
                onSecondaryActionClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showBlockListDialog = true 
                }
            )
        }

        item(key = "cat_restrict") { PreferenceCategory(title = "Restrictions") }

        item(key = "pref_dnd") {
            PreferenceSwitch(
                title = stringResource(R.string.dnd_mode_title),
                summary = stringResource(R.string.dnd_mode_summary),
                checked = state.dndMode,
                enabled = state.masterSwitch,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.updateDndMode(it)
                }
            )
        }
        
        item(key = "pref_scheduled") {
            PreferenceSwitch(
                title = stringResource(R.string.scheduled_dnd_title),
                summary = stringResource(R.string.scheduled_dnd_summary),
                checked = state.scheduledDnd,
                enabled = state.masterSwitch,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.updateScheduledDnd(it)
                }
            )
        }

        if (state.scheduledDnd) {
            item(key = "pref_start") {
                val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
                var startTime by remember { mutableStateOf(prefs.getString("scheduled_dnd_start", "22:00") ?: "22:00") }
                PreferenceItem(
                    title = "Start Time",
                    summary = startTime,
                    enabled = state.masterSwitch,
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
            item(key = "pref_end") {
                val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
                var endTime by remember { mutableStateOf(prefs.getString("scheduled_dnd_end", "07:00") ?: "07:00") }
                PreferenceItem(
                    title = "End Time",
                    summary = endTime,
                    enabled = state.masterSwitch,
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

        item(key = "cat_service") { PreferenceCategory(title = "Service Status") }

        item(key = "pref_secure") {
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
        
        item(key = "pref_notif_access") {
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

        item(key = "footer") {
            Spacer(Modifier.height(48.dp))
            MadeWithLoveFooter(haptic)
            Spacer(Modifier.height(24.dp))
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
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
