package cn.advicenext.render.shader;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 着色器管理器 - 简化版
 */
public class ShaderManager {
    private static final ShaderManager INSTANCE = new ShaderManager();
    private final Map<String, Shader> shaders = new HashMap<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // 帧缓冲区
    private Framebuffer mainBuffer;
    private Framebuffer blurBuffer;
    private Framebuffer bloomBuffer;

    // 基础着色器
    private Shader basicShader;

    private ShaderManager() {}

    public static ShaderManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化着色器系统
     */
    public void init() {
        recreateFramebuffers();
        loadShaders();
    }

    /**
     * 创建帧缓冲区
     */
    public void recreateFramebuffers() {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        // 释放旧的帧缓冲区
        if (mainBuffer != null) mainBuffer.delete();
        if (blurBuffer != null) blurBuffer.delete();
        if (bloomBuffer != null) bloomBuffer.delete();

        // 创建新的帧缓冲区
        mainBuffer = new SimpleFramebuffer("ShaderMain", width, height, true);
        blurBuffer = new SimpleFramebuffer("BlurBuffer", width, height, false);
        bloomBuffer = new SimpleFramebuffer("BloomBuffer", width, height, false);
    }

    /**
     * 加载着色器
     */
    private void loadShaders() {
        // 释放旧的着色器
        for (Shader shader : shaders.values()) {
            shader.close();
        }
        shaders.clear();

        // 加载基础着色器
        try {
            String vertexShader = loadShaderSource("advicenext", "shader/program/basic.vsh");
            String fragmentShader = loadShaderSource("advicenext", "shader/program/basic.fsh");
            basicShader = new Shader(vertexShader, fragmentShader, null);
        } catch (IOException e) {
            // 如果加载失败，使用内联着色器
            basicShader = new Shader(
                "#version 330 core\n" +
                "layout (location = 0) in vec3 position;\n" +
                "layout (location = 1) in vec2 texCoord;\n" +
                "out vec2 fragTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(position.xyz, 1.0);\n" +
                "    fragTexCoord = texCoord;\n" +
                "}\n",

                "#version 330 core\n" +
                "in vec2 fragTexCoord;\n" +
                "out vec4 fragColor;\n" +
                "uniform sampler2D textureSampler;\n" +
                "void main() {\n" +
                "    fragColor = texture(textureSampler, fragTexCoord);\n" +
                "}\n",

                null
            );
        }

        // 加载模糊着色器
        try {
            String vertexShader = loadShaderSource("advicenext", "shader/post/blur.vsh");
            String fragmentShader = loadShaderSource("advicenext", "shader/post/blur.fsh");
            shaders.put("blur", new BlurShader(vertexShader, fragmentShader));
        } catch (IOException e) {
            shaders.put("blur", new BlurShader());
        }

        // 加载泛光着色器
        try {
            String vertexShader = loadShaderSource("advicenext", "shader/post/bloom.vsh");
            String fragmentShader = loadShaderSource("advicenext", "shader/post/bloom.fsh");
            shaders.put("bloom", new BloomShader(vertexShader, fragmentShader));
        } catch (IOException e) {
            shaders.put("bloom", new BloomShader());
        }
    }

