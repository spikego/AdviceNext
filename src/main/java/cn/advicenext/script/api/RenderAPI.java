package cn.advicenext.script.api;

import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public class RenderAPI {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public static void drawText(DrawContext context, String text, int x, int y, int color) {
        context.drawText(mc.textRenderer, text, x, y, color, true);
    }
    
    public static void drawText(DrawContext context, String text, int x, int y) {
        drawText(context, text, x, y, 0xFFFFFFFF);
    }
    
    public static void drawRect(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }
    
    public static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
    }
    
    public static int getStringWidth(String text) {
        return mc.textRenderer.getWidth(text);
    }
    
    public static int getFontHeight() {
        return mc.textRenderer.fontHeight;
    }
    
    public static int getScreenWidth() {
        return mc.getWindow().getScaledWidth();
    }
    
    public static int getScreenHeight() {
        return mc.getWindow().getScaledHeight();
    }
    
    public static int getCurrentColor() {
        return Colors.currentColor().getRGB();
    }
}