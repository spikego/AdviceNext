package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 瞄准辅助。
 *
 * 模式（都经过平滑核心，不会瞬移）：
 * <ul>
 *   <li><b>Adaptive</b> — 自适应：转速与目标角差成正比，
 *       大角度快、小角度慢，最接近人手跟枪曲线。</li>
 *   <li><b>Simple</b> — 简单：固定角速度转动。</li>
 * </ul>
 *
 * Silent 反检测：GCD 对齐（旋转增量对齐为鼠标最小单位的整数倍）+
 * 目标点随机化（不锁死同一个点）+ 目标丢失时平滑回正（视角不跳变）。
 */
public class AimAssist extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Aim smoothing mode", "Adaptive",
        List.of("Adaptive", "Simple"));
    private final DoubleSetting range = new DoubleSetting("Range", "Target range", 4.0, 6.0, 1.0, 0.1);
    private final DoubleSetting fov = new DoubleSetting("FOV", "Field of view", 90.0, 180.0, 30.0, 5.0);
    private final DoubleSetting speed = new DoubleSetting("Speed", "Aim speed (Simple: fixed turn speed, Adaptive: responsiveness)",
        50.0, 100.0, 1.0, 1.0);
    private final ModeSetting priority = new ModeSetting("Priority", "Target selection priority", "Distance",
        List.of("Distance", "Angle", "Health"));
    private final BooleanSetting silent = new BooleanSetting("Silent", "Silent aim (server-side only)", true);
    private final BooleanSetting gcdFix = new BooleanSetting("GCD Fix", "Snap rotation to mouse GCD (bypass aim checks)",
        true, () -> silent.getValue());
    private final DoubleSetting randomization = new DoubleSetting("Randomization", "Target point randomization in degrees",
        1.5, 5.0, 0.0, 0.1);
    private final BooleanSetting onlyAttacking = new BooleanSetting("Only Attacking", "Only assist when attack key held", false);
    private final BooleanSetting walls = new BooleanSetting("Through Walls", "Aim through walls", false);

    private RotationUtils.Rotation lastSmoothedRotation;

    public AimAssist() {
        super("AimAssist", "Assists with aiming at targets", Category.COMBAT);
        this.settings.add(mode);
        this.settings.add(range);
        this.settings.add(fov);
        this.settings.add(speed);
        this.settings.add(priority);
        this.settings.add(silent);
        this.settings.add(gcdFix);
        this.settings.add(randomization);
        this.settings.add(onlyAttacking);
        this.settings.add(walls);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (onlyAttacking.getValue() && !mc.options.attackKey.isPressed()) return;

        LivingEntity target = findTarget();

        // 当前基准：优先上次的平滑结果（连续转动），否则从视觉旋转开始
        RotationUtils.Rotation current = lastSmoothedRotation != null
            ? lastSmoothedRotation
            : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());

        RotationUtils.Rotation next;
        if (target != null) {
            Vec3d aimPoint = getRandomizedAimPoint(target);
            RotationUtils.Rotation targetRotation = RotationUtils.getRotationToPos(aimPoint, mc.player.getEyePos());

            float maxTurn = computeMaxTurn(current, targetRotation);
            next = RotationUtils.smoothRotationLimited(current, targetRotation, maxTurn);

            // GCD 对齐：让旋转增量与真实鼠标输入不可区分
            if (silent.getValue() && gcdFix.getValue()) {
                next = RotationUtils.applyGcd(current, next);
            }
        } else {
            // 目标丢失：平滑回正到玩家真实视角，避免服务端看到跳变
            RotationUtils.Rotation visual = new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
            next = RotationUtils.smoothRotationLimited(current, visual, 8.0F);
            if (Math.abs(RotationUtils.normalizeAngle(next.yaw - visual.yaw)) <= 0.5F
                && Math.abs(next.pitch - visual.pitch) <= 0.5F) {
                // 已回正，停止提交
                lastSmoothedRotation = null;
                return;
            }
        }

        if (silent.getValue()) {
            RotationUtils.setSilentRotation(next);
        } else {
            mc.player.setYaw(next.yaw);
            mc.player.setPitch(next.pitch);
        }
        lastSmoothedRotation = next;
    }

    private LivingEntity findTarget() {
        TargetUtils.TargetPriority prio = switch (priority.getValue()) {
            case "Angle" -> TargetUtils.TargetPriority.ANGLE;
            case "Health" -> TargetUtils.TargetPriority.HEALTH;
            default -> TargetUtils.TargetPriority.DISTANCE;
        };

        float fovValue = fov.getValue().floatValue();
        return TargetUtils.getBestTarget(
            range.getValue(),
            TargetUtils.TargetFilter.PLAYERS,
            prio,
            e -> TargetUtils.isInFOV(e, fovValue)
                && TargetUtils.canAttack(e)
                && (walls.getValue() || TargetUtils.hasLineOfSight(e))
        );
    }

    /**
     * 计算本 tick 最大转角。
     * Simple：固定速度。Adaptive：与角差成正比（角差大转快，角差小转慢）。
     */
    private float computeMaxTurn(RotationUtils.Rotation current, RotationUtils.Rotation target) {
        float speedValue = speed.getValue().floatValue();
        if (mode.is("Simple")) {
            return 1.5F + speedValue * 0.585F;
        }
        // Adaptive：比例系数 0.25~1.5，角差 180° 时封顶 55°/tick
        float angleDiff = Math.abs(RotationUtils.normalizeAngle(target.yaw - current.yaw));
        float factor = 0.25F + speedValue * 0.0125F;
        return Math.min(55.0F, Math.max(2.5F, angleDiff * factor));
    }

    /** 目标瞄准点加随机偏移，避免锁死同一个坐标点 */
    private Vec3d getRandomizedAimPoint(LivingEntity target) {
        Vec3d eye = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.85, target.getZ());
        double rand = randomization.getValue();
        if (rand <= 0.0) return eye;

        // 按角度抖动换算为位置偏移（近似），距离越远偏移越大
        double dist = Math.max(1.0, mc.player.distanceTo(target));
        double offset = Math.tan(Math.toRadians(rand)) * dist;
        return eye.add(
            (Math.random() - 0.5) * 2.0 * offset,
            (Math.random() - 0.5) * offset,
            (Math.random() - 0.5) * 2.0 * offset
        );
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastSmoothedRotation = null;
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}
