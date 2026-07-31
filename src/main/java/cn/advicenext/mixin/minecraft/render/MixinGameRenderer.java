package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.player.GhostHand;
import cn.advicenext.features.module.impl.render.NoHurtCam;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
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
    
    @Redirect(
        method = "updateCrosshairTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;"
        )
    )
    private HitResult redirectGetCrosshairTarget(ClientPlayerEntity player, float tickDelta, Entity entity) {
        float oldYaw = player.getYaw();
        float oldPitch = player.getPitch();

        RotationUtils.Rotation rotation = RotationUtils.getServerRotation();
        if (rotation != null) {
            player.setYaw(rotation.yaw);
            player.setPitch(rotation.pitch);
        }

        try {
            return player.getCrosshairTarget(tickDelta, entity);
        } finally {
            player.setYaw(oldYaw);
            player.setPitch(oldPitch);
        }
    }

    @Inject(method = "updateCrosshairTarget", at = @At("RETURN"))
    private void injectGhostHand(float tickDelta, CallbackInfo ci) {
        GhostHand ghostHand = GhostHand.INSTANCE;
        if (ghostHand == null) return;
        HitResult newTarget = ghostHand.modifyCrosshairTarget(null, tickDelta);
        if (newTarget != null) {
            MinecraftClient.getInstance().crosshairTarget = newTarget;
        }
    }
}