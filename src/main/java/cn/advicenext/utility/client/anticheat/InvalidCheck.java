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
public class InvalidCheck extends AntiCheatCheck {
    public InvalidCheck() { super(AntiCheatType.INVALID); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        float pitch = player.getPitch();
        if (Math.abs(pitch) > 90) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.invalidVL, String.format("Pitch=%.1f", pitch)));
        }
        if (!player.isOnGround() && !data.isInLiquid && !data.isOnClimbable && !player.getAbilities().flying) {
            Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
            if (!Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.invalidVL, "Invalid position"));
            }
        }
        Vec3d motion = player.getVelocity();
        if (motion.y > 4.0 && !data.hasLevitation && !player.getAbilities().flying) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.invalidVL, String.format("Excessive motionY=%.1f", motion.y)));
        }
    }
}