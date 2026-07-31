package cn.advicenext.utility.client.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;

/**
 * 高质量字体渲染器——七层抗锯齿叠加。
 *
 * <p>抗锯齿层次：</p>
 * <ol>
 *   <li><b>LCD 子像素渲染</b>——AWT {@code VALUE_TEXT_ANTIALIAS_LCD_HRGB}，
 *       利用 LCD 的 RGB 子像素获得 3x 水平分辨率</li>
 *   <li><b>几何抗锯齿</b>——{@code VALUE_ANTIALIAS_ON}</li>
 *   <li><b>笔画纯化</b>——{@code VALUE_STROKE_PURE}，统一 stem 宽度</li>
 *   <li><b>亚像素定位</b>——{@code VALUE_FRACTIONALMETRICS_ON}，消除字间距抖动</li>
 *   <li><b>12x~64x 超采样</b>——以高倍率光栅化后缩小，SSAA 消除锯齿</li>
 *   <li><b>Gamma 校正</b>——alpha 通道应用 gamma=1.6 曲线，锐化边缘过渡</li>
 *   <li><b>GPU 双线性过滤</b>——{@code FilterMode.LINEAR} 硬件插值</li>
 * </ol>
 *
 * <p>渲染路径：{@link SimpleGuiElementRenderState} → {@link GuiRenderState}
 * → {@code GuiRenderer} → GPU。调用方需先通过
 * {@link #setRenderState(GuiRenderState, Matrix3x2fc)} 设置当前帧状态。</p>
 */
