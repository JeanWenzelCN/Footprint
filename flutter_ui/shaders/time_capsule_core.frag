#include <flutter/runtime_effect.glsl>

uniform vec2 resolution;
uniform float time;
uniform float progress; // 0.0 to 1.0 (approaching unlock date)

out vec4 fragColor;

void main() {
    vec2 uv = FlutterFragCoord().xy / resolution.xy;
    // Assume square bounds for the glowing sphere marker
    vec2 centered = uv * 2.0 - 1.0;
    float dist = length(centered);
    
    // Heartbeat frequency: slowly approaches ~60-80 BPM (1.0 to 1.33 Hz)
    float bpm = mix(15.0, 75.0, progress); 
    float freq = bpm / 60.0;
    
    // Non-linear brightness growth (pow 3 gives very slow initial growth)
    float coreBrightness = pow(progress, 3.0) * 0.8 + 0.2;
    
    // Breathing/pulsing effect
    float pulse = sin(time * freq * 6.28318) * 0.5 + 0.5;
    
    // Fluid halo perturbation
    float angle = atan(centered.y, centered.x);
    float fluid = sin(angle * 3.0 + time * 1.5) * 0.05 + sin(angle * 5.0 - time) * 0.03;
    
    // Core glow and edge calculation
    float glow = smoothstep(0.7 + fluid, 0.0, dist);
    
    // Color progression: Deep blue-ish initially, transitioning to warm amber/gold
    vec3 color = mix(vec3(0.2, 0.4, 0.6), vec3(1.0, 0.85, 0.4), progress);
    
    // Pulse influences mainly the outer halo
    float edgePulse = smoothstep(0.3, 0.8, dist) * pulse * 0.5;
    
    vec3 finalColor = color * (glow * coreBrightness + edgePulse);
    
    float alpha = smoothstep(0.9, 0.75, dist + fluid);
    
    fragColor = vec4(finalColor, alpha) * alpha;
}
