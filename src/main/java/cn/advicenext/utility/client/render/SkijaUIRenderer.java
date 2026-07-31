package cn.advicenext.utility.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.IRect;

import java.awt.image.BufferedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Skija UI 渲染器——用 Skia 引擎渲染高质量 UI 元素（圆角矩形、圆形、渐变等），
 * 烘焙到 GPU 纹理后通过 {@link SimpleGuiElementRenderState} 提交到 GUI 延迟管线。
 *
 * <p>模糊/辉光使用 GPU 管线（与 {@link KawaseBlur}/{@link KawaseBloom} 相同的着色器），
 * 在本地正确尺寸的 {@link SimpleFramebuffer} 上执行，避免全屏缓冲区浪费。</p>
 *
 * <p>调用前需 {@link #setRenderState(GuiRenderState, Matrix3x2fc)} 设置当前帧状态。</p>
 */
public final class SkijaUIRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkijaUIRenderer.class);
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private SkijaUIRenderer() {}

    // ==================== 静态渲染上下文 ====================

    private static GuiRenderState currentState = null;
    private static Matrix3x2fc currentPose = new org.joml.Matrix3x2f();

    public static void setRenderState(GuiRenderState state, Matrix3x2fc pose) {
        currentState = state;
        currentPose = pose;
    }

    // ==================== Skija 初始化 ====================

    private static boolean skijaReady = false;
    private static boolean skijaAttempted = false;

    private static synchronized boolean ensureSkija() {
        if (skijaAttempted) return skijaReady;
        skijaAttempted = true;
        try {
            FontMgr mgr = FontMgr.getDefault();
            skijaReady = mgr != null;
            if (skijaReady) LOGGER.info("[SkijaUI] Skija initialized successfully");
        } catch (Throwable t) {
            LOGGER.warn("[SkijaUI] Skija not available: {}", t.getMessage());
            skijaReady = false;
        }
        return skijaReady;
    }

    // ==================== 纹理缓存 ====================

    private static final Map<String, UITexture> textureCache = new HashMap<>();
    private static int nextTextureId = 0;

    private static class UITexture extends NativeImageBackedTexture {
        final Identifier id;
        final NativeImage image;
        boolean dirty = false;

        UITexture(int w, int h) {
            super(() -> "skija_ui_" + (nextTextureId), new NativeImage(w, h, true));
            this.id = Identifier.of("advicenext", "skija_ui_" + (nextTextureId++));
            this.image = getImage();
            this.sampler = RenderSystem.getSamplerCache().get(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, false);
            this.dirty = true;
        }

        void register() {
            mc.getTextureManager().registerTexture(id, this);
        }

        void uploadIfDirty() {
            if (dirty) { upload(); dirty = false; }
        }
    }

    // ==================== 圆角矩形 ====================

    /**
     * 带缓存圆角矩形。
     * @param cacheKey 缓存键（null = 不缓存，每帧重绘）
     */
    public static void drawRoundedRect(String cacheKey, float x, float y, float w, float h,
                                        float radius, int color) {
        if (!ensureSkija() || currentState == null) return;
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w, h) / 2);

        int texW = (int) Math.ceil(w) + 4;
        int texH = (int) Math.ceil(h) + 4;
        UITexture tex = cacheKey != null ? textureCache.get(cacheKey) : null;

        if (tex == null || tex.image.getWidth() != texW || tex.image.getHeight() != texH) {
            if (tex != null) { tex.close(); textureCache.remove(cacheKey); }
            tex = new UITexture(texW, texH);
            tex.register();
            if (cacheKey != null) textureCache.put(cacheKey, tex);
        }

        renderRoundedRect(tex, w, h, radius, color);
        tex.uploadIfDirty();
        submitTextureQuad(x - 2, y - 2, texW, texH, tex);
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        drawRoundedRect(null, x, y, w, h, radius, color);
    }

    // ==================== 4 角渐变圆角矩形 ====================

    public static void drawRoundedRect4C(String cacheKey, float x, float y, float w, float h,
                                          float radius, int tl, int tr, int bl, int br) {
        if (!ensureSkija() || currentState == null) return;
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w, h) / 2);

        int texW = (int) Math.ceil(w) + 4;
        int texH = (int) Math.ceil(h) + 4;
        UITexture tex = cacheKey != null ? textureCache.get(cacheKey) : null;

        if (tex == null || tex.image.getWidth() != texW || tex.image.getHeight() != texH) {
            if (tex != null) { tex.close(); textureCache.remove(cacheKey); }
            tex = new UITexture(texW, texH);
            tex.register();
            if (cacheKey != null) textureCache.put(cacheKey, tex);
        }

        renderRoundedRect4C(tex, w, h, radius, tl, tr, bl, br);
        tex.uploadIfDirty();

        submitTextureQuad(x - 2, y - 2, texW, texH, tex);
    }

    // ==================== 普通矩形（无圆角） ====================

    /**
     * 带缓存普通矩形。
     */
    public static void drawRect(String cacheKey, float x, float y, float w, float h, int color) {
        if (!ensureSkija() || currentState == null) return;
        if (w <= 0 || h <= 0) return;

        int texW = (int) Math.ceil(w) + 4;
        int texH = (int) Math.ceil(h) + 4;
        UITexture tex = cacheKey != null ? textureCache.get(cacheKey) : null;

        if (tex == null || tex.image.getWidth() != texW || tex.image.getHeight() != texH) {
            if (tex != null) { tex.close(); textureCache.remove(cacheKey); }
            tex = new UITexture(texW, texH);
            tex.register();
            if (cacheKey != null) textureCache.put(cacheKey, tex);
        }

        renderRect(tex, w, h, color);
        tex.uploadIfDirty();
        submitTextureQuad(x - 2, y - 2, texW, texH, tex);
    }

    public static void drawRect(float x, float y, float w, float h, int color) {
        drawRect(null, x, y, w, h, color);
    }

    private static void renderRect(UITexture tex, float w, float h, int color) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();
        float pad = 2;

        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            try (Paint p = new Paint()) {
                p.setColor(color);
                canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(pad, pad, w, h), p);
            }

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] rect render failed", t);
        }
    }

    // ==================== 圆形 ====================

    public static void drawCircle(String cacheKey, float cx, float cy, float diameter, int color) {
        if (!ensureSkija() || currentState == null) return;
        int texW = (int) Math.ceil(diameter) + 4;
        int texH = texW;
        UITexture tex = cacheKey != null ? textureCache.get(cacheKey) : null;

        if (tex == null || tex.image.getWidth() != texW) {
            if (tex != null) { tex.close(); textureCache.remove(cacheKey); }
            tex = new UITexture(texW, texH);
            tex.register();
            if (cacheKey != null) textureCache.put(cacheKey, tex);
        }

        renderCircleToTexture(tex, diameter, color);
        tex.uploadIfDirty();
        submitTextureQuad(cx - diameter / 2 - 2, cy - diameter / 2 - 2, texW, texH, tex);
    }

    public static void drawCircle(float cx, float cy, float diameter, int color) {
        drawCircle(null, cx, cy, diameter, color);
    }

    // ==================== 内部 Skija 渲染（仅形状，无模糊/辉光） ====================

    private static void renderRoundedRect(UITexture tex, float w, float h, float radius,
                                           int color, float pad, float blur) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();

        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            RRect rrect = RRect.makeXYWH(pad, pad, w, h, radius);

            try (Paint p = new Paint()) {
                p.setColor(color);
                p.setAntiAlias(true);
                canvas.drawRRect(rrect, p);
            }

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] rounded rect render failed", t);
        }
    }

    // ==================== 内部 Skija 渲染（仅形状） ====================

    private static void renderRoundedRect(UITexture tex, float w, float h, float radius, int color) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();
        float pad = 2;

        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            RRect rrect = RRect.makeXYWH(pad, pad, w, h, radius);

            try (Paint p = new Paint()) {
                p.setColor(color);
                p.setAntiAlias(true);
                canvas.drawRRect(rrect, p);
            }

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] rounded rect render failed", t);
        }
    }

    private static void renderRoundedRect4C(UITexture tex, float w, float h, float radius,
                                             int tl, int tr, int bl, int br) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();

        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            float offset = 2.0f;
            RRect rrect = RRect.makeXYWH(offset, offset, w, h, radius);

            if (tl == tr && tl == bl && tl == br) {
                try (Paint paint = new Paint()) {
                    paint.setColor(tl);
                    paint.setAntiAlias(true);
                    canvas.drawRRect(rrect, paint);
                }
            } else {
                try (Paint paint = new Paint()) {
                    paint.setAntiAlias(true);
                    paint.setShader(Shader.makeLinearGradient(
                        offset, offset, offset + w, offset + h,
                        new int[]{tl, tr, br, bl},
                        new float[]{0.0f, 0.33f, 0.66f, 1.0f}));
                    canvas.drawRRect(rrect, paint);
                }
            }

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] 4C render failed", t);
        }
    }

    private static void renderCircleToTexture(UITexture tex, float diameter, int color) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();

        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            float cx = texW / 2.0f;
            float cy = texH / 2.0f;
            float r = diameter / 2.0f;

            try (Paint paint = new Paint()) {
                paint.setColor(color);
                paint.setAntiAlias(true);
                canvas.drawCircle(cx, cy, r, paint);
            }

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] circle render failed", t);
        }
    }

    public static void drawImage(BufferedImage src, float x, float y, float w, float h) {
        drawImage(null, src, x, y, w, h);
    }

    public static void drawImage(String cacheKey, BufferedImage src, float x, float y, float w, float h) {
        if (src == null || currentState == null) return;
        int texW = (int) Math.ceil(w), texH = (int) Math.ceil(h);
        UITexture tex = cacheKey != null ? textureCache.get(cacheKey) : null;

        if (tex != null) {
            tex.uploadIfDirty();
            submitTextureQuad(x, y, texW, texH, tex);
            return;
        }

        tex = new UITexture(texW, texH);
        tex.register();
        if (cacheKey != null) textureCache.put(cacheKey, tex);
        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            int sw = src.getWidth(), sh = src.getHeight();
            try (Bitmap bmp = new Bitmap()) {
                bmp.allocN32Pixels(sw, sh);
                int[] pixels = new int[sw * sh];
                src.getRGB(0, 0, sw, sh, pixels, 0, sw);
                for (int py = 0; py < sh; py++) {
                    for (int px = 0; px < sw; px++) {
                        bmp.erase(pixels[py * sw + px], IRect.makeXYWH(px, py, 1, 1));
                    }
                }
                try (Image img = Image.makeFromBitmap(bmp)) {
                    canvas.drawImageRect(img, Rect.makeXYWH(0, 0, texW, texH));
                }
            }
            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] image render failed", t);
        }
        tex.uploadIfDirty();
        submitTextureQuad(x, y, texW, texH, tex);
    }

    private static void readPixelsToNativeImage(Surface surface, NativeImage image, int w, int h) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocN32Pixels(w, h);
            surface.readPixels(bitmap, 0, 0);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int color = bitmap.getColor(x, y);
                    image.setColor(x, y, color);
                }
            }
        }
    }

    // ==================== 提交到 GUI 管线 ====================

    /** 从 UITexture 提交纹理四边形 */
    private static void submitTextureQuad(float x, float y, float w, float h, UITexture tex) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bw = (int) Math.ceil(w);
        int bh = (int) Math.ceil(h);
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(bw, 1), Math.max(bh, 1));

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                float x1 = x, y1 = y, x2 = x + w, y2 = y + h;
                vertices.vertex(currentPose, x1, y1).texture(0, 0).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x1, y2).texture(0, 1).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x2, y2).texture(1, 1).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x2, y1).texture(1, 0).color(0xFFFFFFFF);
            }

            @Override public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
                return RenderPipelines.GUI_TEXTURED;
            }
            @Override public TextureSetup textureSetup() {
                return TextureSetup.of(tex.getGlTextureView(), tex.getSampler());
            }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    /** 渲染 GPU 处理后的纹理（通过 GUI 管线提交，与 UITexture 路径一致） */
    private static void submitTextureQuad(float x, float y, float w, float h,
                                           GpuTextureView textureView) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bw = (int) Math.ceil(w);
        int bh = (int) Math.ceil(h);
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(bw, 1), Math.max(bh, 1));

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                float x1 = x, y1 = y, x2 = x + w, y2 = y + h;
                vertices.vertex(currentPose, x1, y1).texture(0, 0).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x1, y2).texture(0, 1).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x2, y2).texture(1, 1).color(0xFFFFFFFF);
                vertices.vertex(currentPose, x2, y1).texture(1, 0).color(0xFFFFFFFF);
            }

            @Override public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
                return RenderPipelines.GUI_TEXTURED;
            }
            @Override public TextureSetup textureSetup() {
                return TextureSetup.of(textureView,
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    // ==================== 玩家头像圆角 ====================

    /**
     * 绘制圆角玩家头像。
     * @param profile 玩家 GameProfile
     * @param x 绘制 X 坐标
     * @param y 绘制 Y 坐标
     * @param size 头像尺寸
     * @param radius 圆角半径
     */
    public static void drawPlayerHeadRound(GameProfile profile, float x, float y, float size, float radius) {
        if (!ensureSkija() || currentState == null) return;
        if (size <= 0) return;

        SkinTextures textures = mc.getSkinProvider().supplySkinTextures(profile, false).get();
        drawPlayerHeadRound(textures, x, y, size, radius);
    }

    /**
     * 绘制圆角玩家头像（直接传入 SkinTextures，避免重复查询）。
     */
    public static void drawPlayerHeadRound(SkinTextures textures, float x, float y, float size, float radius) {
        if (!ensureSkija() || currentState == null) return;
        if (size <= 0) return;

        Identifier skinId = textures.body().texturePath();
        AbstractTexture abstractTexture = mc.getTextureManager().getTexture(skinId);
        if (!(abstractTexture instanceof NativeImageBackedTexture nibTex)) {
            drawFallbackHead(x, y, size, radius);
            return;
        }
        NativeImage skinImage = nibTex.getImage();
        if (skinImage == null) {
            drawFallbackHead(x, y, size, radius);
            return;
        }

        int texSize = (int) Math.ceil(size) + 4;
        UITexture tex = new UITexture(texSize, texSize);
        tex.register();

        renderPlayerHeadRound(tex, skinImage, size, radius);
        tex.uploadIfDirty();
        submitTextureQuad(x - 2, y - 2, texSize, texSize, tex);
    }

    private static void renderPlayerHeadRound(UITexture tex, NativeImage skinImage, float size, float radius) {
        int texW = tex.image.getWidth();
        int texH = tex.image.getHeight();
        float pad = 2;

        int headUnit = skinImage.getWidth() / 8;

        try (
            Bitmap headHiRes = new Bitmap();
            Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texW, texH))
        ) {
            int hiResHead = headUnit * 8;
            headHiRes.allocN32Pixels(hiResHead, hiResHead);

            for (int y = 0; y < headUnit; y++) {
                for (int x = 0; x < headUnit; x++) {
                    int baseColor = skinImage.getColorArgb(headUnit + x, headUnit + y);
                    int hatColor = skinImage.getColorArgb(headUnit * 5 + x, headUnit + y);
                    int alpha = (hatColor >> 24) & 0xFF;
                    int color = (alpha > 0) ? hatColor : baseColor;
                    for (int dy = 0; dy < 8; dy++) {
                        for (int dx = 0; dx < 8; dx++) {
                            headHiRes.erase(color,
                                io.github.humbleui.types.IRect.makeXYWH(x * 8 + dx, y * 8 + dy, 1, 1));
                        }
                    }
                }
            }

            Canvas canvas = surface.getCanvas();
            canvas.clear(0);

            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(pad, pad, size, size, radius));

            try (
                Image headImg = Image.makeFromBitmap(headHiRes);
                Paint p = new Paint()
            ) {
                p.setAntiAlias(true);

                canvas.drawImageRect(headImg,
                    Rect.makeXYWH(0, 0, hiResHead, hiResHead),
                    Rect.makeXYWH(pad, pad, size, size),
                    SamplingMode.LINEAR,
                    p, false);
            }

            canvas.restore();

            readPixelsToNativeImage(surface, tex.image, texW, texH);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] player head round render failed", t);
        }
    }

    /**
     * 皮肤加载失败时的回退头像（灰色圆角方块 + Steve 剪影色）。
     */
    private static void drawFallbackHead(float x, float y, float size, float radius) {
        if (!ensureSkija() || currentState == null) return;
        if (size <= 0) return;

        int texSize = (int) Math.ceil(size) + 4;
        UITexture tex = new UITexture(texSize, texSize);
        tex.register();

        try (
            Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(texSize, texSize))
        ) {
            Canvas canvas = surface.getCanvas();
            float pad = 2;

            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(pad, pad, size, size, radius));

            try (Paint p = new Paint()) {
                p.setAntiAlias(true);
                p.setColor(0xFF555555);
                canvas.drawRRect(RRect.makeXYWH(pad, pad, size, size, radius), p);
            }

            canvas.restore();

            readPixelsToNativeImage(surface, tex.image, texSize, texSize);
            tex.dirty = true;
        } catch (Throwable t) {
            LOGGER.error("[SkijaUI] fallback head render failed", t);
        }

        tex.uploadIfDirty();
        submitTextureQuad(x - 2, y - 2, texSize, texSize, tex);
    }

    // ==================== 清理 ====================

    public static void clearCache() {
        for (UITexture tex : textureCache.values()) tex.close();
        textureCache.clear();
    }

    public static void dispose() {
        clearCache();
    }
}