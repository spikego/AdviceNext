package cn.advicenext.features.module.impl.movement;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.mixin.minecraft.client.InputAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;

/**
 * SafeWalk：走到方块边缘时自动停住（等效于潜行的边缘保护，但无需潜行）。
 * 核心逻辑在 {@link #clipInputIfOnEdge(Input)}，由 MixinKeyboardInput 在
 * 按键输入生成后、移动模拟之前调用。
 */
public class SafeWalk extends Module {

    public static SafeWalk INSTANCE;

    /** 向移动方向预测的步长（格），接近 vanilla 潜行的边缘容差 */
    private static final double PREDICT = 0.3;

    public SafeWalk() {
        super("SafeWalk", "Prevents you from walking off edges", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    /**
     * 若玩家处于地面且继续按当前输入移动将走出方块边缘，则清空输入向量。
     */
    public static void clipInputIfOnEdge(Input input) {
        if (!isActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isOnGround()) return;

        InputAccessor accessor = (InputAccessor) input;
        Vec2f vec = accessor.getMovementVector();
        if (vec.x == 0.0F && vec.y == 0.0F) return;

        // 输入向量按视觉 yaw 旋转到世界方向（与 vanilla movementInputToVelocity 一致）
        float yaw = mc.player.getYaw() * ((float) Math.PI / 180.0F);
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        double dx = vec.x * cos - vec.y * sin;
        double dz = vec.y * cos + vec.x * sin;

        // 预测半步后的位置，检查脚下 1 格内是否有支撑
        Box feet = mc.player.getBoundingBox();
        Box predicted = feet.offset(dx * PREDICT, 0, dz * PREDICT);
        Box below = new Box(
            predicted.minX + 0.05, predicted.minY - 1.0, predicted.minZ + 0.05,
            predicted.maxX - 0.05, predicted.minY, predicted.maxZ - 0.05
        );

        if (mc.world.isSpaceEmpty(below)) {
            accessor.setMovementVector(Vec2f.ZERO);
        }
    }
}
