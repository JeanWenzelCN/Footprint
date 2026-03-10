package com.footprint.ui.effects

import org.intellij.lang.annotations.Language

/**
 * AGSL Shader for "Eternal Realm" - Artistic Cloud System.
 * 
 * Features:
 * 1. Low density, filamentous "Cloud of Yunnan" (彩云之南)
 * 2. Time-synced tinting (Dawn Rose -> Sun Gold -> Twilight Violet -> Midnight Blue)
 * 3. High-altitude fractal noise (拉丝高空卷云)
 */
@Language("AGSL")
const val ETERNAL_CLOUD_SHADER = """
    uniform float2 uResolution;
    uniform float  uTime;
    uniform float2 uWindOffset;
    uniform float2 uMapScale;
    uniform float  uTimeOfDay; // 0.0 to 1.0 (Midnight to Midnight)
    
    float hash21(float2 p) {
        float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    float valueNoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        float a = hash21(i + float2(0.0, 0.0));
        float b = hash21(i + float2(1.0, 0.0));
        float c = hash21(i + float2(0.0, 1.0));
        float d = hash21(i + float2(1.0, 1.0));
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }
    
    // Filamentous cloud noise (stretched x axis)
    float filamentNoise(float2 p) {
        float val = 0.0;
        float amp = 0.5;
        // Stretch horizontally for "stringy" clouds
        p.x *= 0.4; 
        
        val += amp * valueNoise(p); p *= 2.1; amp *= 0.5;
        val += amp * valueNoise(p); p *= 2.3; amp *= 0.5;
        val += amp * valueNoise(p); p *= 2.5; amp *= 0.5;
        val += amp * valueNoise(p);
        return val;
    }

    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float2 baseCloudUV = (uv - 0.5) * uMapScale * 0.5 + uWindOffset * 0.1;
        
        // Slow directional drift
        float2 drift = float2(uTime * 0.005, uTime * 0.002);
        float n = filamentNoise(baseCloudUV + drift);
        
        // High altitude feel: very thin, low contrast
        float density = smoothstep(0.4, 0.7, n) * 0.18;
        
        // Time based tinting
        // 0.25 (6am) - Rose Gold
        // 0.5  (12pm) - Pearl White
        // 0.75 (6pm) - Twilight Violet
        // 0.0  (12am) - Midnight Blue
        
        float3 dawn = float3(1.0, 0.7, 0.6);
        float3 day = float3(1.0, 1.0, 0.95);
        float3 dusk = float3(0.5, 0.4, 0.8);
        float3 night = float3(0.2, 0.2, 0.4);
        
        float3 tint;
        if(uTimeOfDay < 0.25) tint = mix(night, dawn, uTimeOfDay * 4.0);
        else if(uTimeOfDay < 0.5) tint = mix(dawn, day, (uTimeOfDay - 0.25) * 4.0);
        else if(uTimeOfDay < 0.75) tint = mix(day, dusk, (uTimeOfDay - 0.5) * 4.0);
        else tint = mix(dusk, night, (uTimeOfDay - 0.75) * 4.0);
        
        return float4(tint * density, density);
    }
"""
