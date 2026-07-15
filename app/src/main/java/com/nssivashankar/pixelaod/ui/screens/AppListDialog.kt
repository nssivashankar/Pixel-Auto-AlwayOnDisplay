package com.nssivashankar.pixelaod.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
            
            // Show the official sequencing animation for at least 1 full cycle (3.2s)
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
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // NEW: Official M3 Expressive Sequencing Loader
                            M3SequencingExpressiveLoader(
                                modifier = Modifier.size(48.dp),
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

/**
 * Implements the official Material 3 Expressive Loading Indicator "Sequencing" logic.
 * Cycles through shapes (Circle, Square, Triangle, Star) with organic transitions.
 */
@Composable
fun M3SequencingExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_sequencing")
    
    // Cycle duration is 800ms per shape transition (4 shapes * 800ms = 3.2s total)
    val totalProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing)
        ),
        label = "total_progress"
    )

    // Scaling/pulsing follows a specific staggered timing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val center = center
        
        val shapeIndex = totalProgress.toInt() % 4
        val transitionProgress = totalProgress % 1.0f
        
        val currentPath = when (shapeIndex) {
            0 -> morph(center, radius, 100, 4, transitionProgress)
            1 -> morph(center, radius, 4, 3, transitionProgress)
            2 -> morph(center, radius, 3, 10, transitionProgress)
            else -> morph(center, radius, 10, 100, transitionProgress)
        }
        
        val rotationAngle = (totalProgress * 90f)
        
        rotate(rotationAngle) {
            scale(scale = pulseScale) {
                drawPath(path = currentPath, color = color, style = Fill)
            }
        }
    }
}

private fun morph(center: Offset, radius: Float, startPoints: Int, endPoints: Int, progress: Float): Path {
    val path = Path()
    val resolution = 120
    
    for (i in 0 until resolution) {
        val angle = (i * 2 * PI / resolution).toFloat()
        val rStart = getRadiusForShape(angle, radius, startPoints)
        val rEnd = getRadiusForShape(angle, radius, endPoints)
        
        val currentR = rStart + (rEnd - rStart) * progress
        val x = center.x + cos(angle) * currentR
        val y = center.y + sin(angle) * currentR
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun getRadiusForShape(angle: Float, maxRadius: Float, points: Int): Float {
    return when (points) {
        100 -> maxRadius // Circle
        4 -> { // Square
            val a = (angle + PI / 4) % (PI / 2) - (PI / 4)
            (maxRadius * 0.95f) / cos(a).toFloat()
        }
        3 -> { // Triangle
            val a = (angle + PI / 6) % (2 * PI / 3) - (PI / 3)
            (maxRadius * 0.85f) / cos(a).toFloat()
        }
        10 -> { // 10-pointed Star
            val isPeak = (angle * 10 / (2 * PI)).toInt() % 2 == 0
            if (isPeak) maxRadius else maxRadius * 0.75f
        }
        else -> maxRadius
    }.coerceAtMost(maxRadius * 1.1f)
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
