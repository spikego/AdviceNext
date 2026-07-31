#version 330 core
in vec2 texCoord;
out vec4 fragColor;
uniform sampler2D textureSampler;
layout(std140) uniform directionX { float dirX; };
layout(std140) uniform directionY { float dirY; };
layout(std140) uniform radius { float rad; };

void main() {
    vec2 uv = texCoord;
    vec2 pixelSize = 1.0 / textureSize(textureSampler, 0);
    float sigma = rad / 3.0;
    float sigmaSquared = sigma * sigma;
    vec2 direction = vec2(dirX, dirY);

    float weightSum = 0.0;
    vec4 colorSum = vec4(0.0);

    float weight = 1.0 / (2.0 * 3.14159265 * sigmaSquared);
    colorSum += texture(textureSampler, uv) * weight;
    weightSum += weight;

    for (float i = 1.0; i <= rad; i += 1.0) {
        weight = exp(-((i * i) / (2.0 * sigmaSquared))) / (2.0 * 3.14159265 * sigmaSquared);

        colorSum += texture(textureSampler, uv + i * pixelSize * direction) * weight;
        colorSum += texture(textureSampler, uv - i * pixelSize * direction) * weight;

        weightSum += 2.0 * weight;
    }

    fragColor = colorSum / weightSum;
}