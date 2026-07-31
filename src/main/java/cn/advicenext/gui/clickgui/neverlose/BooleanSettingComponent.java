package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import cn.advicenext.utility.minecraft.client.ColorUtils;

import java.awt.Color;

public class BooleanSettingComponent extends SettingComponent {
    private final BooleanSetting setting;
    private final ClickAnimation anim = new ClickAnimation(250, 1);
    private final ClickAnimation hoverAnim = new ClickAnimation(250, 1);

    public BooleanSettingComponent(BooleanSetting setting) {
        this.setting = setting;
        this.height = 24;
        anim.setDirection(setting.getValue());
    }

    @Override
    public boolean isVisible() {
        return setting.getVisible().get();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, NeverloseClickGuiScreen screen) {
        SkijaUIRenderer.drawRect(x + 4, y + 10, 172, 1, NeverloseClickGuiScreen.lineColor2.getRGB());

        anim.setDirection(setting.getValue());
        FontRenderer font = Fonts.interSemiBold.get(18f);
        if (font != null) {
            font.drawString(setting.getName(), x + 6, y + 20, setting.getValue() ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        boolean hoverToggle = isHovered(mouseX, mouseY, x + 154, y + 16, 20, 10);
        hoverAnim.setDirection(hoverToggle);

        Color bgBase = ColorUtils.interpolateColorC(
            NeverloseClickGuiScreen.boolBgColor,
            NeverloseClickGuiScreen.boolBgColor2,
            (float) anim.getOutput());
        Color bgHover = ColorUtils.interpolateColorC(bgBase, bgBase.brighter().brighter(), (float) hoverAnim.getOutput());
        SkijaUIRenderer.drawRoundedRect(x + 154, y + 16, 20, 10, 4, bgHover.getRGB());

        Color circleBase = ColorUtils.interpolateColorC(
            NeverloseClickGuiScreen.boolCircleColor2,
            NeverloseClickGuiScreen.boolCircleColor,
            (float) anim.getOutput());
        Color circleHover = ColorUtils.interpolateColorC(
            circleBase.darker().darker(),
            circleBase,
            (float) hoverAnim.getOutput());
        float circleX = x + 159 + 10 * (float) anim.getOutput();
        SkijaUIRenderer.drawCircle(circleX, y + 21, 8, circleHover.getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY, x + 154, y + 16, 20, 10)) {
            setting.setValue(!setting.getValue());
        }
    }
}