package cn.advicenext.gui.hud;

import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * HUD 布局编辑器：拖拽调整 Watermark / ArrayList / TargetInfo 的位置。
 */
public class HUDEditScreen extends Screen {

    private final HUD hudModule;

    private String dragging = null;
    private float dragOffsetX, dragOffsetY;

    private static final int ARRAYLIST_W = 120, ARRAYLIST_H = 50;
    private static final int TARGETINFO_W = 150, TARGETINFO_H = 44;
    private static final int SNAP_DIST = 6;

    public HUDEditScreen() {
        super(Text.literal("HUD Editor"));
        this.hudModule = (HUD) ModuleManager.getModules().stream()
                .filter(m -> m instanceof HUD)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x60000000);
        drawGrid(context);

        context.drawCenteredTextWithShadow(textRenderer, "HUD Editor", width / 2, 16, Colors.currentColor().getRGB());

        if (hudModule == null) {
            context.drawCenteredTextWithShadow(textRenderer, "HUD module not found", width / 2, height / 2, 0xFFFF5555);
            return;
        }

        renderWatermarkPreview(context, mouseX, mouseY);
        renderArrayListPreview(context, mouseX, mouseY);
        renderTargetInfoPreview(context, mouseX, mouseY);

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

    private void renderWatermarkPreview(DrawContext context, int mouseX, int mouseY) {
        int x = resolveX(hudModule.watermarkX);
        int y = resolveY(hudModule.watermarkY);

        String text = hudModule.WaterMarkText.getValue();
        int boxW = textRenderer.getWidth(text) + 12;
        int boxH = 18;

        boolean active = inBounds(mouseX, mouseY, x, y, boxW, boxH) || "watermark".equals(dragging);

        drawElementCard(context, x, y, boxW, boxH, active, "Watermark");
        context.drawText(textRenderer, text, x + 6, y + 5, Colors.currentColor().getRGB(), false);
    }

    private void renderArrayListPreview(DrawContext context, int mouseX, int mouseY) {
        int x = resolveX(hudModule.arrayListX) - (hudModule.arrayListX < 0 ? ARRAYLIST_W : 0);
        int y = resolveY(hudModule.arrayListY);

        boolean active = inBounds(mouseX, mouseY, x, y, ARRAYLIST_W, ARRAYLIST_H) || "arraylist".equals(dragging);

        drawElementCard(context, x, y, ARRAYLIST_W, ARRAYLIST_H, active, "ArrayList");

        String[] preview = {"KillAura", "Scaffold", "ESP", "Speed"};
        int textY = y + 5;
        int i = 0;
        for (String name : preview) {
            context.drawText(textRenderer, name, x + 6, textY, Colors.gradientColor(i, preview.length).getRGB(), false);
            textY += 11;
            i++;
        }
    }

    private void renderTargetInfoPreview(DrawContext context, int mouseX, int mouseY) {
        int x = hudModule.targetInfoX >= 0 ? hudModule.targetInfoX : width / 2 - TARGETINFO_W / 2;
        int y = hudModule.targetInfoY >= 0 ? hudModule.targetInfoY : height / 2 + 25;

        boolean active = inBounds(mouseX, mouseY, x, y, TARGETINFO_W, TARGETINFO_H) || "targetinfo".equals(dragging);

        drawElementCard(context, x, y, TARGETINFO_W, TARGETINFO_H, active, "TargetInfo");

        int accent = Colors.currentColor().getRGB();
        context.fill(x, y, x + TARGETINFO_W, y + 2, accent);
        context.drawCenteredTextWithShadow(textRenderer, "Player", x + TARGETINFO_W / 2, y + 8, 0xFFFFFFFF);
        context.fill(x + 12, y + 22, x + TARGETINFO_W - 12, y + 27, 0xFF202020);
        context.fill(x + 12, y + 22, x + 12 + (int) ((TARGETINFO_W - 24) * 0.7), y + 27, 0xFF4CAF50);
        context.drawCenteredTextWithShadow(textRenderer, "14 HP", x + TARGETINFO_W / 2, y + 33, 0xFFCCCCCC);
    }

