package com.footprint.ui.components.weather

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

// ==========================================
// AGSL Shaders
// ==========================================

const val SHADER_SUNNY =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    mat2 rot(float a) {
        float s = sin(a), c = cos(a);
        return mat2(c, -s, s, c);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        float l = length(uv);
        
        // Breathing core
        float breath = sin(time * 2.0) * 0.1 + 0.9;
        float core = 0.05 / (l * l + 0.01) * breath;
        
        // Rotating halos (sharp neon)
        float2 uv_rot1 = rot(time * 0.3) * uv;
        float halo1 = smoothstep(0.01, 0.0, abs(length(uv_rot1 * float2(1.0, 3.0)) - 0.25));
        
        float2 uv_rot2 = rot(-time * 0.5 + 1.0) * uv;
        float halo2 = smoothstep(0.015, 0.0, abs(length(uv_rot2 * float2(4.0, 1.0)) - 0.3));
        
        float glow = core + halo1*2.0 + halo2*1.5;
        glow *= smoothstep(0.5, 0.1, l); // fade at edges
        
        return half4(baseColor.rgb * glow, min(glow, 1.0) * baseColor.a);
    }
"""

const val SHADER_CLOUDY =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    float smin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5*(b-a)/k, 0.0, 1.0);
        return mix(b, a, h) - k*h*(1.0-h);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        
        // Metaballs squashing
        float2 p1 = float2(sin(time)*0.1 - 0.1, cos(time*1.2)*0.05);
        float2 p2 = float2(cos(time*0.8)*0.1 + 0.1, sin(time*0.9)*0.05);
        
        float d1 = length(uv - p1) - 0.15;
        float d2 = length(uv - p2) - 0.18;
        
        // Liquid fusion
        float d = smin(d1, d2, 0.1);
        
        // Internal flowing light
        float flow = sin(uv.x * 10.0 - time * 3.0) * 0.5 + 0.5;
        
        float alpha = smoothstep(0.01, -0.01, d);
        half3 col = mix(baseColor.rgb * 0.5, baseColor.rgb * 1.5, flow * 0.3);
        
        // Outer glow
        float glow = 0.02 / (abs(d) + 0.01);
        
        return half4(col * alpha + baseColor.rgb * glow * 0.5, max(alpha, glow * 0.5) * baseColor.a);
    }
"""

const val SHADER_RAIN =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    float hash(float n) { return fract(sin(n)*43758.5453); }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        half3 col = half3(0.0, 0.0, 0.0);
        float alpha = 0.0;
        
        // Deep blue clouds top
        float cloud = smoothstep(0.1, -0.2, length(uv - float2(0.0, -0.3)) - 0.2);
        col += half3(0.1, 0.3, 0.6) * cloud;
        alpha += cloud;
        
        // Rain droplets
        for(float i=0.0; i<10.0; i++) {
            float x = (hash(i)-0.5)*0.8;
            float speed = 1.0 + hash(i*13.0);
            float y = fract(time*speed + hash(i*7.0)) * 1.2 - 0.6;
            
            float2 dropUv = uv - float2(x, y);
            // Elongated droplet
            float drop = smoothstep(0.005, 0.0, abs(dropUv.x)) * smoothstep(0.05, 0.0, abs(dropUv.y+0.05));
            
            // Splash at bottom
            float splashZone = smoothstep(-0.4, -0.5, uv.y);
            float splash = smoothstep(0.02, 0.0, length(uv - float2(x, -0.4)) - fract(time*speed*2.0)*0.1) * splashZone * (1.0-fract(time*speed*2.0));
            
            col += (drop + splash) * baseColor.rgb * 2.0;
            alpha += drop + splash;
        }
        
        return half4(col, min(alpha, 1.0) * baseColor.a);
    }
"""

const val SHADER_THUNDER =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    float noise(float2 p) { return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453); }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        
        // Lightning trigger
        float flash = step(0.98, fract(sin(time*10.0)*43758.5));
        
        // Glitch offset
        float2 glitch = float2(noise(float2(time, uv.y*10.0)) * 0.05 * flash, 0.0);
        float2 uv_g = uv + glitch;
        
        // Lightning bolt (simple jagged line)
        float boltX = sin(uv_g.y * 20.0 + time*50.0)*0.05 + sin(uv_g.y * 5.0)*0.1;
        float bolt = smoothstep(0.02, 0.0, abs(uv_g.x - boltX)) * flash;
        
        // Thick purple clouds
        float cloud = smoothstep(0.3, 0.0, length(uv_g - float2(0.0, -0.1)));
        half3 col = baseColor.rgb * cloud * 0.5;
        
        // Chromatic aberration on flash
        float r = bolt * 1.5;
        float g = smoothstep(0.02, 0.0, abs(uv_g.x - boltX - 0.02)) * flash;
        float b = smoothstep(0.02, 0.0, abs(uv_g.x - boltX + 0.02)) * flash;
        
        col += half3(r, g, b) + half3(0.0, 1.0, 1.0) * bolt;
        
        float alpha = cloud + bolt;
        return half4(col, min(alpha, 1.0) * baseColor.a);
    }
"""

