package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.ui.theme.AppHaptics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }

    var hasSecureSettings by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
    }

    var hasNotificationAccess by remember {
        mutableStateOf(
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
        )
    }

    var hasPostNotifications by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var isBatteryOptimized by remember {
        mutableStateOf(
            context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
        )
    }

    // Refresh permissions on activity resume (when user returns from system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSecureSettings = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                hasNotificationAccess = enabledListeners?.contains(context.packageName) == true
                hasPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
                isBatteryOptimized = context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> SecureSettingsPage(
                        isGranted = hasSecureSettings,
                        onGrant = onGrantSecureSettings,
                        onCopyAdb = onCopyAdbCommand,
                        onSkip = {
                            prefs.edit().putBoolean("secure_settings_skipped", true).apply()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
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

                // Navigation Buttons (Back & Next)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }

                    if (pagerState.currentPage < 5) {
                        val canGoNext = when (pagerState.currentPage) {
                            1 -> hasSecureSettings || prefs.getBoolean("secure_settings_skipped", false)
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
}

private enum class SetupAnimationType {
    PULSE_SHIELD,
    SWAY_BELL,
    FLOAT_MESSAGE,
    ENERGY_BATTERY,
    CELEBRATE_CHECK
}

@Composable
private fun AnimatedIconContainer(
    icon: ImageVector,
    animationType: SetupAnimationType,
    isGranted: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pageAnimation")

    val (scale, rotation, offsetY) = when (animationType) {
        SetupAnimationType.PULSE_SHIELD -> {
            val s by infiniteTransition.animateFloat(
                initialValue = 1.0f, targetValue = 1.08f,
                animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "shieldScale"
            )
            val r by infiniteTransition.animateFloat(
                initialValue = -4f, targetValue = 4f,
                animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "shieldRotate"
            )
            Triple(s, r, 0f)
        }
        SetupAnimationType.SWAY_BELL -> {
            val r by infiniteTransition.animateFloat(
                initialValue = -12f, targetValue = 12f,
                animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bellSway"
            )
            Triple(1.0f, r, 0f)
        }
        SetupAnimationType.FLOAT_MESSAGE -> {
            val y by infiniteTransition.animateFloat(
                initialValue = -6f, targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "msgFloat"
            )
            Triple(1.0f, 0f, y)
        }
        SetupAnimationType.ENERGY_BATTERY -> {
            val s by infiniteTransition.animateFloat(
                initialValue = 1.0f, targetValue = 1.10f,
                animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "batteryPulse"
            )
            Triple(s, 0f, 0f)
        }
        SetupAnimationType.CELEBRATE_CHECK -> {
            val s by infiniteTransition.animateFloat(
                initialValue = 1.0f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "celebrateScale"
            )
            Triple(s, 0f, 0f)
        }
    }

    val grantedScale by animateFloatAsState(
        targetValue = if (isGranted) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "grantedScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val finalScale = scale * grantedScale
                scaleX = finalScale
                scaleY = finalScale
                rotationZ = rotation
                translationY = offsetY
            }
            .size(118.dp)
            .clip(CircleShape)
            .background(
                if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SetupPageTemplate(
    icon: ImageVector,
    animationType: SetupAnimationType,
    title: String,
    description: String,
    isGranted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedIconContainer(
            icon = icon,
            animationType = animationType,
            isGranted = isGranted
        )
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
    val haptic = LocalHapticFeedback.current

    // Trigger initial Pixel Welcome Haptic Sequence on entrance
    LaunchedEffect(Unit) {
        delay(300)
        AppHaptics.performTabSelect(haptic)
        delay(180)
        AppHaptics.performClick(haptic)
    }

    // 1. Organic Breathing Pulse Transition
    val infiniteTransition = rememberInfiniteTransition(label = "welcomePulse")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // 2. Interactive Tap Scale & Spring Bounce
    var isTapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isTapped) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { isTapped = false },
        label = "tapScale"
    )

    // 3. Staggered Entrance Animations
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pixel Organic Logo Badge Container
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val currentScale = breathScale * tapScale
                    scaleX = currentScale
                    scaleY = currentScale
                }
                .size(118.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .clickable {
                    isTapped = true
                    AppHaptics.performClick(haptic)
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle Outer Glow Ring
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                contentDescription = "Pixel Auto AOD Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.requiredSize(165.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Headline Text with Slide-Up & Fade-In
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 150)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pixel Auto AOD",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Intelligent AOD automation & battery health for your Pixel.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SecureSettingsPage(
    isGranted: Boolean,
    onGrant: () -> Unit,
    onCopyAdb: () -> Unit,
    onSkip: () -> Unit
) {
    SetupPageTemplate(
        icon = Icons.Default.Security,
        animationType = SetupAnimationType.PULSE_SHIELD,
        title = "Secure Settings",
        description = "Required for AOD automation & charging limits.",
        isGranted = isGranted
    ) {
        if (isGranted) {
            PermissionGrantedChip()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skip (Charging Details Only)",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
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
        animationType = SetupAnimationType.SWAY_BELL,
        title = "Smart Tracking",
        description = "Keeps AOD active for Maps, Uber & live tasks.",
        isGranted = isGranted
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
        animationType = SetupAnimationType.FLOAT_MESSAGE,
        title = "Battery Alerts",
        description = "Notifies when custom charging limit or 100% is reached.",
        isGranted = isGranted
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
        animationType = SetupAnimationType.ENERGY_BATTERY,
        title = "Reliable Background",
        description = "Allows background automation to run reliably.",
        isGranted = isGranted
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
        animationType = SetupAnimationType.CELEBRATE_CHECK,
        title = "All Set!",
        description = "Your Pixel is ready. Customize options in dashboard.",
        isGranted = true
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
