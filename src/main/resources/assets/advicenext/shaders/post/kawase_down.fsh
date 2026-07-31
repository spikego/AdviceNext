#version 330 core
in vec2 texCoord;
uniform sampler2D DiffuseSampler;
out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / textureSize(DiffuseSampler, 0);
    vec2 halfStep = texelSize * 0.5;
    
    vec4 sum  = texture(DiffuseSampler, texCoord + vec2(-halfStep.x, -halfStep.y));
    sum += texture(DiffuseSampler, texCoord + vec2( halfStep.x, -halfStep.y));
    sum += texture(DiffuseSampler, texCoord + vec2(-halfStep.x,  halfStep.y));
    sum += texture(DiffuseSampler, texCoord + vec2( halfStep.x,  halfStep.y));
    
    fragColor = sum * 0.25;
}
