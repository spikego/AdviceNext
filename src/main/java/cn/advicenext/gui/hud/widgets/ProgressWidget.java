package cn.advicenext.gui.hud.widgets;

import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;

public class ProgressWidget extends Widget {

    private float progress;
    private String label;
    private String subLabel;
    private float animatedProgress;
    private int accentColor;
    private int bgColor;
    private int textColor;
    private boolean showLabel;

    public ProgressWidget(float x, float y, float width, float height) {
        super("ProgressWidget", x, y, width, height);
        this.progress = 0f;
        this.label = "";
        this.subLabel = "";
        this.animatedProgress = 0f;
        this.accentColor = Colors.currentColor().getRGB();
        this.bgColor = 0x30FFFFFF;
        this.textColor = 0xCCFFFFFF;
        this.showLabel = true;
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSubLabel(String subLabel) {
        this.subLabel = subLabel;
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
    }

    public void setBgColor(int color) {
        this.bgColor = color;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setShowLabel(boolean show) {
        this.showLabel = show;
    }

    public float getProgress() {
        return progress;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        animatedProgress += (progress - animatedProgress) * 0.12f;

        FontRenderer font = Fonts.robotoMedium.get(11);

        if (showLabel && !label.isEmpty()) {
            font.drawString(label, x, y - 2, textColor);
        }

        float barY = showLabel ? y + 10 : y;
        float barH = 6;
        float barR = barH / 2;

        SkijaUIRenderer.drawRoundedRect(x, barY, width, barH, barR, bgColor);

        float fillW = Math.max(barR * 2, width * animatedProgress);
        if (animatedProgress > 0.01f) {
            SkijaUIRenderer.drawRoundedRect(x, barY, fillW, barH, barR, accentColor);
        }

        if (!subLabel.isEmpty()) {
            FontRenderer subFont = Fonts.roboto.get(9);
            String text = subLabel;
            float tw = subFont.getStringWidth(text);
            subFont.drawString(text, x + width - tw, barY + barH + 2, textColor);
        }
    }
}