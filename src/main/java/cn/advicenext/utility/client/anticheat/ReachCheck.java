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
public class ReachCheck extends AntiCheatCheck {
    public ReachCheck() { super(AntiCheatType.REACH); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (data.lastHitTime == 0 || data.lastHitPos == null) return;
        Vec3d playerEye = player.getEyePos();
        double dist = playerEye.distanceTo(data.lastHitPos);
        double maxReach = 3.0;
        if (player.isCreative()) maxReach = 6.0;
        if (dist > maxReach + 0.3) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.reachVL, String.format("Range=%.1f", dist)));
        } else { data.reachVL = Math.max(0, data.reachVL - 1); }
    }
}