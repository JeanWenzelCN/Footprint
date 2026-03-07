package com.footprint.ui.effects

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlin.random.Random

/**
 * A container that applies a realistic "Glassmorphism" effect.
 *
 * Features:
 * - Real-time background blur (using Haze)
 * - Noise texture overlay for material feel
 * - Specular highlight border (simulating light edge)
 * - Surface reflection gradient (simulating curved/shiny surface)
 * - Inner glow/shadow for depth
 */
@Composable
fun GlassCard(
        modifier: Modifier = Modifier,
        hazeState: HazeState?,
        shape: Shape = RoundedCornerShape(24.dp),
        backgroundColor: Color =
                Color.White.copy(alpha = 0.08f), // Lower opacity for better see-through
        borderWidth: Dp = 1.dp,
        borderColor: Color = Color.White.copy(alpha = 0.4f),
        noiseOpacity: Float = 0.03f, // Subtler noise
        content: @Composable BoxScope.() -> Unit
) {
    Box(
            modifier =
                    modifier.glassBorder(width = borderWidth, color = borderColor, shape = shape)
                            .then(
                                    if (hazeState != null) {
                                        Modifier.hazeChild(state = hazeState, shape = shape)
                                    } else {
                                        Modifier // Fallback if no haze state
                                    }
                            )
                            .clip(shape)
                            .background(backgroundColor) // Base tint
                            .glassReflection() // Add surface reflection
    ) {
        // Noise Texture Layer
        Box(modifier = Modifier.matchParentSize().noiseTexture(opacity = noiseOpacity))

        // Content
        content()
    }
}

/**
 * Modifier to draw a noise texture overlay. Uses a cached small bitmap tiled across the surface.
 */
fun Modifier.noiseTexture(opacity: Float = 0.05f): Modifier =
        this.then(NoiseModifierElement(opacity))

private data class NoiseModifierElement(val opacity: Float) :
        ModifierNodeElement<NoiseModifierNode>() {
    override fun create(): NoiseModifierNode = NoiseModifierNode(opacity)

    override fun update(node: NoiseModifierNode) {
        node.opacity = opacity
    }
}

private class NoiseModifierNode(var opacity: Float) : DrawModifierNode, Modifier.Node() {

    // Cache the noise bitmap
    private val noiseBitmap: ImageBitmap by lazy {
        generateNoiseBitmap(size = 64) // 64x64 generic noise tile
    }

    private val paint =
            Paint().apply {
                this.asFrameworkPaint().apply {
                    isAntiAlias = true
                    alpha = (this@NoiseModifierNode.opacity * 255).toInt()
                }
            }

    override fun ContentDrawScope.draw() {
        drawContent() // Draw actual content first

        // Draw noise overlay
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val androidPaint = paint.asFrameworkPaint()
            androidPaint.alpha = (opacity * 255).toInt()

            // Use a shader to tile the bitmap
            val shader =
                    android.graphics.BitmapShader(
                            noiseBitmap.asAndroidBitmap(),
                            android.graphics.Shader.TileMode.REPEAT,
                            android.graphics.Shader.TileMode.REPEAT
                    )
            androidPaint.shader = shader
            nativeCanvas.drawRect(0f, 0f, size.width, size.height, androidPaint)
        }
    }
}

/** Generates a grayscale noise bitmap. */
private fun generateNoiseBitmap(size: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(size * size)
    val random = Random(System.currentTimeMillis())

    for (i in pixels.indices) {
        // Random grayscale value
        val gray = random.nextInt(256)
        // Set alpha to 255, we control opacity via paint
        pixels[i] = AndroidColor.argb(255, gray, gray, gray)
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/**
 * Modifier to apply a gradient border that simulates light reflection (specular highlight).
 * Top-Left is brighter (light source), Bottom-Right is subtle.
 */
fun Modifier.glassBorder(width: Dp, color: Color, shape: Shape): Modifier {
    // Custom gradient for "Glass" look
    // Simulates a light source from Top-Left
    val brush =
            Brush.linearGradient(
                    0.0f to color.copy(alpha = 0.7f), // Strong highlight top-left
                    0.15f to color.copy(alpha = 0.2f), // Fade out quickly
                    0.5f to color.copy(alpha = 0.05f), // Almost transparent middle
                    0.85f to color.copy(alpha = 0.2f), // Slight return
                    1.0f to color.copy(alpha = 0.5f), // Bottom-right rim light
                    start = Offset.Zero,
                    end = Offset.Infinite
            )

    return this.border(width = width, brush = brush, shape = shape)
}

/**
 * Modifier that adds a subtle linear gradient to simulate a reflection on the glass surface. This
 * gives the "bulge" or "sheen" look often seen in iOS glassmorphism.
 */
fun Modifier.glassReflection(): Modifier {
    val reflectionBrush =
            Brush.linearGradient(
                    colors =
                            listOf(
                                    Color.White.copy(alpha = 0.15f), // Top-left shine
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent, // Fades out to nothing
                                    Color.Transparent
                            ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )

    return this.background(brush = reflectionBrush)
}
