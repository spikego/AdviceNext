package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.features.module.impl.movement.SafeWalk;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 移动修正入口：在 KeyboardInput.tick() 生成按键输入向量之后，
 * 将其按 (视觉 yaw − 服务端 yaw) 旋转，使后续的冲刺判断、
 * tickMovementInput、travel 全部基于修正后的输入运行。
 */
@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hookTick(CallbackInfo ci) {
        MovementCorrection.fixInput((Input) (Object) this);
        SafeWalk.clipInputIfOnEdge((Input) (Object) this);
    }
}
