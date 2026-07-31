#version 330 core
in vec2 texCoord;
uniform sampler2D DiffuseSampler;
layout(std140) uniform uThreshold { float threshold; };
layout(std140) uniform uIntensity { float intensity; };
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    float factor = smoothstep(threshold - 0.1, threshold + 0.1, brightness);
    fragColor = vec4(color.rgb * factor * intensity, color.a);
}