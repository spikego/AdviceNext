package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.AttackUtils;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.network.LagUtils;
import cn.advicenext.utility.minecraft.network.lag.LagDirection;
import cn.advicenext.utility.minecraft.network.lag.LagRequest;
import cn.advicenext.utility.minecraft.network.lag.TimedTimeout;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class KillAura extends Module {

    public static KillAura INSTANCE;

    private final DoubleSetting attackRange = new DoubleSetting("AttackRange", "Attack range", 3.0, 6.0, 1.0, 0.05);
    private final DoubleSetting swingRange = new DoubleSetting("SwingRange", "Swing animation range", 4.5, 8.0, 1.0, 0.05);
    private final DoubleSetting aimRange = new DoubleSetting("AimRange", "Aim range", 4.5, 8.0, 1.0, 0.05);
    private final DoubleSetting wallRange = new DoubleSetting("WallRange", "Range through walls", 3.0, 6.0, 0.0, 0.1);
    private final DoubleSetting fov = new DoubleSetting("FOV", "Field of view", 360.0, 360.0, 30.0, 4.0);
    private final DoubleSetting targetCPS = new DoubleSetting("TargetCPS", "Attacks per second", 10.0, 20.0, 1.0, 0.5);
    private final IntSetting rotationSpeed = new IntSetting("RotationSpeed", "Aim speed", 60, 100, 10, 1);
    private final IntSetting switchDelay = new IntSetting("SwitchDelay", "Switch delay (ms)", 200, 1000, 50, 25);
    private final IntSetting maxTargets = new IntSetting("MaxTargets", "Max concurrent targets", 3, 10, 1, 1);

    private final ModeSetting attackMethod = new ModeSetting("AttackMethod", "How to attack", "Post",
            List.of("Legit", "Post"));
    private final ModeSetting cooldownMode = new ModeSetting("Cooldown", "Attack cooldown timing", "Post",
            List.of("Pre", "Post"));
    private final ModeSetting rotationMode = new ModeSetting("RotationMode", "How to rotate", "Silent",
            List.of("Silent", "LockView", "None"));
    private final ModeSetting sortMode = new ModeSetting("SortMode", "Target sort order", "Distance",
            List.of("Distance", "Health", "HurtTime", "Yaw"));
    private final ModeSetting targetMode = new ModeSetting("TargetMode", "Target selection", "Single",
            List.of("Single", "Switch", "Multi"));
    private final ModeSetting moveFix = new ModeSetting("MoveFix", "Movement correction", "Silent",
            List.of("Off", "Silent", "Strict"));

    private final BooleanSetting attackMobs = new BooleanSetting("AttackMobs", "Attack hostile mobs", false);
    private final BooleanSetting targetInvis = new BooleanSetting("TargetInvis", "Target invisible entities", true);
    private final BooleanSetting disableInInventory = new BooleanSetting("DisableInInv", "Disable in inventory", true);
    private final BooleanSetting aimThroughBlocks = new BooleanSetting("ThroughWalls", "Hit through walls", false);
    private final BooleanSetting aimThroughEntities = new BooleanSetting("ThroughEntities", "Hit through entities", false);
    private final BooleanSetting ignoreTeammates = new BooleanSetting("IgnoreTeammates", "Ignore teammates", true);
    private final BooleanSetting notUsingItem = new BooleanSetting("NotUsingItem", "Only when not using item", false);
    private final BooleanSetting requireMouseDown = new BooleanSetting("RequireMouse", "Require mouse down", false);
    private final BooleanSetting weaponOnly = new BooleanSetting("WeaponOnly", "Only when holding weapon", false);
    private final BooleanSetting showRange = new BooleanSetting("RangeCircle", "Show attack range circle", true);
    private final BooleanSetting targetEsp = new BooleanSetting("TargetESP", "Highlight current target", true);

    private LivingEntity target;
    private RotationUtils.Rotation lastRotation;
    private long lastAttackTime = 0;
    private long nextAttackDelay = 0;
    private double targetDistance = Double.MAX_VALUE;
    private final Random random = new Random();

    private double legitCpsCenter = 12.0;
    private long legitDriftTime = 0;

    private final Map<Integer, Integer> hitMap = new HashMap<>();
    private final List<LivingEntity> multiTargets = new ArrayList<>();
    private final Set<LivingEntity> attackedThisTick = new HashSet<>();

    public KillAura() {
        super("KillAura", "Automatically attacks nearby targets", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(attackRange);
        this.settings.add(swingRange);
        this.settings.add(aimRange);
        this.settings.add(wallRange);
        this.settings.add(fov);
        this.settings.add(targetCPS);
        this.settings.add(rotationSpeed);
        this.settings.add(switchDelay);
        this.settings.add(maxTargets);
        this.settings.add(attackMethod);
        this.settings.add(cooldownMode);
        this.settings.add(rotationMode);
        this.settings.add(sortMode);
        this.settings.add(targetMode);
        this.settings.add(moveFix);
        this.settings.add(attackMobs);
        this.settings.add(targetInvis);
        this.settings.add(disableInInventory);
        this.settings.add(aimThroughBlocks);
        this.settings.add(aimThroughEntities);
        this.settings.add(ignoreTeammates);
        this.settings.add(notUsingItem);
        this.settings.add(requireMouseDown);
        this.settings.add(weaponOnly);
        this.settings.add(showRange);
        this.settings.add(targetEsp);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!basicCondition() || !settingCondition()) {
            setTarget(null);
            return;
        }

        if (Displace.INSTANCE != null && Displace.INSTANCE.isDisplacing()) {
            setTarget(null);
            return;
        }

        attackedThisTick.clear();

        handleTarget();
        if (target == null) {
            smoothResume();
            return;
        }

        targetDistance = mc.player.distanceTo(target);
        applyRotation();

        if (targetMode.is("Multi")) {
            handleMultiAttacks();
        } else if (targetDistance <= swingRange.getValue()) {
            handleAttack();
        }
    }

    private boolean basicCondition() {
        if (mc.player == null || mc.world == null || mc.player.isDead()) return false;
        if (disableInInventory.getValue() && mc.currentScreen != null) return false;
        return true;
    }

    private boolean settingCondition() {
        if (requireMouseDown.getValue() && !mc.options.attackKey.isPressed()) return false;
        if (notUsingItem.getValue() && mc.player.isUsingItem()) return false;
        if (weaponOnly.getValue() && !AttackUtils.isHoldingWeapon()) return false;
        return true;
    }

    private void handleTarget() {
        double maxRange = Math.max(attackRange.getValue(), Math.max(aimRange.getValue(), wallRange.getValue()));
        float fovVal = fov.getValue().floatValue();

        List<TargetCandidate> candidates = new ArrayList<>();
        for (LivingEntity entity : TargetUtils.getTargets(maxRange, TargetUtils.TargetFilter.ALL,
                e -> canTarget(e, fovVal))) {
            double dist = mc.player.distanceTo(entity);
            if (dist > maxRange) continue;

            boolean isEnemyPlayer = entity instanceof PlayerEntity
                    && !TargetUtils.isTeammate((PlayerEntity) entity);
            candidates.add(new TargetCandidate(entity, dist, entity.getHealth(),
                    entity.hurtTime, TargetUtils.getAngleToEntity(entity), isEnemyPlayer));
        }

        candidates.sort(getComparator());

        if (targetMode.is("Multi")) {
            handleMultiTarget(candidates);
            return;
        }

        if (targetMode.is("Switch")) {
            handleSwitchTarget(candidates);
            return;
        }

        if (target != null && target.isAlive() && inRange(target, maxRange)
                && candidates.stream().anyMatch(c -> c.entity == target)) {
            return;
        }

        if (!candidates.isEmpty()) {
            setTarget(candidates.get(0).entity);
        } else {
            setTarget(null);
        }
    }

    private boolean canTarget(LivingEntity entity, float fovVal) {
        if (entity == mc.player || !entity.isAlive()) return false;

        if (entity instanceof PlayerEntity p) {
            if (ignoreTeammates.getValue() && TargetUtils.isTeammate(p)) return false;
        } else if (!attackMobs.getValue()) {
            return false;
        }

        if (entity.isInvisible() && !targetInvis.getValue()) return false;
        if (fovVal != 360.0f && !TargetUtils.isInFOV(entity, fovVal)) return false;

        return true;
    }

    private boolean inRange(LivingEntity entity, double maxRange) {
        Box expanded = entity.getBoundingBox().expand(maxRange);
        Vec3d eyePos = mc.player.getEyePos();
        return expanded.contains(eyePos);
    }

    private void handleSwitchTarget(List<TargetCandidate> candidates) {
        if (candidates.isEmpty()) {
            setTarget(null);
            return;
        }

        int ticksExisted = mc.player.age;
        int switchDelayTicks = (int) (switchDelay.getValue() / 50);
        long noHitTicks = (long) Math.min(candidates.size(), maxTargets.getValue()) * switchDelayTicks;

        for (TargetCandidate candidate : candidates) {
            Integer firstHitTick = hitMap.get(candidate.entity.getId());
            if (firstHitTick != null && ticksExisted - firstHitTick < switchDelayTicks) {
                setTarget(candidate.entity);
                return;
            }
        }

        for (TargetCandidate candidate : candidates) {
            Integer firstHitTick = hitMap.get(candidate.entity.getId());
            if (firstHitTick == null || ticksExisted >= firstHitTick + noHitTicks) {
                hitMap.put(candidate.entity.getId(), ticksExisted);
                setTarget(candidate.entity);
                return;
            }
        }

        setTarget(candidates.get(0).entity);
    }

    private void handleMultiTarget(List<TargetCandidate> candidates) {
        multiTargets.clear();
        int max = Math.min(candidates.size(), maxTargets.getValue());
        for (int i = 0; i < max; i++) {
            multiTargets.add(candidates.get(i).entity);
        }
        if (!multiTargets.isEmpty()) {
            setTarget(multiTargets.get(0));
        } else {
            setTarget(null);
        }
    }

    private void handleMultiAttacks() {
        if (!canAttackNow()) return;
        for (LivingEntity e : multiTargets) {
            if (e == null || !e.isAlive()) continue;
            if (attackedThisTick.contains(e)) continue;
            if (!isAttackValid(e)) continue;
            attackEntity(e);
            attackedThisTick.add(e);
        }
    }

    private void attackEntity(LivingEntity e) {
        if (attackMethod.is("Legit")) {
            mc.options.attackKey.setPressed(true);
            mc.options.attackKey.setPressed(false);
        } else {
            float prevYaw = mc.player.getYaw();
            float prevPitch = mc.player.getPitch();
            float prevHeadYaw = mc.player.headYaw;
            float prevBodyYaw = mc.player.bodyYaw;

            if (lastRotation != null) {
                mc.player.setYaw(lastRotation.yaw);
                mc.player.setPitch(lastRotation.pitch);
                mc.player.headYaw = lastRotation.yaw;
                mc.player.bodyYaw = lastRotation.yaw;
            }

            mc.interactionManager.attackEntity(mc.player, e);
            mc.player.swingHand(Hand.MAIN_HAND);


            if (lastRotation != null) {
                mc.player.setYaw(prevYaw);
                mc.player.setPitch(prevPitch);
                mc.player.headYaw = prevHeadYaw;
                mc.player.bodyYaw = prevBodyYaw;
            }
        }
        lastAttackTime = System.currentTimeMillis();
        nextAttackDelay = computeLegitCpsDelay();
    }

    private long computeLegitCpsDelay() {
        long now = System.currentTimeMillis();
        if (attackMethod.is("Legit")) {
            if (now - legitDriftTime > 2000 + random.nextInt(2000)) {
                double span = 5.0;
                legitCpsCenter = targetCPS.getValue() + (random.nextGaussian() * span);
                legitCpsCenter = Math.max(1.0, Math.min(20.0, legitCpsCenter));
                legitDriftTime = now;
            }
            double stddev = Math.max(0.5, 4.0);
            double sample = legitCpsCenter + random.nextGaussian() * stddev;
            sample = Math.max(1.0, Math.min(20.0, sample));
            long delay = (long) (1000.0 / sample);
            return delay + random.nextInt(15) - 7;
        } else {
            double cps = targetCPS.getValue();
            double randomCPS = cps * (0.85 + random.nextDouble() * 0.3);
            return (long) (1000.0 / randomCPS);
        }
    }

    private Comparator<TargetCandidate> getComparator() {
        return switch (sortMode.getValue()) {
            case "Health" -> Comparator.comparingDouble(c -> c.health);
            case "HurtTime" -> Comparator.comparingInt(c -> c.hurtTime);
            case "Yaw" -> Comparator.comparingDouble(c -> c.yawDelta);
            default -> Comparator.comparingDouble(c -> c.distance);
        };
    }

    private void applyRotation() {
        if (rotationMode.is("None")) return;

        RotationUtils.Rotation current = lastRotation != null
                ? lastRotation
                : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());

        Vec3d aimPoint = getRandomizedAimPoint(target);
        RotationUtils.Rotation targetRot = RotationUtils.getRotationToPos(aimPoint, mc.player.getEyePos());

        float angleDiff = Math.abs(RotationUtils.normalizeAngle(targetRot.yaw - current.yaw));
        float factor = 0.3F + rotationSpeed.getValue() * 0.012F;
        float maxTurn = Math.min(60.0F, Math.max(4.0F, angleDiff * factor));

        RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(current, targetRot, maxTurn);

        if (rotationMode.is("LockView")) {
            mc.player.setYaw(next.yaw);
            mc.player.setPitch(next.pitch);
        } else {
            RotationUtils.setSilentRotation(next, getCorrectionMode());
        }

        lastRotation = next;
    }

    private void smoothResume() {
        RotationUtils.Rotation current = lastRotation != null
                ? lastRotation
                : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
        RotationUtils.Rotation visual = new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());

        RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(current, visual, 10.0F);
        if (Math.abs(RotationUtils.normalizeAngle(next.yaw - visual.yaw)) <= 0.5F
                && Math.abs(next.pitch - visual.pitch) <= 0.5F) {
            lastRotation = null;
            return;
        }

        if (rotationMode.is("Silent")) {
            RotationUtils.setSilentRotation(next, getCorrectionMode());
        } else {
            mc.player.setYaw(next.yaw);
            mc.player.setPitch(next.pitch);
        }
        lastRotation = next;
    }

    private void handleAttack() {
        if (!canAttackNow()) return;
        if (!isAttackValid(target)) return;
        attackEntity(target);
    }

    private boolean isAttackValid(LivingEntity e) {
        if (mc.player == null || mc.world == null) return false;

        boolean visible = TargetUtils.hasLineOfSight(e);
        double maxDist = visible ? attackRange.getValue() : wallRange.getValue();
        Box expanded = e.getBoundingBox().expand(maxDist);
        if (!expanded.contains(mc.player.getEyePos())) return false;

        if (!visible && !aimThroughBlocks.getValue()) return false;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d aimPoint = getRandomizedAimPoint(e);
        Vec3d rayEnd = eyes.add(aimPoint.subtract(eyes).normalize().multiply(maxDist + 1.0));

        Box hitbox = e.getBoundingBox().expand(0.1);
        if (hitbox.raycast(eyes, rayEnd).isEmpty()) return false;

        return true;
    }

    private boolean canAttackNow() {
        if (System.currentTimeMillis() - lastAttackTime < nextAttackDelay) return false;

        float cooldown = AttackUtils.getAttackCooldown();
        if (cooldownMode.is("Pre")) {
            if (cooldown < 0.9f) return false;
        } else {
            if (cooldown < 1.0f) return false;
        }
        return true;
    }

    private Vec3d getRandomizedAimPoint(LivingEntity e) {
        Vec3d eye = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.85, e.getZ());
        double dist = Math.max(1.0, mc.player.distanceTo(e));
        double offset = Math.tan(Math.toRadians(1.5)) * dist;
        return eye.add(
                (random.nextDouble() - 0.5) * 2.0 * offset,
                (random.nextDouble() - 0.5) * offset,
                (random.nextDouble() - 0.5) * 2.0 * offset
        );
    }

    private MovementCorrection.Mode getCorrectionMode() {
        return switch (moveFix.getValue()) {
            case "Silent" -> MovementCorrection.Mode.SILENT;
            case "Strict" -> MovementCorrection.Mode.STRICT;
            default -> MovementCorrection.Mode.OFF;
        };
    }

    private void setTarget(LivingEntity entity) {
        target = entity;
        if (entity == null) {
            targetDistance = Double.MAX_VALUE;
            lastAttackTime = 0;
        }
    }

    public LivingEntity getTarget() {
        return target;
    }

    public boolean isRequireMouseDown() {
        return requireMouseDown.getValue();
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!has3DRender()) return;

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        int color = Colors.currentColor().getRGB();

        if (targetEsp.getValue() && target != null && target.isAlive()) {
            Box box = target.getBoundingBox().expand(0.05, 0.05, 0.05);
            int lineColor = (220 << 24) | (color & 0x00FFFFFF);
            Render3DEngine.drawBox3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(), box, lineColor, 1.0F);
        }

        if (showRange.getValue() && mc.player != null) {
            Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY() + 0.02, mc.player.getZ());
            float range = attackRange.getValue().floatValue();
            int rangeColor = (130 << 24) | (color & 0x00FFFFFF);
            Render3DEngine.drawCircle3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(), pos, range, rangeColor, 56, 1.5F);
        }
    }

    public boolean hasTargetEsp() {
        return targetEsp.getValue() && target != null && target.isAlive();
    }

    public boolean has3DRender() {
        return (targetEsp.getValue() && target != null && target.isAlive())
            || showRange.getValue();
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastRotation = null;
        target = null;
        targetDistance = Double.MAX_VALUE;
        hitMap.clear();
        multiTargets.clear();
        attackedThisTick.clear();
        legitCpsCenter = targetCPS.getValue();
        legitDriftTime = 0;
    }

    @Override
    public String getDisplayValue() {
        return targetMode.getValue();
    }

    private record TargetCandidate(
            LivingEntity entity,
            double distance,
            float health,
            int hurtTime,
            double yawDelta,
            boolean isEnemy
    ) {}
}