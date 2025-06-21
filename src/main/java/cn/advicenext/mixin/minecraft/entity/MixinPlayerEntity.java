package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.features.module.impl.combat.KeepSprint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookSlowVelocity(Vec3d instance, double x, double y, double z) {
        if (KeepSprint.INSTANCE.getEnabled() && MinecraftClient.getInstance().player != null) {
            x = z = KeepSprint.getMotion();
        }
        return instance.multiply(x, y, z);
    }
}
