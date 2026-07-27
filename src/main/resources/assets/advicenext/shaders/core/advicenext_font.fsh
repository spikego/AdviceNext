#version 330 core

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    float smoothness = 0.075;
    float alpha = smoothstep(0.5 - smoothness, 0.5 + smoothness, color.a);
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}