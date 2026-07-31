package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
public class SpeedCheckB extends AntiCheatCheck {
    private static final double MAX_ACCEL = 0.15;
    public SpeedCheckB() { super(AntiCheatType.SPEED_B); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.isInLiquid || data.isInWeb) return;
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d prevPos = data.prevPos;
        if (prevPos == null) return;
        double dx = pos.x - prevPos.x;
        double dz = pos.z - prevPos.z;
        double hSpeed = Math.sqrt(dx * dx + dz * dz);
        double prevSpeed = data.lastHorizontalSpeed;
        double accel = hSpeed - prevSpeed;
        double s = getStrictness();
        double maxAccel = MAX_ACCEL / s;
        if (player.isOnGround() && accel > maxAccel && hSpeed > 0.2) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.speedVL, String.format("Accel=%.3f", accel)));
        } else if (player.isOnGround()) { data.speedVL = Math.max(0, data.speedVL - 1); }
        data.lastHorizontalSpeed = hSpeed;
    }
}