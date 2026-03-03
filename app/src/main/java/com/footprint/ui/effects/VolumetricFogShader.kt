package com.footprint.ui.effects

import org.intellij.lang.annotations.Language

/**
 * AGSL Shader for high-fidelity Volumetric Cloud Fog (体积云迷雾).
 *
 * Implements:
 * 1. Multi-octave Fractal Noise (分形噪声) for realistic cloud texture
 * 2. Time-based churning animation (翻滚动画)
 * 3. Gradient-based pseudo-3D volumetric lighting (体积光照)
 * 4. Exploration mask blending with noise-perturbed soft edges (噪声干涉软边缘)
 * 5. Dynamic erosion at exploration boundaries (动态侵蚀)
 *
 * Performance: Entire computation runs on GPU via AGSL. Noise uses Simplex-like value noise (no
 * texture lookups).
 */
@Language("AGSL")
const val VOLUMETRIC_FOG_SHADER =
        """
    // --- Uniforms ---
    uniform shader maskTexture;      // Exploration mask: white=fog, black=explored
    uniform float2 uResolution;      // View dimensions in px
    uniform float  uTime;            // Elapsed time in seconds (for animation)
    uniform float2 uWindOffset;      // Overall wind drift offset (slow global movement)
    uniform float  uFogDensity;      // Overall fog density multiplier [0.0 - 1.0], default 0.92
    
    // --- Color palette for the cloud ---
    uniform float3 uFogColorBright;  // Pearl white highlight  e.g. (0.89, 0.91, 0.94)
    uniform float3 uFogColorMid;     // Mid-tone grey-blue     e.g. (0.68, 0.73, 0.80)
    uniform float3 uFogColorDark;    // Deep shadow            e.g. (0.15, 0.18, 0.25)
    uniform float3 uLightDir;        // Normalized light direction (e.g. (-0.5, -0.6, 0.8))
    
    // ============================================================
    // Hash & Noise Functions (GPU-friendly, no texture lookups)
    // ============================================================
    
    // Fast 2D hash -> pseudo-random float in [0,1]
    float hash21(float2 p) {
        float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
        p3 += dot(p3, float3(p3.y, p3.z, p3.x) + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    // Smooth Value Noise 2D
    float valueNoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        // Quintic Hermite interpolation (smoother than cubic)
        float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        
        float a = hash21(i + float2(0.0, 0.0));
        float b = hash21(i + float2(1.0, 0.0));
        float c = hash21(i + float2(0.0, 1.0));
        float d = hash21(i + float2(1.0, 1.0));
        
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }
    
    // Multi-octave Fractal Brownian Motion (fBM)
    // 4 octaves for cloud detail
    float fbm(float2 p) {
        float val = 0.0;
        float amp = 0.5;
        float freq = 1.0;
        // Rotation matrix to reduce grid-aligned artifacts between octaves
        float2x2 rot = float2x2(0.8, 0.6, -0.6, 0.8);
        
        // Octave 1
        val += amp * valueNoise(p * freq);
        p = rot * p;
        amp *= 0.5;
        freq *= 2.0;
        
        // Octave 2
        val += amp * valueNoise(p * freq);
        p = rot * p;
        amp *= 0.5;
        freq *= 2.0;
        
        // Octave 3
        val += amp * valueNoise(p * freq);
        p = rot * p;
        amp *= 0.5;
        freq *= 2.0;
        
        // Octave 4
        val += amp * valueNoise(p * freq);
        
        return val;
    }
    
    // Turbulence variant for erosion edges
    float turbulence(float2 p) {
        float val = 0.0;
        float amp = 0.5;
        float freq = 1.0;
        float2x2 rot = float2x2(0.8, 0.6, -0.6, 0.8);
        
        val += amp * abs(valueNoise(p * freq) * 2.0 - 1.0);
        p = rot * p; amp *= 0.5; freq *= 2.0;
        val += amp * abs(valueNoise(p * freq) * 2.0 - 1.0);
        p = rot * p; amp *= 0.5; freq *= 2.0;
        val += amp * abs(valueNoise(p * freq) * 2.0 - 1.0);
        
        return val;
    }
    
    // ============================================================
    // Main Fragment Shader
    // ============================================================
    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        
        // --- 1) Read Exploration Mask ---
        // maskTexture: white(1.0) = unexplored/fog, black(0.0) = explored/clear
        float maskVal = maskTexture.eval(fragCoord).r;
        
        // Early exit: fully explored area -> show pure map
        if (maskVal < 0.01) {
            return float4(0.0);
        }
        
        // --- 2) Generate Dynamic Cloud Noise ---
        // Scale UV to get cloud-appropriate frequency
        float2 cloudUV = uv * 3.5 + uWindOffset;
        
        // Churning: time-varying offset for internal turbulence
        float churnSpeed = 0.08;
        float2 churn = float2(
            sin(uTime * churnSpeed * 0.7) * 0.3,
            cos(uTime * churnSpeed * 1.1) * 0.2
        );
        
        // Primary cloud density from fBM
        float cloudDensity = fbm(cloudUV + churn);
        
        // Secondary layer at different scale for non-uniform detail
        float detailNoise = fbm(cloudUV * 2.3 - churn * 1.5 + float2(5.2, 1.3));
        
        // Combine: main shape + detail variation
        float combinedCloud = cloudDensity * 0.65 + detailNoise * 0.35;
        
        // Remap to create more contrast (dense cores, thin edges)
        combinedCloud = smoothstep(0.15, 0.85, combinedCloud);
        
        // --- 3) Volumetric Lighting (Pseudo-3D) ---
        // Compute gradient of the noise field for normal estimation
        float eps = 0.005;
        float nx = fbm(cloudUV + churn + float2(eps, 0.0)) - fbm(cloudUV + churn - float2(eps, 0.0));
        float ny = fbm(cloudUV + churn + float2(0.0, eps)) - fbm(cloudUV + churn - float2(0.0, eps));
        
        // Surface normal from height field
        float3 normal = normalize(float3(-nx * 8.0, -ny * 8.0, 1.0));
        
        // Diffuse lighting
        float diffuse = max(dot(normal, normalize(uLightDir)), 0.0);
        diffuse = diffuse * 0.6 + 0.4; // Ambient floor
        
        // Specular highlight (subtle)
        float3 viewDir = float3(0.0, 0.0, 1.0);
        float3 halfVec = normalize(normalize(uLightDir) + viewDir);
        float spec = pow(max(dot(normal, halfVec), 0.0), 24.0) * 0.3;
        
        // --- 4) Cloud Coloring with Lighting ---
        // Mix between dark (shadow), mid (ambient), bright (lit) based on density + lighting
        float3 fogColor = mix(uFogColorDark, uFogColorMid, combinedCloud);
        fogColor = mix(fogColor, uFogColorBright, diffuse * combinedCloud);
        fogColor += spec * float3(1.0, 0.98, 0.95); // Warm specular
        
        // Subtle depth darkening at cloud cores
        float coreIntensity = smoothstep(0.4, 0.9, combinedCloud);
        fogColor = mix(fogColor, uFogColorDark * 1.1, coreIntensity * 0.2);
        
        // --- 5) Noise-Perturbed Exploration Edge (Dynamic Erosion) ---
        // Instead of using maskVal directly, perturb it with turbulence noise
        // This creates irregular, fire-like erosion edges
        float2 erosionUV = uv * 8.0 + float2(uTime * 0.04, uTime * 0.03);
        float erosionNoise = turbulence(erosionUV) * 0.35;
        
        // Warp the mask boundary: expand explored area slightly with noise
        float perturbedMask = maskVal - erosionNoise;
        perturbedMask = clamp(perturbedMask, 0.0, 1.0);
        
        // Non-linear ramp for softer transition at the boundary
        // Use smoothstep to create a natural feathered edge 
        float fogAlpha = smoothstep(0.0, 0.45, perturbedMask);
        
        // Apply overall density control
        fogAlpha *= uFogDensity * combinedCloud;
        
        // Clamp final alpha
        fogAlpha = clamp(fogAlpha, 0.0, 0.95);
        
    // --- 6) Final Compositing ---
        // float4 mapColor = mapContent.eval(fragCoord);
        float4 fogFinal = float4(fogColor * fogAlpha, fogAlpha); // Premultiplied alpha
        
        return fogFinal;
    }
"""

/**
 * Simplified fog shader fallback for devices without AGSL support (API < 33). Uses pre-generated
 * BitmapShader textures with DST_OUT masking. This is the legacy approach used in the previous
 * implementation.
 */
const val FOG_SHADER_MIN_API = 33
