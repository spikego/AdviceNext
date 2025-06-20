package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import net.minecraft.client.MinecraftClient;

public class MotionCamera extends Module {
    public static MotionCamera INSTANCE;

    public final MinecraftClient mc = MinecraftClient.getInstance();

    public static final DoubleSetting offset = new DoubleSetting("Offset", "Additional camera distance", 3.5, 6.0, 0.0, 0.5);
    public static final DoubleSetting maxOffset = new DoubleSetting("MaxOffset", "Maximum additional distance", 5.0, 8.0, 1.0, 0.5);
    public static final BooleanSetting smooth = new BooleanSetting("Smooth", "Smooth camera movement", true);
    public static final BooleanSetting onlyThirdPerson = new BooleanSetting("OnlyThirdPerson", "Only work in third person", true);

    public MotionCamera() {
        super("MotionCamera", "Smooth camera movement in third person view.", Category.RENDER);
        this.settings.add(offset);
        this.settings.add(maxOffset);
        this.settings.add(smooth);
        this.settings.add(onlyThirdPerson);
        INSTANCE = this;
        this.enabled = false;
    }

    public static boolean isEnabled() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    public static double getOffset() {
        return offset.getValue();
    }

    public static double getMaxOffset() {
        return maxOffset.getValue();
    }

    public static boolean isSmooth() {
        return smooth.getValue();
    }

    public static boolean isOnlyThirdPerson() {
        return onlyThirdPerson.getValue();
    }
}