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
public class KillAuraCheck extends AntiCheatCheck {
    public KillAuraCheck() { super(AntiCheatType.KILL_AURA); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees(player.getYaw() - data.lastYaw));
        if (data.lastHurtTime < player.hurtTime && player.hurtTime == 10) {
            if (yawDiff > 30 && data.swingCount > 0) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.auraVL, String.format("Snap=%.1f", yawDiff)));
            }
        }
        data.lastYaw = player.getYaw();
        data.lastPitch = player.getPitch();
        data.lastHurtTime = player.hurtTime;
    }
}