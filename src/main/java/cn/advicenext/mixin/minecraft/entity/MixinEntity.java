package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public abstract Vec3d getRotationVector(float pitch, float yaw);

    /**
     * 方块/实体交互的 raycast 方向：当移动修正激活时，
     * 使用服务端 yaw/pitch 来决定视线方向，
     * 使玩家只能破坏或交互 serverYaw 看着的方块。
     */
    @Inject(method = "getRotationVec", at = @At("HEAD"), cancellable = true)
    private void hookGetRotationVec(float tickProgress, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (!RotationUtils.shouldCorrectMovement()) return;

        RotationUtils.Rotation server = RotationUtils.getServerRotation();
        Entity self = (Entity) (Object) this;
        cir.setReturnValue(self.getRotationVector(server.pitch, server.yaw));
    }

    /**
     * 移动速度修正：updateVelocity 内部调用
     * movementInputToVelocity(movementInput, speed, this.getYaw())。
     * 输入向量已在 KeyboardInput.tick 中按 (视觉 yaw − 服务端 yaw) 预旋转，
     * 此处把 getYaw() 替换为服务端 yaw，使最终速度方向与视觉一致：
     * R(S) · R(V−S) · input = R(V) · input。
     *
     * 只替换 yaw，不改动速度大小与垂直分量，保留 vanilla 全部行为
     * （走路、游泳、岩浆、鞘翅均走同一路径）。
     */
    @ModifyExpressionValue(
            method = "updateVelocity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F")
    )
    private float hookUpdateVelocityYaw(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) return original;
        return MovementCorrection.correctYaw(original);
    }
}
