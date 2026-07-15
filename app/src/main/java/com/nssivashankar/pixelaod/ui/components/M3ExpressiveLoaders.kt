package com.nssivashankar.pixelaod.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Implements the official Material 3 Expressive Loading Indicator "Sequencing" logic.
 * Cycles through shapes (Circle, Square, Triangle, Star) with organic transitions.
 */
@Composable
fun M3OfficialExpressiveLoader(
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
            (maxRadius * 0.95f) / cos(a.toDouble()).toFloat()
        }
        3 -> { // Triangle
            val a = (angle + PI / 6) % (2 * PI / 3) - (PI / 3)
            (maxRadius * 0.85f) / cos(a.toDouble()).toFloat()
        }
        10 -> { // 10-pointed Star
            val isPeak = (angle * 10 / (2 * PI)).toInt() % 2 == 0
            if (isPeak) maxRadius else maxRadius * 0.75f
        }
        else -> maxRadius
    }.coerceAtMost(maxRadius * 1.1f)
}
