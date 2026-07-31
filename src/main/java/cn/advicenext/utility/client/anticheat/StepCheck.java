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
public class StepCheck extends AntiCheatCheck {
    public StepCheck() { super(AntiCheatType.STEP); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (data.isInLiquid || data.isOnClimbable) return;
        if (player.isOnGround()) {
            double dy = player.getY() - data.prevPos.y;
            double s = getStrictness();
            double maxStep = 1.1 / s;
            if (dy > maxStep && !player.getAbilities().flying) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.stepVL, String.format("Step=%.1f", dy)));
            }
        }
    }
}