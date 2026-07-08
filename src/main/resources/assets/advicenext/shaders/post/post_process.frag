#version 330 core

in vec2 texCoord;

uniform sampler2D DiffuseSampler;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    fragColor = color;
}