    /**
     * 从资源文件加载着色器源代码
     */
    private String loadShaderSource(String namespace, String path) throws IOException {
        Identifier id = Identifier.of(namespace, path);
        Resource resource = mc.getResourceManager().getResource(id).orElseThrow();

        try (InputStream inputStream = resource.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * 应用模糊效果
     * @param radius 模糊半径
     * @param iterations 迭代次数
     */
    public void applyBlur(float radius, int iterations) {
        Shader blurShader = shaders.get("blur");
        if (blurShader == null) return;

        // 保存当前帧缓冲区
        Framebuffer originalBuffer = mc.getFramebuffer();

        // 复制到主缓冲区
        copyFramebuffer(originalBuffer, mainBuffer);

        // 应用模糊
        for (int i = 0; i < iterations; i++) {
            // 水平模糊
            drawWithShader(mainBuffer, blurBuffer, blurShader, shader -> {
                ((BlurShader) shader).setDirection(1.0f, 0.0f);
                ((BlurShader) shader).setRadius(radius);
            });

            // 垂直模糊
            drawWithShader(blurBuffer, mainBuffer, blurShader, shader -> {
                ((BlurShader) shader).setDirection(0.0f, 1.0f);
                ((BlurShader) shader).setRadius(radius);
            });
        }

        // 复制回原始缓冲区
        copyFramebuffer(mainBuffer, originalBuffer);
    }

    /**
     * 应用泛光效果
     * @param threshold 亮度阈值
     * @param intensity 强度
     */
    public void applyBloom(float threshold, float intensity) {
        Shader bloomShader = shaders.get("bloom");
        Shader blurShader = shaders.get("blur");
        if (bloomShader == null || blurShader == null) return;

        // 保存当前帧缓冲区
        Framebuffer originalBuffer = mc.getFramebuffer();

        // 复制到主缓冲区
        copyFramebuffer(originalBuffer, mainBuffer);

        // 提取高亮部分
        drawWithShader(mainBuffer, bloomBuffer, bloomShader, shader -> {
            ((BloomShader) shader).setThreshold(threshold);
            ((BloomShader) shader).setIntensity(intensity);
        });

        // 应用模糊
        for (int i = 0; i < 3; i++) {
            // 水平模糊
            drawWithShader(bloomBuffer, mainBuffer, blurShader, shader -> {
                ((BlurShader) shader).setDirection(1.0f, 0.0f);
                ((BlurShader) shader).setRadius(5.0f);
            });

            // 垂直模糊
            drawWithShader(mainBuffer, bloomBuffer, blurShader, shader -> {
                ((BlurShader) shader).setDirection(0.0f, 1.0f);
                ((BlurShader) shader).setRadius(5.0f);
            });
        }

        // 混合结果
        copyFramebuffer(mainBuffer, originalBuffer);

        // 启用混合
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);

        // 绘制泛光缓冲区
        drawTexture(bloomBuffer.getColorAttachment());

        // 禁用混合
        GlStateManager._disableBlend();
    }

    /**
     * 使用着色器绘制
     */
    private void drawWithShader(Framebuffer source, Framebuffer target, Shader shader, ShaderSetup setup) {
        // 设置渲染目标
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(target.getColorAttachment(), null).close();

        // 使用着色器
        shader.use();
        setup.setup(shader);

        // 绘制源纹理
        drawTexture(source.getColorAttachment());

        // 停止着色器
        shader.stop();
    }

    /**
     * 复制帧缓冲区
     */
    private void copyFramebuffer(Framebuffer source, Framebuffer target) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            source.getColorAttachment(),
            target.getColorAttachment(),
            0, 0, 0, 0, 0,
            source.textureWidth,
            source.textureHeight
        );
    }

    /**
     * 绘制纹理
     */
    private void drawTexture(GpuTexture texture) {
        // 使用基础着色器
        basicShader.use();

        // 设置纹理
        RenderSystem.setShaderTexture(0, texture);

        // 使用原始OpenGL绘制全屏四边形
        drawFullscreenQuad();

        // 停止着色器
        basicShader.stop();
    }

    /**
     * 绘制全屏四边形
     */
    private void drawFullscreenQuad() {
        // 使用原始OpenGL绘制四边形
        GL11.glBegin(GL11.GL_QUADS);

        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-1.0f, -1.0f, 0.0f);

        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(1.0f, -1.0f, 0.0f);

        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(1.0f, 1.0f, 0.0f);

        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-1.0f, 1.0f, 0.0f);

        GL11.glEnd();
    }
    
    /**
     * 获取着色器
     */
    public Shader getShader(String name) {
        return shaders.get(name);
    }
    
    /**
     * 释放资源
     */
    public void cleanup() {
        for (Shader shader : shaders.values()) {
            shader.close();
        }
        shaders.clear();
        
        if (basicShader != null) {
            basicShader.close();
        }
        
        if (mainBuffer != null) mainBuffer.delete();
        if (blurBuffer != null) blurBuffer.delete();
        if (bloomBuffer != null) bloomBuffer.delete();
    }
    
    /**
     * 着色器设置接口
     */
    private interface ShaderSetup {
        void setup(Shader shader);
    }
}