package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.value.StringSetting;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;

public class StringSettingComponent extends SettingComponent {
    private final StringSetting setting;

    public StringSettingComponent(StringSetting setting) {
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

        SkijaUIRenderer.drawRoundedRect(x + 94, y + 13, 80, 17, 2, NeverloseClickGuiScreen.bgColor.getRGB());
        SkijaUIRenderer.drawRoundedRect(x + 95, y + 14, 78, 15, 2, NeverloseClickGuiScreen.outlineColor.getRGB());

        FontRenderer valFont = Fonts.interSemiBold.get(16f);
        if (valFont != null) {
            String val = setting.getValue();
            if (val != null) {
                valFont.drawString(val, x + 100, y + 15 + valFont.getHeight() / 2, 0xFFFFFFFF);
            }
        }
    }
}