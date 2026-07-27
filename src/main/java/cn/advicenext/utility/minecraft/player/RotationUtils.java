package cn.advicenext.utility.minecraft.player;

import cn.advicenext.utility.Utility;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils extends Utility {

    public static class Rotation {
        public float yaw;
        public float pitch;

        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Rotation add(float yawOffset, float pitchOffset) {
            return new Rotation(this.yaw + yawOffset, this.pitch + pitchOffset);
        }

        public Rotation copy() {
            return new Rotation(this.yaw, this.pitch);
        }
    }

    // ==================== MovementCorrection（已迁移至 movement 包） ====================

    private static final float DEG_TO_RAD = (float) Math.PI / 180F;

    public static MovementCorrection.Mode getMovementCorrection() {
        return MovementCorrection.getMode();
    }

    /**
     * 当前是否需要修正移动方向（仅当 fakeRotation 激活且修正模式不为 OFF 时）。
     * 用于 Mixin 中快速判断是否需要拦截。
     */
    public static boolean shouldCorrectMovement() {
        return MovementCorrection.isActive();
    }

    /**
     * 需要修正服务端 yaw（击退、横扫等场景）。
     * 等价于 shouldCorrectMovement()。
     */
    public static boolean shouldReplaceYaw() {
        return shouldCorrectMovement();
    }

    public static Vec3d getDirectionVector(float yaw, float pitch) {
        float radPitch = pitch * DEG_TO_RAD;
        float radYaw = -yaw * DEG_TO_RAD;
        float cosPitch = MathHelper.cos(radPitch);
        return new Vec3d(
            MathHelper.sin(radYaw) * cosPitch,
            -MathHelper.sin(radPitch),
            MathHelper.cos(radYaw) * cosPitch
        );
    }

    // ==================== 状态 ====================

    /** 即将发送给服务端的旋转 */
    private static Rotation serverRotation;

    /** 是否正在使用静默旋转（fakeRotation） */
    private static boolean fakeRotation;

    /** 保存的视觉旋转，在 pre/post 中用于 swap */
    private static float realYaw;
    private static float realPitch;

    // ==================== 静默旋转 API（参考 Wurst） ====================

    /**
     * 设置静默旋转（不修正移动，适用于 combat 类模块如 AimAssist）。
     */
    public static void setSilentRotation(Rotation rotation) {
        setSilentRotation(rotation, MovementCorrection.Mode.OFF);
    }

    /**
     * 设置静默旋转并指定移动修正模式。
     *
     * @param rotation   要发送给服务端的旋转
     * @param correction 移动修正模式
     */
    public static void setSilentRotation(Rotation rotation, MovementCorrection.Mode correction) {
        if (mc.player == null) return;
        serverRotation = rotation;
        fakeRotation = true;
        MovementCorrection.setMode(correction);
    }

    /** 重置静默旋转。下次发包将使用玩家真实的旋转。 */
    public static void resetSilentRotation() {
        serverRotation = null;
        fakeRotation = false;
        MovementCorrection.setMode(MovementCorrection.Mode.OFF);
    }

    /** 当前是否处于静默旋转状态 */
    public static boolean isFakeRotation() {
        return fakeRotation;
    }

    /** 获取即将发送给服务端的旋转 */
    public static Rotation getServerRotation() {
        return serverRotation;
    }

    // ==================== sendMovementPackets 包裹（Wurst 的 pre/post motion） ====================

    /**
     * 在 sendMovementPackets 之前调用 —— 对应 Wurst 的 onPreMotion()
     * 保存 visual 旋转，替换为 server 旋转。
     * 这样发包时所有 getYaw()/getPitch() 返回的都是 server 值。
     */
    public static void beforeSendMovementPackets() {
        if (!fakeRotation || mc.player == null) return;

        realYaw = mc.player.getYaw();
        realPitch = mc.player.getPitch();
        mc.player.setYaw(serverRotation.yaw);
        mc.player.setPitch(serverRotation.pitch);
    }

    /**
     * 在 sendMovementPackets 之后调用 —— 对应 Wurst 的 onPostMotion()
     * 恢复 visual 旋转。
     */
    public static void afterSendMovementPackets() {
        if (!fakeRotation || mc.player == null) return;

        mc.player.setYaw(realYaw);
        mc.player.setPitch(realPitch);
        fakeRotation = false;
    }

    // ==================== 获取服务端旋转值 ====================

    /** 获取实际发送给服务端的 yaw（参考 Wurst 的 getServerYaw()） */
    public static float getServerYaw() {
        return fakeRotation && serverRotation != null
            ? serverRotation.yaw
            : (mc.player != null ? mc.player.getYaw() : 0);
    }

    /** 获取实际发送给服务端的 pitch（参考 Wurst 的 getServerPitch()） */
    public static float getServerPitch() {
        return fakeRotation && serverRotation != null
            ? serverRotation.pitch
            : (mc.player != null ? mc.player.getPitch() : 0);
    }

    // ==================== 旋转计算 ====================

    public static Rotation getRotationToEntity(Entity entity) {
        return getRotationToEntity(entity, getPlayerPos());
    }

    public static Rotation getRotationToEntity(Entity entity, Vec3d playerPos) {
        return getRotationToPos(getEntityEyePos(entity), playerPos);
    }

    public static Rotation getRotationToPos(Vec3d targetPos) {
        return getRotationToPos(targetPos, getPlayerPos());
    }

    public static Rotation getRotationToPos(Vec3d targetPos, Vec3d playerPos) {
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new Rotation(yaw, pitch);
    }

    public static float normalizeAngle(float angle) {
        while (angle > 180.0F) angle -= 360.0F;
        while (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public static float angleDifference(float a, float b) {
        return normalizeAngle(a - b);
    }

    /** 限制角度变化（参考 Wurst 的 limitAngleChange） */
    public static float limitAngleChange(float current, float target, float maxChange) {
        float diff = normalizeAngle(target - current);
        if (diff > maxChange) diff = maxChange;
        if (diff < -maxChange) diff = -maxChange;
        return current + diff;
    }

    /**
     * 平滑旋转：恒速 + 末端指数收敛。
     * <p>
     * 远距离时以恒定角速度转动（每 tick 固定度数，速度可预测、无起步黏滞感）；
     * 进入末端区间后每 tick 消除一半剩余角度（平滑减速），足够接近时直接吸附，
     * 避免抖动与过冲。
     *
     * @param speed 0~100，映射为每 tick 最大转角约 1.5°~60°（20 tick/s 下 30°/s~1200°/s）
     */
    public static Rotation smoothRotation(Rotation current, Rotation target, float speed) {
        float maxTurn = 1.5F + MathHelper.clamp(speed, 0.0F, 100.0F) * 0.585F;

        float yawDiff = normalizeAngle(target.yaw - current.yaw);
        float pitchDiff = MathHelper.clamp(target.pitch, -90.0F, 90.0F) - current.pitch;

        float newYaw = current.yaw + approachAngle(yawDiff, maxTurn);
        // pitch 稍慢，更接近人手移动
        float newPitch = MathHelper.clamp(
            current.pitch + approachAngle(pitchDiff, maxTurn * 0.7F),
            -90.0F, 90.0F
        );

        return new Rotation(newYaw, newPitch);
    }

    /**
     * 单轴逼近：远处恒速 maxTurn，近处（< 2×maxTurn）每 tick 消除一半剩余，
     * 剩余 ≤ 0.2° 时直接到位。
     */
    private static float approachAngle(float diff, float maxTurn) {
        float abs = Math.abs(diff);
        if (abs <= 0.2F) return diff;

        float step;
        if (abs >= maxTurn * 2.0F) {
            step = maxTurn;
        } else {
            step = Math.max(abs * 0.5F, Math.min(0.4F, abs));
        }
        return Math.copySign(Math.min(step, abs), diff);
    }

    /** 与 {@link #smoothRotation} 相同，但直接指定每 tick 最大转角（度）。 */
    public static Rotation smoothRotationLimited(Rotation current, Rotation target, float maxTurn) {
        float yawDiff = normalizeAngle(target.yaw - current.yaw);
        float pitchDiff = MathHelper.clamp(target.pitch, -90.0F, 90.0F) - current.pitch;

        float newYaw = current.yaw + approachAngle(yawDiff, maxTurn);
        float newPitch = MathHelper.clamp(
            current.pitch + approachAngle(pitchDiff, maxTurn * 0.7F),
            -90.0F, 90.0F
        );
        return new Rotation(newYaw, newPitch);
    }

    // ==================== GCD 修复（反作弊 Aim 检测绕过） ====================

    /**
     * 鼠标输入的最小旋转单位（GCD）。
     * vanilla 的视角变化量 = 鼠标像素增量 × 该值，因此合法玩家的旋转增量
     * 总是它的整数倍。反作弊的 Aim 类检查利用这一点识别平滑旋转。
     */
    public static float getMouseGcd() {
        if (mc.options == null) return 0.0F;
        double sens = mc.options.getMouseSensitivity().getValue();
        double d = sens * 0.6 + 0.2;
        return (float) (d * d * d * 1.2);
    }

    /**
     * GCD 对齐：把 from → to 的旋转增量对齐到鼠标 GCD 的整数倍，
     * 使发包旋转与真实鼠标输入产生的旋转在数学上不可区分。
     */
    public static Rotation applyGcd(Rotation from, Rotation to) {
        float gcd = getMouseGcd();
        if (gcd <= 1.0E-4F) return to;

        float yawDiff = normalizeAngle(to.yaw - from.yaw);
        float pitchDiff = to.pitch - from.pitch;

        float snappedYaw = Math.round(yawDiff / gcd) * gcd;
        float snappedPitch = Math.round(pitchDiff / gcd) * gcd;

        return new Rotation(
            from.yaw + snappedYaw,
            MathHelper.clamp(from.pitch + snappedPitch, -90.0F, 90.0F)
        );
    }

    // ==================== 工具 ====================

    /**
     * 获取视觉 yaw 与服务端 yaw 的差值（visualYaw − serverYaw），
     * 已归一化到 [-180, 180]。
     */
    public static float getYawDifference() {
        if (!fakeRotation || serverRotation == null || mc.player == null) return 0F;
        return normalizeAngle(mc.player.getYaw() - serverRotation.yaw);
    }

    private static Vec3d getPlayerPos() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getEyePos();
    }

    private static Vec3d getEntityEyePos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.85, entity.getZ());
    }
}