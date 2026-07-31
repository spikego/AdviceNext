package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.movement.MovementUtils;
import cn.advicenext.utility.minecraft.player.RotationManager;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Objects;

public class Speed extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Speed", "Speed", List.of("WatchdogHop", "WatchdogLowhop", "NCP", "LegitFast"));
    private final BooleanSetting withStrafe = new BooleanSetting("WithStrafe", "let you air/ground strafe", false,()->mode.is("WatchdogHop"));

    // NCP Speed settings
    private final BooleanSetting ncpPullDown = new BooleanSetting("NCP-PullDown", "NCP pull down on air", true,()->mode.is("NCP"));
    private final BooleanSetting ncpBoost = new BooleanSetting("NCP-Boost", "NCP initial boost", true,()->mode.is("NCP"));
    private final BooleanSetting ncpTimerBoost = new BooleanSetting("NCP-Timer", "NCP timer boost", true,()->mode.is("NCP"));
    private final BooleanSetting ncpDamageBoost = new BooleanSetting("NCP-DamageBoost", "NCP damage boost", true,()->mode.is("NCP"));
    private final BooleanSetting ncpLowHop = new BooleanSetting("NCP-LowHop", "NCP low hop", true,()->mode.is("NCP"));
    private final BooleanSetting ncpAirStrafe = new BooleanSetting("NCP-AirStrafe", "NCP air strafe", true,()->mode.is("NCP"));
    private int ncpAirTicks = 0;

    public Speed() {
        super("Speed", "Speed", Category.MOVEMENT);
        this.settings.add(mode);
        this.settings.add(withStrafe);
        this.settings.add(ncpPullDown);
        this.settings.add(ncpBoost);
        this.settings.add(ncpTimerBoost);
        this.settings.add(ncpDamageBoost);
        this.settings.add(ncpLowHop);
        this.settings.add(ncpAirStrafe);
    }

    int airTicks = 0;
    private int vulcanJumpTicks = 0;
    private boolean vulcanJumpActive = false;

    @Override
    public void onEnable() {
        airTicks = 0;
        ncpAirTicks = 0;
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
                Vec3d newVelocity = MovementUtils.withStrafe(
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
            mc.player.setSprinting(true);
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
                    Vec3d strafeVelocity = MovementUtils.withStrafe(
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
            mc.player.setSprinting(true);
        }

        // NCP Speed mode
        if (mode.getValue().equals("NCP")) {
            if (this.settings.contains(ncpPullDown)) {
                this.settings.add(ncpPullDown);
            }
            if (this.settings.contains(ncpBoost)) {
                this.settings.add(ncpBoost);
            }
            if (this.settings.contains(ncpTimerBoost)) {
                this.settings.add(ncpTimerBoost);
            }
            if (this.settings.contains(ncpDamageBoost)) {
                this.settings.add(ncpDamageBoost);
            }

            if (this.settings.contains(ncpLowHop)) {
                this.settings.add(ncpLowHop);
            }
            if (this.settings.contains(ncpAirStrafe)) {
                this.settings.add(ncpAirStrafe);
            }

            if (mc.player == null) return;

            final double SPEED_CONSTANT = 0.199999999;
            final double GROUND_CONSTANT = 0.281;
            final double AIR_CONSTANT = 0.2;
            final double BOOST_CONSTANT = 0.00718;

            int speedMultiplier = mc.player.hasStatusEffect(StatusEffects.SPEED)
                    ? Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() : 0;

            // PullDown logic
            if (ncpPullDown.getValue()) {
                if (mc.player.isOnGround()) {
                    ncpAirTicks = 0;
                } else {
                    ncpAirTicks++;
                    if (ncpAirTicks == 5) {
                        Vec3d currentVel = mc.player.getVelocity();
                        mc.player.setVelocity(MovementUtils.withStrafe(
                                currentVel,
                                MovementUtils.sqrtSpeed(currentVel),
                                1.0,
                                mc.player.input.getMovementInput().y,
                                mc.player.input.getMovementInput().x,
                                mc.player.getYaw()
                        ));
                        mc.player.setVelocity(mc.player.getVelocity().add(0, -0.1523351824467155, 0));
                    }
                }

                if (mc.player.hurtTime >= 5 && mc.player.getVelocity().y >= 0) {
                    mc.player.setVelocity(mc.player.getVelocity().add(0, -0.1, 0));
                }
            }

            // Boost logic
            if (ncpBoost.getValue() && MovementUtils.isMoving()) {
                mc.player.setVelocity(mc.player.getVelocity().multiply(
                        1.0 + BOOST_CONSTANT,
                        1.0,
                        1.0 + BOOST_CONSTANT
                ));
            }

            // Main speed logic
            if (MovementUtils.isMoving()) {
                if (mc.player.isOnGround()) {
                    double groundMin = GROUND_CONSTANT + SPEED_CONSTANT * speedMultiplier;
                    double currentSpeed = MovementUtils.sqrtSpeed(mc.player.getVelocity());
                    double targetSpeed = Math.max(currentSpeed, groundMin);

                    mc.player.setVelocity(MovementUtils.withStrafe(
                            mc.player.getVelocity(),
                            targetSpeed,
                            1.0,
                            mc.player.input.getMovementInput().y,
                            mc.player.input.getMovementInput().x,
                            mc.player.getYaw()
                    ));
                } else if (ncpAirStrafe.getValue()) {
                    double airMin = AIR_CONSTANT + SPEED_CONSTANT * speedMultiplier;
                    double currentSpeed = MovementUtils.sqrtSpeed(mc.player.getVelocity());
                    double targetSpeed = Math.max(currentSpeed, airMin);

                    mc.player.setVelocity(MovementUtils.withStrafe(
                            mc.player.getVelocity(),
                            targetSpeed,
                            0.7,
                            mc.player.input.getMovementInput().y,
                            mc.player.input.getMovementInput().x,
                            mc.player.getYaw()
                    ));
                }
            }

            // Damage boost
            if (mc.player.hurtTime >= 1 && ncpDamageBoost.getValue()) {
                double currentSpeed = MovementUtils.sqrtSpeed(mc.player.getVelocity());
                double targetSpeed = Math.max(currentSpeed, 0.5);

                mc.player.setVelocity(MovementUtils.withStrafe(
                        mc.player.getVelocity(),
                        targetSpeed,
                        1.0,
                        mc.player.input.getMovementInput().y,
                        mc.player.input.getMovementInput().x,
                        mc.player.getYaw()
                ));
            }

            autoJump();
            mc.player.setSprinting(true);
        } else {
            this.settings.remove(ncpPullDown);
            this.settings.remove(ncpBoost);
            this.settings.remove(ncpTimerBoost);
            this.settings.remove(ncpDamageBoost);
            this.settings.remove(ncpLowHop);
            this.settings.remove(ncpAirStrafe);
        }

        if(mode.is("LegitFast")){
            RotationUtils.resetSilentRotation();
            autoJump();
            if(!mc.player.isOnGround()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw+45, mc.player.getPitch()), MovementCorrection.Mode.STRICT);
            }
        }

    }

    @Override
    public void onPacket(PacketEvent event) {

    }

    @Override
    public void onDisable(){
        RotationUtils.resetSilentRotation();
    }

    private void strafeToSpeed(double speed) {
        float yaw = mc.player.getYaw();
        mc.player.setVelocity(
                -Math.sin(Math.toRadians(yaw)) * speed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * speed
        );
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

    @Override
    public String getDisplayValue(){
        return mode.getValue();
    }
}