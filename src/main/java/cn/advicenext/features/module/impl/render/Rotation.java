package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.utility.minecraft.client.RotateUtils;

public class Rotation extends Module {

    public static Rotation INSTANCE;
    
    public Rotation() {
        super("Rotation", "Show head/body rotation in third person", Category.RENDER);
        INSTANCE = this;
        this.enabled = false;
    }

    public static boolean isEnabled() {
        return INSTANCE.enabled;
    }
    public static float getRenderYaw() {
        RotateUtils.Rotation serverRotation = RotateUtils.getServerRotation();
        return serverRotation != null ? serverRotation.yaw : 0;
    }
    
    public static float getRenderPitch() {
        RotateUtils.Rotation serverRotation = RotateUtils.getServerRotation();
        return serverRotation != null ? serverRotation.pitch : 0;
    }
    
    public static boolean shouldUseServerRotation() {
        return RotateUtils.getServerRotation() != null;
    }
}