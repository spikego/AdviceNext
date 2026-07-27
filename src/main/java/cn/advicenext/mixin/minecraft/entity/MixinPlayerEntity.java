package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.utility.minecraft.player.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends net.minecraft.entity.Entity {

    public MixinPlayerEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * 击退攻击的 yaw 方向：当移动修正激活时，使用服务端 yaw。
     */
    @ModifyExpressionValue(
            method = "knockbackTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F")
    )
    private float hookKnockbackYaw(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) return original;
        if (!RotationUtils.shouldReplaceYaw()) return original;

        return RotationUtils.getServerRotation().yaw;
    }

    /**
     * 横扫攻击的 yaw 方向：当移动修正激活时，使用服务端 yaw。
     */
    @ModifyExpressionValue(
            method = "doSweepingAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F")
    )
    private float hookSweepingYaw(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) return original;
        if (!RotationUtils.shouldReplaceYaw()) return original;

        return RotationUtils.getServerRotation().yaw;
    }
}
