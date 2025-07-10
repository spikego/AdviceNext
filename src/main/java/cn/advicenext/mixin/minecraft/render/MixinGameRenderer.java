package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.features.module.impl.render.NoHurtCam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}