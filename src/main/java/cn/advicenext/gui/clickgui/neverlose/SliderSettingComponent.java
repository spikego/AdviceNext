package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.value.slider.NumberSetting;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import cn.advicenext.utility.minecraft.client.ColorUtils;

import java.awt.Color;

public class SliderSettingComponent extends SettingComponent {
    private final NumberSetting<?> setting;
    private boolean dragging;

    public SliderSettingComponent(NumberSetting<?> setting) {
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

        double min = setting.getMin().doubleValue();
        double max = setting.getMax().doubleValue();
        double val = setting.getValue().doubleValue();
        double pct = (val - min) / (max - min);

        float sliderX = x + 90;
        float sliderY = y + 22;
        float sliderW = 60;

        SkijaUIRenderer.drawRoundedRect(sliderX, sliderY, sliderW, 2, 1, NeverloseClickGuiScreen.sliderBgColor.getRGB());
        float fillW = (float) (sliderW * pct);
        if (fillW > 0) {
            SkijaUIRenderer.drawRoundedRect(sliderX, sliderY, fillW, 2, 1, NeverloseClickGuiScreen.sliderBarColor.getRGB());
        }

        float circleX = sliderX + fillW;
        SkijaUIRenderer.drawCircle(circleX, sliderY + 1, 6, NeverloseClickGuiScreen.sliderCircleColor.getRGB());

        SkijaUIRenderer.drawRoundedRect(x + 154, y + 18, 20, 10, 2, NeverloseClickGuiScreen.bgColor4.getRGB());
        SkijaUIRenderer.drawRoundedRect(x + 155, y + 19, 18, 8, 2, NeverloseClickGuiScreen.outlineColor2.getRGB());

        FontRenderer valFont = Fonts.interSemiBold.get(14f);
        if (valFont != null) {
            String display = String.format("%.1f", val);
            valFont.drawCenteredString(display, x + 164, y + 22, NeverloseClickGuiScreen.textRGB);
        }

        if (dragging) {
            double diff = max - min;
            double newVal = min + Math.max(0, Math.min(1, (mouseX - sliderX) / sliderW)) * diff;
            setting.setValueFromDouble(roundToInc(newVal));
        }
    }

    private double roundToInc(double val) {
        double inc = setting.getStep().doubleValue();
        return Math.round(val / inc) * inc;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY, x + 90, y + 18, 60, 10)) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
    }
}