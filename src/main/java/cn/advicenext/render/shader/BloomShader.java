package cn.advicenext.render.shader;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * 泛光着色器
 */
public class BloomShader extends Shader {
    private int thresholdUniform;
    private int intensityUniform;
    private int resolutionUniform;
    
    private float threshold = 0.7f;
    private float intensity = 1.0f;
    
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
            "uniform float threshold;\n" +
            "uniform float intensity;\n" +
            "uniform vec2 resolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 color = texture(textureSampler, fragTexCoord);\n" +
            "    \n" +
            "    // 计算亮度\n" +
            "    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));\n" +
            "    \n" +
            "    // 提取高亮部分\n" +
            "    vec3 brightPass = color.rgb * smoothstep(threshold, threshold + 0.2, brightness);\n" +
            "    \n" +
            "    // 应用强度\n" +
            "    brightPass *= intensity;\n" +
            "    \n" +
            "    fragColor = vec4(brightPass, color.a);\n" +
            "}\n";
    
    public BloomShader() {
        this(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }
    
    public BloomShader(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader, null);
        
        // 获取Uniform位置
        thresholdUniform = GlStateManager._glGetUniformLocation(program, "threshold");
        intensityUniform = GlStateManager._glGetUniformLocation(program, "intensity");
        resolutionUniform = GlStateManager._glGetUniformLocation(program, "resolution");
    }
    
    @Override
    public void use() {
        super.use();
        
        // 设置Uniform值
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 设置阈值
            FloatBuffer thresholdBuffer = stack.mallocFloat(1);
            thresholdBuffer.put(0, threshold);
            GlStateManager._glUniform1(thresholdUniform, thresholdBuffer);
            
            // 设置强度
            FloatBuffer intensityBuffer = stack.mallocFloat(1);
            intensityBuffer.put(0, intensity);
            GlStateManager._glUniform1(intensityUniform, intensityBuffer);
            
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
     * 设置亮度阈值
     * @param threshold 阈值
     */
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    /**
     * 设置泛光强度
     * @param intensity 强度
     */
    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }
}