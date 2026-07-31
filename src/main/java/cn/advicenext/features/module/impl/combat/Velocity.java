package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Velocity extends Module {

    public static Velocity INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "Velocity reduction mode", "Normal",
            List.of("Normal", "Lag", "JumpReset", "SkipTick", "Boost"));

    private final IntSetting chance = new IntSetting("Chance", "Chance to reduce velocity", 100, 100, 0, 1);

    private final IntSetting horizontalKb = new IntSetting("Horizontal", "Horizontal knockback %", 0, 100, 0, 1,
            () -> mode.is("Normal"));
    private final IntSetting verticalKb = new IntSetting("Vertical", "Vertical knockback %", 0, 100, 0, 1,
            () -> mode.is("Normal"));

    private final IntSetting lagDelay = new IntSetting("Lag Delay", "Delay before applying velocity (ms)", 150, 1000, 0, 10,
            () -> mode.is("Lag"));
    private final BooleanSetting lagWithJump = new BooleanSetting("WithJump", "Jump when delayed velocity applied", false,
            () -> mode.is("Lag"));

    private final IntSetting minDelay = new IntSetting("Min Delay", "Min delay before jump (ms)", 0, 500, 0, 10,
            () -> mode.is("JumpReset"));
    private final IntSetting maxDelay = new IntSetting("Max Delay", "Max delay before jump (ms)", 100, 500, 0, 10,
            () -> mode.is("JumpReset"));
    private final BooleanSetting requireMouseDown = new BooleanSetting("RequireMouse", "Require mouse down", false,
            () -> mode.is("JumpReset"));
    private final BooleanSetting requireMovingForward = new BooleanSetting("RequireForward", "Require moving forward", true,
            () -> mode.is("JumpReset"));
    private final BooleanSetting requireAim = new BooleanSetting("RequireAim", "Require aiming at player", true,
            () -> mode.is("JumpReset"));
    private final BooleanSetting ignoreFall = new BooleanSetting("IgnoreFall", "Ignore after fall damage", true,
            () -> mode.is("JumpReset"));
    private final BooleanSetting checkFov = new BooleanSetting("CheckFOV", "Check FOV when jumping", true,
            () -> mode.is("JumpReset"));

    private final IntSetting skipTicks = new IntSetting("Skip Ticks", "Ticks to skip velocity", 1, 20, 1, 1,
            () -> mode.is("SkipTick"));
    private final IntSetting skipChance = new IntSetting("Skip Chance", "Chance to skip", 100, 100, 0, 1,
            () -> mode.is("SkipTick"));

    private final DoubleSetting boostStrength = new DoubleSetting("Boost Strength", "Boost power", 0.5, 1.0, 0.1, 0.01,
            () -> mode.is("Boost"));

    private boolean shouldJump = false;
    private long jumpTime = 0;
    private int skipTickCounter = 0;
    private boolean veloPacket = false;
    private int boostTick = 0;
    private final Random random = new Random();

    private Vec3d delayedVelocity = null;
    private long delayedVelocityTime = 0;
    private boolean delayedVelocityJump = false;

    private int lastHurtTime = 0;
    private boolean ignoreNext = false;
    private double lastFallDistance = 0;
    private boolean setJump = false;

    public Velocity() {
        super("Velocity", "Reduces or cancels knockback", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(mode);
        this.settings.add(chance);
        this.settings.add(horizontalKb);
        this.settings.add(verticalKb);
        this.settings.add(lagDelay);
        this.settings.add(lagWithJump);
        this.settings.add(minDelay);
        this.settings.add(maxDelay);
        this.settings.add(requireMouseDown);
        this.settings.add(requireMovingForward);
        this.settings.add(requireAim);
        this.settings.add(ignoreFall);
        this.settings.add(checkFov);
        this.settings.add(skipTicks);
        this.settings.add(skipChance);
        this.settings.add(boostStrength);
    }

    @Override
    public void onEnable() {
        shouldJump = false;
        jumpTime = 0;
        skipTickCounter = 0;
        veloPacket = false;
        boostTick = 0;
        delayedVelocity = null;
        delayedVelocityTime = 0;
        delayedVelocityJump = false;
        lastHurtTime = 0;
        ignoreNext = false;
        lastFallDistance = 0;
        setJump = false;
    }

    @Override
    public void onDisable() {
        shouldJump = false;
        jumpTime = 0;
        skipTickCounter = 0;
        veloPacket = false;
        boostTick = 0;
        delayedVelocity = null;
        delayedVelocityTime = 0;
        delayedVelocityJump = false;
        lastHurtTime = 0;
        ignoreNext = false;
        lastFallDistance = 0;
        setJump = false;
        mc.options.jumpKey.setPressed(false);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getOrigin() != PacketEvent.TransferOrigin.RECEIVE) return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId()) return;
            handleVelocity(event, packet);
        } else if (event.getPacket() instanceof ExplosionS2CPacket packet) {
            handleExplosion(event, packet);
        }
    }

    private void handleVelocity(PacketEvent event, EntityVelocityUpdateS2CPacket packet) {
        String currentMode = mode.getValue();

        if (random.nextInt(100) >= chance.getValue()) return;

        if (currentMode.equals("Normal")) {
            event.cancelled = true;
            if (horizontalKb.getValue() == 0 && verticalKb.getValue() == 0) return;

            double hMult = horizontalKb.getValue() / 100.0;
            double vMult = verticalKb.getValue() / 100.0;

            mc.player.setVelocity(
                    mc.player.getVelocity().x + packet.getVelocity().x * hMult,
                    mc.player.getVelocity().y + packet.getVelocity().y * vMult,
                    mc.player.getVelocity().z + packet.getVelocity().z * hMult
            );
        } else if (currentMode.equals("Lag")) {
            event.cancelled = true;
            Vec3d vel = packet.getVelocity();
            delayedVelocity = new Vec3d(
                    mc.player.getVelocity().x + vel.x,
                    mc.player.getVelocity().y + vel.y,
                    mc.player.getVelocity().z + vel.z
            );
            delayedVelocityTime = System.currentTimeMillis() + lagDelay.getValue();
            delayedVelocityJump = lagWithJump.getValue();
        } else if (currentMode.equals("JumpReset")) {
            if (!shouldJump) {
                shouldJump = true;
                int delay = minDelay.getValue();
                if (maxDelay.getValue() > minDelay.getValue()) {
                    delay += random.nextInt(maxDelay.getValue() - minDelay.getValue());
                }
                jumpTime = System.currentTimeMillis() + delay;
            }
        } else if (currentMode.equals("SkipTick")) {
            event.cancelled = true;
            if (random.nextInt(100) < skipChance.getValue()) {
                skipTickCounter = skipTicks.getValue();
            }
        } else if (currentMode.equals("Boost")) {
            if (mc.player.isOnGround()) {
                event.cancelled = true;
                mc.player.setVelocity(
                        mc.player.getVelocity().x * boostStrength.getValue(),
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z * boostStrength.getValue()
                );
            } else {
                veloPacket = true;
            }
        }
    }

    private void handleExplosion(PacketEvent event, ExplosionS2CPacket packet) {
        String currentMode = mode.getValue();

        if (random.nextInt(100) >= chance.getValue()) return;

        if (currentMode.equals("Normal")) {
            if (horizontalKb.getValue() == 0 && verticalKb.getValue() == 0) {
                event.cancelled = true;
            } else {
                Optional<Vec3d> knockbackOpt = packet.playerKnockback();
                if (knockbackOpt.isPresent()) {
                    Vec3d knockback = knockbackOpt.get();
                    event.cancelled = true;
                    double hMult = horizontalKb.getValue() / 100.0;
                    double vMult = verticalKb.getValue() / 100.0;
                    mc.player.setVelocity(
                            mc.player.getVelocity().x + knockback.x * hMult,
                            mc.player.getVelocity().y + knockback.y * vMult,
                            mc.player.getVelocity().z + knockback.z * hMult
                    );
                }
            }
        } else if (currentMode.equals("Lag") || currentMode.equals("SkipTick")) {
            event.cancelled = true;
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Lag") && delayedVelocity != null && System.currentTimeMillis() >= delayedVelocityTime) {
            mc.player.setVelocity(delayedVelocity.x, delayedVelocity.y, delayedVelocity.z);
            if (delayedVelocityJump && mc.player.isOnGround()) {
                mc.player.jump();
            }
            delayedVelocity = null;
            delayedVelocityTime = 0;
            delayedVelocityJump = false;
        }

        if (mode.is("SkipTick") && skipTickCounter > 0) {
            skipTickCounter--;
        }

        if (mode.is("JumpReset")) {
            handleJumpReset();
        }
    }

    private void handleJumpReset() {
        int hurtTime = mc.player.hurtTime;
        boolean onGround = mc.player.isOnGround();

        if (onGround && lastFallDistance > 3.0 && !mc.player.getAbilities().allowFlying && ignoreFall.getValue()) {
            ignoreNext = true;
        }

        if (hurtTime > lastHurtTime && hurtTime > 0 && onGround) {
            boolean mouseDown = mc.options.attackKey.isPressed() || !requireMouseDown.getValue();
            boolean aimingAt = !requireAim.getValue() || checkAim();
            boolean forward = mc.options.forwardKey.isPressed() || !requireMovingForward.getValue();
            boolean randomization = chance.getValue() >= 100 || random.nextInt(100) < chance.getValue();
            boolean fovOk = !checkFov.getValue() || checkFovTarget();

            boolean canJump = !ignoreNext && !mc.player.isOnFire() && onGround
                    && aimingAt && forward && mouseDown && randomization
                    && !hasBadEffects() && fovOk;

            if (canJump) {
                mc.options.jumpKey.setPressed(true);
                setJump = true;
                shouldJump = false;
            }

            ignoreNext = false;
        }

        if (setJump && !mc.options.jumpKey.isPressed()) {
            mc.options.jumpKey.setPressed(false);
            setJump = false;
        }

        if (hurtTime == 0) {
            setJump = false;
            mc.options.jumpKey.setPressed(false);
        }

        lastHurtTime = hurtTime;
        lastFallDistance = mc.player.fallDistance;
    }

    private boolean hasBadEffects() {
        return mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)
                || mc.player.hasStatusEffect(StatusEffects.POISON)
                || mc.player.hasStatusEffect(StatusEffects.WITHER);
    }

    private boolean checkAim() {
        if (mc.crosshairTarget == null) return false;
        return mc.crosshairTarget.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) mc.crosshairTarget).getEntity() instanceof PlayerEntity;
    }

    private boolean checkFovTarget() {
        if (mc.crosshairTarget == null) return false;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return false;
        EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
        if (!(entityHit.getEntity() instanceof PlayerEntity target)) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
        Vec3d dir = targetPos.subtract(eyePos).normalize();
        Vec3d look = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();
        double dot = look.dotProduct(dir);
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
        return angle <= 165.0;
    }
}