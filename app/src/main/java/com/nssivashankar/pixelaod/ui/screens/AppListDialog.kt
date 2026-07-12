package com.nssivashankar.pixelaod.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    
    // Using SnapshotStateList for reliable recomposition of selections
    val currentSelected = remember { 
        mutableStateListOf<String>().apply { addAll(selectedPackages) } 
    }

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
                .sortedBy { it.label.lowercase() }
                .toList()
            allApps = apps
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
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
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
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
    var iconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            val drawable = pm.getApplicationIcon(app.appInfo)
            iconBitmap = drawable.toBitmap().asImageBitmap()
        }
    }

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp)) {
                iconBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center), strokeWidth = 2.dp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = null // Handled by Row click
            )
        }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val appInfo: android.content.pm.ApplicationInfo
)
