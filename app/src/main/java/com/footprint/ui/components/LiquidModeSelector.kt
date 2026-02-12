package com.footprint.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.footprint.ui.effects.GlassCard
import com.footprint.ui.effects.LIQUID_SHADER
import com.footprint.ui.effects.bouncyClick
import com.footprint.ui.effects.liquidRenderEffect
import com.footprint.ui.screens.MapMode
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun LiquidModeSelector(
        modifier: Modifier = Modifier,
        hazeState: HazeState?,
        selectedMode: MapMode,
        onModeSelected: (MapMode) -> Unit
) {
        val modes =
                listOf(
                        MapMode.STANDARD to Icons.Outlined.Layers,
                        MapMode.FOG to Icons.Outlined.Cloud,
                        MapMode.HEATMAP to Icons.Outlined.Whatshot,
                        MapMode.CAPSULE to Icons.Outlined.Inventory2
                )
        val selectedIndex = modes.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)

        Box(modifier = modifier.width(210.dp).height(56.dp), contentAlignment = Alignment.Center) {
                // Layer 1: Glass Background with Liquid Refraction
                GlassCard(
                        modifier =
                                Modifier.matchParentSize()
                                        .then(
                                                if (Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.TIRAMISU
                                                ) {
                                                        Modifier.liquidSelectorRefraction(
                                                                modes,
                                                                selectedIndex
                                                        )
                                                } else {
                                                        Modifier
                                                }
                                        ),
                        hazeState = hazeState,
                        shape = CircleShape,
                        backgroundColor = Color.White.copy(alpha = 0.05f),
                        noiseOpacity = 0.05f
                ) {
                        // Legacy Fallback for Android 12 and below
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Box(modifier = Modifier.fillMaxSize().liquidRenderEffect()) {
                                        LiquidSelectorLayout(modes.size, selectedIndex)
                                }
                                LiquidSelectorGloss(modes.size, selectedIndex)
                        }
                }

                // Layer 2: Icons
                Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        modes.forEachIndexed { index, (mode, icon) ->
                                val isSelected = index == selectedIndex
                                Box(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(56.dp)
                                                        .clip(CircleShape)
                                                        .bouncyClick()
                                                        .clickable { onModeSelected(mode) },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = icon,
                                                contentDescription = mode.label,
                                                tint =
                                                        if (isSelected)
                                                                MaterialTheme.colorScheme.onPrimary
                                                        else
                                                                MaterialTheme.colorScheme.onSurface
                                                                        .copy(alpha = 0.7f),
                                                modifier = Modifier.size(22.dp)
                                        )
                                }
                        }
                }
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.liquidSelectorRefraction(
        modes: List<Pair<MapMode, ImageVector>>,
        selectedIndex: Int
): Modifier {
        val density = LocalDensity.current
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
                                        spring(
                                                dampingRatio = 0.45f,
                                                stiffness = Spring.StiffnessLow
                                        )
                        ) {
                                val stretchValue = (1f + abs(velocity) / 1000f).coerceIn(1f, 1.8f)
                                launch { stretch.snapTo(stretchValue) }
                        }
                        launch {
                                squash.animateTo(
                                        0.75f,
                                        spring(
                                                dampingRatio = 0.2f,
                                                stiffness = Spring.StiffnessMedium
                                        )
                                )
                                squash.animateTo(
                                        1f,
                                        spring(
                                                dampingRatio = 0.4f,
                                                stiffness = Spring.StiffnessMedium
                                        )
                                )
                        }
                }
        }

        val shader = remember { RuntimeShader(LIQUID_SHADER) }
        val infiniteTransition = rememberInfiniteTransition(label = "LiquidBreath")
        val time by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 6.28f,
                        animationSpec =
                                infiniteRepeatable(
                                        tween(4000, easing = LinearEasing),
                                        RepeatMode.Restart
                                ),
                        label = "Time"
                )

        return this.graphicsLayer {
                val w = size.width
                val h = size.height
                if (modes.isNotEmpty() && w > 0 && h > 0) {
                        val slotWidth = w / modes.size
                        val activeBlobSize = with(density) { 48.dp.toPx() } / 2f
                        val anchorSize = with(density) { 20.dp.toPx() } / 2f

                        shader.setFloatUniform("uResolution", w, h)
                        shader.setFloatUniform("uTime", time)
                        shader.setFloatUniform("uColor", 1f, 1f, 1f, 0.25f)
                        shader.setFloatUniform("uSmoothness", 55f)
                        shader.setFloatUniform("uScaleX", stretch.value * squash.value)
                        shader.setFloatUniform("uScaleY", (1f / stretch.value) * squash.value)

                        val blobCoords = FloatArray(12)
                        val blobRadii = FloatArray(6)

                        for (i in 0 until modes.size.coerceAtMost(5)) {
                                val cx = (slotWidth * i) + (slotWidth / 2)
                                blobCoords[i * 2] = cx
                                blobCoords[i * 2 + 1] = h / 2
                                blobRadii[i] = anchorSize
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

@Composable
private fun LiquidSelectorLayout(itemCount: Int, selectedIndex: Int) {
        val animatedIndex by
                animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec =
                                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                        label = "LiquidMove"
                )

        Canvas(modifier = Modifier.fillMaxSize()) {
                val slotWidth = size.width / itemCount
                val staticBlobRadius = 20.dp.toPx() / 2
                val movingBlobRadius = 48.dp.toPx() / 2
                val cy = size.height / 2

                for (i in 0 until itemCount) {
                        val cx = (slotWidth * i) + (slotWidth / 2)
                        drawCircle(
                                Color.White.copy(alpha = 0.01f),
                                radius = staticBlobRadius,
                                center = Offset(cx, cy)
                        )
                }

                val targetX = (slotWidth * animatedIndex) + (slotWidth / 2)
                drawCircle(
                        Color.White.copy(alpha = 0.01f),
                        radius = movingBlobRadius,
                        center = Offset(targetX, cy)
                )
        }
}

@Composable
private fun LiquidSelectorGloss(itemCount: Int, selectedIndex: Int) {
        val animatedIndex by
                animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec =
                                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                        label = "LiquidMove"
                )

        Canvas(modifier = Modifier.fillMaxSize()) {
                val slotWidth = size.width / itemCount
                val blobSize = 48.dp.toPx()
                val cy = size.height / 2
                val targetX = (slotWidth * animatedIndex) + (slotWidth / 2)

                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        Color.White.copy(alpha = 0.8f),
                                                        Color.Transparent
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
