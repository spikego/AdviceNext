package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import java.awt.Color;

public class RenderUtils {
    
    public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, 
                                      float radius, int color) {
        if (radius <= 0) {
            context.fill((int)x, (int)y, (int)(x + width), (int)(y + height), color);
            return;
        }
        
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        int ix = (int) x;
        int iy = (int) y;
        int iw = (int) width;
        int ih = (int) height;
        int ir = (int) radius;
        int argb = color;
        
        // Main body
        context.fill(ix + ir, iy, ix + iw - ir, iy + ih, argb);
        // Left and right sides
        context.fill(ix, iy + ir, ix + ir, iy + ih - ir, argb);
        context.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih - ir, argb);
        
        // Four corners
        fillCircleQuarter(context, ix + ir, iy + ir, ir, 0, argb);
        fillCircleQuarter(context, ix + iw - ir, iy + ir, ir, 1, argb);
        fillCircleQuarter(context, ix + ir, iy + ih - ir, ir, 2, argb);
        fillCircleQuarter(context, ix + iw - ir, iy + ih - ir, ir, 3, argb);
    }
    
    private static void fillCircleQuarter(DrawContext context, int cx, int cy, int radius, int quarter, int color) {
        for (int i = 0; i <= radius; i++) {
            for (int j = 0; j <= radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    int dx = 0, dy = 0;
                    switch (quarter) {
                        case 0: dx = -i; dy = -j; break;
                        case 1: dx = i; dy = -j; break;
                        case 2: dx = -i; dy = j; break;
                        case 3: dx = i; dy = j; break;
                    }
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }
    
    public static void drawRoundedBorder(DrawContext context, float x, float y, float width, float height,
                                        float radius, float borderWidth, int borderColor) {
        if (radius <= 0) {
            int bw = (int) borderWidth;
            context.fill((int)x, (int)y, (int)(x + width), (int)(y + bw), borderColor);
            context.fill((int)x, (int)(y + height - bw), (int)(x + width), (int)(y + height), borderColor);
            context.fill((int)x, (int)(y + bw), (int)(x + bw), (int)(y + height - bw), borderColor);
            context.fill((int)(x + width - bw), (int)(y + bw), (int)(x + width), (int)(y + height - bw), borderColor);
            return;
        }
        
        radius = Math.min(radius, Math.min(width, height) / 2);
        float innerRadius = Math.max(0, radius - borderWidth);
        
        int ix = (int) x;
        int iy = (int) y;
        int iw = (int) width;
        int ih = (int) height;
        int ir = (int) radius;
        int iir = (int) innerRadius;
        int bw = (int) borderWidth;
        int argb = borderColor;
        
        // Top and bottom borders
        context.fill(ix + ir, iy, ix + iw - ir, iy + bw, argb);
        context.fill(ix + ir, iy + ih - bw, ix + iw - ir, iy + ih, argb);
        
        // Left and right borders
        context.fill(ix, iy + ir, ix + bw, iy + ih - ir, argb);
        context.fill(ix + iw - bw, iy + ir, ix + iw, iy + ih - ir, argb);
        
        // Corner borders
        fillCircleQuarterBorder(context, ix + ir, iy + ir, ir, iir, 0, argb);
        fillCircleQuarterBorder(context, ix + iw - ir, iy + ir, ir, iir, 1, argb);
        fillCircleQuarterBorder(context, ix + ir, iy + ih - ir, ir, iir, 2, argb);
        fillCircleQuarterBorder(context, ix + iw - ir, iy + ih - ir, ir, iir, 3, argb);
    }
    
    private static void fillCircleQuarterBorder(DrawContext context, int cx, int cy, int outerRadius, int innerRadius, int quarter, int color) {
        for (int i = 0; i <= outerRadius; i++) {
            for (int j = 0; j <= outerRadius; j++) {
                int dist = i * i + j * j;
                if (dist <= outerRadius * outerRadius && dist >= innerRadius * innerRadius) {
                    int dx = 0, dy = 0;
                    switch (quarter) {
                        case 0: dx = -i; dy = -j; break;
                        case 1: dx = i; dy = -j; break;
                        case 2: dx = -i; dy = j; break;
                        case 3: dx = i; dy = j; break;
                    }
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }
    
    public static void drawBlurredBackground(DrawContext context, float x, float y, float width, float height,
                                            float radius, int backgroundColor) {
        drawRoundedRect(context, x, y, width, height, radius, backgroundColor);
    }
    
    public static void drawGradientRect(DrawContext context, float x, float y, float width, float height,
                                       Color startColor, Color endColor, boolean vertical) {
        int steps = vertical ? (int) height : (int) width;
        for (int i = 0; i < steps; i++) {
            float progress = (float) i / steps;
            int r = (int) (startColor.getRed() * (1 - progress) + endColor.getRed() * progress);
            int g = (int) (startColor.getGreen() * (1 - progress) + endColor.getGreen() * progress);
            int b = (int) (startColor.getBlue() * (1 - progress) + endColor.getBlue() * progress);
            int a = (int) (startColor.getAlpha() * (1 - progress) + endColor.getAlpha() * progress);
            int color = (a << 24) | (r << 16) | (g << 8) | b;
            
            if (vertical) {
                context.fill((int)x, (int)(y + i), (int)(x + width), (int)(y + i + 1), color);
            } else {
                context.fill((int)(x + i), (int)y, (int)(x + i + 1), (int)(y + height), color);
            }
        }
    }
    
    public static void drawBorder(DrawContext context, float x, float y, float width, float height,
                                 float radius, int borderColor, float borderWidth) {
        drawRoundedBorder(context, x, y, width, height, radius, borderWidth, borderColor);
    }
    
    public static void drawShadow(DrawContext context, float x, float y, float width, float height,
                                 float radius, int shadowColor, float shadowSize) {
        for (int i = (int)shadowSize; i > 0; i--) {
            int alpha = (int)((float)i / shadowSize * 50);
            int color = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            drawRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2,
                           radius + i, color);
        }
    }
}