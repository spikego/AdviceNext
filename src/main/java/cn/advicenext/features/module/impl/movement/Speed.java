package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.utility.minecraft.movement.MoveUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Objects;

import static cn.advicenext.utility.Utility.player;

public class Speed extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Speed", "Speed", List.of("WatchdogHop", "WatchdogLowhop"));
    private final BooleanSetting withStrafe = new BooleanSetting("WithStrafe", "let you air/ground strafe", false);

    public Speed() {
        super("Speed", "Speed", Category.MOVEMENT);
        this.settings.add(mode);
        this.settings.add(withStrafe);
        this.enabled = false;
    }

    int airTicks = 0;

    @Override
    public void onEnable() {
        airTicks = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mode.getValue().equals("WatchdogHop")) {
            final double BASE_HORIZONTAL_MODIFIER = 0.0004;
            final double HORIZONTAL_SPEED_AMPLIFIER = 0.0007;
            final double VERTICAL_SPEED_AMPLIFIER = 0.0004;
            final double AT_LEAST = 0.281;
            final double BASH = 0.2857671997172534;
            final double SPEED_EFFECT_CONST = 0.008003278196411223;
            if (mc.player.isOnGround()) {
                Vec3d newVelocity = MoveUtils.withStrafe(
                        mc.player.getVelocity(),
                        0.2875,
                        1.0,
                        mc.player.input.getMovementInput().y,
                        mc.player.input.getMovementInput().x,
                        mc.player.getYaw()
                );
                mc.player.setVelocity(newVelocity);
            } else {
                double horizontalMod = BASE_HORIZONTAL_MODIFIER + HORIZONTAL_SPEED_AMPLIFIER *
                        (mc.player.hasStatusEffect(StatusEffects.SPEED)
                                ? Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier()
                                : 0);

                double yMod = (mc.player.getVelocity().y < 0 && mc.player.fallDistance < 1)
                        ? VERTICAL_SPEED_AMPLIFIER
                        : 0.0;

                mc.player.setVelocity(
                        mc.player.getVelocity().multiply(
                                1.0 + horizontalMod,
                                1.0 + yMod,
                                1.0 + horizontalMod
                        )
                );
            }
            autoJump();
        }

        // 在 Speed.java 的 onTick 方法 WatchdogLowhop 分支内添加
        if (mode.getValue().equals("WatchdogLowhop")) {
            if (mc.player == null) return;
            if (mc.player.isOnGround()) {
                airTicks = 0;
            } else {
                airTicks++;
            }

            switch (airTicks) {
                case 1 -> {
                    // 先设置Y速度
                    mc.player.setVelocity(new Vec3d(mc.player.getVelocity().x, 0.39, mc.player.getVelocity().z));
                    // 再应用withStrafe
                    Vec3d strafeVelocity = MoveUtils.withStrafe(
                            mc.player.getVelocity(),
                            0.2875,
                            1.0,
                            mc.player.input.getMovementInput().y,
                            mc.player.input.getMovementInput().x,
                            mc.player.getYaw()
                    );
                    mc.player.setVelocity(strafeVelocity);
                }
                case 3 ->
                        mc.player.setVelocity(new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.13, mc.player.getVelocity().z));
                case 4 ->
                        mc.player.setVelocity(new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.2, mc.player.getVelocity().z));
            }

            if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
                mc.player.addVelocity(0.0, -0.1, 0.0);
                airTicks = 0;
            }

            int speedAmp = mc.player.hasStatusEffect(StatusEffects.SPEED)
                    ? Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() : 0;
            if (speedAmp == 2) {
                if (airTicks == 1 || airTicks == 2 || airTicks == 5 || airTicks == 6 || airTicks == 8) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.multiply(1.2, 1.0, 1.2));
                }
            }

            if (mc.player.isOnGround()) {
                airTicks = 0;
            }

            autoJump();
        }
    }

    public void autoJump() {
        if (!mc.player.isOnGround() || mc.player.isSneaking() || mc.options.jumpKey.isPressed()) {
            return;
        }
        if (mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.forwardKey.isPressed()) {
            mc.player.jump();
        }
    }
}