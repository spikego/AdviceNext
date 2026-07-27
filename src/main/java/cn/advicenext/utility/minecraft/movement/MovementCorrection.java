package cn.advicenext.utility.minecraft.movement;

import cn.advicenext.mixin.minecraft.client.InputAccessor;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

/**
 * 移动修正（Movement Correction）
 *
 * 当静默旋转激活时，服务端认为的玩家朝向（serverYaw）与客户端视觉朝向
 * （visualYaw）不同。本类修正移动输入与速度计算，使得：
 * <ul>
 *   <li>玩家按键方向始终对应视觉方向（按 W 朝视觉前方移动）；</li>
 *   <li>客户端模拟出的移动与发包位置一致，不因朝向差自相矛盾。</li>
 * </ul>
 *
 * 原理：vanilla 在 {@code Entity.updateVelocity} 中计算
 * {@code velocity = R(yaw) · input}。令 V = 视觉 yaw，S = 服务端 yaw。
 * 先把输入旋转 δ = V − S 得到 input' = R(δ) · input，再让 updateVelocity
 * 使用 S 计算：
 * <pre>
 *   velocity = R(S) · input' = R(S) · R(V−S) · input = R(V) · input
 * </pre>
 * 即最终移动方向与视觉一致。
 *
 * 模式：
 * <ul>
 *   <li>{@link Mode#OFF} — 不修正。</li>
 *   <li>{@link Mode#SILENT} — 浮点精度修正，移动方向与视觉完全一致。</li>
 *   <li>{@link Mode#STRICT} — 在 SILENT 基础上把输入对齐到原版 8 方向
 *       （长度归一），输入值与真实按键完全一致，更难被移动预测类反作弊检测。</li>
 * </ul>
 */
public final class MovementCorrection {

    public enum Mode {
        OFF,
        SILENT,
        STRICT
    }

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static Mode mode = Mode.OFF;

    private MovementCorrection() {
    }

    // ==================== 模式管理 ====================

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode newMode) {
        mode = newMode == null ? Mode.OFF : newMode;
    }

    /** 当前是否需要修正（存在静默旋转且模式不为 OFF） */
    public static boolean isActive() {
        return mode != Mode.OFF
            && RotationUtils.isFakeRotation()
            && RotationUtils.getServerRotation() != null;
    }

    // ==================== 输入修正（KeyboardInput.tick TAIL 调用） ====================

    public static void fixInput(Input input) {
        if (!isActive() || mc.player == null) return;

        InputAccessor accessor = (InputAccessor) input;
        Vec2f vec = accessor.getMovementVector();
        float strafe = vec.x;
        float forward = vec.y;
        if (strafe == 0.0F && forward == 0.0F) return;

        float[] fixed = rotate(strafe, forward);
        accessor.setMovementVector(new Vec2f(fixed[0], fixed[1]));
    }

    /**
     * 核心旋转公式（已对照 vanilla {@code movementInputToVelocity} 验证）：
     * <pre>
     *   strafe'  = strafe·cosδ − forward·sinδ
     *   forward' = forward·cosδ + strafe·sinδ
     *   δ = wrapDegrees(visualYaw − serverYaw)
     * </pre>
     *
     * @return {@code [newStrafe, newForward]}
     */
    public static float[] rotate(float strafe, float forward) {
        float delta = RotationUtils.normalizeAngle(mc.player.getYaw() - RotationUtils.getServerYaw());
        double rad = Math.toRadians(delta);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        float newStrafe = strafe * cos - forward * sin;
        float newForward = forward * cos + strafe * sin;

        if (mode == Mode.STRICT) {
            newStrafe = snap(newStrafe);
            newForward = snap(newForward);
            // snap 到 (±1, ±1) 后长度可能为 √2，归一化回原版对角输入长度
            float len = MathHelper.sqrt(newStrafe * newStrafe + newForward * newForward);
            if (len > 1.0F) {
                newStrafe /= len;
                newForward /= len;
            }
        }

        return new float[]{newStrafe, newForward};
    }

    private static float snap(float value) {
        if (value > 0.5F) return 1.0F;
        if (value < -0.5F) return -1.0F;
        return 0.0F;
    }

    // ==================== 速度计算修正（Entity.updateVelocity 调用） ====================

    /**
     * updateVelocity 中 {@code getYaw()} 的替换值：
     * 修正激活时返回服务端 yaw，使 R(S) · input' = R(V) · input 成立。
     */
    public static float correctYaw(float visualYaw) {
        if (!isActive()) return visualYaw;
        return RotationUtils.getServerYaw();
    }
}
