package com.nssivashankar.pixelaod.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nssivashankar.pixelaod.data.AppRepository
import com.nssivashankar.pixelaod.data.CachedAppInfo
import com.nssivashankar.pixelaod.ui.components.M3OfficialExpressiveLoader
import com.nssivashankar.pixelaod.ui.theme.iosTouchFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppListDialog(
    title: String,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<CachedAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val currentSelected = remember { 
        mutableStateListOf<String>().apply { addAll(selectedPackages) } 
    }

    // --- High-Efficiency Initial Load ---
    LaunchedEffect(Unit) {
        val apps = AppRepository.getInstalledApps(context)
        // Sort with selected apps first
        val sortedApps = apps.sortedWith(
            compareByDescending<CachedAppInfo> { selectedPackages.contains(it.packageName) }
                .thenBy { it.label.lowercase() }
        )
        allApps = sortedApps
        isLoading = false
    }

    // --- Performance: Use derivedStateOf for heavy list filtering ---
    val filteredApps by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) allApps
            else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
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
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            M3OfficialExpressiveLoader(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Loading apps...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = filteredApps,
                                key = { it.packageName },
                                contentType = { "app_item" } // Optimization for list recycling
                            ) { app ->
                                val isSelected = currentSelected.contains(app.packageName)
                                val onToggleApp = remember(app.packageName, isSelected) {
                                    {
                                        if (isSelected) {
                                            currentSelected.remove(app.packageName)
                                        } else {
                                            currentSelected.add(app.packageName)
                                        }
                                        Unit
                                    }
                                }
                                AppListItem(
                                    app = app,
                                    isSelected = isSelected,
                                    onToggle = onToggleApp
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
    app: CachedAppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    
    // Performance: produceState with key guarantees non-blocking async icon load
    // and automatically cancels pending icon decodes if item scrolls off screen
    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = AppRepository.getCachedIcon(app.packageName),
        key1 = app.packageName
    ) {
        if (value == null) {
            value = AppRepository.getIcon(context, app.appInfo)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .iosTouchFeedback(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = iconBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
    }
}
