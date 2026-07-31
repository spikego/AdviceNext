package cn.advicenext.utility.client.render.font;

/**
 * 字体包装（兼容旧接口）。新代码请直接使用 {@link Fonts} 枚举获取 {@link FontRenderer} 实例。
 * <p>
 * {@code Fonts.REGULAR_14.get().drawString(ctx, text, x, y, color)}
 */
public final class Font {
    private final FontRenderer renderer;
    private final String fontName;
    private final float size;

    Font(FontRenderer renderer, String fontName, float size) {
        this.renderer = renderer;
        this.fontName = fontName;
        this.size = size;
    }

    public void drawString(String text, float x, float y, int color) {
        renderer.drawString(text, x, y, color);
    }

    public void drawCenteredString(String text, float cx, float y, int color) {
        renderer.drawCenteredString(text, cx, y, color);
    }

    public float getStringWidth(String text) {
        return renderer.getStringWidth(text);
    }
}
