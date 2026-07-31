#version 330 core

out vec2 texCoord;

void main() {
    vec2 pos = vec2(
        float((gl_VertexID & 1) << 2) - 1.0,
        float((gl_VertexID & 2) << 1) - 1.0
    );
    gl_Position = vec4(pos, 0.0, 1.0);
    texCoord = pos * 0.5 + 0.5;
}