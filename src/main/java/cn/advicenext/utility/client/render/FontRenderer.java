package cn.advicenext.utility.client.render;

import cn.advicenext.event.impl.Render2DEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class FontRenderer {
    private Font font;
    private final int fontSize;
    
    public FontRenderer(String fontPath, int fontSize) {
        this.fontSize = fontSize;
        loadFont(fontPath);
    }
    
    private void loadFont(String fontPath) {
        try (InputStream is = getClass().getResourceAsStream(fontPath)) {
            if (is != null) {
                font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont((float) fontSize);
            } else {
                font = new Font("Arial", Font.PLAIN, fontSize);
            }
        } catch (Exception e) {
            font = new Font("Arial", Font.PLAIN, fontSize);
        }
    }
    
    public void drawString(DrawContext context, String text, int x, int y, int color) {
        // 简化版本：使用原版TextRenderer作为后备
        context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, color, false);
    }
    
    public int getStringWidth(String text) {
        Graphics2D g2d = createGraphics();
        if (g2d == null) return MinecraftClient.getInstance().textRenderer.getWidth(text);
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        g2d.dispose();
        return width;
    }
    
    public int getFontHeight() {
        return fontSize;
    }
    
    private Graphics2D createGraphics() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        return img.createGraphics();
    }
}