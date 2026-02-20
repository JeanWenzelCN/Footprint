package com.footprint.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.footprint.data.model.Mood

// ==========================================
// 1. MOOD SHADER: Morphing Fluid
// ==========================================
private const val MOOD_SHADER_SRC = """
    uniform float2 resolution;
    uniform float time;
    uniform float3 baseColor;
    
    // Simplex noise inspired hash
    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }
    
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }
    
    // Fractional Brownian Motion (fBm)
    float fbm(float2 p) {
        float f = 0.0;
        float w = 0.5;
        for (int i = 0; i < 4; i++) {
            f += w * noise(p);
            p *= 2.0;
            w *= 0.5;
        }
        return f;
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        uv = uv * 2.0 - 1.0;
        uv.x *= resolution.x / resolution.y;
        
        // Fluid distortion
        float2 q = float2(0.0);
        q.x = fbm(uv + 0.1 * time);
        q.y = fbm(uv + float2(1.0) + 0.1 * time);
        
        float2 r = float2(0.0);
        r.x = fbm(uv + 1.0 * q + float2(1.7, 9.2) + 0.15 * time);
        r.y = fbm(uv + 1.0 * q + float2(8.3, 2.8) + 0.126 * time);
        
        float f = fbm(uv + r);
        
        // Base color mixed with fluid variation
        float3 color = mix(baseColor * 0.5, baseColor * 1.5, f);
        
        // Soft circle mask
        float d = length(uv);
        float mask = smoothstep(1.0, 0.9, d);
        
        return half4(color * mask, mask * 0.9);
    }
"""

// ==========================================
// 2. ENERGY SHADER: Pulsating Electric Core
// ==========================================
private const val ENERGY_SHADER_SRC = """
    uniform float2 resolution;
    uniform float time;
    uniform float intensity; // 1.0 to 10.0
    uniform float3 coreColor;
    
    float hash(float n) { return fract(sin(n) * 1e4); }
    float hash(float2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }

    float noise(float2 x) {
        float2 i = floor(x);
        float2 f = fract(x);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        uv = uv * 2.0 - 1.0;
        
        float speed = 0.5 + intensity * 0.3; // Speed based on energy level
        
        // Core pulse
        float r = length(uv);
        float pulse = sin(time * speed * 2.0) * 0.1 + 0.9;
        
        // Electric noise ring
        float angle = atan(uv.y, uv.x);
        float n = noise(float2(angle * 5.0, time * speed * 3.0));
        
        float radius = 0.6 + intensity * 0.02; // Size scales slightly with intensity
        float dist = abs(r - radius - n * 0.1);
        
        // Sparkle intensity
        float glow = 0.02 / dist;
        glow *= pulse;
        
        float3 color = coreColor * glow;
        
        // Central bright core
        float core = smoothstep(radius * 0.8, 0.0, r) * pulse;
        color += float3(1.0, 0.9, 0.5) * core * (intensity / 10.0);
        
        // Outer mask
        float mask = smoothstep(1.0, 0.9, r);
        
        // Alpha based on brightness
        float maxCol = max(color.r, max(color.g, color.b));
        
        return half4(color * mask, min(maxCol * 1.5, 1.0) * mask);
    }
"""

// ==========================================
// 3. MILEAGE SHADER: Forward Motion / Radar
// ==========================================
private const val MILEAGE_SHADER_SRC = """
    uniform float2 resolution;
    uniform float time;
    uniform float3 tintColor;
    
    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        uv = uv * 2.0 - 1.0;
        
        float r = length(uv);
        float a = atan(uv.y, uv.x);
        
        // Radar sweep
        float sweep = fract(a / (3.14159 * 2.0) - time * 0.5);
        sweep = smoothstep(0.0, 0.1, sweep) * smoothstep(1.0, 0.8, sweep);
        
        // Concentric distance rings
        float rings = sin(r * 20.0 - time * 3.0);
        rings = smoothstep(0.8, 1.0, rings) * 0.3;
        
        // Forward motion lines
        float forward = sin(uv.x * 10.0 + uv.y * 5.0 + time * 4.0);
        forward = smoothstep(0.9, 1.0, forward) * 0.4;
        forward *= smoothstep(0.0, 0.5, uv.x + 1.0); // fade towards left
        
        float grid = max(rings, forward);
        
        float3 color = tintColor * (grid + sweep * 0.8 + 0.1); // Add base glow
        
        float mask = smoothstep(1.0, 0.95, r);
        
        return half4(color * mask, mask * 0.8);
    }
"""

