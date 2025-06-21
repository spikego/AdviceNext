package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import java.util.List;

public class Fly extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Select the fly mode", "Vanilla", List.of("Vanilla","Verus"));
    private final DoubleSetting speed = new DoubleSetting("Speed", "Vanilla fly speed",1.0, 5.0, 0.0, 0.1);

    public Fly() {
        super("Fly", "Fly", Category.MOVEMENT);
        this.settings.add(mode);
        if ("Vanilla".equals(mode.getValue())) {
            this.settings.add(speed);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if ("Vanilla".equals(mode.getValue())) {
            if (!this.settings.contains(speed)) {
                this.settings.add(speed);
            }
        } else {
            this.settings.remove(speed);
        }

        if(mode.getValue() == "Vanilla"){
            // 在 onTick(TickEvent event) 方法中
            mc.player.setVelocity(0.0, 0.0, 0.0);
            mc.player.fallDistance = 0.0;

// 垂直移动
            double verticalMovement = 0.0;
            if (mc.options.jumpKey.isPressed()) {
                verticalMovement = speed.getValue();
            } else if (mc.options.sneakKey.isPressed()) {
                verticalMovement = -speed.getValue();
            }

// 水平移动
            double forward = 0.0;
            double strafe = 0.0;
            double s = speed.getValue();

            if (mc.options.forwardKey.isPressed()) forward += s;
            if (mc.options.backKey.isPressed()) forward -= s;
            if (mc.options.leftKey.isPressed()) strafe += s;
            if (mc.options.rightKey.isPressed()) strafe -= s;

            double yaw = Math.toRadians(mc.player.getYaw());

            double horizontalX = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            double horizontalZ = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;

// 应用速度
            mc.player.setVelocity(horizontalX, verticalMovement, horizontalZ);
        }
    }
}