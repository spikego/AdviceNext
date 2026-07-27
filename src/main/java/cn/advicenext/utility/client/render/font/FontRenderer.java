package cn.advicenext.utility.client.render.font;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import org.lwjgl.system.MemoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 字体渲染器：Skija 离屏渲染文本为纹理，再通过 GUI 管线绘制。
 * 纹理按 (文本, 字号, 颜色) 缓存，避免每帧重排。
 * Skija 不可用或出错时自动回退到原版字体渲染。
 */
public final class FontRenderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Map<Integer, Font> fontCache = new HashMap<>();
    private static final Map<String, TextureEntry> textureCache = new HashMap<>();
    private static final int MAX_TEXTURE_CACHE = 512;

    private FontRenderer() {
    }

    private static class TextureEntry {
        final Identifier id;
        final NativeImageBackedTexture texture;
        final int width;
        final int height;

        TextureEntry(Identifier id, NativeImageBackedTexture texture, int width, int height) {
            this.id = id;
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    // ==================== 公开 API ====================

    public static boolean isAvailable() {
        return SkijaManager.isInitialized();
    }

    /** 文本宽度（像素），不可用时用原版字体宽度估算 */
    public static int getStringWidth(String text, float size) {
        if (!isAvailable()) {
            return mc.textRenderer.getWidth(text) * (int) size / 9;
        }
        Font font = getFont(size);
        if (font == null) return mc.textRenderer.getWidth(text) * (int) size / 9;
        try {
            return (int) Math.ceil(font.measureTextWidth(text));
        } catch (Throwable t) {
            return mc.textRenderer.getWidth(text) * (int) size / 9;
        }
    }

    /** 行高（像素） */
    public static int getFontHeight(float size) {
        if (!isAvailable()) return 9 * (int) size / 9;
        Font font = getFont(size);
        if (font == null) return (int) size;
        try {
            FontMetrics metrics = font.getMetrics();
            return (int) Math.ceil(metrics.getHeight());
        } catch (Throwable t) {
            return (int) size;
        }
    }

    /**
     * 绘制文本。颜色为 ARGB。
     * 纹理按 GUI 缩放因子超采样渲染，保证任意缩放下清晰。
     *
     * @return 绘制宽度（逻辑像素）
     */
    public static int drawString(DrawContext context, String text, float x, float y, float size, int color) {
        if (!isAvailable() || text == null || text.isEmpty()) {
            context.drawText(mc.textRenderer, text == null ? "" : text, (int) x, (int) y, color, false);
            return mc.textRenderer.getWidth(text == null ? "" : text);
        }

        try {
            TextureEntry entry = getOrCreateTexture(text, size, color);
            if (entry == null) {
                context.drawText(mc.textRenderer, text, (int) x, (int) y, color, false);
                return mc.textRenderer.getWidth(text);
            }

            // 逻辑尺寸 = 纹理像素尺寸 / 缩放因子
            float scaleFactor = getScaleFactor();
            int drawWidth = Math.max(1, Math.round(entry.width / scaleFactor));
            int drawHeight = Math.max(1, Math.round(entry.height / scaleFactor));

            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                entry.id,
                Math.round(x), Math.round(y),
                0.0F, 0.0F,
                drawWidth, drawHeight,
                entry.width, entry.height
            );
            return drawWidth;
        } catch (Throwable t) {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color, false);
            return mc.textRenderer.getWidth(text);
        }
    }

    /** 带投影的文本 */
    public static int drawStringWithShadow(DrawContext context, String text, float x, float y, float size, int color) {
        int shadowColor = (((color >>> 24) & 0xFF) / 3 << 24) | 0x00000000;
        drawString(context, text, x + 1, y + 1, size, shadowColor);
        return drawString(context, text, x, y, size, color);
    }

    /** 居中绘制 */
    public static int drawCenteredString(DrawContext context, String text, float centerX, float y, float size, int color) {
        int width = getStringWidth(text, size);
        return drawString(context, text, centerX - width / 2.0F, y, size, color);
    }

    // ==================== 内部 ====================

    private static Font getFont(float size) {
        if (!isAvailable()) return null;
        int key = (int) (size * 2.0F);
        return fontCache.computeIfAbsent(key, k -> new Font(SkijaManager.getTypeface(), size));
    }

    private static float getScaleFactor() {
        return (float) mc.getWindow().getScaleFactor();
    }

    private static TextureEntry getOrCreateTexture(String text, float size, int color) {
        float scaleFactor = getScaleFactor();
        String key = text + "|" + size + "|" + color + "|" + scaleFactor;
        TextureEntry cached = textureCache.get(key);
        if (cached != null) return cached;

        if (textureCache.size() >= MAX_TEXTURE_CACHE) {
            clearTextureCache();
        }

        // 按缩放因子放大字号渲染，纹理像素密度与屏幕匹配 → 清晰
        Font font = getFont(size * scaleFactor);
        if (font == null) return null;

        int width = Math.max(1, (int) Math.ceil(font.measureTextWidth(text)) + 2);
        FontMetrics metrics = font.getMetrics();
        int height = Math.max(1, (int) Math.ceil(metrics.getHeight()) + 2);

        // UNPREMUL：直读 alpha，避免 premultiplied→straight 转换损失精度
        ImageInfo imageInfo = new ImageInfo(width, height,
            io.github.humbleui.skija.ColorType.BGRA_8888,
            io.github.humbleui.skija.ColorAlphaType.UNPREMUL);
        try (Surface surface = Surface.makeRaster(imageInfo);
             Paint paint = new Paint()) {

            Canvas canvas = surface.getCanvas();
            paint.setColor(0xFFFFFFFF);
            // 文本基线：-ascent 处
            canvas.drawString(text, 1.0F, -metrics.getAscent() + 1.0F, font, paint);

            // 读取像素（BGRA 字节序）
            java.nio.ByteBuffer pixelBuffer = MemoryUtil.memAlloc(width * height * 4);
            boolean readOk;
            try (Pixmap pixmap = Pixmap.make(imageInfo, MemoryUtil.memAddress(pixelBuffer), width * 4)) {
                readOk = surface.readPixels(pixmap, 0, 0);
            }
            if (!readOk) {
                MemoryUtil.memFree(pixelBuffer);
                return null;
            }

            int argb = applyTextColor(0xFFFFFFFF, color);
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int i = (py * width + px) * 4;
                    int b = pixelBuffer.get(i) & 0xFF;
                    int g = pixelBuffer.get(i + 1) & 0xFF;
                    int r = pixelBuffer.get(i + 2) & 0xFF;
                    int a = pixelBuffer.get(i + 3) & 0xFF;

                    // 应用文本颜色（UNPREMUL：直读，无精度损失）
                    int textR = (argb >>> 16) & 0xFF;
                    int textG = (argb >>> 8) & 0xFF;
                    int textB = argb & 0xFF;
                    int textA = (argb >>> 24) & 0xFF;

                    int outA = a * textA / 255;
                    int outR = r * textR / 255;
                    int outG = g * textG / 255;
                    int outB = b * textB / 255;

                    // NativeImage setColor 使用 ABGR
                    image.setColor(px, py, (outA << 24) | (outB << 16) | (outG << 8) | outR);
                }
            }
            MemoryUtil.memFree(pixelBuffer);

            String texName = "font_" + Integer.toHexString(key.hashCode());
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> texName, image);
            texture.upload();
            Identifier id = Identifier.of("advicenext", "font/" + texName);
            mc.getTextureManager().registerTexture(id, texture);

            TextureEntry entry = new TextureEntry(id, texture, width, height);
            textureCache.put(key, entry);
            return entry;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int applyTextColor(int base, int color) {
        return color;
    }

    private static void clearTextureCache() {
        for (TextureEntry entry : textureCache.values()) {
            try {
                mc.getTextureManager().destroyTexture(entry.id);
                entry.texture.close();
            } catch (Throwable ignored) {
            }
        }
        textureCache.clear();
    }

    /** 资源释放（客户端关闭时调用） */
    public static void shutdown() {
        clearTextureCache();
        fontCache.values().forEach(f -> {
            try {
                f.close();
            } catch (Throwable ignored) {
            }
        });
        fontCache.clear();
    }
}
