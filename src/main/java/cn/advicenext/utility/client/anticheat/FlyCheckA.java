package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
public class FlyCheckA extends AntiCheatCheck {
    public FlyCheckA() { super(AntiCheatType.FLY_A); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.hasLevitation || data.hasSlowFalling) return;
        if (data.isInLiquid || data.isInWeb || data.isOnClimbable) return;
        if (player.getAbilities().flying) return;
        boolean onGround = player.isOnGround();
        Vec3d motion = player.getVelocity();
        double motionY = motion.y;
        double s = getStrictness();
        int hoverAirTicks = (int) Math.max(3, 20 / s);
        int ascendAirTicks = (int) Math.max(5, 40 / s);
        if (onGround) {
            data.airTicks = 0;
            data.groundTicks++;
            data.lastGroundPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            data.flyVL = Math.max(0, data.flyVL - 1);
        } else {
            data.airTicks++;
            data.groundTicks = 0;
            if (data.airTicks > hoverAirTicks && motionY > -0.08 && motionY < 0.01) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.flyVL, "Hovering in air for " + data.airTicks + " ticks"));
            }
            if (data.airTicks > ascendAirTicks && motionY >= 0.01) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.flyVL, "Ascending in air " + data.airTicks + "t"));
            }
        }
    }
}