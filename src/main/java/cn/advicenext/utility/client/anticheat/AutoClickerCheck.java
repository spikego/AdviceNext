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
public class AutoClickerCheck extends AntiCheatCheck {
    public AutoClickerCheck() { super(AntiCheatType.AUTO_CLICKER); }
    public void check(PlayerEntity player, AntiCheatPlayerData data) {
        long now = System.currentTimeMillis();
        if (now - data.swingWindowStart > 1000) {
            if (data.swingInWindow > 20) {
                data.incrementVL(type);
                data.addFlag(new AntiCheatFlag(type, data.name, data.autoClickerVL, String.format("CPS=%d", data.swingInWindow)));
            }
            data.swingWindowStart = now;
            data.swingInWindow = 0;
        }
    }
}