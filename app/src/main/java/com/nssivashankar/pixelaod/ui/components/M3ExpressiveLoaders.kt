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
import kotlin.math.*

/**
 * A High-Fidelity implementation of the Material 3 Expressive Loading Indicator.
 * Strictly follows the organic morphing and sequencing seen in the M3 documentation.
 * Shapes: Circle -> Rounded Square -> Rounded Triangle -> 8-Point Wavy Star.
 */
@Composable
fun M3OfficialExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_organic")
    
    // Official cycle: ~3333ms total (4 shapes * 833ms)
    val totalProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3333, easing = LinearEasing)
        ),
        label = "total_progress"
    )

    // Staggered pulsing: Organic scale contraction during morph
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(416, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val center = center
        
        val shapeIndex = totalProgress.toInt() % 4
        val transitionProgress = totalProgress % 1.0f
        
        // Custom Easing for the morph phase to make it look "organic/elastic"
        val easedProgress = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f).transform(transitionProgress)

        val currentPath = when (shapeIndex) {
            0 -> morphOrganic(center, radius, ShapeType.Circle, ShapeType.Square, easedProgress)
            1 -> morphOrganic(center, radius, ShapeType.Square, ShapeType.Triangle, easedProgress)
            2 -> morphOrganic(center, radius, ShapeType.Triangle, ShapeType.Star, easedProgress)
            else -> morphOrganic(center, radius, ShapeType.Star, ShapeType.Circle, easedProgress)
        }
        
        // Organic rotation: Slows down at shape arrival, accelerates during transition
        val rotationAngle = (totalProgress * 90f)
        
        rotate(rotationAngle) {
            scale(scale = pulseScale) {
                drawPath(path = currentPath, color = color, style = Fill)
            }
        }
    }
}

private enum class ShapeType { Circle, Square, Triangle, Star }

private fun morphOrganic(
    center: Offset, 
    radius: Float, 
    start: ShapeType, 
    end: ShapeType, 
    progress: Float
): Path {
    val path = Path()
    val resolution = 180 // Higher resolution for Bezier-like smoothness
    
    for (i in 0 until resolution) {
        val angle = (i * 2 * PI / resolution).toFloat()
        val rStart = getOrganicRadius(angle, radius, start)
        val rEnd = getOrganicRadius(angle, radius, end)
        
        val currentR = rStart + (rEnd - rStart) * progress
        val x = center.x + cos(angle) * currentR
        val y = center.y + sin(angle) * currentR
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun getOrganicRadius(angle: Float, maxRadius: Float, type: ShapeType): Float {
    return when (type) {
        ShapeType.Circle -> maxRadius
        
        ShapeType.Square -> {
            // Rounded Square logic: Interpolate between circle and pure square
            val a = (angle + PI.toFloat() / 4) % (PI.toFloat() / 2) - (PI.toFloat() / 4)
            val squareR = (maxRadius * 0.95f) / cos(a)
            // Blend with circle for organic rounded look (85% square, 15% circle)
            squareR * 0.85f + maxRadius * 0.15f
        }
        
        ShapeType.Triangle -> {
            // Organic Triangle: Large corner radii
            val a = (angle + PI.toFloat() / 6) % (2 * PI.toFloat() / 3) - (PI.toFloat() / 3)
            val triR = (maxRadius * 0.85f) / cos(a)
            // High blend for extremely soft corners
            triR * 0.7f + maxRadius * 0.3f
        }
        
        ShapeType.Star -> {
            // M3 Documentation Star: 8-point wavy sunburst
            val points = 8
            val innerRadius = maxRadius * 0.75f
            val wave = sin(angle * points) * 0.5f + 0.5f
            innerRadius + (maxRadius - innerRadius) * wave
        }
    }
}
