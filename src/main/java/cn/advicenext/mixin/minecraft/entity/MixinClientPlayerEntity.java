package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.features.module.impl.render.Rotation;
import cn.advicenext.features.module.ModuleManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (Rotation.INSTANCE.getEnabled() && Rotation.shouldUseServerRotation()) {
            float renderYaw = Rotation.getRenderYaw();
            
            // 设置头部和身体旋转为服务端旋转
            player.headYaw = renderYaw;
            player.bodyYaw = renderYaw;
        }
    }
}