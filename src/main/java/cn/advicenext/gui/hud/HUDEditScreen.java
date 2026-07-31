package cn.advicenext.gui.hud;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.gui.hud.widget.WidgetRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HUDEditScreen extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private String draggingId = null;
    private float dragOffsetX, dragOffsetY;

    private static final int SNAP_DIST = 6;

    public HUDEditScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x60000000);
        drawGrid(context);

        context.drawCenteredTextWithShadow(textRenderer, "HUD Editor", width / 2, 16, Colors.currentColor().getRGB());

        for (Widget widget : WidgetRegistry.getAll()) {
            if (!widget.isVisible()) continue;

            float wx = widget.getX();
            float wy = widget.getY();
            float ww = widget.getWidth();
            float wh = widget.getHeight();

            boolean active = draggingId != null && draggingId.equals(widget.getId());
            if (!active) {
                active = widget.isHovered(mouseX, mouseY);
            }

            drawElementCard(context, (int) wx, (int) wy, (int) ww, (int) wh, active, widget.getId());

            widget.render(context, mouseX, mouseY, delta);
        }

        context.drawCenteredTextWithShadow(textRenderer, "Drag to move | Double click to reset | ESC to exit",
                width / 2, height - 20, 0xFF888888);
    }

    private void drawGrid(DrawContext context) {
        int gridColor = 0x12FFFFFF;
        int gridSize = 20;
        for (int x = 0; x < width; x += gridSize) {
            context.fill(x, 0, x + 1, height, gridColor);
        }
        for (int y = 0; y < height; y += gridSize) {
            context.fill(0, y, width, y + 1, gridColor);
        }
        context.fill(width / 2, 0, width / 2 + 1, height, 0x28FFFFFF);
        context.fill(0, height / 2, width, height / 2 + 1, 0x28FFFFFF);
    }

    private void drawElementCard(DrawContext context, int x, int y, int w, int h, boolean active, String label) {
        int bg = active ? 0x90101010 : 0x50101010;
        context.fill(x, y, x + w, y + h, bg);
        int borderColor = active ? Colors.currentColor().getRGB() : 0x60FFFFFF;
        context.fill(x, y, x + w, y + 1, borderColor);
        context.fill(x, y + h - 1, x + w, y + h, borderColor);
        context.fill(x, y, x + 1, y + h, borderColor);
        context.fill(x + w - 1, y, x + w, y + h, borderColor);
        if (active) {
            context.drawTextWithShadow(textRenderer, label, x, y - 11, Colors.currentColor().getRGB());
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        if (doubled) {
            for (Widget widget : WidgetRegistry.getAll()) {
                if (!widget.isDraggable()) continue;
                if (widget.isHovered(mx, my)) {
                    widget.setPosition(10, 10);
                    return true;
                }
            }
            return true;
        }

        for (Widget widget : WidgetRegistry.getAll()) {
            if (!widget.isDraggable()) continue;
            if (widget.isHovered(mx, my)) {
                draggingId = widget.getId();
                dragOffsetX = (float) (mx - widget.getX());
                dragOffsetY = (float) (my - widget.getY());
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingId == null) return super.mouseDragged(click, offsetX, offsetY);

        Widget widget = WidgetRegistry.get(draggingId);
        if (widget == null) return true;

        int newX = snap((int) (click.x() - dragOffsetX), width, 0);
        int newY = snap((int) (click.y() - dragOffsetY), height, 0);

        widget.setPosition(newX, newY);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingId = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            saveWidgetPositions();
            ConfigManager.getInstance().getModuleConfig().saveConfig("default");
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void saveWidgetPositions() {
        for (Widget widget : WidgetRegistry.getAll()) {
            WidgetRegistry.register(widget);
        }
    }

    private int snap(int value, int max, int min) {
        if (Math.abs(value - min) < SNAP_DIST) return min;
        if (Math.abs(value - max / 2) < SNAP_DIST) return max / 2;
        return Math.max(min, Math.min(max, value));
    }
}