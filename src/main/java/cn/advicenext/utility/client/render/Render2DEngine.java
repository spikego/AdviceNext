package cn.advicenext.utility.client.render;

import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

/**
 * 2D 渲染引擎：基于逐行扫描线的高质量圆角绘制。
 * 每行一个 fill 调用（deferred quad），平滑圆角且性能可控。
 * 提供圆角矩形、圆角边框、渐变、Glow（多层扩散）、投影。
 */
public final class Render2DEngine {

    private Render2DEngine() {
    }

    // ==================== 圆角矩形 ====================

    public static void fillRoundRect(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
        radius = RoundUtils.clampRadius(radius, width, height);
        if (radius < 0.5F) {
            ctx.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
            return;
        }

        int intRadius = (int) Math.ceil(radius);

        // 中间主体（左右贯通的矩形区域）
        ctx.fill((int) (x + radius), (int) y, (int) (x + width - radius), (int) (y + height), color);
        // 左右中部的直筒区域
        ctx.fill((int) x, (int) (y + radius), (int) (x + radius), (int) (y + height - radius), color);
        ctx.fill((int) (x + width - radius), (int) (y + radius), (int) (x + width), (int) (y + height - radius), color);

        // 上下圆角：逐行扫描线
        for (int row = 0; row < intRadius; row++) {
            float inset = RoundUtils.insetForRow(radius, row + 0.5F);
            // 顶行
            ctx.fill((int) (x + inset), (int) (y + row), (int) (x + width - inset), (int) (y + row + 1), color);
            // 底行
            int bottomRow = (int) (y + height - row - 1);
            ctx.fill((int) (x + inset), bottomRow, (int) (x + width - inset), bottomRow + 1, color);
        }
    }

    // ==================== 圆角边框 ====================

    public static void drawRoundOutline(DrawContext ctx, float x, float y, float width, float height,
                                        float radius, float thickness, int color) {
        for (float t = 0; t < thickness; t += 1.0F) {
            float r = Math.max(0.0F, radius - t);
            drawRoundOutlinePass(ctx, x + t, y + t, width - t * 2, height - t * 2, r, color);
        }
    }

    private static void drawRoundOutlinePass(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
        radius = RoundUtils.clampRadius(radius, width, height);
        int intRadius = (int) Math.ceil(radius);

        // 四条直边
        ctx.fill((int) (x + radius), (int) y, (int) (x + width - radius), (int) (y + 1), color);
        ctx.fill((int) (x + radius), (int) (y + height - 1), (int) (x + width - radius), (int) (y + height), color);
        ctx.fill((int) x, (int) (y + radius), (int) (x + 1), (int) (y + height - radius), color);
        ctx.fill((int) (x + width - 1), (int) (y + radius), (int) (x + width), (int) (y + height - radius), color);

        // 四角：逐行绘制上下边线段
        for (int row = 0; row < intRadius; row++) {
            float inset = RoundUtils.insetForRow(radius, row + 0.5F);
            // 顶部左右角
            ctx.fill((int) (x + inset), (int) (y + row), (int) (x + radius), (int) (y + row + 1), color);
            ctx.fill((int) (x + width - radius), (int) (y + row), (int) (x + width - inset), (int) (y + row + 1), color);
            // 底部左右角
            int bottomRow = (int) (y + height - row - 1);
            ctx.fill((int) (x + inset), bottomRow, (int) (x + radius), bottomRow + 1, color);
            ctx.fill((int) (x + width - radius), bottomRow, (int) (x + width - inset), bottomRow + 1, color);
        }
    }

    // ==================== 渐变圆角 ====================

    public static void fillRoundGradient(DrawContext ctx, float x, float y, float width, float height,
                                         float radius, int colorTop, int colorBottom) {
        radius = RoundUtils.clampRadius(radius, width, height);
        int totalHeight = (int) height;
        for (int row = 0; row < totalHeight; row++) {
            float t = totalHeight <= 1 ? 0.0F : (float) row / (totalHeight - 1);
            int color = lerpColor(colorTop, colorBottom, t);

            float inset = 0.0F;
            if (row < radius) {
                inset = RoundUtils.insetForRow(radius, row + 0.5F);
            } else if (row >= totalHeight - radius) {
                inset = RoundUtils.insetForRow(radius, totalHeight - row - 0.5F);
            }
            ctx.fill((int) (x + inset), (int) (y + row), (int) (x + width - inset), (int) (y + row + 1), color);
        }
    }

    // ==================== Glow（外发光） ====================

