#include <flutter/runtime_effect.glsl>

// Uniforms passed from Dart
uniform vec2 resolution;    // Screen resolution
uniform float progress;     // 0.0 → 1.0, ripple expansion factor

out vec4 fragColor;

void main() {
    vec2 fragCoord = FlutterFragCoord().xy;
    vec2 uv = fragCoord / resolution;
    vec2 center = vec2(0.5, 0.5);

    // Distance from center
    float dist = length(uv - center);

    // The wave front position (expands from 0 to ~1.4)
    float waveFront = progress * 1.4;

    // Width of the distortion/highlight band
    float bandWidth = 0.15;
    float distFromWave = dist - waveFront;

    // Behind the wave = fully transparent (reveal content beneath)
    if (distFromWave < -bandWidth * 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    // Base "frosted" color to match the UI dialog overlay
    vec4 baseColor = vec4(0.85, 0.80, 0.75, 0.25);

    // Ahead of the wave = frosted overlay
    if (distFromWave > bandWidth * 0.5) {
        fragColor = baseColor;
        return;
    }

    // Inside the wave band: smooth transition and highlight
    float t = (distFromWave + bandWidth * 0.5) / bandWidth; // 0→1 across band
    
    // Wave highlight peak in the middle of the band
    float highlight = pow(sin(t * 3.14159), 2.0) * 0.4;
    vec3 highlightColor = vec3(1.0, 1.0, 0.95);

    // Fade alpha from 0 (behind) to baseColor.a (ahead)
    float alpha = baseColor.a * smoothstep(0.0, 1.0, t);

    fragColor = vec4(baseColor.rgb + highlightColor * highlight, alpha);
}

