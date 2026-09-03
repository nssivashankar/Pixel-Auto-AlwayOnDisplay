package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    onGrantSecureSettings: () -> Unit,
    onCopyAdbCommand: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 6 })

    val hasSecureSettings by produceState(initialValue = false) {
        while (!value) {
            value = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
            if (!value) delay(1000)
        }
    }

    val hasNotificationAccess by produceState(initialValue = false) {
        while (!value) {
            val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            value = enabledListeners?.contains(context.packageName) == true
            if (!value) delay(1000)
        }
    }

    val hasPostNotifications by produceState(initialValue = false) {
        while (!value) {
            value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
            if (!value) delay(1000)
        }
    }

    val isBatteryOptimized by produceState(initialValue = false) {
        while (!value) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            value = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            if (!value) delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> SecureSettingsPage(
                        isGranted = hasSecureSettings,
                        onGrant = onGrantSecureSettings,
                        onCopyAdb = onCopyAdbCommand
                    )
                    2 -> NotificationAccessPage(
                        isGranted = hasNotificationAccess,
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                    3 -> PostNotificationsPage(
                        isGranted = hasPostNotifications,
                        onGrant = onRequestNotifications
                    )
                    4 -> BatteryOptimizationPage(
                        isGranted = isBatteryOptimized,
                        onGrant = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                    5 -> FinalPage(onStart = onComplete)
                }
            }

            // Bottom Navigation Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        val color = if (pagerState.currentPage == i) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == i) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next Button
                if (pagerState.currentPage < 5) {
                    val canGoNext = when (pagerState.currentPage) {
                        1 -> hasSecureSettings
                        2 -> hasNotificationAccess
                        3 -> hasPostNotifications
                        else -> true
                    }

                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        enabled = canGoNext,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Next")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPageTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(32.dp))
        content()
    }
}

@Composable
private fun WelcomePage() {
    SetupPageTemplate(
        icon = Icons.Default.AutoAwesome,
        title = "Welcome to Pixel Auto AOD",
        description = "Let's get your Pixel set up for intelligent Always-On Display automation and battery health management."
    )
}

@Composable
private fun SecureSettingsPage(
    isGranted: Boolean,
    onGrant: () -> Unit,
    onCopyAdb: () -> Unit
) {
    SetupPageTemplate(
        icon = Icons.Default.Security,
        title = "Secure Settings",
        description = "To control system features like AOD and Adaptive Charging, we need a special permission that can only be granted via Shizuku or your PC."
    ) {
        if (isGranted) {
            PermissionGrantedChip()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant via Shizuku")
                }
                OutlinedButton(
                    onClick = onCopyAdb,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy ADB Command")
                }
            }
        }
    }
}

@Composable
private fun NotificationAccessPage(
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    SetupPageTemplate(
        icon = Icons.Default.NotificationsActive,
        title = "Smart Tracking",
        description = "This allows the app to detect ongoing events from other apps (like Navigation or Uber) to intelligently keep your display awake during active tasks."
    ) {
        if (isGranted) {
            PermissionGrantedChip()
        } else {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Enable Notification Access")
            }
        }
    }
}

@Composable
private fun PostNotificationsPage(
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    SetupPageTemplate(
        icon = Icons.Default.Notifications,
        title = "Battery Alerts",
        description = "To receive important health alerts, like when your battery reaches its custom limit or is fully charged, please allow the app to post notifications."
    ) {
        if (isGranted) {
            PermissionGrantedChip()
        } else {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Allow Notifications")
            }
        }
    }
}

@Composable
private fun BatteryOptimizationPage(
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    SetupPageTemplate(
        icon = Icons.Default.BatteryChargingFull,
        title = "Reliable Background",
        description = "To ensure automation works every time, the system must not put the service to sleep. Please allow 'Unrestricted' battery usage."
    ) {
        if (isGranted) {
            PermissionGrantedChip()
        } else {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Allow Background Running")
            }
        }
    }
}

@Composable
private fun FinalPage(onStart: () -> Unit) {
    SetupPageTemplate(
        icon = Icons.Default.CheckCircle,
        title = "All Set!",
        description = "You're ready to enjoy a more intelligent Always-On Display. You can customize all triggers in the dashboard."
    ) {
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Finish Setup", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionGrantedChip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                "Permission Granted",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
