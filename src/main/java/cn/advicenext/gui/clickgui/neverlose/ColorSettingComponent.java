package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.value.ColorSetting;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;

public class ColorSettingComponent extends SettingComponent {
    private final ColorSetting setting;

    public ColorSettingComponent(ColorSetting setting) {
        this.setting = setting;
        this.height = 24;
    }

    @Override
    public boolean isVisible() {
        return setting.getVisible().get();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, NeverloseClickGuiScreen screen) {
        SkijaUIRenderer.drawRect(x + 4, y + 10, 172, 1, NeverloseClickGuiScreen.lineColor2.getRGB());

        FontRenderer font = Fonts.interSemiBold.get(18f);
        if (font != null) {
            font.drawString(setting.getName(), x + 6, y + 20, NeverloseClickGuiScreen.textRGB);
        }

        SkijaUIRenderer.drawRoundedRect(x + 154, y + 16, 20, 10, 2, NeverloseClickGuiScreen.bgColor.getRGB());
        SkijaUIRenderer.drawRoundedRect(x + 155, y + 17, 18, 8, 2, setting.getValue());
    }
}