package cn.advicenext.gui.hud;

import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HUDEditScreen extends Screen {
    private final HUD hudModule;
    private boolean dragging = false;
    private String dragElement = "";
    private int dragOffsetX, dragOffsetY;

    public HUDEditScreen() {
        super(Text.literal("HUD Editor"));
        this.hudModule = (HUD) ModuleManager.getModules().stream()
                .filter(m -> m instanceof HUD)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exit"), button -> this.close())
                .dimensions(this.width - 60, 10, 50, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        // 渲染水印
        int waterX = hudModule.watermarkX < 0 ? width + hudModule.watermarkX : hudModule.watermarkX;
        int waterY = hudModule.watermarkY < 0 ? height + hudModule.watermarkY : hudModule.watermarkY;
        context.drawText(textRenderer, "AdviceNext", waterX, waterY, Colors.currentColor().getRGB(), true);
        
        // 渲染ArrayList预览
        int arrayX = hudModule.arrayListX < 0 ? width + hudModule.arrayListX : hudModule.arrayListX;
        context.drawText(textRenderer, "ArrayList", arrayX, hudModule.arrayListY, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "Module1", arrayX, hudModule.arrayListY + 10, 0xFFFF0000, true);
        context.drawText(textRenderer, "Module2", arrayX, hudModule.arrayListY + 20, 0xFF00FF00, true);
        
        // 渲染TargetInfo预览
        int targetX = hudModule.targetInfoX < 0 ? width + hudModule.targetInfoX : hudModule.targetInfoX;
        int targetY = hudModule.targetInfoY < 0 ? height + hudModule.targetInfoY : hudModule.targetInfoY;
        context.fill(targetX, targetY, targetX + 150, targetY + 60, 0x80000000);
        int borderColor = Colors.currentColor().getRGB();
        context.fill(targetX, targetY, targetX + 150, targetY + 1, borderColor);
        context.fill(targetX, targetY, targetX + 1, targetY + 60, borderColor);
        context.fill(targetX + 149, targetY, targetX + 150, targetY + 60, borderColor);
        context.fill(targetX, targetY + 59, targetX + 150, targetY + 60, borderColor);
        context.drawText(textRenderer, "Target Info", targetX + 5, targetY + 5, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "Health: 20.0/20.0", targetX + 5, targetY + 30, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "5.2m", targetX + 5, targetY + 45, 0xFFAAAAAA, true);
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 检查点击的元素
            if (isInBounds(mouseX, mouseY, hudModule.watermarkX, hudModule.watermarkY, 80, 10)) {
                dragging = true;
                dragElement = "watermark";
                dragOffsetX = (int) (mouseX - hudModule.watermarkX);
                dragOffsetY = (int) (mouseY - hudModule.watermarkY);
                return true;
            }
            
            int arrayX = hudModule.arrayListX < 0 ? width + hudModule.arrayListX : hudModule.arrayListX;
            if (isInBounds(mouseX, mouseY, arrayX, hudModule.arrayListY, 120, 10)) {
                dragging = true;
                dragElement = "arraylist";
                dragOffsetX = (int) (mouseX - arrayX);
                dragOffsetY = (int) (mouseY - hudModule.arrayListY);
                return true;
            }
            
            int targetX = hudModule.targetInfoX < 0 ? width + hudModule.targetInfoX : hudModule.targetInfoX;
            int targetY = hudModule.targetInfoY < 0 ? height + hudModule.targetInfoY : hudModule.targetInfoY;
            if (isInBounds(mouseX, mouseY, targetX, targetY, 150, 60)) {
                dragging = true;
                dragElement = "targetinfo";
                dragOffsetX = (int) (mouseX - targetX);
                dragOffsetY = (int) (mouseY - targetY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            
            switch (dragElement) {
                case "watermark":
                    hudModule.watermarkX = newX;
                    hudModule.watermarkY = newY;
                    break;
                case "arraylist":
                    hudModule.arrayListX = newX > width / 2 ? newX - width : newX;
                    hudModule.arrayListY = newY;
                    break;
                case "targetinfo":
                    hudModule.targetInfoX = newX;
                    hudModule.targetInfoY = newY > height / 2 ? newY - height : newY;
                    break;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            dragElement = "";
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}