package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.render.Animation;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void hookSwingArm(float swingProgress, MatrixStack matrices, int i, Arm arm, CallbackInfo ci) {
        if (Animation.INSTANCE != null && Animation.INSTANCE.getEnabled()) {
            Animation.INSTANCE.applySwing(swingProgress, matrices, i, arm, ci);
        }
    }

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    private void hookEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        if (Animation.INSTANCE != null && Animation.INSTANCE.getEnabled()) {
            int i = arm == Arm.RIGHT ? 1 : -1;
            Animation.INSTANCE.applyHit(equipProgress, matrices, i, arm, ci);
        }
    }
}