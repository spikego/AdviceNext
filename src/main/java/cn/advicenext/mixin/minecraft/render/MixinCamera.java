package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.render.MotionCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    private Vec3d currentPos = Vec3d.ZERO;
    private Vec3d lastPlayerVelocity = Vec3d.ZERO;
    private Vec3d cameraVelocity = Vec3d.ZERO;
    private boolean isReturning = false;
    private static final double RETURN_THRESHOLD = 0.05;
    private static final double DEFAULT_THIRD_PERSON_DISTANCE = -3.5;
    private static final double CAMERA_HEIGHT_OFFSET = 2.5;

    // 用于存储上一帧的时间，计算delta time
    private long lastFrameTime = System.nanoTime();

    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        // Return if module disabled, no player, or in second person view
        if (!MotionCamera.isEnabled() ||
                player == null ||
                (thirdPerson && inverseView)) return;

        if (MotionCamera.isOnlyThirdPerson() && !thirdPerson) return;

        // 计算delta time，用于平滑运动
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0; // 转换为秒
        lastFrameTime = currentTime;

        // 限制deltaTime，防止帧率过低时摄像机移动过大
        deltaTime = MathHelper.clamp(deltaTime, 0.001, 0.05);

        // 基础位置：玩家位置加上高度偏移
        Vec3d basePos = new Vec3d(
                player.getX(),
                player.getY() + CAMERA_HEIGHT_OFFSET,
                player.getZ()
        );

        // 初始化摄像机位置
        if (currentPos.equals(Vec3d.ZERO)) {
            double x = -Math.sin(Math.toRadians(player.getYaw())) * DEFAULT_THIRD_PERSON_DISTANCE;
            double z = Math.cos(Math.toRadians(player.getYaw())) * DEFAULT_THIRD_PERSON_DISTANCE;
            currentPos = basePos.add(x, 0, z);
            lastPlayerVelocity = player.getVelocity();
            cameraVelocity = Vec3d.ZERO;
            return;
        }

        Vec3d playerVelocity = player.getVelocity();
        double velocityMagnitude = playerVelocity.horizontalLength();

        // 判断是否需要返回默认位置
        if (velocityMagnitude < RETURN_THRESHOLD) {
            isReturning = true;
        } else {
            isReturning = false;
            lastPlayerVelocity = playerVelocity;
        }

        Vec3d targetPos;
        double smoothingFactor;

        if (isReturning) {
            // 返回默认位置
            double x = -Math.sin(Math.toRadians(player.getYaw())) * DEFAULT_THIRD_PERSON_DISTANCE;
            double z = Math.cos(Math.toRadians(player.getYaw())) * DEFAULT_THIRD_PERSON_DISTANCE;
            targetPos = basePos.add(x, 0, z);
            smoothingFactor = 2.5; // 返回时的平滑系数
        } else {
            // 根据玩家速度计算偏移
            double offset = MotionCamera.getOffset();
            double maxOffset = MotionCamera.getMaxOffset();

            // 使用二次函数使加速更自然
            double motionOffset = Math.min(velocityMagnitude * velocityMagnitude * offset, maxOffset);

            // 计算目标位置，考虑玩家朝向和速度方向
            double yaw = player.getYaw();

            // 混合玩家朝向和移动方向，使摄像机更自然地跟随
            if (velocityMagnitude > 0.1) {
                double moveAngle = Math.toDegrees(Math.atan2(-playerVelocity.x, playerVelocity.z));
                // 权重混合，速度越大，移动方向的权重越高
                double blendFactor = Math.min(velocityMagnitude * 0.5, 0.7);
                yaw = MathHelper.lerpAngleDegrees((float)blendFactor, yaw, (float)moveAngle);
            }

            double x = -Math.sin(Math.toRadians(yaw)) * (DEFAULT_THIRD_PERSON_DISTANCE + motionOffset);
            double z = Math.cos(Math.toRadians(yaw)) * (DEFAULT_THIRD_PERSON_DISTANCE + motionOffset);

            // 添加垂直方向的偏移，根据玩家垂直速度
            double verticalOffset = playerVelocity.y * 0.5;

            targetPos = basePos.add(x, verticalOffset, z);
            smoothingFactor = 4.0 + velocityMagnitude * 2.0; // 根据速度调整平滑系数
        }

        if (MotionCamera.isSmooth()) {
            // 使用物理模拟的平滑移动
            currentPos = physicsBasedSmoothing(currentPos, targetPos, cameraVelocity, smoothingFactor, deltaTime);
        } else {
            currentPos = targetPos;
            cameraVelocity = Vec3d.ZERO;
        }

        this.setPos(currentPos.x, currentPos.y, currentPos.z);
    }

    private Vec3d physicsBasedSmoothing(Vec3d current, Vec3d target, Vec3d velocity, double smoothingFactor, double deltaTime) {
        // 弹簧-阻尼系统模拟
        double springConstant = smoothingFactor * 10.0;
        double dampingFactor = smoothingFactor * 1.2;

        // 计算弹簧力 (基于距离)
        Vec3d displacement = target.subtract(current);
        Vec3d springForce = displacement.multiply(springConstant);

        // 计算阻尼力 (基于速度)
        Vec3d dampingForce = velocity.multiply(-dampingFactor);

        // 合力
        Vec3d totalForce = springForce.add(dampingForce);

        // 更新速度 (F = ma, 假设质量为1)
        Vec3d acceleration = totalForce;
        Vec3d newVelocity = velocity.add(acceleration.multiply(deltaTime));

        // 限制最大速度，防止过冲
        double maxSpeed = 20.0;
        if (newVelocity.lengthSquared() > maxSpeed * maxSpeed) {
            newVelocity = newVelocity.normalize().multiply(maxSpeed);
        }

        // 更新位置
        Vec3d newPosition = current.add(newVelocity.multiply(deltaTime));

        // 更新全局速度变量
        cameraVelocity = newVelocity;

        return newPosition;
    }
}