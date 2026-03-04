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
    uniform shader maskTexture;      // Exploration mask: white(1.0)=fog, black(0.0)=explored
    uniform float2 uResolution;      // View dimensions in px
    uniform float  uTime;            // Elapsed time in seconds
    uniform float2 uWindOffset;      // Map center in noise space + slow wind drift
    uniform float2 uMapScale;        // Scale factor for zoom
    uniform float  uFogDensity;      // Overall fog density multiplier
    
    // --- Palette ---
    uniform float3 uFogColorBright;  // Pearl white highlight
    uniform float3 uFogColorMid;     // Mid-tone grey-blue
    uniform float3 uFogColorDark;    // Deep shadow
    uniform float3 uLightDir;        // Normalized light direction
    
    // ============================================================
    // Hash & Noise Functions (GPU-fast)
    // ============================================================
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
    
    // Procedural multi-octave noise for rich clouds (Unrolled for strict AGSL compliance)
    float fbm(float2 p) {
        float val = 0.0;
        float amp = 0.5;
        float2x2 rot = float2x2(0.8, 0.6, -0.6, 0.8);
        
        val += amp * valueNoise(p); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * valueNoise(p); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * valueNoise(p); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * valueNoise(p); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * valueNoise(p);
        
        return val;
    }
    
    // Turbulence for dynamic erosion and fire-like edges
    float turbulence(float2 p) {
        float val = 0.0;
        float amp = 0.5;
        float2x2 rot = float2x2(0.8, 0.6, -0.6, 0.8);
        
        val += amp * abs(valueNoise(p) * 2.0 - 1.0); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * abs(valueNoise(p) * 2.0 - 1.0); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * abs(valueNoise(p) * 2.0 - 1.0); p = rot * (p * 2.0); amp *= 0.5;
        val += amp * abs(valueNoise(p) * 2.0 - 1.0);
        
        return val;
    }

    // Comprehensive density function
    float getDensity(float2 p) {
        float n1 = fbm(p);
        float n2 = fbm(p + float2(5.2, 1.3));
        return smoothstep(0.15, 0.85, n1 * 0.7 + n2 * 0.3);
    }
    
    // ============================================================
    // MAIN
    // ============================================================
    float4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        
        // 1. Read Mask (Opaque logic: R channel. White=1=>fog, Black=0=>explored)
        float maskVal = maskTexture.eval(fragCoord).r;
        if (maskVal < 0.01) return float4(0.0);
        
        // 2. Coords anchored to real map scale + wind drift
        float2 centeredUV = uv - 0.5;
        float2 baseCloudUV = centeredUV * uMapScale + uWindOffset;
        
        // Time based churning / internal drift
        float timeWarp = uTime * 0.04;
        float2 churnOffset = float2(
            fbm(baseCloudUV * 0.5 + timeWarp),
            fbm(baseCloudUV * 0.5 - timeWarp)
        ) * 0.6;
        
        float2 cloudUV = baseCloudUV * 0.8 + churnOffset;
        
        // 3. Volumetric Density
        float density = getDensity(cloudUV);
        float core = smoothstep(0.4, 0.9, density); // Deep inner cloud
        
        // 4. Lighting & Normal Estimation
        float eps = 0.02; // sampling dist for normals
        float dx = getDensity(cloudUV + float2(eps, 0.0)) - density;
        float dy = getDensity(cloudUV + float2(0.0, eps)) - density;
        
        // Pseudo 3D normal derived from density slope
        float3 normal = normalize(float3(-dx, -dy, eps * 0.8));
        float3 lightDir = normalize(uLightDir);
        
        // Diffuse factor
        float ndotl = max(dot(normal, lightDir), 0.0);
        float diffuse = mix(0.4, 1.0, ndotl); // 0.4 ambient light
        
        // Fake subsurface scattering / backlighting on thin edges
        float scatter = smoothstep(0.7, 0.1, density) * max(dot(normal, -lightDir), 0.0);
        
        // 5. Coloring
        // Base color transition from shadow mapping
        float3 col = mix(uFogColorDark, uFogColorMid, smoothstep(0.0, 0.6, density));
        col = mix(col, uFogColorBright, diffuse * density);
        
        // Apply back-scattering rim light
        col += uFogColorBright * scatter * 1.5;
        
        // Deep shadow for thick cores
        col = mix(col, uFogColorDark * 0.8, core * 0.5);
        
        // 6. Natural Mask Softening & Dynamic Erosion
        // Create an encroaching "fire edge" turbulence effect on the borders
        float2 edgeUV = baseCloudUV * 2.5 + float2(uTime * 0.1, -uTime * 0.05);
        float edgeErosion = turbulence(edgeUV);
        
        // Perturb the mask significantly at the boundaries
        float perturbedMask = maskVal - edgeErosion * 0.35;
        perturbedMask = clamp(perturbedMask, 0.0, 1.0);
        
        // Non-linear fade so edges look like dissipating vapor
        float maskAlpha = smoothstep(0.0, 0.5, perturbedMask);
        
        // 7. Final Composite
        float finalAlpha = density * maskAlpha * uFogDensity;
        finalAlpha = clamp(finalAlpha, 0.0, 0.98); // Never 100% opaque, let some map detail leak
        
        return float4(col * finalAlpha, finalAlpha); // Premultiplied output
    }
    """

/**
 * Simplified fog shader fallback for devices without AGSL support (API < 33). Uses pre-generated
 * BitmapShader textures with DST_OUT masking. This is the legacy approach used in the previous
 * implementation.
 */
const val FOG_SHADER_MIN_API = 33