const val SHADER_SNOW =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    float hex(float2 p) {
        p = abs(p);
        return max(p.x + p.y*0.57735, p.y*1.1547);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        float alpha = 0.0;
        half3 col = half3(0.0, 0.0, 0.0);
        
        // Falling snowflakes
        for(float i=0.0; i<8.0; i++) {
            float x = fract(sin(i*7.0)*43.0)*0.8-0.4;
            float y = fract(time * (0.2 + fract(sin(i)*11.0)*0.3) + i*0.13) * 1.2 - 0.6;
            
            // Sine float
            x += sin(time + i) * 0.1;
            
            float2 p = uv - float2(x, -y);
            // Hexagon shape
            float h = hex(p*15.0);
            float flake = smoothstep(0.1, 0.05, h);
            float glow = 0.02 / (h + 0.01);
            
            col += (flake + glow*0.5) * baseColor.rgb;
            alpha += flake + glow*0.5;
        }
        
        // Frost vignette edge
        float frost = smoothstep(0.3, 0.5, length(uv));
        col += baseColor.rgb * frost * 0.3;
        alpha += frost * 0.5;
        
        return half4(col, min(alpha, 1.0) * baseColor.a);
    }
"""

const val SHADER_FOG =
        """
    uniform float2 resolution;
    uniform float time;
    uniform half4 baseColor;
    
    // Simplex noise (approximate)
    float noise(float2 p) { return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453); }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * resolution.xy) / min(resolution.x, resolution.y);
        
        // Fast horizontal neon bands
        float band1 = smoothstep(0.02, 0.0, abs(uv.y - 0.1 + sin(uv.x*2.0 - time*5.0)*0.05));
        float band2 = smoothstep(0.015, 0.0, abs(uv.y + 0.15 + cos(uv.x*3.0 - time*4.0)*0.03));
        
        // Fog fluid density
        float n = noise(uv * 3.0 + float2(time*0.5, 0.0));
        float fog = smoothstep(0.2, 0.8, n) * (sin(time)*0.2 + 0.6);
        
        float alpha = fog + band1 + band2;
        half3 col = baseColor.rgb * fog + half3(1.0, 1.0, 1.0)*band1 + half3(1.0, 1.0, 1.0)*band2;
        
        // Edge mask
        float mask = smoothstep(0.5, 0.2, length(uv));
        
        return half4(col * mask, min(alpha * mask, 1.0) * baseColor.a);
    }
"""

@Composable
fun HolographicWeatherIcon(
        type: WeatherType,
        modifier: Modifier = Modifier,
        size: Dp = 64.dp,
        isActive: Boolean = true
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shaderString =
                remember(type.baseShader) {
                    when (type.baseShader) {
                        BaseShader.SUNNY -> SHADER_SUNNY
                        BaseShader.CLOUDY -> SHADER_CLOUDY
                        BaseShader.RAIN -> SHADER_RAIN
                        BaseShader.THUNDERSTORM -> SHADER_THUNDER
                        BaseShader.SNOW -> SHADER_SNOW
                        BaseShader.FOG -> SHADER_FOG
                    }
                }

        val runtimeShader = remember(shaderString) { RuntimeShader(shaderString) }
        var time by remember { mutableFloatStateOf(0f) }

        // Animation loop
        LaunchedEffect(isActive, type) {
            if (isActive) {
                var lastTime = withFrameNanos { it }
                while (isActive) {
                    val frameTime = withFrameNanos { it }
                    time += (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime
                }
            } else {
                time = 0f
            }
        }

        val animatedAlpha by
                animateFloatAsState(targetValue = if (isActive) 1f else 0.5f, label = "alpha")
        val animatedColor by
                animateColorAsState(
                        targetValue = if (isActive) type.color else Color.Gray,
                        label = "colorFade"
                )

        Box(modifier = modifier.size(size).graphicsLayer { alpha = animatedAlpha }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                runtimeShader.setFloatUniform("resolution", size.toPx(), size.toPx())
                runtimeShader.setFloatUniform("time", time)
                runtimeShader.setFloatUniform(
                        "baseColor",
                        animatedColor.red,
                        animatedColor.green,
                        animatedColor.blue,
                        animatedColor.alpha
                )
                drawRect(brush = ShaderBrush(runtimeShader))
            }
        }
    } else {
        // Fallback for older devices (< API 33)
        val animatedAlpha by
                animateFloatAsState(targetValue = if (isActive) 1f else 0.5f, label = "alpha")

        Box(modifier = modifier.size(size).graphicsLayer { alpha = animatedAlpha }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.toPx() / 2, size.toPx() / 2)
                val radius = size.toPx() / 2 * 0.8f
                drawCircle(color = type.color, radius = radius, center = center)
            }
        }
    }
}
