package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;

import java.util.List;

public class ModeSettingComponent extends SettingComponent {
    private final ModeSetting setting;
    private boolean expanded;

    public ModeSettingComponent(ModeSetting setting) {
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

        if (expanded) {
            List<String> modes = setting.getModes();
            float totalH = modes.size() * 20 + 2;
            SkijaUIRenderer.drawRoundedRect(x + 94, y + 13, 80, totalH, 2, NeverloseClickGuiScreen.bgColor.getRGB());
            SkijaUIRenderer.drawRoundedRect(x + 95, y + 14, 78, totalH - 2, 2, NeverloseClickGuiScreen.outlineColor.getRGB());

            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                float my = y + 15 + i * 20;
                if (mode.equals(setting.getValue())) {
                    SkijaUIRenderer.drawRoundedRect(x + 98, my, 72, 16, 2, NeverloseClickGuiScreen.bgColor3.getRGB());
                }
                FontRenderer mFont = Fonts.interSemiBold.get(16f);
                if (mFont != null) {
                    mFont.drawString(mode, x + 104, my + 6, mode.equals(setting.getValue()) ? 0xFFFFFFFF : 0xFFAAAAAA);
                }
            }
        } else {
            SkijaUIRenderer.drawRoundedRect(x + 94, y + 13, 80, 17, 2, NeverloseClickGuiScreen.bgColor.getRGB());
            SkijaUIRenderer.drawRoundedRect(x + 95, y + 14, 78, 15, 2, NeverloseClickGuiScreen.outlineColor.getRGB());
            FontRenderer valFont = Fonts.interSemiBold.get(16f);
            if (valFont != null) {
                valFont.drawString(setting.getValue(), x + 98, y + 15 + valFont.getHeight() / 2, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY, x + 94, y + 14, 80, 20) && mouseButton == 1) {
            expanded = !expanded;
        }
        if (expanded) {
            List<String> modes = setting.getModes();
            for (int i = 0; i < modes.size(); i++) {
                if (mouseButton == 0 && isHovered(mouseX, mouseY, x + 98, y + 15 + i * 20, 72, 16)) {
                    setting.setValue(modes.get(i));
                    expanded = false;
                }
            }
        }
    }
}