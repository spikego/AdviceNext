package cn.advicenext.utility.minecraft.movement;

import cn.advicenext.utility.Utility;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MovementUtils extends Utility {

    public static final double WALK_SPEED = 0.221;
    public static final double BUNNY_SLOPE = 0.66;
    public static final double MOD_SPRINTING = 1.3F;
    public static final double MOD_SNEAK = 0.3F;
    public static final double MOD_ICE = 2.5F;
    public static final double MOD_WEB = 0.105 / WALK_SPEED;
    public static final double JUMP_HEIGHT = 0.42F;
    public static final double BUNNY_FRICTION = 159.9F;
    public static final double Y_ON_GROUND_MIN = 0.00001;
    public static final double Y_ON_GROUND_MAX = 0.0626;

    public static final double AIR_FRICTION = 0.9800000190734863D;
    public static final double WATER_FRICTION = 0.800000011920929D;
    public static final double LAVA_FRICTION = 0.5D;
    public static final double MOD_SWIM = 0.115F / WALK_SPEED;
    public static final double[] MOD_DEPTH_STRIDER = {
            1.0F,
            0.1645F / MOD_SWIM / WALK_SPEED,
            0.1995F / MOD_SWIM / WALK_SPEED,
            1.0F / MOD_SWIM,
    };
    public static final double BASE_JUMP_HEIGHT = 0.41999998688698;
    public static final double UNLOADED_CHUNK_MOTION = -0.09800000190735147;
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    private static final float DEG_TO_RAD = (float) Math.PI / 180F;

    public static class FixedInput {
        public final float forward;
        public final float strafe;
        public FixedInput(float forward, float strafe) {
            this.forward = forward;
            this.strafe = strafe;
        }
    }

    public static double direction() {
        float yaw = mc.player.getYaw();
        return Math.toRadians(yaw);
    }

    public static void strafe(final double speed) {
        final double yaw = direction();
        mc.player.setVelocity(
                -MathHelper.sin((float) yaw) * speed,
                mc.player.getVelocity().y,
                MathHelper.cos((float) yaw) * speed
        );
    }

    public static Boolean isMoving() {
        return mc.player.getVelocity().x != 0 || mc.player.getVelocity().z != 0;
    }

    public static double sqrtSpeed(Vec3d vec) {
        return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
    }

    public static Vec3d withStrafe(
            Vec3d vec,
            double speed,
            double strength,
            float movementForward,
            float movementSideways,
            float yaw
    ) {
        if (movementForward == 0.0F && movementSideways == 0.0F) {
            return new Vec3d(0.0, vec.y, 0.0);
        }

        double rad = Math.toRadians(yaw);
        double sin = -Math.sin(rad);
        double cos = Math.cos(rad);

        double inputLength = Math.sqrt(movementForward * movementForward + movementSideways * movementSideways);
        if (inputLength < 1.0) inputLength = 1.0;
        movementForward /= inputLength;
        movementSideways /= inputLength;

        double motionX = (movementForward * sin + movementSideways * cos) * speed * strength;
        double motionZ = (movementForward * cos - movementSideways * sin) * speed * strength;

        return new Vec3d(motionX, vec.y, motionZ);
    }

    public static double direction(float yaw, float forward, float strafe) {
        if (forward < 0F) yaw += 180F;

        float side = 1F;
        if (forward < 0F) side = -0.5F;
        else if (forward > 0F) side = 0.5F;

        if (strafe > 0F) yaw -= 90F * side;
        if (strafe < 0F) yaw += 90F * side;

        return Math.toRadians(yaw);
    }

    public static float calculateYawFromDelta(double deltaX, double deltaZ) {
        return (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
    }

    public static float calculatePitchFromDelta(double deltaX, double deltaY, double deltaZ) {
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));
    }

    /**
     * 根据服务端 yaw 差值修正玩家的移动输入。
     * 委托 {@link MovementCorrection#rotate(float, float)}，
     * 实际运行时的输入修正在 KeyboardInput.tick 中统一完成。
     *
     * @param forward 原始 forward 输入 (W/S)
     * @param strafe  原始 strafe 输入 (A/D)
     * @return 修正后的输入
     */
    public static FixedInput transformInput(float forward, float strafe) {
        if (mc.player == null || !MovementCorrection.isActive()) {
            return new FixedInput(forward, strafe);
        }

        float[] fixed = MovementCorrection.rotate(strafe, forward);
        return new FixedInput(fixed[1], fixed[0]);
    }

    public static Vec3d getRotationVector(float yaw, float pitch) {
        float radPitch = pitch * DEG_TO_RAD;
        float radYaw = -yaw * DEG_TO_RAD;
        float cosPitch = MathHelper.cos(radPitch);
        return new Vec3d(
            MathHelper.sin(radYaw) * cosPitch,
            -MathHelper.sin(radPitch),
            MathHelper.cos(radYaw) * cosPitch
        );
    }
}