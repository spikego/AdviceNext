package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.RangeSetting;
import cn.advicenext.utility.minecraft.combat.AttackUtils;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class TriggerBot extends Module {

    public static TriggerBot INSTANCE;

    private final ModeSetting clickMode = new ModeSetting("Click Mode", "Click pattern", "Legit",
            List.of("Legit", "Constant", "Butterfly", "Jitter"));
    private final RangeSetting cps = new RangeSetting("CPS", "Clicks per second range",
            10.0, 14.0, 1.0, 30.0, 0.5);
    private final ModeSetting cooldownMode = new ModeSetting("Cooldown", "Attack cooldown timing", "Post",
            List.of("Pre", "Post"));
    private final BooleanSetting attackMobs = new BooleanSetting("AttackMobs", "Attack hostile mobs", false);
    private final BooleanSetting ignoreTeammates = new BooleanSetting("IgnoreTeammates", "Ignore teammates", true);
    private final BooleanSetting requireMouseDown = new BooleanSetting("RequireMouse", "Require mouse down", false);
    private final BooleanSetting weaponOnly = new BooleanSetting("WeaponOnly", "Only when holding weapon", false);

    private final Random random = new Random();
    private long lastClick = 0;
    private long nextDelay = 0;
    private int butterflyPhase = 0;
    private double legitCenter = 12.0;
    private long lastDriftTime = 0;

    public TriggerBot() {
        super("TriggerBot", "Auto-attacks when crosshair is on an entity", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(clickMode);
        this.settings.add(cps);
        this.settings.add(cooldownMode);
        this.settings.add(attackMobs);
        this.settings.add(ignoreTeammates);
        this.settings.add(requireMouseDown);
        this.settings.add(weaponOnly);
    }

    @Override
    public void onTick(TickEvent event) {
        if (!this.enabled) return;
        if (mc.player == null || mc.world == null || mc.player.isDead()) return;
        if (mc.currentScreen != null) return;

        if (Displace.INSTANCE != null && Displace.INSTANCE.isDisplacing()) return;

        if (requireMouseDown.getValue() && !mc.options.attackKey.isPressed()) return;
        if (weaponOnly.getValue() && !AttackUtils.isHoldingWeapon()) return;

        Entity target = getCrosshairEntity();
        if (target == null) return;

        if (!canAttack((LivingEntity) target)) return;

        long now = System.currentTimeMillis();
        if (now - lastClick < nextDelay) return;

        float cooldown = AttackUtils.getAttackCooldown();
        if (cooldownMode.is("Pre")) {
            if (cooldown < 0.9f) return;
        } else {
            if (cooldown < 1.0f) return;
        }

        if (!isAttackValid((LivingEntity) target)) return;

        RotationUtils.Rotation targetRot = RotationUtils.getRotationToPos(
                target.getEyePos(), mc.player.getEyePos());

        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();
        float prevHeadYaw = mc.player.headYaw;
        float prevBodyYaw = mc.player.bodyYaw;

        mc.player.setYaw(targetRot.yaw);
        mc.player.setPitch(targetRot.pitch);
        mc.player.headYaw = targetRot.yaw;
        mc.player.bodyYaw = targetRot.yaw;

        mc.interactionManager.attackEntity(mc.player, (LivingEntity) target);
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);
        mc.player.headYaw = prevHeadYaw;
        mc.player.bodyYaw = prevBodyYaw;

        lastClick = now;
        nextDelay = computeNextDelay();
    }

    private Entity getCrosshairEntity() {
        if (mc.player == null || mc.world == null) return null;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity && canAttack((LivingEntity) entity)) {
                return entity;
            }
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        double maxDist = mc.player.getEntityInteractionRange() + 0.5;
        Vec3d rayEnd = eyePos.add(lookVec.multiply(maxDist));

        Entity bestEntity = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!canAttack((LivingEntity) entity)) continue;

            Box hitbox = entity.getBoundingBox().expand(0.1);
            if (hitbox.raycast(eyePos, rayEnd).isPresent()) {
                double dist = mc.player.squaredDistanceTo(entity);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestEntity = entity;
                }
            }
        }

        return bestEntity;
    }

    private boolean canAttack(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;
        if (entity instanceof PlayerEntity p) {
            if (ignoreTeammates.getValue() && TargetUtils.isTeammate(p)) return false;
            if (AntiBot.isBotStatic(p)) return false;
        } else if (!attackMobs.getValue()) {
            return false;
        }
        return true;
    }

    private boolean isAttackValid(LivingEntity entity) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        double maxDist = mc.player.getEntityInteractionRange() + 0.5;
        Vec3d rayEnd = eyes.add(lookVec.multiply(maxDist));

        Box hitbox = entity.getBoundingBox().expand(0.1);
        return hitbox.raycast(eyes, rayEnd).isPresent();
    }

    private long computeNextDelay() {
        return switch (clickMode.getValue()) {
            case "Constant" -> constantDelay();
            case "Butterfly" -> butterflyDelay();
            case "Jitter" -> jitterDelay();
            default -> legitDelay();
        };
    }

    private long constantDelay() {
        return (long) (1000.0 / cps.getCenter());
    }

    private long jitterDelay() {
        return (long) (1000.0 / cps.getRandom());
    }

    private long butterflyDelay() {
        long delay;
        if (butterflyPhase < 2) {
            delay = 40 + random.nextInt(50);
        } else {
            delay = (long) (1000.0 / cps.getRandom());
        }
        butterflyPhase = (butterflyPhase + 1) % 3;
        return delay;
    }

    private long legitDelay() {
        long now = System.currentTimeMillis();
        if (now - lastDriftTime > 2000 + random.nextInt(2000)) {
            double span = (cps.getMaxValue() - cps.getMinValue()) * 0.25;
            legitCenter = cps.getCenter() + (random.nextGaussian() * span);
            legitCenter = Math.max(cps.getMinValue(), Math.min(cps.getMaxValue(), legitCenter));
            lastDriftTime = now;
        }
        double stddev = Math.max(0.5, (cps.getMaxValue() - cps.getMinValue()) / 4.0);
        double sample = legitCenter + random.nextGaussian() * stddev;
        sample = Math.max(cps.getMinValue(), Math.min(cps.getMaxValue(), sample));
        long delay = (long) (1000.0 / sample);
        return delay + random.nextInt(15) - 7;
    }

    @Override
    public void onEnable() {
        lastClick = 0;
        nextDelay = 0;
        butterflyPhase = 0;
        legitCenter = cps.getCenter();
        lastDriftTime = 0;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getDisplayValue() {
        return clickMode.getValue();
    }
}