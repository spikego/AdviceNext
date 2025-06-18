package cn.spikego.advicenext.gui.clickgui.animation;

import java.awt.*;

public class AnimationUtil {
    
    /**
     * Smoothly animates a value towards a target
     * @param current Current value
     * @param target Target value
     * @param speed Animation speed (0-1), higher is faster
     * @return The new value
     */
    public static float animate(float current, float target, float speed) {
        float difference = target - current;
        if (Math.abs(difference) < 0.01f) return target;
        return current + difference * Math.min(1.0f, speed);
    }
    
    /**
     * Blends two colors based on a ratio
     * @param color1 First color
     * @param color2 Second color
     * @param ratio Blend ratio (0-1), 0 = color1, 1 = color2
     * @return Blended color
     */
    public static Color blendColors(Color color1, Color color2, float ratio) {
        float inverseRatio = 1.0f - ratio;
        float r = color1.getRed() * inverseRatio + color2.getRed() * ratio;
        float g = color1.getGreen() * inverseRatio + color2.getGreen() * ratio;
        float b = color1.getBlue() * inverseRatio + color2.getBlue() * ratio;
        float a = color1.getAlpha() * inverseRatio + color2.getAlpha() * ratio;
        return new Color((int)r, (int)g, (int)b, (int)a);
    }
    
    /**
     * Eases a value using a sine function for smooth animation
     * @param value Value between 0 and 1
     * @return Eased value between 0 and 1
     */
    public static float easeInOutSine(float value) {
        return (float)-(Math.cos(Math.PI * value) - 1) / 2;
    }
}