package com.footprint.ui.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * An advanced Liquid Navigation Bar using Metaballs for the selection indicator and Refraction
 * effects for a "water globule" look.
 */
@Composable
fun LiquidNavigationBar(
        modifier: Modifier = Modifier,
        hazeState: HazeState,
        items: List<LiquidNavItem>,
        selectedIndex: Int,
        onItemSelected: (Int) -> Unit
) {
        Box(modifier = modifier) {
                // Layer 1: Glass Background (Refracted/Distorted)
                // We separate this so the icons on top are NOT distorted by the liquid effect.
                GlassCard(
                        modifier =
                                Modifier.matchParentSize()
                                        .then(
                                                if (Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.TIRAMISU
                                                ) {
                                                        Modifier.liquidRefraction(
                                                                items,
                                                                selectedIndex
                                                        )
                                                } else {
                                                        Modifier
                                                }
                                        ),
                        hazeState = hazeState,
                        shape = CircleShape,
                        backgroundColor = Color.White.copy(alpha = 0.0f),
                        noiseOpacity = 0.05f
                ) {
                        // Check for Legacy Fallback (Android 12 and below)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                // Universal High-Fidelity Rendering (Metaball Threshold) for older
                                // devices
                                Box(modifier = Modifier.fillMaxSize().liquidRenderEffect()) {
                                        LiquidNavLayout(
                                                items = items,
                                                selectedIndex = selectedIndex
                                        )
                                }
                                LiquidRefractionLayer(items = items, selectedIndex = selectedIndex)
                        }
                }

                // Layer 2: Icons (Always on top, undistorted)
                LiquidNavIcons(
                        items = items,
                        selectedIndex = selectedIndex,
                        onItemSelected = onItemSelected
                )
        }
}

data class LiquidNavItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Modifier that applies the Advanced Liquid Refraction Shader (AGSL). Only for Android 13+
 * (Tiramisu).
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Modifier.liquidRefraction(items: List<LiquidNavItem>, selectedIndex: Int): Modifier {
        val density = LocalDensity.current

        // Animation State
        val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }
        val stretch = remember { Animatable(1f) }
        val squash = remember { Animatable(1f) }

        LaunchedEffect(selectedIndex) {
                launch { stretch.snapTo(1f) }
                launch { squash.snapTo(1f) }

                launch {
                        animatedIndex.animateTo(
                                targetValue = selectedIndex.toFloat(),
                                animationSpec =
                                        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)
                        ) {
                                val stretchValue = (1f + abs(velocity) / 1000f).coerceIn(1f, 2.0f)
                                launch { stretch.snapTo(stretchValue) }
                        }
                        launch {
                                squash.animateTo(
                                        targetValue = 0.7f,
                                        animationSpec =
                                                spring(
                                                        dampingRatio = 0.2f,
                                                        stiffness = Spring.StiffnessMedium
                                                )
                                )
                                squash.animateTo(
                                        targetValue = 1f,
                                        animationSpec =
                                                spring(
                                                        dampingRatio = 0.4f,
                                                        stiffness = Spring.StiffnessMedium
                                                )
                                )
                        }
                }
        }

        // Shader & Time
        val shader = remember { RuntimeShader(LIQUID_SHADER) }
        // Infinite animation for "breathing"
        val infiniteTransition = rememberInfiniteTransition(label = "LiquidBreath")
        val time by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 6.28f, // 2*PI
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(4000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                ),
                        label = "Time"
                )

        return this.graphicsLayer {
                val w = size.width
                val h = size.height
                val itemCount = items.size

                if (itemCount > 0 && w > 0 && h > 0) {
                        val slotWidth = w / itemCount
                        val activeBlobSize = with(density) { 56.dp.toPx() } / 2f
                        val anchorSize = with(density) { 24.dp.toPx() } / 2f

                        shader.setFloatUniform("uResolution", w, h)
                        shader.setFloatUniform("uTime", time)

                        // Tint Color: Visible White Glass (0.3 alpha)
                        val liquidColor = Color.White.copy(alpha = 0.3f)
                        shader.setFloatUniform(
                                "uColor",
                                liquidColor.red,
                                liquidColor.green,
                                liquidColor.blue,
                                liquidColor.alpha
                        )

                        shader.setFloatUniform("uSmoothness", 60f)

                        val scaleX = stretch.value * squash.value
                        val scaleY = (1f / stretch.value) * squash.value
                        shader.setFloatUniform("uScaleX", scaleX)
                        shader.setFloatUniform("uScaleY", scaleY)

                        val blobCoords = FloatArray(12)
                        val blobRadii = FloatArray(6)

                        for (i in 0 until itemCount) {
                                if (i >= 5) break
                                val cx = (slotWidth * i) + (slotWidth / 2)
                                val cy = h / 2
                                blobCoords[i * 2] = cx
                                blobCoords[i * 2 + 1] = cy
                                blobRadii[i] = anchorSize
                        }
                        for (i in itemCount until 5) {
                                blobRadii[i] = 0f
                        }

                        val targetX = (slotWidth * animatedIndex.value) + (slotWidth / 2)
                        blobCoords[10] = targetX
                        blobCoords[11] = h / 2
                        blobRadii[5] = activeBlobSize

                        shader.setFloatUniform("uBlobCoords", blobCoords)
                        shader.setFloatUniform("uRadii", blobRadii)

                        renderEffect =
                                RenderEffect.createRuntimeShaderEffect(shader, "composable")
                                        .asComposeRenderEffect()
                }
        }
}

