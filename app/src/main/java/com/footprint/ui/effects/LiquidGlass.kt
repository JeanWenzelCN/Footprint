package com.footprint.ui.effects

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.asAndroidBitmap
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
import androidx.compose.ui.graphics.TileMode
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
 * A container that applies the "Glassmorphism" effect:
 * - Real-time background blur (using Haze)
 * - Noise texture overlay
 * - Specular highlight border
 * - Tinting
 */
@Composable
fun GlassCard(
        modifier: Modifier = Modifier,
        hazeState: HazeState,
        shape: Shape = RoundedCornerShape(24.dp),
        backgroundColor: Color = Color.White.copy(alpha = 0.2f),
        borderWidth: Dp = 1.dp,
        borderColor: Color = Color.White.copy(alpha = 0.5f),
        noiseOpacity: Float = 0.05f,
        blurRadius: Dp = 20.dp,
        content: @Composable BoxScope.() -> Unit
) {
    Box(
            modifier =
                    modifier.glassBorder(
                                    width = borderWidth,
                                    color = borderColor, // Fallback solid color, but we prefer
                                    // gradient usually
                                    shape = shape
                            )
                            .hazeChild(
                                    state = hazeState,
                                    shape = shape,
                                    // Haze doesn't support blur radius config directly in hazeChild
                                    // yet in some versions,
                                    // but usually it mimics the background blur.
                                    // We rely on the global HazeState configuration or defaults.
                                    )
                            // Clip content to shape, but removed noise here to avoid covering content
                            .clip(shape) 
    ) {
        // Tint layer (drawn on top of blur, behind content)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundColor, shape)
                // Add Noise Texture here so it is behind the content
                .noiseTexture(opacity = noiseOpacity)
        )
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
            // Use Overlay or SoftLight blend mode if possible, but standard SrcOver with low alpha
            // works for generic noise
            // Android Canvas BlendModes require API 29+ for some.
            // For simple noise, normal blending with low alpha is usually sufficient "film grain"
            // look.

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
 * Top-left is brighter (light source), Bottom-right is subtler.
 */
fun Modifier.glassBorder(width: Dp, color: Color, shape: Shape): Modifier {
    // Custom gradient for "Glass" look
    val brush =
            Brush.linearGradient(
                    0f to color.copy(alpha = 0.7f), // Top-Left Highlight
                    0.5f to color.copy(alpha = 0.1f), // Middle transparent
                    1f to color.copy(alpha = 0.3f), // Bottom-Right Refraction
                    start = Offset.Zero,
                    end = Offset.Infinite // Uses DrawScope size basically, mimics diagonal
            )

    return this.border(width = width, brush = brush, shape = shape)
}
