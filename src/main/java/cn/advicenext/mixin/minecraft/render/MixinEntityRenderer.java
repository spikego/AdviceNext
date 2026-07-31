package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.render.Nametags;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaNametag(EntityRenderState state, MatrixStack matrices,
                                       OrderedRenderCommandQueue renderQueue, CameraRenderState cameraState,
                                       CallbackInfo ci) {
        if (Nametags.INSTANCE != null && Nametags.INSTANCE.shouldCancelVanilla()) {
            ci.cancel();
        }
    }
}