package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.RangeSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * KillAura：自动攻击附近目标。
 *
 * 旋转：自适应平滑（角差比例转速）+ GCD 对齐 + 目标点随机化 + 丢失平滑回正。
 * 攻击：Cooldown（1.9 冷却感知）或 CPS（范围随机）。
 * 移动修正：Off / Silent / Strict（与 MovementCorrection 系统联动）。
 * 视觉：范围圈 + 目标高亮盒，均可开关。
 */
public class KillAura extends Module {

    public static KillAura INSTANCE;

    // 战斗
    private final DoubleSetting range = new DoubleSetting("Range", "Attack range", 3.0, 6.0, 1.0, 0.1);
    private final DoubleSetting wallRange = new DoubleSetting("Wall Range", "Range through walls", 3.0, 6.0, 1.0, 0.1);
    private final ModeSetting attackMode = new ModeSetting("Attack Mode", "Attack timing mode", "Cooldown",
        List.of("Cooldown", "CPS"));
    private final RangeSetting cps = new RangeSetting("CPS", "Attacks per second range",
        9.0, 12.0, 1.0, 20.0, 0.5, () -> attackMode.is("CPS"));
    private final ModeSetting targetMode = new ModeSetting("Target Mode", "How to pick targets", "Single",
        List.of("Single", "Switch"));
    private final ModeSetting priority = new ModeSetting("Priority", "Target priority", "Distance",
        List.of("Distance", "Angle", "Health"));
    private final BooleanSetting raycastCheck = new BooleanSetting("Raycast", "Only attack when crosshair actually hits", true);

    // 旋转
    private final DoubleSetting rotationSpeed = new DoubleSetting("Rotation Speed", "Aim responsiveness",
        60.0, 100.0, 10.0, 1.0);
    private final ModeSetting moveFix = new ModeSetting("Move Fix", "Movement correction mode", "Silent",
        List.of("Off", "Silent", "Strict"));
    private final BooleanSetting gcdFix = new BooleanSetting("GCD Fix", "Snap rotations to mouse GCD", true);
    private final DoubleSetting randomization = new DoubleSetting("Randomization", "Aim point randomization",
        1.5, 5.0, 0.0, 0.1);

    // 视觉
    private final BooleanSetting showRange = new BooleanSetting("Range Circle", "Show attack range circle", true);
    private final BooleanSetting targetEsp = new BooleanSetting("Target ESP", "Highlight current target", true);

