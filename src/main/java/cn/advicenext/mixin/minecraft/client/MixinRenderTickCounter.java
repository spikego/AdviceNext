package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.utility.minecraft.client.TimerUtils;
import net.minecraft.client.render.RenderTickCounter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class MixinRenderTickCounter {
    @Shadow
    private float dynamicDeltaTicks;

    @Shadow public abstract float getDynamicDeltaTicks();

    @Inject(at = {
            @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", opcode = Opcodes.PUTFIELD, ordinal = 0) },
            method = {"beginRenderTick(J)I"})
    public void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        // 直接设置为timerSpeed，这样在之前有效果的情况下应该仍然有效
        if(TimerUtils.getTimerSpeed() != 1.0f) {
            this.dynamicDeltaTicks = getDynamicDeltaTicks() * (float) TimerUtils.getTimerSpeed();
        }
    }
}

