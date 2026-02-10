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
    uniform shader composable;
    uniform vec2 uResolution;
    // Flattened array: [x0, y0, x1, y1, ... x5, y5]
    // Supports exactly 6 blobs: 0..4 (anchors) + 5 (active cursor)
    uniform float uBlobCoords[12]; 
    uniform float uRadii[6];
    uniform vec4 uColor; 
    uniform float uSmoothness; 
    uniform float uScaleX;
    uniform float uScaleY;
    uniform float uTime; // For organic breathing

    // Polynomial Smooth Min
    float smin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    float sdCircle(vec2 p, vec2 center, float r) {
        vec2 scaled_p = (p - center) / vec2(uScaleX, uScaleY);
        return length(scaled_p) - r;
    }

    vec4 main(vec2 fragCoord) {
        // --- FLUID BREATHING ---
        // Organic pulse for the blobs
        float breathe = sin(uTime * 3.0) * 2.0; 
        
        float d = 1000.0;
        
        // --- UNROLLED COMBINATION LOOP (0 to 5) ---
        // We add 'breathe' to the radius of the active cursor (index 5)
        
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
        // Blob 5 (Active Cursor) - Added Breathing
        d = smin(d, sdCircle(fragCoord, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5] + breathe), uSmoothness);
        
        // Edge Anti-aliasing / Mask
        float alpha = 1.0 - smoothstep(-1.0, 0.5, d);
        
        // Optimization: If completely outside, return original content
        if (alpha <= 0.001) {
            return composable.eval(fragCoord);
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
        nx_val = smin(nx_val, sdCircle(fragCoord + e.xy, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5] + breathe), uSmoothness);

        // Y-offset
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[0], uBlobCoords[1]), uRadii[0]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[2], uBlobCoords[3]), uRadii[1]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[4], uBlobCoords[5]), uRadii[2]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[6], uBlobCoords[7]), uRadii[3]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[8], uBlobCoords[9]), uRadii[4]), uSmoothness);
        ny_val = smin(ny_val, sdCircle(fragCoord + e.yx, vec2(uBlobCoords[10], uBlobCoords[11]), uRadii[5] + breathe), uSmoothness);

        float dx = nx_val - d;
        float dy = ny_val - d;
        vec3 normal = normalize(vec3(dx, dy, 2.0)); 

        // --- REFRACTION & CHROMATIC ABERRATION ---
        // Calculate displacement based on normal and distance to center (lens effect)
        // Stronger distortion at edges
        vec2 distortion = normal.xy * 25.0 * alpha;
        
        // Chromatic Aberration (Split RGB channels)
        // Red is displaced less, Blue is displaced more (like real prism)
        float r = composable.eval(fragCoord - distortion * 0.98).r;
        float g = composable.eval(fragCoord - distortion).g;
        float b = composable.eval(fragCoord - distortion * 1.02).b;
        
        vec4 refractedColor = vec4(r, g, b, 1.0);

        // --- LIGHTING ---
        vec3 lightDir = normalize(vec3(-0.5, -0.5, 1.0)); 
        vec3 viewDir = vec3(0.0, 0.0, 1.0);
        
        // Specular
        vec3 reflectDir = reflect(-lightDir, normal);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
        
        // Rim Light
        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
        float rimIntensity = smoothstep(0.5, 1.0, rim) * 0.8; 

        // Combine
        // Mix Refracted background with Tint Color (uColor)
        vec4 finalColor = mix(refractedColor, uColor, uColor.a * alpha);
        
        // Add Highlights
        finalColor.rgb += spec * 0.9;
        finalColor.rgb += rimIntensity;

        // Force full opacity for the liquid body if inside blob (to hide original non-distorted bg)
        // But we want to blend edges smoothly.
        // We return 'finalColor' but mixed with original composable based on alpha.
        
        vec4 original = composable.eval(fragCoord);
        return mix(original, finalColor, alpha);
    }
"""
