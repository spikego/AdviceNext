#version 330 core
in vec2 texCoord;
out vec4 fragColor;
uniform sampler2D textureSampler;
layout(std140) uniform threshold { float t; };
layout(std140) uniform intensity { float i; };
layout(std140) uniform resolution { vec2 res; };

void main() {
    vec4 color = texture(textureSampler, texCoord);

    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    vec3 brightPass = color.rgb * smoothstep(t, t + 0.2, brightness);

    brightPass *= i;

    fragColor = vec4(brightPass, color.a);
}