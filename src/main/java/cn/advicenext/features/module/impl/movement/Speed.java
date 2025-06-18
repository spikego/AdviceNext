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

        if (mode.getValue().equals("WatchdogLowhop")) {
            final double BASE_HORIZONTAL_MODIFIER = 0.0004;
            final double HORIZONTAL_SPEED_AMPLIFIER = 0.0007;
            final double VERTICAL_SPEED_AMPLIFIER = 0.0004;
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