// =======================
// Compose Components
// =======================

@Composable
fun HolographicMoodIcon(mood: Mood, size: Dp = 48.dp, isActive: Boolean = true) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isActive) {
        val shader = remember { RuntimeShader(MOOD_SHADER_SRC) }
        var time by remember { mutableStateOf(0f) }

        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            while (true) {
                withInfiniteAnimationFrameMillis {
                    time = (System.currentTimeMillis() - startTime) / 1000f
                }
            }
        }

        val baseColor = when (mood) {
            Mood.EXCITED -> Color(0xFFFFB300)   // Golden
            Mood.CURIOUS -> Color(0xFF4FC3F7)   // Cyan/Teal
            Mood.RELAXED -> Color(0xFF64B5F6)   // Blue
            Mood.REFLECTIVE -> Color(0xFF9575CD) // Purple
            else -> mood.color
        }

        Canvas(modifier = Modifier.size(size).clip(CircleShape)) {
            shader.setFloatUniform("resolution", size.toPx(), size.toPx())
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("baseColor", baseColor.red, baseColor.green, baseColor.blue)

            drawRect(brush = ShaderBrush(shader))
        }
    } else {
        // Fallback or Inactive State
        val color = when (mood) {
            Mood.EXCITED -> Color(0xFFFFB300)
            Mood.CURIOUS -> Color(0xFF4FC3F7)
            Mood.RELAXED -> Color(0xFF64B5F6)
            Mood.REFLECTIVE -> Color(0xFF9575CD)
            else -> mood.color
        }
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Face, contentDescription = "Mood", tint = Color.White)
        }
    }
}

@Composable
fun HolographicEnergyIcon(energyLevel: Int, size: Dp = 48.dp, isActive: Boolean = true) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isActive) {
        val shader = remember { RuntimeShader(ENERGY_SHADER_SRC) }
        var time by remember { mutableStateOf(0f) }

        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            while (true) {
                withInfiniteAnimationFrameMillis {
                    time = (System.currentTimeMillis() - startTime) / 1000f
                }
            }
        }

        val coreColor = if (energyLevel >= 8) {
            Color(0xFFFF3D00) // Intense Orange/Red
        } else if (energyLevel >= 5) {
            Color(0xFF00E676) // Bright Green
        } else {
            Color(0xFF29B6F6) // Low Blue
        }

        Canvas(modifier = Modifier.size(size).clip(CircleShape)) {
            shader.setFloatUniform("resolution", size.toPx(), size.toPx())
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("intensity", energyLevel.coerceIn(1, 10).toFloat())
            shader.setFloatUniform("coreColor", coreColor.red, coreColor.green, coreColor.blue)

            drawRect(brush = ShaderBrush(shader))
        }
    } else {
         val color = if (energyLevel >= 8) Color(0xFFFF3D00) else if (energyLevel >= 5) Color(0xFF00E676) else Color(0xFF29B6F6)
         Box(
            modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Bolt, contentDescription = "Energy", tint = Color.White)
        }
    }
}

@Composable
fun HolographicMileageIcon(distanceKm: Double, size: Dp = 48.dp, isActive: Boolean = true) {
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isActive) {
        val shader = remember { RuntimeShader(MILEAGE_SHADER_SRC) }
        var time by remember { mutableStateOf(0f) }

        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            while (true) {
                withInfiniteAnimationFrameMillis {
                    time = (System.currentTimeMillis() - startTime) / 1000f
                }
            }
        }

        val tintColor = Color(0xFF00E5FF) // Cyan/Teal tech vibe

        Canvas(modifier = Modifier.size(size).clip(CircleShape)) {
            shader.setFloatUniform("resolution", size.toPx(), size.toPx())
            shader.setFloatUniform("time", time + (distanceKm * 0.01).toFloat()) // offset time slightly by distance
            shader.setFloatUniform("tintColor", tintColor.red, tintColor.green, tintColor.blue)

            drawRect(brush = ShaderBrush(shader))
        }
    } else {
         Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Map, contentDescription = "Mileage", tint = Color.White)
        }
    }
}