    /**
     * 多层外扩圆角形成的发光效果。alpha 从 color 的原始值向外逐层衰减。
     *
     * @param layers 扩散层数（即发光范围，像素）
     */
    public static void drawGlow(DrawContext ctx, float x, float y, float width, float height,
                                float radius, int color, int layers) {
        int baseAlpha = (color >>> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            // 外层更透明：平方衰减更自然
            float factor = (float) i / layers;
            int alpha = (int) (baseAlpha * 0.35F * (1.0F - factor) * (1.0F - factor));
            if (alpha <= 0) continue;
            int layerColor = (alpha << 24) | (color & 0x00FFFFFF);
            fillRoundRect(ctx, x - i, y - i, width + i * 2, height + i * 2, radius + i, layerColor);
        }
        // 主体
        fillRoundRect(ctx, x, y, width, height, radius, color);
    }

    // ==================== 投影 ====================

    public static void drawDropShadow(DrawContext ctx, float x, float y, float width, float height,
                                      float radius, int layers) {
        for (int i = layers; i >= 1; i--) {
            float factor = (float) i / layers;
            int alpha = (int) (60.0F * (1.0F - factor) * (1.0F - factor));
            if (alpha <= 0) continue;
            int shadowColor = alpha << 24;
            fillRoundRect(ctx, x - i, y - i + 1, width + i * 2, height + i * 2, radius + i, shadowColor);
        }
    }

    // ==================== 线条 ====================

    /** 两点间粗线（沿方向步进绘制 thickness×thickness 方块） */
    public static void drawLine(DrawContext ctx, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.5F) {
            ctx.fill((int) (x1 - thickness / 2), (int) (y1 - thickness / 2),
                (int) (x1 + thickness / 2), (int) (y1 + thickness / 2), color);
            return;
        }

        int steps = (int) Math.ceil(dist / Math.max(1.0F, thickness * 0.5F));
        float half = thickness / 2.0F;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float cx = x1 + dx * t;
            float cy = y1 + dy * t;
            ctx.fill((int) (cx - half), (int) (cy - half), (int) (cx + half), (int) (cy + half), color);
        }
    }

    // ==================== 三角形 ====================

    /** scanline 三角形填充（3D 投影面/圆盘填充用） */
    public static void fillTriangle(DrawContext ctx, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        // 按 y 排序
        if (y2 < y1) { float tx = x1, ty = y1; x1 = x2; y1 = y2; x2 = tx; y2 = ty; }
        if (y3 < y1) { float tx = x1, ty = y1; x1 = x3; y1 = y3; x3 = tx; y3 = ty; }
        if (y3 < y2) { float tx = x2, ty = y2; x2 = x3; y2 = y3; x3 = tx; y3 = ty; }

        int screenWidth = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight();

        int startY = Math.max(0, (int) y1);
        int endY = Math.min(screenHeight, (int) Math.ceil(y3));

        for (int y = startY; y <= endY; y++) {
            float xa = interpolateX(x1, y1, x3, y3, y);
            float xb;
            if (y < y2) {
                xb = interpolateX(x1, y1, x2, y2, y);
            } else {
                xb = interpolateX(x2, y2, x3, y3, y);
            }

            if (xa > xb) { float t = xa; xa = xb; xb = t; }

            int ix1 = Math.max(0, (int) xa);
            int ix2 = Math.min(screenWidth, (int) Math.ceil(xb));
            if (ix2 > ix1) {
                ctx.fill(ix1, y, ix2, y + 1, color);
            }
        }
    }

    private static float interpolateX(float x1, float y1, float x2, float y2, float y) {
        if (Math.abs(y2 - y1) < 1.0E-4F) return x1;
        return x1 + (x2 - x1) * ((y - y1) / (y2 - y1));
    }

    // ==================== 圆 ====================

    public static void fillCircle(DrawContext ctx, float centerX, float centerY, float radius, int color) {
        int intRadius = (int) Math.ceil(radius);
        for (int row = -intRadius; row <= intRadius; row++) {
            float halfWidth = (float) Math.sqrt(Math.max(0.0, radius * radius - row * row));
            int drawY = (int) (centerY + row);
            ctx.fill((int) (centerX - halfWidth), drawY, (int) (centerX + halfWidth), drawY + 1, color);
        }
    }

    // ==================== 工具 ====================

    public static int lerpColor(int colorA, int colorB, float t) {
        int a1 = (colorA >>> 24) & 0xFF, r1 = (colorA >>> 16) & 0xFF, g1 = (colorA >>> 8) & 0xFF, b1 = colorA & 0xFF;
        int a2 = (colorB >>> 24) & 0xFF, r2 = (colorB >>> 16) & 0xFF, g2 = (colorB >>> 8) & 0xFF, b2 = colorB & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int applyAlpha(int color, float multiplier) {
        int alpha = (int) (((color >>> 24) & 0xFF) * multiplier);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int colorToInt(Color color) {
        return color.getRGB();
    }
}
