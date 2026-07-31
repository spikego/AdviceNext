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
public class JesusCheck extends AntiCheatCheck {
    public JesusCheck() { super(AntiCheatType.JESUS); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        if (player.isCreative() || player.isSpectator()) return;
        if (player.getAbilities().flying) return;
        if (data.isOnClimbable) return;
        if (!data.isInLiquid) { data.jesusVL = Math.max(0, data.jesusVL - 1); return; }
        if (player.isOnGround()) {
            Box bb = player.getBoundingBox().contract(0.001, 0.001, 0.001);
            if (mc.world.getBlockState(player.getBlockPos().down()).getBlock() instanceof FluidBlock) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.jesusVL, "Standing on liquid"));
            }
        }
    }
}