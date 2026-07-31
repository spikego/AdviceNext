#version 330 core
in vec2 texCoord;
uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
out vec4 fragColor;

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec4 bloom = texture(BloomSampler, texCoord);
    fragColor = original + bloom;
}
