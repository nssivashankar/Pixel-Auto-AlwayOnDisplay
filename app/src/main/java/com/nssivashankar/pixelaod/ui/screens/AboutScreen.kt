package com.nssivashankar.pixelaod.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nssivashankar.pixelaod.ui.components.M3OfficialExpressiveLoader
import com.nssivashankar.pixelaod.utils.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(contentPadding: PaddingValues) {
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
            item {
                Spacer(Modifier.height(32.dp))
                // App Logo
                Box(
                    modifier = Modifier
                        .size(110.dp) // Slightly larger for better presence
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
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

            item {
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

            item {
                PreferenceItem(
                    title = "Check for Updates",
                    summary = if (isCheckingUpdates) "Checking backend..." else "Manually verify latest version",
                    icon = Icons.Default.Update,
                    onClick = {
                        if (!isCheckingUpdates) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                isCheckingUpdates = true
                                delay(2000)
                                UpdateChecker.checkForUpdates(context, currentVersion, isManual = true) { latest, notes, url ->
                                    UpdateChecker.showUpdateDialog(context, latest, notes, url)
                                }
                                isCheckingUpdates = false
                            }
                        }
                    }
                )
                
                // NEW: Official M3 Expressive Morphing Loader for Updates
                if (isCheckingUpdates) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        M3OfficialExpressiveLoader(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
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

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
