package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.BooleanSetting;
import java.util.List;

public class Fly extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Select the fly mode", "Vanilla", List.of("Vanilla","Verus"));
    private final DoubleSetting speed = new DoubleSetting("Speed", "Vanilla fly speed",1.0, 5.0, 0.0, 0.1);
    private final BooleanSetting vanillaBypass = new BooleanSetting("Vanilla Bypass", "Bypass vanilla fly detection", false);
    private int tickCounter = 0;

    public Fly() {
        super("Fly", "Fly", Category.MOVEMENT);
        this.settings.add(mode);
        if ("Vanilla".equals(mode.getValue())) {
            this.settings.add(speed);
            this.settings.add(vanillaBypass);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if ("Vanilla".equals(mode.getValue())) {
            if (!this.settings.contains(speed)) {
                this.settings.add(speed);
            }
            if (!this.settings.contains(vanillaBypass)) {
                this.settings.add(vanillaBypass);
            }
        } else {
            this.settings.remove(speed);
            this.settings.remove(vanillaBypass);
        }

        if("Vanilla".equals(mode.getValue())){
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
            
            // Vanilla Bypass
            if (vanillaBypass.getValue() && tickCounter % 40 == 0) {
                mc.player.setVelocity(mc.player.getVelocity().x, -0.04, mc.player.getVelocity().z);
            }
            tickCounter++;
        }
    }
}