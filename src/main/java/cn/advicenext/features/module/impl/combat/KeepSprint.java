package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.slider.DoubleSetting;

public class KeepSprint extends Module{
    public static KeepSprint INSTANCE;
    private static final DoubleSetting motion = new DoubleSetting("Motion", "The motion to keep sprinting", 1.0, 10.0, 0.0, 0.1);
    public KeepSprint() {
        super("KeepSprint", "KeepSprint", Category.COMBAT);
        this.settings.add(motion);
        INSTANCE = this;
    }

    @Override
    public void onTick(TickEvent event) {
        if(mc.options.forwardKey.isPressed()) {
            mc.player.setSprinting(true);
        }
    }

    public static double getMotion() {
        return KeepSprint.motion.getValue();
    }
}
