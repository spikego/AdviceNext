#version 330 core
in vec2 texCoord;
uniform sampler2D DiffuseSampler;
out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / textureSize(DiffuseSampler, 0);
    
    vec4 center = texture(DiffuseSampler, texCoord);
    vec4 up     = texture(DiffuseSampler, texCoord + vec2(0.0, -texelSize.y));
    vec4 down   = texture(DiffuseSampler, texCoord + vec2(0.0,  texelSize.y));
    vec4 left   = texture(DiffuseSampler, texCoord + vec2(-texelSize.x, 0.0));
    vec4 right  = texture(DiffuseSampler, texCoord + vec2( texelSize.x, 0.0));
    
    fragColor = center * 0.5 + (up + down + left + right) * 0.125;
}
