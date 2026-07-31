package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.utility.minecraft.movement.MovementUtils;

public class Strafe extends Module {

    private final BooleanSetting ground = new BooleanSetting("Ground", "Ground strafe", true);
    private final BooleanSetting air = new BooleanSetting("Air", "Air strafe", true);

    public Strafe() {
        super("Strafe", "Auto strafe", Category.MOVEMENT);
        this.enabled = false;
        this.settings.add(ground);
        this.settings.add(air);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!MovementUtils.isMoving()) return;

        double speed = MovementUtils.sqrtSpeed(mc.player.getVelocity());

        if (mc.player.isOnGround() && ground.getValue()) {
            MovementUtils.strafe(speed);
        }
        if (!mc.player.isOnGround() && air.getValue()) {
            MovementUtils.strafe(speed);
        }
    }
}