#include <flutter/runtime_effect.glsl>

uniform vec2 resolution;
uniform vec3 lightPos;
uniform vec4 baseColor;
uniform float materialType;
uniform float uUnlocked;

out vec4 fragColor;

void main() {
    vec2 cllCrd = FlutterFragCoord().xy;
    vec2 uv = cllCrd / resolution.xy;
    vec2 p = (uv - 0.5) * 2.0; // [-1, 1]
    
    // adjust aspect ratio
    p.x *= resolution.x / resolution.y;
    
    // Simple Circular Badge SDF
    float sdf = length(p) - 0.8; 
    
    if (sdf > 0.0) {
        fragColor = vec4(0.0);
        return;
    }
    
    // Faux 3D Normal mapping (Hemisphere)
    float z = sqrt(max(1.0 - dot(p, p), 0.01));
    vec3 normal = normalize(vec3(p.x, p.y, z));
    
    // Lighting
    vec3 lightDir = normalize(lightPos - vec3(p, 0.0));
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfVector = normalize(lightDir + viewDir);
    
    float diff = max(dot(normal, lightDir), 0.0);
    float spec = 0.0;
    
    vec3 finalColor = baseColor.rgb;
    
    if (materialType < 0.5) {
        // 0. Metal: Anisotropic Highlight & Laser Carving Look
        float NdotH = dot(normal, halfVector);
        // Fake anisotropic stretching along x-axis
        vec3 tangent = vec3(0.0, 1.0, 0.0);
        float tDotL = dot(tangent, lightDir);
        float tDotV = dot(tangent, viewDir);
        float aniso = sqrt(1.0 - tDotL * tDotL) * sqrt(1.0 - tDotV * tDotV) - tDotL * tDotV;
        spec = pow(max(aniso, 0.0), 32.0) * 0.08;
        
        // Brighter base, less wash-out
        finalColor = finalColor * (0.7 + 0.3 * diff) + vec3(1.0, 0.9, 0.8) * spec;
        
        // Edge AO to simulate physical object thickness
        float edge = smoothstep(-0.15, 0.0, sdf);
        finalColor *= mix(1.0, 0.4, edge);
        
    } else if (materialType < 1.5) {
        // 1. Cyber: SDF Neon Glow & Bloom
        // The edge glows intensely
        float intenseGlow = exp(sdf * 25.0);
        
        // Pulsating or active rim light based on lightPos
        float rim = 1.0 - max(dot(normal, viewDir), 0.0);
        rim = smoothstep(0.6, 1.0, rim);
        
        finalColor = baseColor.rgb * (0.75 + 0.25 * diff);
        finalColor += baseColor.rgb * intenseGlow * 0.4; // Whisper of glow
        finalColor += vec3(0.0, 1.0, 1.0) * rim * 0.1; // Minimal rim
    } else if (materialType < 2.5) {
        // 2. Liquid Glass / Void Amber
        // Dispersion and high specularity
        float refractionX = dot(normal, normalize(viewDir + vec3(0.1, 0.0, 0.0)));
        float refractionY = dot(normal, normalize(viewDir + vec3(0.0, 0.1, 0.0)));
        float refractionZ = dot(normal, normalize(viewDir + vec3(-0.1, -0.1, 0.0)));
        
        vec3 rColor = vec3(
            pow(refractionX * 0.9, 3.0),
            pow(refractionY * 1.0, 3.0),
            pow(refractionZ * 1.1, 3.0)
        );
        spec = pow(max(dot(normal, halfVector), 0.0), 128.0) * 0.1; 
        
        // Much higher base color contribution (0.85) to avoid shallow look
        finalColor = baseColor.rgb * 0.85 + rColor * 0.2 + vec3(1.0) * spec;
        
        // Internal dark edge for glass simulation
        float edge = smoothstep(-0.2, 0.0, sdf);
        finalColor *= mix(1.0, 0.2, edge);
    } else {
        // 3. Gold: High-end reflective metal with subtle grain and iridescent sheen
        float NdotH = dot(normal, halfVector);
        spec = pow(max(NdotH, 0.0), 128.0) * 3.0;
        
        // Simulating gold's characteristic orange-yellow reflection
        vec3 goldBase = vec3(1.0, 0.85, 0.3) * baseColor.rgb;
        
        // Fake environment reflection (simplified)
        float env = 0.5 + 0.5 * normal.y;
        spec *= 0.15;
        
        // Darker gold base
        finalColor = goldBase * (0.6 + 0.4 * diff) + vec3(1.0, 0.9, 0.5) * spec;
        finalColor += vec3(0.015, 0.01, 0.0) * env; 
        
        // Glitter effect (Procedural noise simulation)
        float glitter = sin(p.x * 100.0) * cos(p.y * 100.0);
        if (glitter > 0.95) finalColor += vec3(1.0) * spec * 0.5;
    }
    
    // Soft anti-aliased edge
    float alpha = smoothstep(0.0, -0.015, sdf);
    
    // Desaturate if locked
    if (uUnlocked < 0.5) {
        float luminance = dot(finalColor, vec3(0.299, 0.587, 0.114));
        // Ultra-dark matte for maximum contrast with iconography
        finalColor = vec3(0.05); 
    }
    
    fragColor = vec4(finalColor * alpha, alpha);
}
