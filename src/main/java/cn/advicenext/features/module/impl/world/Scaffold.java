package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Scaffold：自动在脚下放置方块。
 *
 * 旋转：自适应平滑朝向下一个放置点（静默旋转 + 移动修正）。
 * Tower：按住跳跃键时直上。
 * 方块计数通过 {@link #getBlockCount()} 暴露给 HUD/灵动岛显示。
 */
public class Scaffold extends Module {

    public static Scaffold INSTANCE;

    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", "How to rotate towards place position", "Smooth",
        List.of("Smooth", "Snap", "None"));
    private final DoubleSetting rotationSpeed = new DoubleSetting("Rotation Speed", "Rotation responsiveness",
        60.0, 100.0, 10.0, 1.0, () -> rotationMode.is("Smooth"));
    private final ModeSetting moveFix = new ModeSetting("Move Fix", "Movement correction mode", "Silent",
        List.of("Off", "Silent", "Strict"), () -> !rotationMode.is("None"));
    private final BooleanSetting tower = new BooleanSetting("Tower", "Build straight up when holding jump", true);
    private final BooleanSetting down = new BooleanSetting("Down", "Build downwards when sneaking", true);
    private final DoubleSetting placeDelay = new DoubleSetting("Delay", "Delay between placements (ticks)",
        0.0, 5.0, 0.0, 1.0);
    private final BooleanSetting gcdFix = new BooleanSetting("GCD Fix", "Snap rotations to mouse GCD",
        true, () -> rotationMode.is("Smooth"));

    private RotationUtils.Rotation lastRotation;
    private int delayTicks = 0;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks beneath you", Category.WORLD);
        INSTANCE = this;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            resetRotationSmoothly();
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
        }

        // 预测放置点：正下方 → 沿移动方向 1~2 tick 后的脚下位置
        BlockPos placePos = findPlacePos();
        if (placePos == null) {
            resetRotationSmoothly();
            return;
        }

        // 找到可依附的面
        BlockHitResult hitResult = findHitResult(placePos);
        if (hitResult == null) {
            resetRotationSmoothly();
            return;
        }

        // 旋转朝向放置点
        Vec3d hitPos = hitResult.getPos();
        RotationUtils.Rotation current = lastRotation != null
            ? lastRotation
            : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
        RotationUtils.Rotation targetRotation = RotationUtils.getRotationToPos(hitPos, mc.player.getEyePos());

        RotationUtils.Rotation next;
        boolean closeEnough;
        if (rotationMode.is("Snap")) {
            next = targetRotation;
            closeEnough = true;
        } else if (rotationMode.is("None")) {
            next = null;
            closeEnough = true; // 不旋转，直接放置
        } else {
            float angleDiff = Math.abs(RotationUtils.normalizeAngle(targetRotation.yaw - current.yaw));
            float factor = 0.35F + rotationSpeed.getValue().floatValue() * 0.011F;
            float maxTurn = Math.min(70.0F, Math.max(5.0F, angleDiff * factor));

            next = RotationUtils.smoothRotationLimited(current, targetRotation, maxTurn);
            if (gcdFix.getValue()) {
                next = RotationUtils.applyGcd(current, next);
            }
            closeEnough = isRotationClose(next, targetRotation);
        }

        if (next != null) {
            RotationUtils.setSilentRotation(next, getCorrectionMode());
            lastRotation = next;
        }

        // 放置
        if (delayTicks <= 0 && closeEnough) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            delayTicks = placeDelay.getValue().intValue();
        }
    }

    /**
     * 脚下待放置位置：从正下方开始，沿当前移动速度预测未来 1~2 tick 的位置，
     * 取第一个可放置且有依附面的位置——避免移动中放置点落后于玩家导致踩空。
     */
    private BlockPos findPlacePos() {
        Vec3d vel = mc.player.getVelocity();
        double baseY = mc.player.getY() - 0.5;

        if (tower.getValue() && mc.options.jumpKey.isPressed() && vel.y > 0.1) {
            baseY -= 1.0;
        }
        if (down.getValue() && mc.options.sneakKey.isPressed()) {
            baseY -= 1.0;
        }

        for (int i = 0; i <= 2; i++) {
            BlockPos pos = BlockPos.ofFloored(
                mc.player.getX() + vel.x * i,
                baseY,
                mc.player.getZ() + vel.z * i
            );
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (findHitResult(pos) != null) return pos;
        }
        return null;
    }

    /** 在目标位置的邻接面上构造命中结果 */
    private BlockHitResult findHitResult(BlockPos placePos) {
        // 优先从下方依附（放置面顶），其次侧面
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction side : order) {
            BlockPos neighbor = placePos.offset(side);
            if (mc.world.getBlockState(neighbor).isReplaceable()) continue;

            Direction face = side.getOpposite();
            Vec3d hit = Vec3d.ofCenter(neighbor).add(
                face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            return new BlockHitResult(hit, face, neighbor, false);
        }
        return null;
    }

    private boolean isRotationClose(RotationUtils.Rotation current, RotationUtils.Rotation target) {
        float yawDiff = Math.abs(RotationUtils.normalizeAngle(target.yaw - current.yaw));
        float pitchDiff = Math.abs(target.pitch - current.pitch);
        return yawDiff <= 12.0F && pitchDiff <= 12.0F;
    }

    private void resetRotationSmoothly() {
        if (lastRotation == null || mc.player == null) return;
        RotationUtils.Rotation visual = new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
        RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(lastRotation, visual, 12.0F);
        if (Math.abs(RotationUtils.normalizeAngle(next.yaw - visual.yaw)) <= 0.5F
            && Math.abs(next.pitch - visual.pitch) <= 0.5F) {
            lastRotation = null;
            return;
        }
        RotationUtils.setSilentRotation(next, getCorrectionMode());
        lastRotation = next;
    }

    private MovementCorrection.Mode getCorrectionMode() {
        return switch (moveFix.getValue()) {
            case "Silent" -> MovementCorrection.Mode.SILENT;
            case "Strict" -> MovementCorrection.Mode.STRICT;
            default -> MovementCorrection.Mode.OFF;
        };
    }

    /** 主手方块数量（供 HUD/灵动岛显示） */
    public static int getBlockCount() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** 模块是否激活（供灵动岛查询） */
    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastRotation = null;
        delayTicks = 0;
    }

    @Override
    public String getDisplayValue() {
        return String.valueOf(getBlockCount());
    }
}
