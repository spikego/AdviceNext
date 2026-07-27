package cn.advicenext.mixin.minecraft.network;

import cn.advicenext.utility.minecraft.player.RotationManager;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 使用物品（弓、钓竿、雪球、末影珍珠等）的旋转修正。
 *
 * interactItem 发出的 PlayerInteractItemC2SPacket 携带 player.getYaw()/getPitch()，
 * 服务器用该朝向生成投射物。静默旋转激活时，若直接用视觉 yaw 发包，
 * 服务器视角下就是"人朝前、箭朝后"的矛盾。此处在整个 interactItem
 * 执行期间把玩家旋转临时换成服务端旋转，使：
 * <ul>
 *   <li>发包朝向 = 服务端朝向（投射物方向与服务器视角自洽）；</li>
 *   <li>itemStack.use() 的客户端逻辑同样使用服务端朝向，行为一致。</li>
 * </ul>
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {

    @Unique
    private float savedYaw;

    @Unique
    private float savedPitch;

    @Unique
    private boolean swapped;

    @Unique
    private float savedYawStop;

    @Unique
    private float savedPitchStop;

    @Unique
    private boolean swappedStop;

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void swapToServerRotation(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        swapped = false;
        if (!shouldSwap(player)) return;

        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        player.setYaw(getEffectiveServerYaw());
        player.setPitch(getEffectiveServerPitch());
        swapped = true;
    }

    @Inject(method = "interactItem", at = @At("RETURN"))
    private void restoreRotation(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!swapped) return;
        player.setYaw(savedYaw);
        player.setPitch(savedPitch);
        swapped = false;
    }

    /**
     * 松开使用键的时刻（弓放箭、钓竿收杆、弩发射后的释放）：
     * stopUsingItem 不在 doItemUse 内，不受 doItemUse 的旋转 swap 覆盖，
     * 必须单独处理，否则客户端的释放逻辑会回到视觉旋转执行。
     */
    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void swapToServerRotationOnStop(PlayerEntity player, CallbackInfo ci) {
        swappedStop = false;
        if (!shouldSwap(player)) return;

        savedYawStop = player.getYaw();
        savedPitchStop = player.getPitch();
        player.setYaw(getEffectiveServerYaw());
        player.setPitch(getEffectiveServerPitch());
        swappedStop = true;
    }

    @Inject(method = "stopUsingItem", at = @At("RETURN"))
    private void restoreRotationOnStop(PlayerEntity player, CallbackInfo ci) {
        if (!swappedStop) return;
        player.setYaw(savedYawStop);
        player.setPitch(savedPitchStop);
        swappedStop = false;
    }

    @Unique
    private boolean shouldSwap(PlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player) return false;
        return RotationManager.INSTANCE.hasActiveRequest()
            || (RotationUtils.isFakeRotation() && RotationUtils.getServerRotation() != null);
    }

    @Unique
    private float getEffectiveServerYaw() {
        if (RotationManager.INSTANCE.hasActiveRequest()) {
            return RotationManager.INSTANCE.getServerYaw();
        }
        return RotationUtils.getServerYaw();
    }

    @Unique
    private float getEffectiveServerPitch() {
        if (RotationManager.INSTANCE.hasActiveRequest()) {
            return RotationManager.INSTANCE.getServerPitch();
        }
        return RotationUtils.getServerPitch();
    }
}
