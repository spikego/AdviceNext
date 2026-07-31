package cn.advicenext.gui.clickgui.neverlose;

public abstract class SettingComponent {
    protected float x, y;
    protected float height = 24;

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getHeight() { return height; }

    public boolean isVisible() { return true; }

    public abstract void drawScreen(int mouseX, int mouseY, NeverloseClickGuiScreen screen);
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {}
    public void mouseReleased(int mouseX, int mouseY, int state) {}
    public void keyTyped(char typedChar, int keyCode) {}

    protected static boolean isHovered(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}