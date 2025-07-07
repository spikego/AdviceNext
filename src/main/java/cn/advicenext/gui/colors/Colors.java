package cn.advicenext.gui.colors;

import cn.advicenext.features.module.impl.client.ClientTheme;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public class Colors {
    private static Color color1 = new Color(193, 0,0 , 255); // 粉色
    private static Color color2 = new Color(255, 255, 255, 255); // 白色

    private static Color blendColors(Color color1, Color color2, float progress) {
        int r = (int) (color1.getRed() * (1 - progress) + color2.getRed() * progress);
        int g = (int) (color1.getGreen() * (1 - progress) + color2.getGreen() * progress);
        int b = (int) (color1.getBlue() * (1 - progress) + color2.getBlue() * progress);
        int a = (int) (color1.getAlpha() * (1 - progress) + color2.getAlpha() * progress);
        return new Color(r, g, b, a);
    }

    public static Color currentColor() {
        float progress = MathHelper.clamp((System.currentTimeMillis() % 4000) / 2000f, 0f, 1f);
        return blendColors(color1, color2, progress <= 0.5f ? progress * 2 : (1 - progress) * 2);
    }

    public static void setColors(Color c1, Color c2) {
        color1 = c1;
        color2 = c2;
    }

    public static Color gradientColor(int index, int total) {
        if (total <= 1) return currentColor();
        
        float baseProgress = MathHelper.clamp((System.currentTimeMillis() % 4000) / 2000f, 0f, 1f);
        float indexOffset = (float) index / (total - 1) * 0.5f; // 50%的偏移范围
        float progress = (baseProgress + indexOffset) % 1.0f;
        
        float blendFactor = progress <= 0.5f ? progress * 2 : (1 - progress) * 2;
        return blendColors(color1, color2, blendFactor);
    }
    
    public static void showCurrentColors() {

    }
}