    private void drawElementCard(DrawContext context, int x, int y, int w, int h, boolean active, String label) {
        context.fill(x, y, x + w, y + h, 0x90101010);
        int borderColor = active ? Colors.currentColor().getRGB() : 0x60FFFFFF;
        // 边框
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
        if (hudModule == null) return super.mouseClicked(click, doubled);

        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        // 双击重置
        if (doubled) {
            if (inBounds(mouseX, mouseY, resolveX(hudModule.watermarkX), resolveY(hudModule.watermarkY), 120, 18)) {
                hudModule.watermarkX = 10;
                hudModule.watermarkY = 10;
                return true;
            }
            if (inBounds(mouseX, mouseY, resolveX(hudModule.arrayListX) - (hudModule.arrayListX < 0 ? ARRAYLIST_W : 0), resolveY(hudModule.arrayListY), ARRAYLIST_W, ARRAYLIST_H)) {
                hudModule.arrayListX = -5;
                hudModule.arrayListY = 10;
                return true;
            }
            int tx = hudModule.targetInfoX >= 0 ? hudModule.targetInfoX : width / 2 - TARGETINFO_W / 2;
            int ty = hudModule.targetInfoY >= 0 ? hudModule.targetInfoY : height / 2 + 25;
            if (inBounds(mouseX, mouseY, tx, ty, TARGETINFO_W, TARGETINFO_H)) {
                hudModule.targetInfoX = 520;
                hudModule.targetInfoY = 150;
                return true;
            }
        }

        // Watermark
        int wmX = resolveX(hudModule.watermarkX);
        int wmY = resolveY(hudModule.watermarkY);
        int wmW = textRenderer.getWidth(hudModule.WaterMarkText.getValue()) + 12;
        if (inBounds(mouseX, mouseY, wmX, wmY, wmW, 18)) {
            dragging = "watermark";
            dragOffsetX = (float) (mouseX - wmX);
            dragOffsetY = (float) (mouseY - wmY);
            return true;
        }

        // ArrayList
        int alX = resolveX(hudModule.arrayListX) - (hudModule.arrayListX < 0 ? ARRAYLIST_W : 0);
        int alY = resolveY(hudModule.arrayListY);
        if (inBounds(mouseX, mouseY, alX, alY, ARRAYLIST_W, ARRAYLIST_H)) {
            dragging = "arraylist";
            dragOffsetX = (float) (mouseX - alX);
            dragOffsetY = (float) (mouseY - alY);
            return true;
        }

        // TargetInfo
        int tiX = hudModule.targetInfoX >= 0 ? hudModule.targetInfoX : width / 2 - TARGETINFO_W / 2;
        int tiY = hudModule.targetInfoY >= 0 ? hudModule.targetInfoY : height / 2 + 25;
        if (inBounds(mouseX, mouseY, tiX, tiY, TARGETINFO_W, TARGETINFO_H)) {
            dragging = "targetinfo";
            dragOffsetX = (float) (mouseX - tiX);
            dragOffsetY = (float) (mouseY - tiY);
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging == null || hudModule == null) {
            return super.mouseDragged(click, offsetX, offsetY);
        }

        int newX = snap((int) (click.x() - dragOffsetX), width, 0);
        int newY = snap((int) (click.y() - dragOffsetY), height, 0);

        switch (dragging) {
            case "watermark" -> {
                hudModule.watermarkX = newX > width - 100 ? newX - width : newX;
                hudModule.watermarkY = newY > height - 20 ? newY - height : newY;
            }
            case "arraylist" -> {
                hudModule.arrayListX = newX > width / 2 ? newX - width : newX;
                hudModule.arrayListY = newY;
            }
            case "targetinfo" -> {
                hudModule.targetInfoX = Math.max(0, Math.min(width - TARGETINFO_W, newX));
                hudModule.targetInfoY = Math.max(0, Math.min(height - TARGETINFO_H, newY));
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int resolveX(int stored) {
        return stored < 0 ? width + stored : stored;
    }

    private int resolveY(int stored) {
        return stored < 0 ? height + stored : stored;
    }

    private int snap(int value, int max, int min) {
        if (Math.abs(value - min) < SNAP_DIST) return min;
        if (Math.abs(value - max / 2) < SNAP_DIST) return max / 2;
        return Math.max(min, Math.min(max, value));
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
