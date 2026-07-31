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
public class BlinkCheck extends AntiCheatCheck {
    public BlinkCheck() { super(AntiCheatType.BLINK); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d prevPos = data.prevPos;
        if (prevPos == null) return;
        double dx = pos.x - prevPos.x;
        double dy = pos.y - prevPos.y;
        double dz = pos.z - prevPos.z;
        double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double hDist = Math.sqrt(dx * dx + dz * dz);
        long delta = System.currentTimeMillis() - data.prevUpdateTime;
        if (delta <= 0) return;
        double s = getStrictness();
        double maxTickDist = (delta * 0.025) / s;
        double teleportThreshold = maxTickDist + 2.0 / s;
        if (totalDist > teleportThreshold) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.blinkVL, String.format("Teleport=%.1f in %dms", totalDist, delta)));
        } else if (hDist > 0.5 && data.lastTickDelta > 200) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.blinkVL, String.format("Lag blink=%.1f in %dms", hDist, (int)data.lastTickDelta)));
        } else { data.blinkVL = Math.max(0, data.blinkVL - 1); }
    }
}