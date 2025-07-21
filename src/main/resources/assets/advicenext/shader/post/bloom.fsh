#version 330 core
in vec2 fragTexCoord;
out vec4 fragColor;
uniform sampler2D textureSampler;
uniform float threshold;
uniform float intensity;
uniform vec2 resolution;

void main() {
    vec4 color = texture(textureSampler, fragTexCoord);
    
    // 计算亮度
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    
    // 提取高亮部分
    vec3 brightPass = color.rgb * smoothstep(threshold, threshold + 0.2, brightness);
    
    // 应用强度
    brightPass *= intensity;
    
    fragColor = vec4(brightPass, color.a);
}