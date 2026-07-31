package cn.advicenext.features.value;

import java.awt.Color;
import java.util.function.Supplier;

/**
 * 颜色设置，存储 ARGB int，渲染为调色板控件。
 * 在 ClickGUI 中显示为可点击的颜色方块 + 展开的 RGB 滑块。
 */
public class ColorSetting extends AbstractSetting<Integer> {

    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
    }

    public ColorSetting(String name, String description, int defaultColor, Supplier<Boolean> visible) {
        super(name, description, defaultColor, visible);
    }

    public Color getColor() {
        return new Color(value, true);
    }

    public void setColor(Color color) {
        setValue(color.getRGB());
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return value & 0xFF;
    }

    public int getAlpha() {
        return (value >> 24) & 0xFF;
    }

    public void setRed(int r) {
        int a = getAlpha();
        setValue((a << 24) | (clamp(r) << 16) | (getGreen() << 8) | getBlue());
    }

    public void setGreen(int g) {
        int a = getAlpha();
        setValue((a << 24) | (getRed() << 16) | (clamp(g) << 8) | getBlue());
    }

    public void setBlue(int b) {
        int a = getAlpha();
        setValue((a << 24) | (getRed() << 16) | (getGreen() << 8) | clamp(b));
    }

    public void setAlpha(int a) {
        setValue((clamp(a) << 24) | (getRed() << 16) | (getGreen() << 8) | getBlue());
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}