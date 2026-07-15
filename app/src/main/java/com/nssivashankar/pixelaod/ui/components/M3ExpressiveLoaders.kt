package com.nssivashankar.pixelaod.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
 * Official Material 3 Contained Expressive Loading Indicator.
 * Strictly follows the documentation shapes: Circle, Rounded Square, 
 * Rounded Triangle, and 10-Pointed Star.
 */
@Composable
fun M3OfficialExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            M3ExpressiveAnimation(
                modifier = Modifier.size(34.dp),
                color = color
            )
        }
    }
}

@Composable
private fun M3ExpressiveAnimation(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive")
    
    // Total cycle: 4000ms (1000ms per shape: 500ms idle + 500ms transition)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val size = size.minDimension
        val radius = size / 2
        val center = center
        
        val currentStep = progress % 1.0f 
        val shapeIndex = progress.toInt() % 4
        
        val isTransitioning = currentStep > 0.5f
        val transitionProgress = if (isTransitioning) {
            val raw = (currentStep - 0.5f) * 2f
            FastOutSlowInEasing.transform(raw)
        } else 0f

        val rotationZ = (shapeIndex * 90f) + (transitionProgress * 90f)
        val pulseScale = 1.0f - (sin(transitionProgress * PI.toFloat()) * 0.15f)

        val shapeStart = getShapeType(shapeIndex)
        val shapeEnd = getShapeType((shapeIndex + 1) % 4)

        val path = createMorphedPath(center, radius, shapeStart, shapeEnd, transitionProgress)

        rotate(rotationZ, center) {
            scale(pulseScale, pulseScale, center) {
                drawPath(path = path, color = color, style = Fill)
            }
        }
    }
}

private enum class M3Shape { Circle, Square, Triangle, Star }

private fun getShapeType(index: Int) = when(index) {
    0 -> M3Shape.Circle
    1 -> M3Shape.Square
    2 -> M3Shape.Triangle
    else -> M3Shape.Star
}

private fun createMorphedPath(
    center: Offset,
    radius: Float,
    start: M3Shape,
    end: M3Shape,
    progress: Float
): Path {
    val path = Path()
    val resolution = 180 // Increased resolution for ultra-smooth organic curves
    
    for (i in 0 until resolution) {
        val angle = (i * 2 * PI / resolution).toFloat()
        val rStart = getOrganicRadius(angle, radius, start)
        val rEnd = getOrganicRadius(angle, radius, end)
        
        val currentRadius = rStart + (rEnd - rStart) * progress
        val x = center.x + cos(angle) * currentRadius
        val y = center.y + sin(angle) * currentRadius
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun getOrganicRadius(angle: Float, maxRadius: Float, shape: M3Shape): Float {
    return when (shape) {
        M3Shape.Circle -> maxRadius
        
        M3Shape.Square -> {
            // Rounded Square (Squircle)
            val a = (angle + PI.toFloat() / 4) % (PI.toFloat() / 2) - (PI.toFloat() / 4)
            val squareRadius = (maxRadius * 0.88f) / cos(a)
            // Organic blend to ensure very soft corners
            squareRadius * 0.7f + maxRadius * 0.3f
        }
        
        M3Shape.Triangle -> {
            // Rounded Triangle
            val a = (angle + PI.toFloat() / 6) % (2 * PI.toFloat() / 3) - (PI.toFloat() / 3)
            val triangleRadius = (maxRadius * 0.82f) / cos(a)
            // High blend for "liquid" corners
            triangleRadius * 0.6f + maxRadius * 0.4f
        }
        
        M3Shape.Star -> {
            // Official 10-Pointed Star (Sunburst)
            val points = 10 
            val innerRadius = maxRadius * 0.72f
            // Wavy sunburst logic for rounded peaks
            val wave = (sin(angle * points) + 1f) / 2f
            innerRadius + (maxRadius - innerRadius) * wave
        }
    }
}
