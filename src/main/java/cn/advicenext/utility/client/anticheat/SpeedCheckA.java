package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
public class SpeedCheckA extends AntiCheatCheck {
    private static final double WALK_SPEED = 0.221;
    private static final double SPRINT_MULT = 1.3;
    private static final double SPEED_EFFECT_MULT = 1.2;
    public SpeedCheckA() { super(AntiCheatType.SPEED_A); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.isOnGround()) return;
        if (data.isInLiquid || data.isInWeb) return;
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d prevPos = data.prevPos;
        if (prevPos == null) return;
        double dx = pos.x - prevPos.x;
        double dz = pos.z - prevPos.z;
        double hSpeed = Math.sqrt(dx * dx + dz * dz);
        double baseSpeed = player.isSprinting() ? WALK_SPEED * SPRINT_MULT : WALK_SPEED;
        if (data.hasSpeed) baseSpeed *= SPEED_EFFECT_MULT;
        if (data.isOnClimbable) baseSpeed = 0.2;
        double s = getStrictness();
        double tolerance = 0.06 / s;
        double maxSpeed = baseSpeed + tolerance;
        if (hSpeed > maxSpeed) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.speedVL, String.format("Speed=%.2f max=%.2f", hSpeed, maxSpeed)));
        } else { data.speedVL = Math.max(0, data.speedVL - 1); }
    }
}