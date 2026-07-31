package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * 高斯模糊——MoonLight 风格两遍分离（水平 → 垂直）。
 * 使用 {@link RenderPass} 直接提交，不依赖 DrawContext。
 *
 * <p>用法：
 * <pre>{@code
 * // 全屏模糊
 * KawaseBlur.renderBlur(5.0f);
 *
 * // 区域模糊（先画背景，再模糊，再画前景）
 * KawaseBlur.startBlur();
 * // ... 绘制需要模糊的内容 ...
 * KawaseBlur.endBlur(5.0f, 1.0f);
 * }</pre>
 */
public final class KawaseBlur {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static SimpleFramebuffer blurBuffer;
    private static int lastW = -1, lastH = -1;
    private static RenderPipeline blurPipeline;

    private KawaseBlur() {}

    // ==================== 内部初始化 ====================

    private static void ensureResources() {
        int w = mc.getFramebuffer().textureWidth;
        int h = mc.getFramebuffer().textureHeight;
        if (blurBuffer != null && lastW == w && lastH == h) return;

        if (blurBuffer != null) blurBuffer.delete();
        blurBuffer = new SimpleFramebuffer("kawase_blur", w, h, false);
        lastW = w; lastH = h;

        if (blurPipeline == null) {
            blurPipeline = ShaderUtils.createPipeline(
                Identifier.of("advicenext", "post/blur"),
                Identifier.of("advicenext", "gaussian_blur"),
                new String[]{"texture"}, new String[]{"directionX", "directionY", "radius"});
        }
    }

    // ==================== 全屏模糊 ====================

    /**
     * 对当前主帧缓冲执行全屏高斯模糊。
     * @param radius 模糊半径（越大越模糊，建议 1-10）
     */
    public static void renderBlur(float radius) {
        ensureResources();
        GpuTextureView mainView = mc.getFramebuffer().getColorAttachmentView();

        // 第一遍：水平模糊 主屏 → blurBuffer
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 1.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 0.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(mainView, blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", mainView),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        // 第二遍：垂直模糊 blurBuffer → 主屏
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 0.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 1.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(blurBuffer.getColorAttachmentView(), mainView, blurPipeline,
                Map.of("texture", blurBuffer.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }
    }

    // ==================== 区域模糊 ====================

    /**
     * 对主帧缓冲的指定区域执行高斯模糊。
     * @param radius 模糊半径（像素）
     * @param x      区域左上角 X（GUI 缩放坐标）
     * @param y      区域左上角 Y（GUI 缩放坐标）
     * @param w      区域宽度（GUI 缩放坐标）
     * @param h      区域高度（GUI 缩放坐标）
     */
    public static void renderBlurRegion(float radius, float x, float y, float w, float h) {
        ensureResources();
        var window = mc.getWindow();
        int fbWidth = window.getFramebufferWidth();
        int fbHeight = window.getFramebufferHeight();
        double scale = (double) fbWidth / window.getScaledWidth();

        int pxX = (int) (x * scale);
        int pxY = (int) (y * scale);
        int pxW = (int) Math.ceil(w * scale);
        int pxH = (int) Math.ceil(h * scale);

        int r = (int) Math.ceil(radius * scale);
        int sx = Math.max(0, pxX - r);
        int sy = Math.max(0, fbHeight - pxY - pxH - r);
        int sw = Math.min(fbWidth - sx, pxW + 2 * r);
        int sh = Math.min(fbHeight - sy, pxH + 2 * r);
        if (sw <= 0 || sh <= 0) return;

        GpuTextureView mainView = mc.getFramebuffer().getColorAttachmentView();

        // 水平模糊：主屏 → blurBuffer（scissor = 区域 + padding）
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 1.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 0.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(mainView, blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", mainView),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf),
                sx, sy, sw, sh);
        }

        // 垂直模糊：blurBuffer → 主屏（scissor = 精确区域）
        int sx2 = Math.max(0, pxX);
        int sy2 = Math.max(0, fbHeight - pxY - pxH);
        int sw2 = Math.min(fbWidth - sx2, pxW);
        int sh2 = Math.min(fbHeight - sy2, pxH);
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 0.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 1.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(blurBuffer.getColorAttachmentView(), mainView, blurPipeline,
                Map.of("texture", blurBuffer.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf),
                sx2, sy2, sw2, sh2);
        }
    }

    /**
     * 开始区域模糊：复制主屏到内部缓冲。
     * 之后调用方绘制需要模糊的内容，最后调用 {@link #endBlur(float, float)}。
     */
    public static void startBlur() {
        ensureResources();
        ShaderUtils.blit(mc.getFramebuffer().getColorAttachmentView(),
            blurBuffer.getColorAttachmentView());
    }

    /**
     * 完成区域模糊：对保存的背景做高斯模糊，然后绘制回主屏。
     * @param radius      模糊半径
     * @param compression 采样压缩比（0.5-2.0）
     */
    public static void endBlur(float radius, float compression) {
        ensureResources();
        GpuTextureView mainView = mc.getFramebuffer().getColorAttachmentView();
        float r = radius * compression;

        // 水平
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 1.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 0.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", r)) {
            ShaderUtils.blitWithPipeline(blurBuffer.getColorAttachmentView(), blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", blurBuffer.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        // 垂直
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 0.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 1.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", r)) {
            ShaderUtils.blitWithPipeline(blurBuffer.getColorAttachmentView(), blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", blurBuffer.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        // 模糊结果回写主屏
        ShaderUtils.drawToMain(blurBuffer.getColorAttachmentView());
    }

    /**
     * 对指定纹理执行高斯模糊，返回模糊结果纹理视图。
     * @param src  源纹理
     * @param radius 模糊半径
     * @return 模糊结果纹理视图（内部管理，下次调用时无效）
     */
    public static GpuTextureView renderBlurOn(GpuTextureView src, float radius) {
        ensureResources();

        // 水平
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 1.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 0.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(src, blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", src),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        // 垂直（写回 src 不行，所以用 blurBuffer 作为中间缓冲）
        try (GpuBuffer dxBuf = ShaderUtils.createFloatUniform("dirX", 0.0f);
             GpuBuffer dyBuf = ShaderUtils.createFloatUniform("dirY", 1.0f);
             GpuBuffer rBuf = ShaderUtils.createFloatUniform("radius", radius)) {
            ShaderUtils.blitWithPipeline(blurBuffer.getColorAttachmentView(), blurBuffer.getColorAttachmentView(), blurPipeline,
                Map.of("texture", blurBuffer.getColorAttachmentView()),
                Map.of("directionX", dxBuf, "directionY", dyBuf, "radius", rBuf));
        }

        return blurBuffer.getColorAttachmentView();
    }

    /** 释放 GPU 资源 */
    public static void dispose() {
        if (blurBuffer != null) blurBuffer.delete();
        blurBuffer = null;
        lastW = -1;
    }
}