public final class FontRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int ATLAS_SIZE = 2048;
    /** 目标光栅分辨率（每 em 像素数），小字号自动放大 */
    private static final float TARGET_RASTER_EM = 128f;
    private static final float MIN_RASTER_SCALE = 12f;
    private static final float MAX_RASTER_SCALE = 64f;
    private static final float GAMMA = 1.6f;

    static float rasterScaleFor(float logicalSize) {
        float scale = TARGET_RASTER_EM / logicalSize;
        return Math.max(MIN_RASTER_SCALE, Math.min(MAX_RASTER_SCALE, scale));
    }

    // ==================== 静态渲染上下文 ====================

    private static GuiRenderState currentState = null;
    private static Matrix3x2fc currentPose = new Matrix3x2f();

    /**
     * 每帧渲染前调用。
     * 调用方：HUD/Render2DEvent 监听器
     * {@code FontRenderer.setRenderState(event.getContext().state, event.getContext().getMatrices())}
     */
    public static void setRenderState(GuiRenderState state, Matrix3x2fc pose) {
        currentState = state;
        currentPose = pose;
    }

    // ==================== 实例字段 ====================

    private final java.awt.Font rasterFont;  // 放大后的字体，用于光栅化
    private final float rasterScale;         // 光栅化倍率（动态计算）
    private final float scale;               // 1.0 / rasterScale
    private final float logicalSize;         // 逻辑字号
    private final float logicalAscent;       // 逻辑 ascent（基线到大写字母顶部）
    private final float logicalDescent;      // 逻辑 descent
    private final boolean antiAlias;
    private final FontRenderContext frc;
    private final java.util.List<AtlasPage> pages = new ArrayList<>();
    private FontRenderer fallback;

    FontRenderer(java.awt.Font baseFont, boolean antiAlias) {
        this.logicalSize = baseFont.getSize2D();
        this.rasterScale = rasterScaleFor(logicalSize);
        this.rasterFont = baseFont.deriveFont(baseFont.getSize2D() * rasterScale);
        this.scale = 1.0f / rasterScale;
        this.antiAlias = antiAlias;
        this.frc = new FontRenderContext(null, true, true);

        // 从 rasterFont 计算逻辑 ascent/descent
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dummy.createGraphics();
        g2d.setFont(rasterFont);
        FontMetrics fm = g2d.getFontMetrics();
        this.logicalAscent = fm.getAscent() * scale;
        this.logicalDescent = fm.getDescent() * scale;
        g2d.dispose();
        dummy.flush();

        // 延迟初始化第一页到第一次使用时
    }

    private void ensurePage() {
        if (pages.isEmpty()) {
            newPage();
        }
    }

    // ==================== 图集页 ====================

    private static class AtlasPage extends AbstractTexture {
        private static int NEXT_ID = 0;

        final Identifier id;
        final NativeImage image;
        int cursorX = 1, cursorY = 0, rowHeight = 0;
        final Map<Integer, GlyphInfo> glyphs = new HashMap<>();
        boolean dirty = false;

        AtlasPage(int index, NativeImage img) {
            int uniqueId = NEXT_ID++;
            this.id = Identifier.of("advicenext", "font_atlas_" + uniqueId);
            this.image = img;
            this.sampler = RenderSystem.getSamplerCache().get(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, false);

            GpuDevice device = RenderSystem.getDevice();
            this.glTexture = device.createTexture(
                () -> "font_atlas_" + uniqueId,
                5,
                TextureFormat.RGBA8,
                img.getWidth(),
                img.getHeight(),
                1,
                1
            );
            this.glTextureView = device.createTextureView(this.glTexture);
            this.dirty = true;
        }

        void register() {
            mc.getTextureManager().registerTexture(id, this);
        }

        private void uploadTexture() {
            if (image != null && glTexture != null) {
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToTexture(glTexture, image);
            }
        }

        void uploadIfDirty() {
            if (dirty) {
                uploadTexture();
                dirty = false;
            }
        }
    }

    // ==================== 字形 ====================

    public static final class GlyphInfo {
        public final int codePoint;
        public final float advance;          // 逻辑单位 advance
        public final int rasterW, rasterH;   // 2x 尺寸（图集单元格大小）
        public final float baselineOffset;   // 基线到单元格顶部的距离（2x 像素单位）
        public float u0, v0, u1, v1;
        AtlasPage page;
        GlyphInfo(int cp, float adv, int w, int h, float baselineOffset) {
            this.codePoint = cp;
            this.advance = adv;
            this.rasterW = w;
            this.rasterH = h;
            this.baselineOffset = baselineOffset;
        }
    }

    // ==================== 光栅化（2x 超采样） ====================

    public void setFallback(FontRenderer fallback) {
        this.fallback = fallback;
    }

    private synchronized GlyphInfo getGlyph(int codePoint) {
        ensurePage();
        for (int i = pages.size() - 1; i >= 0; i--) {
            GlyphInfo g = pages.get(i).glyphs.get(codePoint);
            if (g != null) return g;
        }
        if (!rasterFont.canDisplay(codePoint) && fallback != null) {
            return fallback.getGlyph(codePoint);
        }
        return rasterize(codePoint);
    }

    private GlyphInfo rasterize(int codePoint) {
        String ch = new String(Character.toChars(codePoint));
        GlyphVector gv = rasterFont.createGlyphVector(frc, ch);
        Rectangle2D bounds = gv.getVisualBounds();

        // advance：从 glyph metrics 获取（2x 尺寸），转换为逻辑单位
        float advance = 0;
        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            advance += gv.getGlyphMetrics(i).getAdvanceX();
        }
        advance *= scale;

        int pad = 2;
        int cellW = Math.max(1, (int) Math.ceil(bounds.getWidth()) + pad * 2);
        int cellH = Math.max(1, (int) Math.ceil(bounds.getHeight()) + pad * 2);
        AtlasPage page = reserveSlot(cellW, cellH);

        int drawX = (int) (pad - bounds.getX());
        int drawY = (int) (pad - bounds.getY());
        float baselineOffset = (float) drawY; // 基线到单元格顶部的距离（2x 像素）

        BufferedImage bi = new BufferedImage(cellW, cellH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        if (antiAlias) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setColor(Color.WHITE);
        g2d.setFont(rasterFont);
        g2d.drawString(ch, drawX, drawY);
        g2d.dispose();

        int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < cellH; y++) for (int x = 0; x < cellW; x++) {
            int argb = pixels[y * cellW + x];
            int a = (argb >>> 24) & 0xFF, r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
            float fa = (a & 0xFF) / 255.0f;
            fa = (float) Math.pow(fa, 1.0 / GAMMA);
            a = (int) (fa * 255.0f + 0.5f);
            float fr = (r & 0xFF) / 255.0f, fg = (g & 0xFF) / 255.0f, fb = (b & 0xFF) / 255.0f;
            int ra = (int) (fr * a + 0.5f), ga = (int) (fg * a + 0.5f), ba = (int) (fb * a + 0.5f);
            page.image.setColor(page.cursorX + x, page.cursorY + y, (a << 24) | (ba << 16) | (ga << 8) | ra);
        }
        bi.flush();
        page.dirty = true;

        float invW = 1.0F / ATLAS_SIZE, invH = 1.0F / ATLAS_SIZE;
        GlyphInfo gi = new GlyphInfo(codePoint, advance, cellW, cellH, baselineOffset);
        gi.u0 = page.cursorX * invW; gi.v0 = page.cursorY * invH;
        gi.u1 = (page.cursorX + cellW) * invW; gi.v1 = (page.cursorY + cellH) * invH;
        gi.page = page;
        page.glyphs.put(codePoint, gi);
        page.cursorX += cellW;
        page.rowHeight = Math.max(page.rowHeight, cellH);
        return gi;
    }

    private AtlasPage reserveSlot(int cellW, int cellH) {
        AtlasPage last = pages.getLast();
        if (last.cursorX + cellW > ATLAS_SIZE) {
            last.cursorX = 1;
            last.cursorY += last.rowHeight + 1;
            last.rowHeight = 0;
        }
        if (last.cursorY + cellH > ATLAS_SIZE) {
            last.uploadTexture();
            last.dirty = false;
            newPage();
            last = pages.getLast();
        }
        return last;
    }

    private void newPage() {
        AtlasPage p = new AtlasPage(pages.size(), new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true));
        p.register();
        pages.add(p);
    }

    // ==================== 文本度量 ====================

    public float getStringWidth(String text) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) i++;
            w += getGlyph(cp).advance;
        }
        return w;
    }

    public float getHeight() {
        return logicalAscent + logicalDescent;
    }

    // ==================== 绘制 ====================

    /**
     * 将文本绘制到屏幕上。
     * 字形在图集里以 2x 尺寸存储，渲染时用 0.5x 缩放，产生超采样抗锯齿效果。
     * 不依赖 DrawContext，调用前需 {@link #setRenderState(GuiRenderState, Matrix3x2fc)}。
     */
    public void drawString(String text, float x, float y, int color) {
        if (text == null || text.isEmpty()) return;
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        // 按图集页分组
        java.util.List<Float> penXs = new ArrayList<>();
        Map<AtlasPage, java.util.List<GlyphInfo>> pageGlyphs = new LinkedHashMap<>();
        float penX = x;
        float maxH = 0;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) i++;
            GlyphInfo gi = getGlyph(cp);
            pageGlyphs.computeIfAbsent(gi.page, k -> new ArrayList<>()).add(gi);
            penXs.add(penX);
            penX += gi.advance;
            float h = gi.rasterH * scale;
            if (h > maxH) maxH = h;
        }
        if (maxH <= 0) maxH = logicalSize;
        ScreenRect textBounds = new ScreenRect((int) x, (int) y,
            Math.max((int) Math.ceil(penX - x), 1), Math.max((int) Math.ceil(maxH), 1));

        // 所有字形对齐到共同基线：baselineY = y + ascent
        float baselineY = y + logicalAscent;

        for (Map.Entry<AtlasPage, java.util.List<GlyphInfo>> entry : pageGlyphs.entrySet()) {
            AtlasPage page = entry.getKey();
            java.util.List<GlyphInfo> glyphs = entry.getValue();
            page.uploadIfDirty();

            int qc = glyphs.size();
            float[] pos = new float[qc * 8];
            float[] uvs = new float[qc * 8];
            int ci = 0, pi = 0;
            for (GlyphInfo gi : glyphs) {
                float gx = penXs.get(ci);
                // 基线对齐：单元格顶部 = 基线 - 基线偏移 * 缩放
                float gy = baselineY - gi.baselineOffset * scale;
                // 渲染时用 0.5x 缩放（超采样关键）
                float gx2 = gx + gi.rasterW * scale;
                float gy2 = gy + gi.rasterH * scale;

                pos[pi]   = gx;  pos[pi+1] = gy;   uvs[pi]   = gi.u0; uvs[pi+1] = gi.v0;
                pos[pi+2] = gx;  pos[pi+3] = gy2;  uvs[pi+2] = gi.u0; uvs[pi+3] = gi.v1;
                pos[pi+4] = gx2; pos[pi+5] = gy2;  uvs[pi+4] = gi.u1; uvs[pi+5] = gi.v1;
                pos[pi+6] = gx2; pos[pi+7] = gy;   uvs[pi+6] = gi.u1; uvs[pi+7] = gi.v0;
                pi += 8; ci++;
            }

            final int quadCount = qc;
            final float[] finalPos = pos;
            final float[] finalUvs = uvs;

            currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
                @Override
                public void setupVertices(VertexConsumer vertices) {
                    int idx = 0;
                    for (int q = 0; q < quadCount; q++) {
                        vertices.vertex(currentPose, finalPos[idx],   finalPos[idx+1])
                            .texture(finalUvs[idx],   finalUvs[idx+1]).color(color);
                        vertices.vertex(currentPose, finalPos[idx+2], finalPos[idx+3])
                            .texture(finalUvs[idx+2], finalUvs[idx+3]).color(color);
                        vertices.vertex(currentPose, finalPos[idx+4], finalPos[idx+5])
                            .texture(finalUvs[idx+4], finalUvs[idx+5]).color(color);
                        vertices.vertex(currentPose, finalPos[idx+6], finalPos[idx+7])
                            .texture(finalUvs[idx+6], finalUvs[idx+7]).color(color);
                        idx += 8;
                    }
                }

                @Override
                public RenderPipeline pipeline() {
                    return RenderPipelines.GUI_TEXTURED;
                }

                @Override
                public TextureSetup textureSetup() {
                    // 确保纹理已上传
                    page.uploadIfDirty();
                    return TextureSetup.of(page.getGlTextureView(), page.getSampler());
                }

                @Override
                public ScreenRect scissorArea() {
                    return null;
                }

                @Override
                public ScreenRect bounds() {
                    return textBounds;
                }
            });
        }
    }

    public void drawCenteredString(String text, float cx, float y, int color) {
        drawString(text, cx - getStringWidth(text) / 2, y, color);
    }

    // ==================== 调试 ====================

    /** 导出图集到 PNG 用于调试 */
    public void dumpAtlas(String prefix) {
        for (int i = 0; i < pages.size(); i++) {
            AtlasPage p = pages.get(i);
            java.io.File out = new java.io.File(prefix + "_page" + i + ".png");
            try {
                p.image.writeTo(out.toPath());
                LOGGER.info("[FontRenderer] dumped atlas to {}", out.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("[FontRenderer] dump failed", e);
            }
        }
    }
}