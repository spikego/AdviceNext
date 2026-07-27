package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.features.module.impl.render.NoHurtCam;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void injectHurtCam(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (NoHurtCam.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }
    
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
        MatrixStack matrices = new MatrixStack();
        float tickDelta = renderTickCounter.getDynamicDeltaTicks();
        EventBus.post(new Render3DEvent(matrices, tickDelta));
    }

    @Redirect(
        method = "updateCrosshairTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;"
        )
    )
    private HitResult redirectGetCrosshairTarget(ClientPlayerEntity player, float tickDelta, Entity entity) {
        // 保存原始视觉旋转
        float oldYaw = player.getYaw();
        float oldPitch = player.getPitch();

        // 如果有静默旋转，临时使用服务器旋转来计算射线投射
        RotationUtils.Rotation rotation = RotationUtils.getServerRotation();
        if (rotation != null) {
            player.setYaw(rotation.yaw);
            player.setPitch(rotation.pitch);
        }

        try {
            // 使用服务器旋转计算命中结果
            return player.getCrosshairTarget(tickDelta, entity);
        } finally {
            // 恢复原始视觉旋转
            player.setYaw(oldYaw);
            player.setPitch(oldPitch);
        }
    }
}