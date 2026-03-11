#include <flutter/runtime_effect.glsl>

uniform vec2 resolution;
uniform float time;
uniform float intensity; // how heavy the rain is 0.0 to 1.0

out vec4 fragColor;

// Random function for raindrops
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec2 uv = FlutterFragCoord().xy / resolution.xy;
    
    // Gaussian-like blur at the edges (Vignette blur proxy)
    // We achieve the "blur" mask by outputting high alpha at edges, so flutter can use an ImageFilter.blur 
    // Wait, the prompt asks for "整个记忆地图的边缘会被叠加一层淡淡的高斯模糊，几滴由着色器实时计算出折射光影的“虚拟雨滴”，会沿着屏幕玻璃缓缓滑落"
    
    // Edge vignette calculation for blur mask
    float edge = distance(uv, vec2(0.5));
    float blurMask = smoothstep(0.3, 0.8, edge);
    
    // Virtual Raindrops overlapping
    vec2 grid = uv * vec2(10.0, 5.0); // Rain grid
    vec2 id = floor(grid);
    vec2 f = fract(grid);
    
    // Offset each column randomly
    float randomColumn = hash(vec2(id.x, 0.0));
    id.y += time * (1.0 + randomColumn); // falling down
    
    vec2 dropId = floor(grid + vec2(0.0, time * (1.0 + randomColumn)));
    vec2 dropLocal = fract(grid + vec2(0.0, time * (1.0 + randomColumn))) - 0.5;
    
    // Position of drop in cell
    float randomDrop = hash(dropId);
    vec2 dropPos = vec2(
        hash(dropId + 0.1) * 0.4 - 0.2,
        hash(dropId + 0.2) * 0.8 - 0.4
    );
    
    float distToDrop = length(dropLocal - dropPos);
    
    // Refractive light calculation
    // Simulated normal
    vec2 normal = normalize(dropLocal - dropPos);
    
    // Reflection/refraction highlights
    float highlight = max(0.0, dot(normal, normalize(vec2(1.0, 1.0))));
    highlight = pow(highlight, 4.0) * smoothstep(0.2, 0.1, distToDrop) * intensity;
    
    // Water droplet alpha
    float dropAlpha = smoothstep(0.15, 0.1, distToDrop) * intensity;
    
    // Overlay the drop on top of the map
    vec3 baseColor = vec3(0.1, 0.1, 0.1); 
    // We just output an alpha map, then Flutter can layer it.
    
    vec4 finalOut = vec4(highlight + baseColor * blurMask * 0.2, max(blurMask * 0.3 * intensity, dropAlpha));
    fragColor = finalOut;
}
