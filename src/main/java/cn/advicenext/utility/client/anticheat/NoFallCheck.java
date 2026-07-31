package cn.advicenext.utility.client.anticheat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
public class NoFallCheck extends AntiCheatCheck {
    public NoFallCheck() { super(AntiCheatType.NO_FALL); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.hasSlowFalling) return;
        if (data.isInLiquid || data.isInWeb || data.isOnClimbable) return;
        double fallDist = player.fallDistance;
        if (fallDist > data.lastFallDistance) {
            data.highestFallDist = Math.max(data.highestFallDist, fallDist);
        }
        if (player.isOnGround() && data.highestFallDist > 3.5) {
            int hurtTime = player.hurtTime;
            double s = getStrictness();
            if (hurtTime == 0 && data.highestFallDist > 3.5 / s) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.noFallVL, String.format("Fall=%.1f no dmg", data.highestFallDist)));
            }
            data.highestFallDist = 0;
        }
        data.lastFallDistance = fallDist;
    }
}