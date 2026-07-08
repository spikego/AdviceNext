package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.render.Rotation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer<S extends LivingEntityRenderState> {
    
    @Unique
    private cn.advicenext.utility.minecraft.player.RotationUtils.Rotation getOverwriteRotation() {
        if (Rotation.INSTANCE.getEnabled() && Rotation.shouldUseServerRotation()) {
            return new cn.advicenext.utility.minecraft.player.RotationUtils.Rotation(
                Rotation.getRenderYaw(), 
                Rotation.getRenderPitch()
            );
        }
        return null;
    }

    @ModifyExpressionValue(method = "updateRenderState*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation();
        if (overwriteRotation != null) {
            return overwriteRotation.yaw;
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState*", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation();
        if (overwriteRotation != null) {
            return overwriteRotation.yaw;
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState*", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation();
        if (overwriteRotation != null) {
            return overwriteRotation.pitch;
        }

        return original;
    }
}