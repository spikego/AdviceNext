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
public class ScaffoldCheck extends AntiCheatCheck {
    public ScaffoldCheck() { super(AntiCheatType.SCAFFOLD); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        float yawDelta = Math.abs(MathHelper.wrapDegrees(player.getYaw() - data.lastYaw));
        float pitchDelta = Math.abs(player.getPitch() - data.lastPitch);
        if (yawDelta > 30 && pitchDelta < 5 && pitchDelta > 0.1 && !player.isOnGround()) {
            if (data.yawDelta > 30 && data.pitchDelta < 5) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.scaffoldVL, "Snap yaw placing"));
            }
        }
        data.lastYaw = player.getYaw();
        data.lastPitch = player.getPitch();
        data.yawDelta = yawDelta;
        data.pitchDelta = pitchDelta;
    }
}