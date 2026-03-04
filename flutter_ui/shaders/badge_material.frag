#include <flutter/runtime_effect.glsl>

uniform vec2 resolution;
uniform vec3 lightPos;
uniform vec4 baseColor;
uniform float materialType;

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
        spec = pow(max(aniso, 0.0), 32.0) * 1.5;
        
        finalColor = finalColor * (0.3 + 0.7 * diff) + vec3(1.0, 0.9, 0.8) * spec;
        
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
        
        finalColor = baseColor.rgb * (0.5 + 0.5 * diff);
        finalColor += baseColor.rgb * intenseGlow * 2.0;
        finalColor += vec3(0.0, 1.0, 1.0) * rim * 1.5; // Cyan rim
    } else {
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
        spec = pow(max(dot(normal, halfVector), 0.0), 64.0) * 2.5;
        
        finalColor = baseColor.rgb * 0.2 + rColor * 1.5 + vec3(1.0) * spec;
        
        // Internal dark edge for glass simulation
        float edge = smoothstep(-0.2, 0.0, sdf);
        finalColor *= mix(1.0, 0.2, edge);
    }
    
    // Soft anti-aliased edge
    float alpha = smoothstep(0.0, -0.015, sdf);
    
    fragColor = vec4(finalColor * alpha, alpha);
}
