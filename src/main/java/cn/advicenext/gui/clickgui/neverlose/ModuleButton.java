package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.*;
import cn.advicenext.features.value.slider.*;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import cn.advicenext.utility.minecraft.client.ColorUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Module module;
    private int x, y, height = 20;
    private int scroll;
    private boolean left = true;

    private final List<SettingComponent> components = new ArrayList<>();

    private final ClickAnimation enabledAnim = new ClickAnimation(250, 1);
    private final ClickAnimation hoverAnim = new ClickAnimation(250, 1);

    public ModuleButton(Module module) {
        this.module = module;
        for (AbstractSetting<?> setting : module.settings) {
            if (setting instanceof BooleanSetting bs) {
                components.add(new BooleanSettingComponent(bs));
            } else if (setting instanceof ModeSetting ms) {
                components.add(new ModeSettingComponent(ms));
            } else if (setting instanceof DoubleSetting ds) {
                components.add(new SliderSettingComponent(ds));
            } else if (setting instanceof IntSetting is) {
                components.add(new SliderSettingComponent(is));
            } else if (setting instanceof FloatSetting fs) {
                components.add(new SliderSettingComponent(fs));
            } else if (setting instanceof ColorSetting cs) {
                components.add(new ColorSettingComponent(cs));
            } else if (setting instanceof StringSetting ss) {
                components.add(new StringSettingComponent(ss));
            }
        }
        enabledAnim.setDirection(module.getEnabled());
    }

    public Module getModule() { return module; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public void setScroll(int scroll) { this.scroll = scroll; }
    public boolean isLeft() { return left; }
    public void setLeft(boolean left) { this.left = left; }

    public float getComponentsHeight() {
        float h = 0;
        for (SettingComponent c : components) {
            if (c.isVisible()) h += c.getHeight();
        }
        return h;
    }

    public float getMaxScroll(NeverloseClickGuiScreen screen) {
        return ((getY() - screen.getPosY()) + getHeight()) * 4;
    }

    public void drawScreen(int mouseX, int mouseY, NeverloseClickGuiScreen screen) {
        float ey = y + 6 + scroll;

        FontRenderer nameFont = Fonts.interSemiBold.get(14f);
        String name = module.getName().replaceAll("(?<=[a-z])(?=[A-Z])", " ").toUpperCase();
        nameFont.drawString(name, x + 4, ey, NeverloseClickGuiScreen.moduleTextRGB);

        SkijaUIRenderer.drawRoundedRect(x, ey + 10, 185 / 2f, height, 4, NeverloseClickGuiScreen.bgColor4.getRGB());
        SkijaUIRenderer.drawRect(x + 1, ey + 11, 185 / 2f - 2, height - 2, NeverloseClickGuiScreen.outlineColor.getRGB());

        enabledAnim.setDirection(module.getEnabled());
        int toggleX = x + 154;
        int toggleY = (int) ey + 16;
        boolean hoverToggle = isHovered(mouseX, mouseY, toggleX, toggleY, 20, 10);
        hoverAnim.setDirection(hoverToggle);

        FontRenderer enFont = Fonts.interSemiBold.get(18f);
        if (enFont != null) {
            enFont.drawString("Enabled", x + 6, ey + 18, NeverloseClickGuiScreen.textRGB);
        }

        Color bgBase = ColorUtils.interpolateColorC(
            NeverloseClickGuiScreen.boolBgColor,
            NeverloseClickGuiScreen.boolBgColor2,
            (float) enabledAnim.getOutput());
        Color bgHover = ColorUtils.interpolateColorC(bgBase, bgBase.brighter().brighter(), (float) hoverAnim.getOutput());
        SkijaUIRenderer.drawRoundedRect(toggleX, toggleY, 20, 10, 4, bgHover.getRGB());

        Color circleBase = ColorUtils.interpolateColorC(
            NeverloseClickGuiScreen.boolCircleColor2,
            NeverloseClickGuiScreen.boolCircleColor,
            (float) enabledAnim.getOutput());
        Color circleHover = ColorUtils.interpolateColorC(
            circleBase.darker().darker(),
            circleBase,
            (float) hoverAnim.getOutput());
        float circleX = toggleX + 5 + 10 * (float) enabledAnim.getOutput();
        SkijaUIRenderer.drawCircle(circleX, toggleY + 5, 8, circleHover.getRGB());

        float componentY = ey + 22;
        for (SettingComponent component : components) {
            if (!component.isVisible()) continue;
            component.setX(x);
            component.setY(componentY);
            component.drawScreen(mouseX, mouseY, screen);
            componentY += component.getHeight();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float ey = y + 6 + scroll;
        if (isHovered(mouseX, mouseY, x + 154, (int) ey + 16, 20, 10) && mouseButton == 0) {
            module.toggle();
        }
        for (SettingComponent component : components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (SettingComponent component : components) {
            component.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (SettingComponent component : components) {
            component.keyTyped(typedChar, keyCode);
        }
    }

    private static boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}