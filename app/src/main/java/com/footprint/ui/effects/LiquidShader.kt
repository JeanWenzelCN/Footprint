package com.footprint.ui.effects

import org.intellij.lang.annotations.Language

/**
 * AGSL Shader for high-fidelity Liquid/Metaball effects.
 *
 * ROBUST IMPLEMENTATION:
 * - No loops (manually unrolled for constant time execution).
 * - No 'discard' (uses transparent return).
 * - Flat arrays (uBlobCoords) for safe memory alignment.
 * - float4/vec4 precision throughout.
 *
 * Implements:
 * 1. SDF (Signed Distance Field) for resolution-independent shapes.
 * 2. SmoothMin (Polynomial) for fluid fusing.
 * 3. Normal Mapping (Finite Difference) for 3D lighting/refraction simulation.
 */
@Language("AGSL")
const val LIQUID_SHADER =
        """
    uniform vec2 uResolution;
    // Flattened array: [x0, y0, x1, y1, ... x5, y5]
    // Supports exactly 6 blobs: 0..4 (anchors) + 5 (active cursor)
    uniform float uBlobCoords[12]; 
    uniform float uRadii[6];
    uniform vec4 uColor; 
    uniform float uSmoothness; 

    // Polynomial Smooth Min
    float smin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    float sdCircle(vec2 p, vec2 center, float r) {
        return length(p - center) - r;
    }

    vec4 main(vec2 fragCoord) {
        float d = 1000.0;
        
        // --- UNROLLED COMBINATION LOOP (0 to 5) ---
        // We use '1000.0' or huge distance for 'inactive' blobs (radius <= 0) effectively.
        // We assume Java side sends valid coords or 0-radius for unused slots.
        
        // Blob 0
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[0], uBlobCoords[1]), uRadii[0]), uSmoothness);
        // Blob 1
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[2], uBlobCoords[3]), uRadii[1]), uSmoothness);
        // Blob 2
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[4], uBlobCoords[5]), uRadii[2]), uSmoothness);
        // Blob 3
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[6], uBlobCoords[7]), uRadii[3]), uSmoothness);
        // Blob 4
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[8], uBlobCoords[9]), uRadii[4]), uSmoothness);
        // Blob 5 (Active Cursor)
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5]), uSmoothness);
        
        // Edge Anti-aliasing
        float alpha = 1.0 - smoothstep(-1.0, 0.5, d);
        
        // Early exit optimization (Safe return)
        if (alpha <= 0.0) {
            return vec4(0.0);
        }

        // --- UNROLLED NORMAL CALCULATION (Finite Difference) ---
        vec2 e = vec2(1.0, 0.0);
        float nx_val = 1000.0;
        float ny_val = 1000.0;
        
        // X-offset
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[0], uBlobCoords[1]), uRadii[0]), uSmoothness);
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[2], uBlobCoords[3]), uRadii[1]), uSmoothness);
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[4], uBlobCoords[5]), uRadii[2]), uSmoothness);
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[6], uBlobCoords[7]), uRadii[3]), uSmoothness);
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[8], uBlobCoords[9]), uRadii[4]), uSmoothness);
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5]), uSmoothness);

        // Y-offset
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[0], uBlobCoords[1]), uRadii[0]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[2], uBlobCoords[3]), uRadii[1]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[4], uBlobCoords[5]), uRadii[2]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[6], uBlobCoords[7]), uRadii[3]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[8], uBlobCoords[9]), uRadii[4]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5]), uSmoothness);

        float dx = nx_val - d;
        float dy = ny_val - d;
        vec3 normal = normalize(vec3(dx, dy, 2.0)); // Z=2.0 softens the normal curve

        // --- LIGHTING ---
        vec3 lightDir = normalize(vec3(-0.5, -0.5, 1.0)); // Top-Left Light
        vec3 viewDir = vec3(0.0, 0.0, 1.0);
        
        // Specular (Phong)
        vec3 reflectDir = reflect(-lightDir, normal);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
        
        // Fresnel / Inner Shadow
        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
        float innerShadow = smoothstep(0.4, 0.8, rim) * 0.4;

        vec4 finalColor = uColor;
        finalColor.rgb += spec * 0.9; // Strong gloss
        finalColor.rgb -= innerShadow;
        
        finalColor.a = alpha;
        return finalColor;
    }
"""
