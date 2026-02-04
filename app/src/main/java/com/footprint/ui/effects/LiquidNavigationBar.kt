package com.footprint.ui.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.footprint.ui.effects.bouncyClick
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        // Layer 1: Glass Background (Blur the app content)
        GlassCard(
                modifier = modifier,
                hazeState = hazeState,
                shape = CircleShape,
                backgroundColor = Color.Transparent,
                noiseOpacity = 0.05f
        ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // High-Fidelity Rendering for API 33+ (AGSL Shader)
                        // This path uses the robust, unrolled AGSL shader for hardware-accelerated
                        // SDF, metaballs, and normal-mapped lighting.
                        LiquidNavShaderLayout(items = items, selectedIndex = selectedIndex)
                } else {
                        // Universal High-Fidelity Rendering (Blur + Threshold + Refraction Overlay)
                        // This fallback uses the "Metaball Threshold" technique (Blur + high-contrast ColorMatrix)
                        // which gives the exact same liquid fusion effect but is 100% stable on all devices/emulators.

                        // 1. Metaball Layer (Fused Geometry)
                        Box(modifier = Modifier.fillMaxSize().liquidRenderEffect()) {
                                LiquidNavLayout(items = items, selectedIndex = selectedIndex)
                        }

                        // 2. Refraction/Gloss Overlay (lighting details)
                        LiquidRefractionLayer(items = items, selectedIndex = selectedIndex)
                }

                // Layer 4: Icons (Always on top)
                LiquidNavIcons(
                        items = items,
                        selectedIndex = selectedIndex,
                        onItemSelected = onItemSelected
                )
        }
}

data class LiquidNavItem(val route: String, val label: String, val icon: ImageVector)

/**
 * ROBUST AGSL Shader implementation for Android 13+. Uses manually unrolled logic and simple arrays
 * to guarantee driver stability.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LiquidNavShaderLayout(items: List<LiquidNavItem>, selectedIndex: Int) {
        val density = LocalDensity.current
        
        val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }
        val stretch = remember { Animatable(1f) }
        val squash = remember { Animatable(1f) }

        LaunchedEffect(selectedIndex) {
                // Reset stretch/squash before starting
                launch { stretch.snapTo(1f) }
                launch { squash.snapTo(1f) }

                launch {
                        animatedIndex.animateTo(
                                targetValue = selectedIndex.toFloat(),
                                animationSpec = spring(
                                        dampingRatio = 0.4f, // More bouncy
                                        stiffness = Spring.StiffnessLow
                                )
                        ) {
                                // Stretch based on velocity
                                val stretchValue = (1f + abs(velocity) / 1000f).coerceIn(1f, 2.0f)
                                launch { stretch.snapTo(stretchValue) }
                        }
                        
                        // Squash and rebound when finished
                        launch {
                                squash.animateTo(
                                        targetValue = 0.7f, // Deeper squash
                                        animationSpec = spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium)
                                )
                                squash.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
                                )
                        }
                }
        }
        
        val shader = remember { RuntimeShader(LIQUID_SHADER) }

        Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val itemCount = items.size
                if (itemCount == 0 || w <= 0f || h <= 0f) return@Canvas

                val slotWidth = w / itemCount
                val activeBlobSize = with(density) { 56.dp.toPx() } / 2f
                val anchorSize = with(density) { 24.dp.toPx() } / 2f

                shader.setFloatUniform("uResolution", w, h)
                val liquidColor = Color.Transparent
                shader.setFloatUniform("uColor", liquidColor.red, liquidColor.green, liquidColor.blue, liquidColor.alpha)
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

                drawRect(brush = ShaderBrush(shader), size = size)
        }
}

/** Universal Layout using Blur + Threshold */
@Composable
private fun LiquidNavLayout(items: List<LiquidNavItem>, selectedIndex: Int) {
    // val primaryColor = MaterialTheme.colorScheme.primary // Removed as per request to remove blue background

    val animatedIndex by
        animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
                        animationSpec =
                                spring(
                                        dampingRatio = 0.6f,
                                        stiffness = Spring.StiffnessLow
                                ),
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
                color = Color.White.copy(alpha = 0.01f), // Changed to very low alpha white for liquid effect source
                radius = staticBlobRadius,
                center = Offset(cx, cy)
            )
        }

        // Draw the moving blob
        val targetX = (slotWidth * animatedIndex) + (slotWidth / 2)
        drawCircle(
            color = Color.White.copy(alpha = 0.01f), // Changed to very low alpha white for liquid effect source
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
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = Spring.StiffnessLow
                                    ),                        label = "LiquidMove"
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
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.3f)
                                                ),
                                        center =
                                                Offset(
                                                        targetX - blobSize * 0.1f,
                                                        cy - blobSize * 0.1f
                                                ),
                                        radius = blobSize * 0.6f
                                ),
                        radius = blobSize * 0.45f,
                        center = Offset(targetX, cy)
                )

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
                        80f,
                        -4000f
                )
        )
}
