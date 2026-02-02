package com.footprint.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance

// Android Graphics Shading Language (AGSL) code for generating noise.
// This gives the glass a more realistic, textured appearance, similar to iOS's frosted glass.
private const val AGSL_NOISE_SHADER = """
    uniform shader composable;
    uniform float2 iResolution;
    uniform float iTime;

    // Generates a pseudo-random value from a 2D vector.
    float random(vec2 st) {
        return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
    }

    half4 main(vec2 fragCoord) {
        // Base color from the composable content
        half4 color = composable.eval(fragCoord);

        // Reduce noise resolution to make grains larger and more visible
        float2 noiseCoord = fragCoord.xy / 2.5;

        // Generate a random noise value and apply a small time-based shift for a subtle shimmering effect.
        float noise = (random(noiseCoord + iTime * 0.1) - 0.5) * 0.15;

        // Add the noise to the base color.
        color.rgb += noise;

        return color;
    }
"""

/**
 * A modifier that applies a subtle, dynamic noise texture over its content.
 * This is used to enhance the realism of the "glass" material.
 *
 * It uses a RuntimeShader with AGSL, which is only supported on Android 13 (API 33) and above.
 * On older versions, this modifier has no effect.
 */
@Composable
fun Modifier.noiseGrain(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }
    
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shader = remember(isDark) { RuntimeShader(AGSL_NOISE_SHADER) }

    return this.drawWithCache {
        val brush = ShaderBrush(shader)
        shader.setFloatUniform("iResolution", size.width, size.height)
        
        onDrawWithContent {
            drawContent()
            drawRect(brush)
        }
    }
}
