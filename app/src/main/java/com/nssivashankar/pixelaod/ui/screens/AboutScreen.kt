package com.nssivashankar.pixelaod.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nssivashankar.pixelaod.R
import com.nssivashankar.pixelaod.ui.components.M3OfficialExpressiveLoader
import com.nssivashankar.pixelaod.utils.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onPermissionRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var isCheckingUpdates by remember { mutableStateOf(false) }

    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.0"
        } catch (e: Exception) { "1.1.0" }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "about_header", contentType = "header") {
                Spacer(Modifier.height(32.dp))
                // App Logo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Pixel Auto AOD Logo",
                        modifier = Modifier.requiredSize(140.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Pixel Auto AOD",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Version $currentVersion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(40.dp))
            }

            item(key = "about_troubleshooting", contentType = "card") {
                var isExpanded by remember { mutableStateOf(false) }

                val hasWriteSecurePermission = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                val enabledListeners = AndroidSettings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                val hasNotificationAccess = enabledListeners?.contains(context.packageName) == true

                PreferenceCategory(title = "TROUBLESHOOTING")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExpanded = !isExpanded
                        },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasWriteSecurePermission && hasNotificationAccess)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = if (hasWriteSecurePermission && hasNotificationAccess)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App not working as expected?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isExpanded) "Tap to hide app permissions & status" else "Tap to check app permissions & status",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isExpanded) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))

                            // 1. Write Secure Settings
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPermissionRequest()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Write Secure Settings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (hasWriteSecurePermission) "Permission Granted" else "Permission Missing - Tap to grant via Shizuku/ADB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasWriteSecurePermission) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    imageVector = if (hasWriteSecurePermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasWriteSecurePermission) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // 2. Notification Access
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notification Access",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (hasNotificationAccess) "Permission Granted" else "Permission Missing - Tap to grant in system settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasNotificationAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    imageVector = if (hasNotificationAccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasNotificationAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // 3. Reset Onboarding
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
                                            .edit().putBoolean("is_setup_complete", false).apply()
                                        (context as? Activity)?.recreate()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reset Onboarding",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Re-run first-time setup guide",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "about_feedback", contentType = "preference_item") {
                PreferenceCategory(title = "Support & Community")

                PreferenceItem(
                    title = "Send Bug Report / Feedback",
                    summary = "Email developer with device info & issue template",
                    icon = Icons.Default.Email,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val hasWriteSecurePermission = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                        val enabledListeners = AndroidSettings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                        val hasNotificationAccess = enabledListeners?.contains(context.packageName) == true

                        val body = """
                            --- DEVICE & SYSTEM DIAGNOSTICS ---
                            App Version: $currentVersion
                            Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}
                            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            Write Secure Settings: ${if (hasWriteSecurePermission) "Granted" else "Missing"}
                            Notification Access: ${if (hasNotificationAccess) "Granted" else "Missing"}
                            
                            --- ISSUE DETAILS & FEEDBACK ---
                            [Describe what happened, what you expected, or feature requested here]
                            
                            
                            --- STEPS TO REPRODUCE (if bug) ---
                            1. 
                            2. 
                            
                            --- ATTACHMENTS ---
                            [Please attach any relevant screenshots or screen recordings to this email]
                        """.trimIndent()

                        val subject = "Pixel Auto AOD v$currentVersion - Bug Report & Feedback"
                        val mailtoUrl = "mailto:nssivashankar@gmail.com" +
                                "?subject=" + Uri.encode(subject) +
                                "&body=" + Uri.encode(body)

                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUrl)).apply {
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("nssivashankar@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mailtoUrl)))
                            } catch (_: Exception) {}
                        }
                    }
                )

                PreferenceItem(
                    title = "GitHub Issues & Requests",
                    summary = "Report issues or suggest features directly on GitHub",
                    icon = Icons.Default.Forum,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/issues"))
                        context.startActivity(intent)
                    }
                )
            }

            item(key = "about_info", contentType = "preference_item") {
                PreferenceCategory(title = "Information")
                PreferenceItem(
                    title = "View on GitHub",
                    summary = "Check source code and releases",
                    icon = Icons.Default.Code,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay"))
                        context.startActivity(intent)
                    }
                )
            }

            item(key = "about_updates", contentType = "preference_item") {
                var isUpToDate by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = "Check for Updates",
                    summary = if (isCheckingUpdates) "Checking backend..." else "Manually verify latest version",
                    icon = Icons.Default.Update,
                    onClick = {
                        if (!isCheckingUpdates) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                isCheckingUpdates = true
                                isUpToDate = false
                                var upToDateDetected = false
                                UpdateChecker.checkForUpdates(
                                    context = context,
                                    currentVersion = currentVersion,
                                    isManual = true,
                                    onUpToDate = {
                                        upToDateDetected = true
                                    },
                                    onUpdateAvailable = { latest, notes, url ->
                                        UpdateChecker.showUpdateDialog(context, latest, notes, url)
                                    }
                                )
                                delay(1200) // allow the morphing loader to display smoothly
                                isCheckingUpdates = false
                                if (upToDateDetected) {
                                    isUpToDate = true
                                }
                            }
                        }
                    }
                )
                
                // Official M3 Expressive Morphing Loader for Updates
                if (isCheckingUpdates) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        M3OfficialExpressiveLoader(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isUpToDate) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF2E7D32), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Your app is up to date (v$currentVersion)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item(key = "about_whats_new", contentType = "card") {
                var isFetchingNotes by remember { mutableStateOf(false) }
                var releaseInfo by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
                var showDialog by remember { mutableStateOf(false) }

                if (showDialog && releaseInfo != null) {
                    val info = releaseInfo!!
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("What's New (${info.version})") },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(
                                rememberScrollState()
                            )) {
                                Text(
                                    text = info.changelog.ifBlank { "No detailed release notes provided." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Close")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                context.startActivity(intent)
                            }) {
                                Text("View on GitHub")
                            }
                        }
                    )
                }

                PreferenceCategory(title = "What's New!")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isFetchingNotes) {
                                scope.launch {
                                    isFetchingNotes = true
                                    val result = UpdateChecker.fetchLatestReleaseInfo()
                                    isFetchingNotes = false
                                    if (result != null) {
                                        releaseInfo = result
                                        showDialog = true
                                    } else {
                                        Toast.makeText(context, "Could not fetch release notes", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Latest Release Notes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (isFetchingNotes) "Fetching from GitHub..." else "Tap to view full changelog from GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "about_credits", contentType = "card") {
                PreferenceCategory(title = "Credits")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Maintaining & Modernizing AOD automation for the Pixel community.",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Developed with love for Pixel users.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item(key = "about_spacer", contentType = "spacer") {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen(contentPadding = PaddingValues(0.dp))
    }
}