/** Universal Layout using Blur + Threshold */
@Composable
private fun LiquidNavLayout(items: List<LiquidNavItem>, selectedIndex: Int) {
        // val primaryColor = MaterialTheme.colorScheme.primary // Removed as per request to remove
        // blue background

        val animatedIndex by
                animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec =
                                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                        label = "LiquidMove"
                )

        Canvas(modifier = Modifier.fillMaxSize()) {
                val slotWidth = size.width / items.size
                val staticBlobRadius = 24.dp.toPx() / 2
                val movingBlobRadius = 56.dp.toPx() / 2
                val cy = size.height / 2

                // Draw static anchors for all items
                for (i in items.indices) {
                        val cx = (slotWidth * i) + (slotWidth / 2)
                        drawCircle(
                                color =
                                        Color.White.copy(
                                                alpha = 0.01f
                                        ), // Changed to very low alpha white for liquid effect
                                // source
                                radius = staticBlobRadius,
                                center = Offset(cx, cy)
                        )
                }

                // Draw the moving blob
                val targetX = (slotWidth * animatedIndex) + (slotWidth / 2)
                drawCircle(
                        color =
                                Color.White.copy(
                                        alpha = 0.01f
                                ), // Changed to very low alpha white for liquid effect source
                        radius = movingBlobRadius,
                        center = Offset(targetX, cy)
                )
        }
}

/** Universal Overlay for Gloss */
@Composable
private fun LiquidRefractionLayer(
        modifier: Modifier = Modifier,
        items: List<LiquidNavItem>,
        selectedIndex: Int
) {
        val animatedIndex by
                animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec =
                                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                        label = "LiquidMove"
                )

        Canvas(modifier = modifier.fillMaxSize()) {
                val slotWidth = size.width / items.size
                val blobSize = 56.dp.toPx()
                val cy = size.height / 2
                val targetX = (slotWidth * animatedIndex) + (slotWidth / 2)

                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        Color.White.copy(alpha = 0.95f),
                                                        Color.White.copy(alpha = 0.0f)
                                                ),
                                        center =
                                                Offset(
                                                        targetX - blobSize * 0.15f,
                                                        cy - blobSize * 0.15f
                                                ),
                                        radius = blobSize * 0.25f
                                ),
                        radius = blobSize * 0.25f,
                        center = Offset(targetX - blobSize * 0.15f, cy - blobSize * 0.15f)
                )
        }
}

@Composable
private fun LiquidNavIcons(
        items: List<LiquidNavItem>,
        selectedIndex: Int,
        onItemSelected: (Int) -> Unit
) {
        Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
                items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                                modifier =
                                        Modifier.weight(1f)
                                                .height(64.dp)
                                                .clip(CircleShape)
                                                .bouncyClick()
                                                .clickable { onItemSelected(index) },
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint =
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                }
        }
}

fun Modifier.liquidRenderEffect(): Modifier = composed {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.graphicsLayer {
                        val blurRadius = 40f
                        val blurEffect =
                                RenderEffect.createBlurEffect(
                                        blurRadius,
                                        blurRadius,
                                        Shader.TileMode.DECAL
                                )
                        val alphaMatrix =
                                ColorMatrix(
                                        floatArrayOf(
                                                1f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                80f,
                                                -4000f
                                        )
                                )
                        val thresholdEffect =
                                RenderEffect.createColorFilterEffect(
                                        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                        alphaMatrix
                                                )
                                                .asAndroidColorFilter()
                                )
                        renderEffect =
                                RenderEffect.createChainEffect(thresholdEffect, blurEffect)
                                        .asComposeRenderEffect()
                }
        } else {
                this
        }
}

private fun androidx.compose.ui.graphics.ColorFilter.asAndroidColorFilter():
        android.graphics.ColorFilter {
        return android.graphics.ColorMatrixColorFilter(
                floatArrayOf(
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        80f,
                        -4000f
                )
        )
}
