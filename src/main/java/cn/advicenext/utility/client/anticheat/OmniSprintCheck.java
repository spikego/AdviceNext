package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
public class OmniSprintCheck extends AntiCheatCheck {
    public OmniSprintCheck() { super(AntiCheatType.OMNI_SPRINT); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (!player.isSprinting()) return;
        if (player.isCreative() || player.isSpectator()) return;
        Vec3d motion = player.getVelocity();
        if (motion.lengthSquared() < 0.01) return;
        float yaw = player.getYaw();
        double moveAngle = Math.toDegrees(Math.atan2(-motion.x, motion.z));
        double diff = Math.abs(MathHelper.wrapDegrees(yaw - (float) moveAngle));
        double s = getStrictness();
        double angleThreshold = 80 / s;
        double minSpeed = 0.15 / s;
        if (diff > angleThreshold && motion.horizontalLength() > minSpeed) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.omniSprintVL, String.format("Angle diff=%.1f", diff)));
        } else { data.omniSprintVL = Math.max(0, data.omniSprintVL - 1); }
    }
}