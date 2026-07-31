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
public class HighJumpCheck extends AntiCheatCheck {
    public HighJumpCheck() { super(AntiCheatType.HIGH_JUMP); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.hasLevitation || data.hasJumpBoost) return;
        if (data.isInLiquid) return;
        Vec3d motion = player.getVelocity();
        double maxJumpVel = 0.42;
        if (data.isOnClimbable) maxJumpVel = 0.2;
        boolean justJumped = !player.isOnGround() && data.airTicks == 1 && data.wasOnGround;
        double s = getStrictness();
        double tolerance = 0.05 / s;
        if (justJumped && motion.y > maxJumpVel + tolerance) {
            data.incrementVL(type);
            data.addFlag(new AntiCheatFlag(type, data.name, data.highJumpVL, String.format("JumpVel=%.3f max=%.3f", motion.y, maxJumpVel + tolerance)));
        }
        data.wasOnGround = player.isOnGround();
    }
}