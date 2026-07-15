package com.nssivashankar.pixelaod.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Official Material 3 Expressive Loading Indicator.
 * Uses the experimental Material 3 APIs and Graphics Shapes library.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3OfficialExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    ContainedLoadingIndicator(
        modifier = modifier.size(64.dp),
        indicatorColor = color,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
    )
}

/**
 * Basic Loading Indicator without a container.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3BasicExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LoadingIndicator(
        modifier = modifier,
        color = color
    )
}
