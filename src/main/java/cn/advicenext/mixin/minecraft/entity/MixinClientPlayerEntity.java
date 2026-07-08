package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.debug.DebugServer;
import cn.advicenext.utility.minecraft.movement.MovementUtils;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.MovementEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    
    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(float original) {
        RotationUtils.Rotation rotation = RotationUtils.getServerRotation();
        if (rotation == null || rotation.yaw == original) {
            return original;
        }
        return rotation.yaw;
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(float original) {
        RotationUtils.Rotation rotation = RotationUtils.getServerRotation();
        if (rotation == null || rotation.pitch == original) {
            return original;
        }
        return rotation.pitch;
    }
    
    @Inject(method = "tickMovementInput", at = @At("TAIL"))
    private void onMovementInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        
        MovementEvent event = new MovementEvent(player.forwardSpeed, player.sidewaysSpeed);
        EventBus.post(event);
        
        player.forwardSpeed = event.getForward();
        player.sidewaysSpeed = event.getStrafe();
    }

}