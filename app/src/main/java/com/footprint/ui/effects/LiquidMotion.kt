package com.footprint.ui.effects

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A container that applies a threshold filter to create "Metaball" (liquid) effects.
 *
 * How it works:
 * 1. It blurs the content (RenderEffect.createBlurEffect).
 * 2. It applies a ColorMatrix that boosts Alpha contrast (Thresholding).
 * ```
 *    Alpha = Alpha * 18 - 7 (example values).
 *    This causes blurred edges to become sharp again, but where two blurred shapes overlap,
 *    their alphas add up and "snap" together before the threshold cut-off.
 * ```
 * Note: Requires API 31+ (Android 12) for RenderEffect. Fallback: Standard container (no special
 * liquid effect).
 */
@Composable
fun MetaballBox(
        modifier: Modifier = Modifier,
        blurRadius: Float = 40f,
        alphaCutoff: Float =
                0.5f, // Not strictly used in matrix calc directly here, hardcoded for now
        content: @Composable BoxScope.() -> Unit
) {
    // 1. Define the Blur Effect
    // 2. Define the ColorMatrix (Threshold)
    // 3. Chain them: Blur -> Matrix

    val renderEffect =
            remember(blurRadius) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurEffect =
                            RenderEffect.createBlurEffect(
                                    blurRadius,
                                    blurRadius,
                                    Shader.TileMode.DECAL
                            )

                    // The "Gooey" Matrix
                    // We want to take existing alpha, multiply it by a large factor,
                    // and subtract a large constant.
                    // Result:
                    // - Low alpha (edges of blur) becomes < 0 (transparent)
                    // - High alpha (center) stays > 1 (opaque)
                    // - Overlapping blurred regions sum up and cross the threshold together

                    val matrix =
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
                                            60f,
                                            -3000f / (255f) // Optimized experimental values.
                                            // e.g. Alpha * 60 - 11.
                                            // Careful calibration needed. If too aggressive, shapes
                                            // disappear.
                                            )
                            )
                    // Adjusting constants for smoother result:
                    // Standard approach: Alpha * 18 - 7 (for approx 0-1 range if using floats
                    // directly?
                    // Android ColorMatrix operates on 0-255 usually but passed as floats acting on
                    // unmultiplied?
                    // Actually usually it operates on [0..255] range logic but inputs are
                    // normalized [0..1]?
                    // Documentation says: R' = a*R + b*G + c*B + d*A + e
                    // If inputs are 0-255: matrix is applied directly.

                    // Let's use a standard "gooey" matrix constant.
                    // Alpha * 18 - 7 * 255 (if offset is 0-255 range)
                    val gooMatrix =
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
                                            25f,
                                            -10f * 255f
                                    )
                            )

                    val alphaFilter =
                            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(gooMatrix))

                    // Chain: Blur then Alpha Filter
                    RenderEffect.createChainEffect(alphaFilter, blurEffect).asComposeRenderEffect()
                } else {
                    null
                }
            }

    Box(modifier = modifier.graphicsLayer { this.renderEffect = renderEffect }) { content() }
}

/** A simple circular drop component to be used inside MetaballBox. */
@Composable
fun LiquidDrop(
        modifier: Modifier = Modifier,
        size: Dp = 50.dp,
        color: Color = Color.Black // Metaballs usually single color or gradient.
) {
    Box(modifier = modifier.size(size).background(color, CircleShape))
}