    // 状态
    private LivingEntity target;
    private RotationUtils.Rotation lastRotation;
    private long lastAttackTime = 0;
    private long nextAttackDelay = 0;
    private int switchIndex = 0;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby targets", Category.COMBAT);
        INSTANCE = this;
    }

    // ==================== Tick ====================

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        target = selectTarget();

        RotationUtils.Rotation current = lastRotation != null
            ? lastRotation
            : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());

        if (target == null) {
            // 平滑回正到真实视角
            RotationUtils.Rotation visual = new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
            RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(current, visual, 10.0F);
            if (Math.abs(RotationUtils.normalizeAngle(next.yaw - visual.yaw)) <= 0.5F
                && Math.abs(next.pitch - visual.pitch) <= 0.5F) {
                lastRotation = null;
                return;
            }
            RotationUtils.setSilentRotation(next, getCorrectionMode());
            lastRotation = next;
            return;
        }

        // 旋转到目标
        Vec3d aimPoint = getRandomizedAimPoint(target);
        RotationUtils.Rotation targetRotation = RotationUtils.getRotationToPos(aimPoint, mc.player.getEyePos());

        float angleDiff = Math.abs(RotationUtils.normalizeAngle(targetRotation.yaw - current.yaw));
        float factor = 0.3F + rotationSpeed.getValue().floatValue() * 0.012F;
        float maxTurn = Math.min(60.0F, Math.max(4.0F, angleDiff * factor));

        RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(current, targetRotation, maxTurn);
        if (gcdFix.getValue()) {
            next = RotationUtils.applyGcd(current, next);
        }

        RotationUtils.setSilentRotation(next, getCorrectionMode());
        lastRotation = next;

        // 攻击
        if (canAttackNow() && (!raycastCheck.getValue() || isCrosshairOnTarget())) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
            nextAttackDelay = (long) (1000.0 / cps.getRandom());

            if (targetMode.is("Switch")) {
                switchIndex++;
            }
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;

        net.minecraft.client.gui.DrawContext ctx = event.getContext();
        int accent = Colors.currentColor().getRGB();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        if (showRange.getValue()) {
            double px = mc.player.lastRenderX + (mc.player.getX() - mc.player.lastRenderX) * tickDelta;
            double py = mc.player.lastRenderY + (mc.player.getY() - mc.player.lastRenderY) * tickDelta;
            double pz = mc.player.lastRenderZ + (mc.player.getZ() - mc.player.lastRenderZ) * tickDelta;
            Vec3d pos = new Vec3d(px, py + 0.02, pz);
            Render3DEngine.drawCircleFilled(ctx, pos, range.getValue().floatValue(),
                withAlpha(accent, 20), 40);
            Render3DEngine.drawCircleOutline(ctx, pos, range.getValue().floatValue(),
                withAlpha(accent, 130), 56, 1.5F);
        }

        if (targetEsp.getValue() && target != null && target.isAlive()) {
            double x = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
            double y = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
            double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
            float w = target.getWidth() / 2.0F + 0.05F;
            Box box = new Box(x - w, y - 0.05, z - w, x + w, y + target.getHeight() + 0.1, z + w);
            Render3DEngine.drawBox(ctx, box, withAlpha(accent, 40), withAlpha(accent, 220), 1.0F);
        }
    }

    // ==================== 内部 ====================

    private LivingEntity selectTarget() {
        TargetUtils.TargetPriority prio = switch (priority.getValue()) {
            case "Angle" -> TargetUtils.TargetPriority.ANGLE;
            case "Health" -> TargetUtils.TargetPriority.HEALTH;
            default -> TargetUtils.TargetPriority.DISTANCE;
        };

        double maxRange = Math.max(range.getValue(), wallRange.getValue());
        List<LivingEntity> targets = TargetUtils.getTargets(maxRange, TargetUtils.TargetFilter.PLAYERS,
            e -> TargetUtils.canAttack(e) && inRange(e));

        if (targets.isEmpty()) return null;

        if (targetMode.is("Switch")) {
            if (switchIndex >= targets.size()) switchIndex = 0;
            // Switch：按距离排序后轮换
            targets = TargetUtils.sortByDistance(targets);
            LivingEntity t = targets.get(switchIndex % targets.size());
            if (target == t && t.isAlive()) return t;
            return t;
        }

        // Single：保持当前目标直到死亡/出范围
        if (target != null && target.isAlive() && inRange(target) && targets.contains(target)) {
            return target;
        }

        return switch (prio) {
            case DISTANCE -> TargetUtils.sortByDistance(targets).get(0);
            case HEALTH -> TargetUtils.sortByHealth(targets).get(0);
            default -> TargetUtils.sortByAngle(targets).get(0);
        };
    }

    private boolean inRange(LivingEntity e) {
        double dist = mc.player.distanceTo(e);
        boolean visible = TargetUtils.hasLineOfSight(e);
        return visible ? dist <= range.getValue() : dist <= wallRange.getValue();
    }

    private boolean canAttackNow() {
        if (attackMode.is("Cooldown")) {
            return mc.player.getAttackCooldownProgress(0.5F) >= 1.0F;
        }
        return System.currentTimeMillis() - lastAttackTime >= nextAttackDelay;
    }

    /**
     * 准星 raycast 检查：updateCrosshairTarget 已被 hook 为按服务端旋转计算，
     * 因此 crosshairTarget 命中目标即代表"服务器视角下准星在目标身上"。
     */
    private boolean isCrosshairOnTarget() {
        HitResult hit = mc.crosshairTarget;
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target;
    }

    private Vec3d getRandomizedAimPoint(LivingEntity e) {
        Vec3d eye = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.85, e.getZ());
        double rand = randomization.getValue();
        if (rand <= 0.0) return eye;

        double dist = Math.max(1.0, mc.player.distanceTo(e));
        double offset = Math.tan(Math.toRadians(rand)) * dist;
        return eye.add(
            (Math.random() - 0.5) * 2.0 * offset,
            (Math.random() - 0.5) * offset,
            (Math.random() - 0.5) * 2.0 * offset
        );
    }

    private MovementCorrection.Mode getCorrectionMode() {
        return switch (moveFix.getValue()) {
            case "Silent" -> MovementCorrection.Mode.SILENT;
            case "Strict" -> MovementCorrection.Mode.STRICT;
            default -> MovementCorrection.Mode.OFF;
        };
    }

    /** 当前目标（供 TargetInfo 等外部显示） */
    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastRotation = null;
        target = null;
        switchIndex = 0;
    }

    @Override
    public String getDisplayValue() {
        return target != null ? target.getName().getString() : null;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
