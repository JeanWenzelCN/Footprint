#include <flutter/runtime_effect.glsl>

// Uniforms passed from Dart
uniform vec2 resolution;    // Screen resolution
uniform float progress;     // 0.0 → 1.0, ripple expansion factor
uniform sampler2D inputTex; // The source image (frosted overlay + astrolabe)

out vec4 fragColor;

void main() {
    vec2 fragCoord = FlutterFragCoord().xy;
    vec2 uv = fragCoord / resolution;
    vec2 center = vec2(0.5, 0.5);

    // Distance from center
    float dist = length(uv - center);

    // The wave front position (expands from 0 to ~1.2)
    float waveFront = progress * 1.3;

    // Width of the sine-wave distortion band
    float bandWidth = 0.12;
    float distFromWave = dist - waveFront;

    // Behind the wave = fully transparent (reveal map beneath)
    if (distFromWave < -bandWidth * 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    // Ahead of the wave = original content (frosted overlay)
    if (distFromWave > bandWidth * 0.5) {
        fragColor = texture(inputTex, uv);
        return;
    }

    // Inside the wave band: sine displacement for "glass refraction"
    float t = (distFromWave + bandWidth * 0.5) / bandWidth; // 0→1 across band
    float wave = sin(t * 3.14159265 * 3.0); // 3 sine peaks

    // Displacement magnitude (stronger at the front edge)
    float amplitude = 0.025 * (1.0 - progress * 0.5);
    vec2 dir = normalize(uv - center + vec2(0.001));
    vec2 displaceUV = uv + dir * wave * amplitude;

    // Clamp UV
    displaceUV = clamp(displaceUV, vec2(0.0), vec2(1.0));

    // Sample displaced content
    vec4 displaced = texture(inputTex, displaceUV);

    // Fade factor across the band (1 at front → 0 behind)
    float fadeFactor = smoothstep(-bandWidth * 0.5, bandWidth * 0.5, distFromWave);

    // Add a bright "water highlight" at the wave edge
    float highlight = pow(1.0 - abs(t - 0.5) * 2.0, 3.0) * 0.35;
    vec3 highlightColor = vec3(1.0, 0.98, 0.92); // warm white

    fragColor = vec4(displaced.rgb + highlightColor * highlight, displaced.a * fadeFactor);
}
