#version 330 core

in vec2 texCoord;
in vec4 vertexColor;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord);
    fragColor = texel * vertexColor;
}
