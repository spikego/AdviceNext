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

    // ==================== 圆角边框（扫描线法，无 Alpha Overlap） ====================

    /**
     * 绘制圆角边框：通过逐行扫描计算外矩形与内矩形之间的间隙，
     * 一块 fill 完成整条边框，避免多层叠加造成的重叠区颜色加深。
     */
    public static void drawRoundOutline(DrawContext ctx, float x, float y, float width, float height,
                                        float radius, float thickness, int color) {
        if (thickness <= 0 || ((color >>> 24) & 0xFF) == 0) return;
        radius = RoundUtils.clampRadius(radius, width, height);

        // 外圆角半径 → 内圆角半径（偏移厚度）
        float outR = radius;
        float inR = Math.max(0, radius - thickness);
        float inX = x + thickness, inY = y + thickness;
        float inW = width - thickness * 2, inH = height - thickness * 2;

        int startY = (int) y;
        int endY = (int) (y + height);
        int intR = (int) Math.ceil(outR);

        for (int row = startY; row <= endY; row++) {
            float dyToTop = row - y;
            float dyToBot = (y + height) - row - 1;

            // 外矩形左右边界
            float outLeft = x;
            float outRight = x + width;
            if (dyToTop < intR) {
                float inset = RoundUtils.insetForRow(outR, dyToTop);
                outLeft = x + inset;
                outRight = x + width - inset;
            } else if (dyToBot < intR && dyToTop >= height - intR) {
                float inset = RoundUtils.insetForRow(outR, dyToBot);
                outLeft = x + inset;
                outRight = x + width - inset;
            }

            // 内矩形左右边界
            float inLeft = inX;
            float inRight = inX + inW;
            if (inR > 0) {
                float dyInTop = row - inY;
                float dyInBot = (inY + inH) - row - 1;
                if (dyInTop < inR) {
                    float inset = RoundUtils.insetForRow(inR, dyInTop);
                    inLeft = inX + inset;
                    inRight = inX + inW - inset;
                } else if (dyInBot < inR && dyInTop >= inH - inR) {
                    float inset = RoundUtils.insetForRow(inR, dyInBot);
                    inLeft = inX + inset;
                    inRight = inX + inW - inset;
                }
            }

            // 左侧边
            int l1 = (int) Math.ceil(outLeft);
            int l2 = (int) inLeft;
            if (l2 > l1) ctx.fill(l1, row, l2, row + 1, color);

            // 右侧边
            int r1 = (int) Math.ceil(inRight);
            int r2 = (int) outRight;
            if (r2 > r1) ctx.fill(r1, row, r2, row + 1, color);
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
    /** 1px 细线，比 drawLine 快约 2 倍（3D 线框专用） */
    public static void drawThinLine(DrawContext ctx, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float adx = Math.abs(dx), ady = Math.abs(dy);

        if (adx < 0.1F && ady < 0.1F) {
            ctx.fill((int) x1, (int) y1, (int) x1 + 1, (int) y1 + 1, color);
            return;
        }

        if (adx > ady) {
            if (x1 > x2) { float t = x1; x1 = x2; x2 = t; t = y1; y1 = y2; y2 = t; }
            float slope = (y2 - y1) / (x2 - x1);
            int ix1 = (int) x1, ix2 = (int) Math.ceil(x2);
            for (int x = ix1; x <= ix2; x++) {
                int y = (int) (y1 + (x - x1) * slope);
                ctx.fill(x, y, x + 1, y + 1, color);
            }
        } else {
            if (y1 > y2) { float t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
            float slope = (x2 - x1) / (y2 - y1);
            int iy1 = (int) y1, iy2 = (int) Math.ceil(y2);
            for (int y = iy1; y <= iy2; y++) {
                int x = (int) (x1 + (y - y1) * slope);
                ctx.fill(x, y, x + 1, y + 1, color);
            }
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