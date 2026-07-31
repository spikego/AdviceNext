package cn.advicenext.mixin.minecraft.network;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.AttackEvent;
import cn.advicenext.utility.minecraft.player.RotationManager;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
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
 * 交互管理器 Mixin。
 * 在 interactItem 中临时替换玩家旋转为服务端旋转，
 * 确保数据包中的 yaw/pitch 与静默旋转一致。
 * 由于 sendSequencedPacket 同步执行 lambda，旋转在 HEAD 设置、TAIL 恢复，
 * 不会影响渲染（渲染在独立的帧循环中）。
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {

    @Unique
    private float savedYaw;

    @Unique
    private float savedPitch;

    @Unique
    private boolean rotationPatched;

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(target);
        EventBus.post(event);
    }

    /**
     * interactItem 中 Packet 构造使用 player.getYaw()/getPitch()，
     * 在 HEAD 替换为服务端旋转，TAIL 恢复。
     */
    @Inject(method = "interactItem", at = @At("HEAD"))
    private void onInteractItemHead(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        rotationPatched = false;

        if (RotationManager.INSTANCE.hasActiveRequest()) {
            player.setYaw(RotationManager.INSTANCE.getServerYaw());
            player.setPitch(RotationManager.INSTANCE.getServerPitch());
            rotationPatched = true;
        } else if (RotationUtils.isFakeRotation() && RotationUtils.getServerRotation() != null) {
            player.setYaw(RotationUtils.getServerYaw());
            player.setPitch(RotationUtils.getServerPitch());
            rotationPatched = true;
        }
    }

    @Inject(method = "interactItem", at = @At("TAIL"))
    private void onInteractItemTail(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (rotationPatched) {
            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
        }
    }
}