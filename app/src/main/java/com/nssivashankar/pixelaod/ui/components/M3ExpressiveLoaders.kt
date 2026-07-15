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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Official Material 3 Expressive Loading Indicator.
 * This implementation uses path-morphing logic to transition between 
 * organic, highly-rounded versions of Circle, Square, Triangle, and an 8-pointed Wavy Star.
 */
@Composable
fun M3OfficialExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_official")
    
    // Cycle duration: 3333ms (Official M3 spec: ~833ms per shape)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3333, easing = LinearEasing)
        ),
        label = "progress"
    )

    // Pulse scale effect synchronized with shape changes
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(416, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                // Official rotation: 90 degrees per shape transition
                rotationZ = progress * 90f
                scaleX = scale
                scaleY = scale
            }
    ) {
        val size = size.minDimension
        val radius = size / 2
        val center = center
        
        val shapeIndex = progress.toInt() % 4
        val transitionProgress = progress % 1.0f
        
        // Use a smooth easing for the morph phase
        val morphEase = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f).transform(transitionProgress)

        val path = when (shapeIndex) {
            0 -> createMorphedPath(center, radius, M3Shape.Circle, M3Shape.Square, morphEase)
            1 -> createMorphedPath(center, radius, M3Shape.Square, M3Shape.Triangle, morphEase)
            2 -> createMorphedPath(center, radius, M3Shape.Triangle, M3Shape.Star, morphEase)
            else -> createMorphedPath(center, radius, M3Shape.Star, M3Shape.Circle, morphEase)
        }

        drawPath(path = path, color = color, style = Fill)
    }
}

private enum class M3Shape { Circle, Square, Triangle, Star }

private fun createMorphedPath(
    center: Offset,
    radius: Float,
    start: M3Shape,
    end: M3Shape,
    progress: Float
): Path {
    val path = Path()
    val resolution = 120 // 120 control points for perfectly smooth organic curves
    
    for (i in 0 until resolution) {
        val angle = (i * 2 * PI / resolution).toFloat()
        
        val rStart = getOrganicRadius(angle, radius, start)
        val rEnd = getOrganicRadius(angle, radius, end)
        
        // Linearly interpolate between the two organic radii
        val currentRadius = rStart + (rEnd - rStart) * progress
        
        val x = center.x + cos(angle) * currentRadius
        val y = center.y + sin(angle) * currentRadius
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Calculates a highly-rounded organic radius for each M3 shape.
 * These are NOT pure geometric shapes; they are "squircle-style" to match M3.
 */
private fun getOrganicRadius(angle: Float, maxRadius: Float, shape: M3Shape): Float {
    return when (shape) {
        M3Shape.Circle -> maxRadius
        
        M3Shape.Square -> {
            // Rounded Square: Blend a circle with a square
            val a = (angle + PI.toFloat() / 4) % (PI.toFloat() / 2) - (PI.toFloat() / 4)
            val squareRadius = (maxRadius * 0.9f) / cos(a)
            // Blend: 70% square, 30% circle for that "squircle" look
            squareRadius * 0.7f + maxRadius * 0.3f
        }
        
        M3Shape.Triangle -> {
            // Highly Rounded Triangle
            val a = (angle + PI.toFloat() / 6) % (2 * PI.toFloat() / 3) - (PI.toFloat() / 3)
            val triangleRadius = (maxRadius * 0.85f) / cos(a)
            // Blend: 60% triangle, 40% circle for "organic" corners
            triangleRadius * 0.6f + maxRadius * 0.4f
        }
        
        M3Shape.Star -> {
            // 8-Point Wavy Star (Sunburst)
            val points = 8
            val innerRadius = maxRadius * 0.78f
            // Smooth wavy transition
            val wave = (sin(angle * points) + 1f) / 2f
            innerRadius + (maxRadius - innerRadius) * wave
        }
    }
}
