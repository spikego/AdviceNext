package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * 疾跑跳跃冲量方向修正：LivingEntity.jump() 在疾跑时向 getYaw() 方向
     * 额外加 0.2 的水平冲量。移动修正激活时，实际移动方向由服务端 yaw
     * 决定（R(S)·input'），冲量也必须朝服务端 yaw，否则会出现
     * "人往一个方向走、跳跃冲量往另一个方向推"的矛盾，
     * 同时保证与服务器端的移动模拟一致（斜跳速度正确）。
     */
    @ModifyExpressionValue(
            method = "jump",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F")
    )
    private float hookJumpSprintBoostYaw(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) return original;
        return MovementCorrection.correctYaw(original);
    }
}
