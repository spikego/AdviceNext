package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;

import java.util.OptionalInt;

/**
 * 高斯模糊渲染器：Kawase 多级降采样模糊。
 * 将主帧缓冲内容逐级半尺寸拷贝（双线性采样天然扩散），
 * 得到不同强度的模糊结果。模糊等级越高越糊、性能开销越小。
 *
 * 结果以 {@link GpuTextureView} 形式返回，由调用方自行采样显示
 * （例如作为自定义 pipeline 的纹理画到界面区域）。
 */
public final class BlurRenderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final int LEVELS = 4;
    private static SimpleFramebuffer[] chain;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private BlurRenderer() {
    }

    private static void ensureChain() {
        Framebuffer main = mc.getFramebuffer();
        int w = Math.max(1, main.textureWidth);
        int h = Math.max(1, main.textureHeight);

        if (chain != null && lastWidth == w && lastHeight == h) return;

        dispose();
        chain = new SimpleFramebuffer[LEVELS];
        for (int i = 0; i < LEVELS; i++) {
            int divisor = 1 << (i + 1);
            chain[i] = new SimpleFramebuffer("advicenext_blur_" + i,
                Math.max(1, w / divisor), Math.max(1, h / divisor), false);
        }
        lastWidth = w;
        lastHeight = h;
    }

    /**
     * 对当前主帧缓冲执行模糊。
     *
     * @param level 模糊等级 1~{@link #LEVELS}，越大越模糊
     * @return 模糊结果的纹理视图
     */
    public static GpuTextureView blur(int level) {
        ensureChain();
        level = Math.max(1, Math.min(LEVELS, level));

        Framebuffer main = mc.getFramebuffer();
        GpuTextureView src = main.getColorAttachmentView();

        for (int i = 0; i < level; i++) {
            blitTo(src, chain[i]);
            src = chain[i].getColorAttachmentView();
        }
        return src;
    }

    /**
     * Bloom：模糊结果与原始画面叠加（亮部发光感）。
     * 返回模糊纹理视图，调用方以 additive 方式叠加绘制即可。
     */
    public static GpuTextureView bloom(int level) {
        return blur(level);
    }

    /** 把 src 纹理以双线性过滤绘制到 dst 帧缓冲（自动按 dst 尺寸缩放） */
    private static void blitTo(GpuTextureView src, SimpleFramebuffer dst) {
        RenderPipeline pipeline = RenderPipelines.ENTITY_OUTLINE_BLIT;
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advicenext_blur", dst.getColorAttachmentView(), OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", src, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
    }

    public static void dispose() {
        if (chain != null) {
            for (SimpleFramebuffer fb : chain) {
                fb.delete();
            }
            chain = null;
        }
        lastWidth = -1;
        lastHeight = -1;
    }
}
