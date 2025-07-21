package cn.advicenext.render.shader;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * 高斯模糊着色器
 */
public class BlurShader extends Shader {
    private int directionUniform;
    private int radiusUniform;
    private int resolutionUniform;
    
    private float dirX = 0.0f;
    private float dirY = 0.0f;
    private float radius = 5.0f;
    
    // 默认着色器代码
    private static final String DEFAULT_VERTEX_SHADER = 
            "#version 330 core\n" +
            "layout (location = 0) in vec3 position;\n" +
            "layout (location = 1) in vec2 texCoord;\n" +
            "out vec2 fragTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(position.xyz, 1.0);\n" +
            "    fragTexCoord = texCoord;\n" +
            "}\n";
    
    private static final String DEFAULT_FRAGMENT_SHADER = 
            "#version 330 core\n" +
            "in vec2 fragTexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D textureSampler;\n" +
            "uniform vec2 direction;\n" +
            "uniform float radius;\n" +
            "uniform vec2 resolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = fragTexCoord;\n" +
            "    vec2 pixelSize = 1.0 / resolution;\n" +
            "    float sigma = radius / 3.0;\n" +
            "    float sigmaSquared = sigma * sigma;\n" +
            "    \n" +
            "    // 计算高斯权重\n" +
            "    float weightSum = 0.0;\n" +
            "    vec4 colorSum = vec4(0.0);\n" +
            "    \n" +
            "    // 中心像素\n" +
            "    float weight = 1.0 / (2.0 * 3.14159265 * sigmaSquared);\n" +
            "    colorSum += texture(textureSampler, uv) * weight;\n" +
            "    weightSum += weight;\n" +
            "    \n" +
            "    // 采样周围像素\n" +
            "    for (float i = 1.0; i <= radius; i += 1.0) {\n" +
            "        weight = exp(-((i * i) / (2.0 * sigmaSquared))) / (2.0 * 3.14159265 * sigmaSquared);\n" +
            "        \n" +
            "        colorSum += texture(textureSampler, uv + i * pixelSize * direction) * weight;\n" +
            "        colorSum += texture(textureSampler, uv - i * pixelSize * direction) * weight;\n" +
            "        \n" +
            "        weightSum += 2.0 * weight;\n" +
            "    }\n" +
            "    \n" +
            "    fragColor = colorSum / weightSum;\n" +
            "}\n";
    
    public BlurShader() {
        this(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }
    
    public BlurShader(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader, null);
        
        // 获取Uniform位置
        directionUniform = GlStateManager._glGetUniformLocation(program, "direction");
        radiusUniform = GlStateManager._glGetUniformLocation(program, "radius");
        resolutionUniform = GlStateManager._glGetUniformLocation(program, "resolution");
    }
    
    @Override
    public void use() {
        super.use();
        
        // 设置Uniform值
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 设置方向向量
            FloatBuffer dirBuffer = stack.mallocFloat(2);
            dirBuffer.put(0, dirX);
            dirBuffer.put(1, dirY);
            GlStateManager._glUniform2(directionUniform, dirBuffer);
            
            // 设置半径
            FloatBuffer radiusBuffer = stack.mallocFloat(1);
            radiusBuffer.put(0, radius);
            GlStateManager._glUniform1(radiusUniform, radiusBuffer);
            
            // 设置分辨率
            MinecraftClient mc = MinecraftClient.getInstance();
            float width = mc.getWindow().getFramebufferWidth();
            float height = mc.getWindow().getFramebufferHeight();
            
            FloatBuffer resBuffer = stack.mallocFloat(2);
            resBuffer.put(0, width);
            resBuffer.put(1, height);
            GlStateManager._glUniform2(resolutionUniform, resBuffer);
        }
    }
    
    /**
     * 设置模糊方向
     * @param x X方向
     * @param y Y方向
     */
    public void setDirection(float x, float y) {
        this.dirX = x;
        this.dirY = y;
    }
    
    /**
     * 设置模糊半径
     * @param radius 半径
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }
}