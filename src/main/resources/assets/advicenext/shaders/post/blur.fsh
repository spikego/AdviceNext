#version 330 core
in vec2 fragTexCoord;
out vec4 fragColor;
uniform sampler2D textureSampler;
uniform vec2 direction;
uniform float radius;
uniform vec2 resolution;

void main() {
    vec2 uv = fragTexCoord;
    vec2 pixelSize = 1.0 / resolution;
    float sigma = radius / 3.0;
    float sigmaSquared = sigma * sigma;
    
    // 计算高斯权重
    float weightSum = 0.0;
    vec4 colorSum = vec4(0.0);
    
    // 中心像素
    float weight = 1.0 / (2.0 * 3.14159265 * sigmaSquared);
    colorSum += texture(textureSampler, uv) * weight;
    weightSum += weight;
    
    // 采样周围像素
    for (float i = 1.0; i <= radius; i += 1.0) {
        weight = exp(-((i * i) / (2.0 * sigmaSquared))) / (2.0 * 3.14159265 * sigmaSquared);
        
        colorSum += texture(textureSampler, uv + i * pixelSize * direction) * weight;
        colorSum += texture(textureSampler, uv - i * pixelSize * direction) * weight;
        
        weightSum += 2.0 * weight;
    }
    
    fragColor = colorSum / weightSum;
}