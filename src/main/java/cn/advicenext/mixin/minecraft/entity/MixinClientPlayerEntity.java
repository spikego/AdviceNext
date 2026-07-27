package cn.advicenext.mixin.minecraft.entity;

import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationManager;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    @Unique
    private float visualYaw;

    @Unique
    private float visualPitch;

    // ==================== RotationManager 覆盖旋转 ====================

    /**
     * 在 sendMovementPackets 之前：
     * 1. 保存视觉 yaw/pitch
     * 2. 让旧 RotationUtils 做它的设置（如果激活）
     * 3. 用 RotationManager 的值覆盖（优先级更高）
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onPreSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        // 1. 保存视觉 yaw/pitch
        visualYaw = player.getYaw();
        visualPitch = player.getPitch();

        // 2. 让旧 RotationUtils 做它的设置（保存视觉值、设置为旧 server 值）
        RotationUtils.beforeSendMovementPackets();

        // 3. 用 RotationManager 的值覆盖（优先级更高）
        player.setYaw(RotationManager.INSTANCE.getServerYaw());
        player.setPitch(RotationManager.INSTANCE.getServerPitch());
    }

    /**
     * 在 sendMovementPackets 之后：
     * 1. 恢复视觉 yaw/pitch
     * 2. 让旧 RotationUtils 做它的恢复（无害，因为已恢复）
     */
    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onPostSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        // 1. 恢复视觉 yaw/pitch
        player.setYaw(visualYaw);
        player.setPitch(visualPitch);

        // 2. 让旧 RotationUtils 做它的恢复（如果 fakeRotation 为 true，它也会恢复）
        RotationUtils.afterSendMovementPackets();
    }

    // ==================== 移动修正：自动疾跑 ====================

    /**
     * 服务器视角前进时自动疾跑：vanilla 只在按下 sprint 键时启动疾跑。
     * 移动修正激活时，输入已被旋转到服务器视角，canStartSprinting() 中的
     * hasForwardMovement() 判断的就是"服务器视角是否前进"。此处把 sprint 键
     * 条件放宽——只要修正在服务器视角下构成前进移动，即视为冲刺意图，
     * 其余合法性检查（饥饿、失明、使用物品减速等）全部保留 vanilla 逻辑。
     */
    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PlayerInput;sprint()Z")
    )
    private boolean hookSprintKeyForCorrection(boolean original) {
        if (original) return true;
        return MovementCorrection.isActive();
    }

    // ==================== AntiAim 速度控制 ====================

    /**
     * sprint 状态同步：强制客户端 sprint=false，使 vanilla 自动发出 STOP_SPRINTING 包
     */
    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void syncSprintState(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (!RotationManager.INSTANCE.hasActiveRequest()) return;

        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    /**
     * 水平速度 cap：限制水平速度到行走速度上限（0.22 blocks/tick）
     * 保持方向不变，只降速度大小，不碰垂直速度 vy
     */
    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void capMovementSpeed(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (!RotationManager.INSTANCE.hasActiveRequest()) return;

        Vec3d v = player.getVelocity();
        double hSpeed = Math.sqrt(v.x * v.x + v.z * v.z);
        double maxSpeed = 0.22;
        if (hSpeed > maxSpeed) {
            double scale = maxSpeed / hSpeed;
            player.setVelocity(v.x * scale, v.y, v.z * scale);
        }
    }
}