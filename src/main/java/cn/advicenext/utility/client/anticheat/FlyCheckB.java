package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
public class FlyCheckB extends AntiCheatCheck {
    public FlyCheckB() { super(AntiCheatType.FLY_B); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.hasLevitation || data.hasSlowFalling) return;
        if (data.isInLiquid || data.isOnClimbable) return;
        if (player.getAbilities().flying) return;
        double s = getStrictness();
        double gravityTolerance = 0.03 / s;
        int minAirTicksLowGrav = (int) Math.max(5, 15 / s);
        int minAirTicksHover = (int) Math.max(5, 30 / s);
        Vec3d motion = player.getVelocity();
        Vec3d prevMotion = data.prevMotion;
        if (prevMotion == null) return;
        double prevY = prevMotion.y;
        double curY = motion.y;
        if (!player.isOnGround() && data.airTicks > 5 && curY < 0 && prevY < 0) {
            double expectedY = prevY - 0.08;
            double diff = Math.abs(curY - expectedY);
            if (diff < gravityTolerance && data.airTicks > minAirTicksLowGrav) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.flyVL, "Low gravity pattern dt=" + data.airTicks));
            }
        }
        if (!player.isOnGround() && curY > -0.04 && data.airTicks > 10) {
            double fallDist = data.prevPos.y - player.getY();
            if (fallDist < 0.1 && data.airTicks > minAirTicksHover) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.flyVL, "Anti-gravity hover " + data.airTicks + "t"));
            }
        }
    }
}