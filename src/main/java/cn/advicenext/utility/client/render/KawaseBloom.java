package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * 高斯 Bloom——亮部提取 → 两遍高斯模糊 → 与原图叠加。
 * 全部使用 {@link RenderPass} 直接提交，不依赖 DrawContext。
 */
public final class KawaseBloom {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static SimpleFramebuffer brightFB;
    private static SimpleFramebuffer blurFB;
    private static SimpleFramebuffer compositeFB;
    private static int lastW = -1, lastH = -1;

    private static RenderPipeline brightPipeline;
    private static RenderPipeline blurPipeline;
    private static RenderPipeline compositePipeline;

    private KawaseBloom() {}

    // ==================== 公共方法 ====================

    /**
     * 对指定纹理执行 Bloom。
     * @param sourceTexture 源纹理
     * @param radius        模糊半径 1-10
     * @param offset        阈值偏移 0.2-1.0（越大亮部越少）
     * @return 复合结果纹理视图
     */
    public static GpuTextureView render(GpuTextureView sourceTexture, int radius, float offset) {
        ensureResources();
        float threshold = Math.max(0.1F, Math.min(0.9F, 0.8F - offset * 0.6F));

        // 缓存管线
        if (brightPipeline == null) {
            brightPipeline = ShaderUtils.createPipeline(
                Identifier.of("advicenext", "post/bright_pass"),
                Identifier.of("advicenext", "bloom_bright"),
                new String[]{"Diffuse"}, new String[]{"uThreshold", "uIntensity"});
        }
        if (blurPipeline == null) {
            blurPipeline = ShaderUtils.createPipeline(
                Identifier.of("advicenext", "post/blur"),
                Identifier.of("advicenext", "bloom_blur"),
                new String[]{"texture"}, new String[]{"directionX", "directionY", "radius"});
        }
        if (compositePipeline == null) {
            compositePipeline = ShaderUtils.createPipeline(
                Identifier.of("advicenext", "post/bloom_composite"),
                Identifier.of("advicenext", "bloom_composite"),
                new String[]{"Diffuse", "Bloom"}, new String[]{});
        }

        // 1. 亮部提取
        try (GpuBuffer tBuf = ShaderUtils.createFloatUniform("bloom_threshold", threshold);
             GpuBuffer iBuf = ShaderUtils.createFloatUniform("bloom_intensity", 1.0f)) {
            ShaderUtils.blitWithPipeline(sourceTexture, brightFB.getColorAttachmentView(), brightPipeline,
                Map.of("Diffuse", sourceTexture),
                Map.of("uThreshold", tBuf, "uIntensity", iBuf));
        }

        // 2. 两遍高斯模糊（水平 → 垂直）
        float r = radius;
        // 水平
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 1.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 0.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", r)) {
            ShaderUtils.blitWithPipeline(brightFB.getColorAttachmentView(), blurFB.getColorAttachmentView(), blurPipeline,
                Map.of("texture", brightFB.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }
        // 垂直
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 0.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 1.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", r)) {
            ShaderUtils.blitWithPipeline(blurFB.getColorAttachmentView(), brightFB.getColorAttachmentView(), blurPipeline,
                Map.of("texture", blurFB.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        // 3. 复合（原图 + 模糊亮部）
        ShaderUtils.blitWithPipeline(sourceTexture, compositeFB.getColorAttachmentView(), compositePipeline,
            Map.of("Diffuse", sourceTexture, "Bloom", brightFB.getColorAttachmentView()),
            Map.of());

        return compositeFB.getColorAttachmentView();
    }

    /** 对主帧缓冲执行 Bloom */
    public static GpuTextureView render(int radius) {
        return render(ShaderUtils.mainColorView(), radius, 0.5F);
    }

    // ==================== 内部 ====================

    private static void ensureResources() {
        int w = mc.getFramebuffer().textureWidth;
        int h = mc.getFramebuffer().textureHeight;
        if (brightFB != null && lastW == w && lastH == h) return;

        if (brightFB != null) { brightFB.delete(); blurFB.delete(); compositeFB.delete(); }

        brightFB = new SimpleFramebuffer("bloom_bright", w, h, false);
        blurFB = new SimpleFramebuffer("bloom_blur", w, h, false);
        compositeFB = new SimpleFramebuffer("bloom_composite", w, h, false);
        lastW = w; lastH = h;
    }

    /** 释放 GPU 资源 */
    public static void dispose() {
        if (brightFB != null) brightFB.delete();
        if (blurFB != null) blurFB.delete();
        if (compositeFB != null) compositeFB.delete();
        lastW = -1;
    }
}
