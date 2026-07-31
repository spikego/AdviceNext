package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class Panel {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final Category category;
    private boolean selected;
    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    private float rawScroll;
    private float maxScroll = Float.MAX_VALUE;
    private final ClickAnimation scrollAnimation = new ClickAnimation(0, 0, false);
    private final ClickAnimation animation = new ClickAnimation(250, 1);

    public Panel(Category category) {
        this.category = category;
        for (Module module : ModuleManager.getModules()) {
            if (module.getCategory() == category) {
                moduleButtons.add(new ModuleButton(module));
            }
        }
    }

    public Category getCategory() { return category; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public ClickAnimation getAnimation() { return animation; }

    public void drawScreen(int mouseX, int mouseY) {
        NeverloseClickGuiScreen screen = null;
        if (mc.currentScreen instanceof NeverloseClickGuiScreen s) {
            screen = s;
        }
        if (screen == null) return;

        animation.setDirection(selected);

        if (isSelected()) {
            int posX = screen.getPosX();
            int posY = screen.getPosY();

            float left = 0, right = 0;
            for (int i = 0; i < moduleButtons.size(); i++) {
                ModuleButton module = moduleButtons.get(i);
                float componentOffset = getComponentOffset(i, left, right, screen);

                module.drawScreen(mouseX, mouseY, screen);

                double scroll = getScroll();
                module.setScroll((int) Math.round(scroll / 2.0) * 2);

                maxScroll = Math.max(0, moduleButtons.isEmpty() ? 0 :
                    moduleButtons.get(moduleButtons.size() - 1).getMaxScroll(screen));

                if ((i + 1) % 2 == 0) {
                    left += 40 + componentOffset;
                } else {
                    right += 40 + componentOffset;
                }
            }
        }
    }

    private float getComponentOffset(int i, float left, float right, NeverloseClickGuiScreen screen) {
        ModuleButton component = moduleButtons.get(i);
        component.setLeft((i + 1) % 2 != 0);
        int posX = screen.getPosX();
        int posY = screen.getPosY();
        component.setX(component.isLeft() ? posX + 140 : posX + 330);
        component.setHeight(20);
        component.setY((int) (posY + 32 + component.getHeight() + ((i + 1) % 2 == 0 ? left : right)));
        float componentOffset = component.getComponentsHeight();
        component.setHeight((int) (component.getHeight() + componentOffset));
        return componentOffset;
    }

    public float getScroll() {
        return (float) (rawScroll - scrollAnimation.getSmoothOutput());
    }

    public void addScroll(float amount) {
        NeverloseClickGuiScreen screen = null;
        if (mc.currentScreen instanceof NeverloseClickGuiScreen s) {
            screen = s;
        }
        if (screen == null) return;

        rawScroll += amount;
        rawScroll = Math.max(Math.min(0, rawScroll), -maxScroll);
        scrollAnimation.setEndPoint(rawScroll);
        scrollAnimation.setDirection(true);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isSelected()) {
            for (ModuleButton button : moduleButtons) {
                button.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (isSelected()) {
            for (ModuleButton button : moduleButtons) {
                button.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (isSelected()) {
            for (ModuleButton button : moduleButtons) {
                button.keyTyped(typedChar, keyCode);
            }
        }
    }
}