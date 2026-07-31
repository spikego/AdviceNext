package cn.advicenext.gui.hud.widgets;

import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WatermarkWidget extends Widget {

    private String text = "AdviceNext";

    public WatermarkWidget() {
        super("watermark", 10, 10, 120, 18);
    }

    public void setText(String text) {
        this.text = text;
        this.width = Fonts.interSemiBold.get(16).getStringWidth(text) + 4;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        HUD hud = HUD.getHudInstance();
        String mode = hud != null ? hud.WatermarkMode.getValue() : "Text";

        if (mode.equals("Bar")) {
            renderBarMode(context);
        } else {
            renderTextMode(context);
        }
    }

    private void renderTextMode(DrawContext context) {
        Fonts.interSemiBold.get(16).drawString(text, x, y, Colors.currentColor().getRGB());
    }

    private void renderBarMode(DrawContext context) {
        // TODO: Bar mode implementation pending user requirements
        renderTextMode(context);
    }
}