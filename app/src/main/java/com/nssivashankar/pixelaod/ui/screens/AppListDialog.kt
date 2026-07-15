package com.nssivashankar.pixelaod.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.nssivashankar.pixelaod.ui.components.M3OfficialExpressiveLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// Global cache for icons with thread-safe access
private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
fun AppListDialog(
    title: String,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val currentSelected = remember { 
        mutableStateListOf<String>().apply { addAll(selectedPackages) } 
    }

    // --- Forced Pre-Warm Loader (Zero-Stutter Engineering) ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        appInfo = appInfo
                    )
                }
                .sortedWith(compareByDescending<AppInfo> { selectedPackages.contains(it.packageName) }
                    .thenBy { it.label.lowercase() })
                .toList()
            
            // Pre-warm top 30 icons for instant smoothness
            apps.take(30).forEach { app ->
                if (!iconCache.containsKey(app.packageName)) {
                    try {
                        val drawable = pm.getApplicationIcon(app.appInfo)
                        val bitmap = drawable.toBitmap(width = 100, height = 100).asImageBitmap()
                        iconCache[app.packageName] = bitmap
                    } catch (e: Exception) {}
                }
            }
            
            // Show the animation for a consistent duration
            delay(3200)

            withContext(Dispatchers.Main) {
                allApps = apps
                isLoading = false
            }

            // Background warming for the rest
            apps.drop(30).forEach { app ->
                if (!iconCache.containsKey(app.packageName)) {
                    try {
                        val drawable = pm.getApplicationIcon(app.appInfo)
                        val bitmap = drawable.toBitmap(width = 100, height = 100).asImageBitmap()
                        iconCache[app.packageName] = bitmap
                    } catch (e: Exception) {}
                }
            }
        }
    }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    // ISOLATED LOADING CONTAINER
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // CLEAN ISOLATED LOADER
                            M3OfficialExpressiveLoader(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Building smooth list...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppListItem(
                                    app = app,
                                    isSelected = currentSelected.contains(app.packageName),
                                    pm = pm,
                                    onToggle = {
                                        if (currentSelected.contains(app.packageName)) {
                                            currentSelected.remove(app.packageName)
                                        } else {
                                            currentSelected.add(app.packageName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentSelected.toSet()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    pm: PackageManager,
    onToggle: () -> Unit
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf(iconCache[app.packageName]) }
    
    if (iconBitmap == null) {
        LaunchedEffect(app.packageName) {
            withContext(Dispatchers.IO) {
                try {
                    val drawable = pm.getApplicationIcon(app.appInfo)
                    val bitmap = drawable.toBitmap(width = 100, height = 100).asImageBitmap()
                    iconCache[app.packageName] = bitmap
                    withContext(Dispatchers.Main) {
                        iconBitmap = bitmap
                    }
                } catch (e: Exception) {}
            }
        }
    }

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp)) {
                iconBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val appInfo: android.content.pm.ApplicationInfo
)
