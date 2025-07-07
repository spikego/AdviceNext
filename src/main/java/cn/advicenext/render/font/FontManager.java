package cn.advicenext.render.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static FontManager instance;
    private final Map<String, FontRenderer> fonts = new HashMap<>();
    private FontRenderer currentFont;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private FontManager() {
        // 默认使用Minecraft的字体
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    /**
     * 加载自定义字体
     * @param name 字体名称
     * @param path 字体路径 (assets/advicenext/fonts/xxx.ttf)
     * @param size 字体大小
     */
    public void loadFont(String name, String path, float size) {
        fonts.put(name, new FontRenderer(path, size));
    }

    /**
     * 设置当前使用的字体
     * @param name 字体名称
     */
    public void setFont(String name) {
        if (fonts.containsKey(name)) {
            currentFont = fonts.get(name);
        }
    }

    /**
     * 获取指定名称的字体
     * @param name 字体名称
     * @return 字体渲染器
     */
    public FontRenderer getFont(String name) {
        return fonts.getOrDefault(name, null);
    }

    /**
     * 获取当前使用的字体
     * @return 当前字体渲染器
     */
    public FontRenderer getCurrentFont() {
        return currentFont;
    }

    /**
     * 使用当前字体绘制文本
     * @param matrices 矩阵堆栈
     * @param text 文本内容
     * @param x X坐标
     * @param y Y坐标
     * @param color 颜色
     * @param shadow 是否有阴影
     */
    public void drawString(DrawContext context,MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
        if (currentFont != null) {
            currentFont.drawString(matrices, text, x, y, color, shadow);
        } else {
            // 如果没有设置自定义字体，使用Minecraft默认字体
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color, shadow);
        }
    }

    /**
     * 获取文本宽度
     * @param text 文本内容
     * @return 文本宽度
     */
    public int getStringWidth(String text) {
        if (currentFont != null) {
            return currentFont.getStringWidth(text);
        } else {
            return mc.textRenderer.getWidth(text);
        }
    }

    /**
     * 获取字体高度
     * @return 字体高度
     */
    public int getFontHeight() {
        if (currentFont != null) {
            return currentFont.getHeight();
        } else {
            return mc.textRenderer.fontHeight;
        }
    }
}