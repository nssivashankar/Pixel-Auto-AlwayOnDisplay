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
            
            // Wait for the expressive M3 animation to finish a few cycles
            delay(2000)

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
                            // NEW: Official M3 Expressive Morphing Loader
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

/**
 * A precise implementation of the Material 3 Expressive Loading Indicator
 * following the logic found in LoadingIndicator.java
 */
@Composable
fun M3OfficialExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_official_loader")
    
    // 1. Morphing Shape State (Square -> Circle -> Triangle -> Star)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "morph"
    )

    // 2. Continuous Organic Rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // 3. Pulse Scale Effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .graphicsLayer { 
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    ) {
        val size = size.minDimension
        val center = center
        val radius = size / 2
        
        val shapeIndex = morphProgress.toInt() % 4
        val shapeProgress = morphProgress % 1.0f
        
        val path = when (shapeIndex) {
            0 -> createMorphedPath(center, radius, 4, 100, shapeProgress) // Square to Star-like
            1 -> createMorphedPath(center, radius, 100, 3, shapeProgress) // Circle to Triangle
            2 -> createMorphedPath(center, radius, 3, 10, shapeProgress)  // Triangle to Star
            else -> createMorphedPath(center, radius, 10, 4, shapeProgress) // Star back to Square
        }

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}

private fun createMorphedPath(center: Offset, radius: Float, startPoints: Int, endPoints: Int, progress: Float): Path {
    val path = Path()
    val maxPoints = 120 // High resolution for smooth morphing
    
    for (i in 0 until maxPoints) {
        val angle = (i * 2 * PI / maxPoints).toFloat()
        
        // Calculate radius for start shape
        val rStart = getRadiusForShape(angle, radius, startPoints)
        // Calculate radius for end shape
        val rEnd = getRadiusForShape(angle, radius, endPoints)
        
        // Interpolate radius
        val currentRadius = rStart + (rEnd - rStart) * progress
        
        val x = center.x + cos(angle) * currentRadius
        val y = center.y + sin(angle) * currentRadius
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun getRadiusForShape(angle: Float, maxRadius: Float, points: Int): Float {
    return when (points) {
        100 -> maxRadius // Circle
        4 -> { // Square (Approximate)
            maxRadius * (1.0f / maxOf(abs(cos(angle)), abs(sin(angle)))) * 0.7f
        }
        3 -> { // Triangle (Approximate)
            val a = (2 * PI / 3).toFloat()
            maxRadius * 0.8f * (cos(PI.toFloat()/6) / cos((angle % a) - PI.toFloat()/6))
        }
        else -> { // Star/Sunburst
            val innerRadius = maxRadius * 0.7f
            if ((angle * points / (2 * PI)).toInt() % 2 == 0) maxRadius else innerRadius
        }
    }.coerceAtMost(maxRadius * 1.2f) // Sanity check
}

private fun abs(v: Float) = if (v < 0) -v else v

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
