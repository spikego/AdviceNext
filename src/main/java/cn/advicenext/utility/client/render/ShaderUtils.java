package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.UniformValue;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 着色器工具类：管线创建、帧缓冲链管理、全屏 blit。
 * 所有方法使用 {@link RenderPass} 直接提交，不依赖 DrawContext 或 RenderLayer。
 */
public final class ShaderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private ShaderUtils() {}

    // ==================== Pipeline 创建 ====================

    /**
     * 以 POST_EFFECT_PROCESSOR_SNIPPET 为基础创建全屏三角形管线。
     *
     * @param fragId   片段着色器路径（如 {@code Identifier.of("advicenext", "post/bright_pass")}）
     * @param locId    管线唯一标识
     * @param samplers 采样器名称列表
     * @param uniforms 统一变量名称列表（会包装为 UNIFORM_BUFFER）
     */
    public static RenderPipeline createPipeline(Identifier fragId, Identifier locId, String[] samplers, String[] uniforms) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withVertexShader(Identifier.of("advicenext", "post/post_process"))
            .withFragmentShader(fragId)
            .withLocation(locId)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES);

        for (String s : samplers) builder.withSampler(s + "Sampler");
        for (String u : uniforms) builder.withUniform(u, UniformType.UNIFORM_BUFFER);

        return builder.build();
    }

    // ==================== 帧缓冲链 ====================

    /** 创建 1/2 -> 1/4 -> … 递减的帧缓冲链 */
    public static SimpleFramebuffer[] createChain(int levels, int baseW, int baseH) {
        SimpleFramebuffer[] chain = new SimpleFramebuffer[levels];
        for (int i = 0; i < levels; i++) {
            int divisor = 1 << (i + 1);
            chain[i] = new SimpleFramebuffer("advicenext_shader_" + i,
                Math.max(1, baseW / divisor), Math.max(1, baseH / divisor), false);
        }
        return chain;
    }

    /** 按实际主屏尺寸重建链（尺寸变化时自动重分配） */
    public static SimpleFramebuffer[] ensureChain(SimpleFramebuffer[] old, int levels, int[] lastDim) {
        int w = mc.getFramebuffer().textureWidth;
        int h = mc.getFramebuffer().textureHeight;
        if (old != null && lastDim != null && lastDim[0] == w && lastDim[1] == h) return old;

        if (old != null) for (SimpleFramebuffer fb : old) fb.delete();
        if (lastDim != null) { lastDim[0] = w; lastDim[1] = h; }
        return createChain(levels, w, h);
    }

    // ==================== Blit ====================

    /** 用 ENTITY_OUTLINE_BLIT 管线做双线性缩放 blit（线性过滤） */
    public static void blit(GpuTextureView src, GpuTextureView dst) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_blit", dst, OptionalInt.empty())) {
            pass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("InSampler", src, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    /** 用自定义管线做 blit */
    public static void blitWithPipeline(GpuTextureView src, GpuTextureView dst, RenderPipeline pipeline,
                                         Map<String, GpuTextureView> textures, Map<String, GpuBuffer> uniforms) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_custom_blit", dst, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);

            if (textures != null) {
                for (Map.Entry<String, GpuTextureView> e : textures.entrySet()) {
                    pass.bindTexture(e.getKey() + "Sampler", e.getValue(),
                        RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                }
            }

            if (uniforms != null) {
                for (Map.Entry<String, GpuBuffer> e : uniforms.entrySet()) {
                    pass.setUniform(e.getKey(), e.getValue());
                }
            }

            pass.draw(0, 3);
        }
    }

    /** 用自定义管线做 blit，带 scissor（像素坐标，左下角原点） */
    public static void blitWithPipeline(GpuTextureView src, GpuTextureView dst, RenderPipeline pipeline,
                                         Map<String, GpuTextureView> textures, Map<String, GpuBuffer> uniforms,
                                         int scissorX, int scissorY, int scissorW, int scissorH) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_custom_scissor", dst, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.enableScissor(scissorX, scissorY, scissorW, scissorH);
            RenderSystem.bindDefaultUniforms(pass);

            if (textures != null) {
                for (Map.Entry<String, GpuTextureView> e : textures.entrySet()) {
                    pass.bindTexture(e.getKey() + "Sampler", e.getValue(),
                        RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                }
            }

            if (uniforms != null) {
                for (Map.Entry<String, GpuBuffer> e : uniforms.entrySet()) {
                    pass.setUniform(e.getKey(), e.getValue());
                }
            }

            pass.draw(0, 3);
        }
    }

    /** 简化的单纹理自定义管线 blit */
    public static void blitCustom(GpuTextureView src, GpuTextureView dst, RenderPipeline pipeline) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_custom", dst, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("DiffuseSampler", src, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    /**
     * 绘制全屏三角形到主帧缓冲。
     * 用于 blur 结果回写、bloom 复合等场景。
     */
    public static void drawQuads(RenderPipeline pipeline, String samplerName, GpuTextureView texture) {
        GpuTextureView mainView = mc.getFramebuffer().getColorAttachmentView();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_quads", mainView, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture(samplerName + "Sampler", texture,
                RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    /** 将纹理全屏绘制到主帧缓冲（用 ENTITY_OUTLINE_BLIT 管线） */
    public static void drawToMain(GpuTextureView texture) {
        drawQuads(RenderPipelines.ENTITY_OUTLINE_BLIT, "In", texture);
    }

    /**
     * 绘制全屏三角形到指定目标帧缓冲。
     * @param samplerName 采样器名称（如 "In"、"Diffuse"）
     * @param texture     源纹理
     * @param dst         目标帧缓冲（其颜色附着将作为渲染目标）
     */
    public static void drawQuads(String samplerName, GpuTextureView texture, SimpleFramebuffer dst) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_quads_dst", dst.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture(samplerName + "Sampler", texture,
                RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    /**
     * 将纹理全屏绘制到指定帧缓冲（用 ENTITY_OUTLINE_BLIT 管线）。
     */
    public static void drawToFramebuffer(GpuTextureView texture, SimpleFramebuffer dst) {
        drawQuads("In", texture, dst);
    }

    /**
     * 创建指定尺寸的帧缓冲。
     */
    public static SimpleFramebuffer createFramebuffer(String name, int w, int h) {
        return new SimpleFramebuffer(name, Math.max(1, w), Math.max(1, h), false);
    }

    // ==================== Uniform 缓冲 ====================

    /** 创建单值浮点 uniform 的 GPU 缓冲区（Std140） */
    public static GpuBuffer createFloatUniform(String name, float value) {
        return createUniformBuffer(name, List.of(new UniformValue.FloatValue(value)));
    }

    /** 从 UniformValue 列表创建统一缓冲区 */
    public static GpuBuffer createUniformBuffer(String name, List<UniformValue> values) {
        Std140SizeCalculator calc = new Std140SizeCalculator();
        for (UniformValue v : values) v.addSize(calc);
        int size = calc.get();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, size);
            for (UniformValue v : values) v.write(builder);
            return RenderSystem.getDevice().createBuffer(
                () -> name, GpuBuffer.USAGE_UNIFORM, builder.get());
        }
    }

    // ==================== 帧缓冲视图 ====================

    /** 主帧缓冲颜色纹理视图 */
    public static GpuTextureView mainColorView() {
        return mc.getFramebuffer().getColorAttachmentView();
    }
}