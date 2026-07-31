package cn.advicenext.gui.hud.widget;

import cn.advicenext.utility.client.render.SkijaUIRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class Widget {

    protected final String id;
    protected float x, y;
    protected float width, height;
    protected boolean visible = true;
    protected boolean draggable = true;

    public Widget(String id, float x, float y, float width, float height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float w, float h) {
        this.width = w;
        this.height = h;
    }

    public String getId() { return id; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
    public boolean isDraggable() { return draggable; }
    public void setDraggable(boolean d) { this.draggable = d; }

    protected void drawRoundedRect(String key, float x, float y, float w, float h, float r, int color) {
        SkijaUIRenderer.drawRoundedRect(key, x, y, w, h, r, color);
    }

    protected void drawRoundedRect(float x, float y, float w, float h, float r, int color) {
        SkijaUIRenderer.drawRoundedRect(x, y, w, h, r, color);
    }
}