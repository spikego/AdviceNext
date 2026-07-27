package cn.advicenext.mixin.minecraft.render;

import cn.advicenext.features.module.impl.render.MotionCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 动感相机实现：Camera.update 每帧算出 vanilla 目标位置后，
 * 不直接使用，而是让相机位置按所选模式向目标位置平滑过渡。
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private Vec3d pos;

    @Unique
    private Vec3d smoothedPos = null;

    @Unique
    private Vec3d springVelocity = Vec3d.ZERO;

    @Unique
    private long lastFrameTime = 0;

    @Inject(method = "update", at = @At("RETURN"))
    private void smoothCameraPosition(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                      float tickProgress, CallbackInfo ci) {
        if (!MotionCamera.isEnabled()
                || focusedEntity != MinecraftClient.getInstance().player
                || (MotionCamera.onlyThirdPerson.getValue() && !thirdPerson)) {
            smoothedPos = null;
            springVelocity = Vec3d.ZERO;
            lastFrameTime = 0;
            return;
        }

        Vec3d target = this.pos;

        // 帧时间（秒），限制上限防掉帧时瞬移
        long now = System.nanoTime();
        if (lastFrameTime == 0 || smoothedPos == null) {
            lastFrameTime = now;
            smoothedPos = target;
            springVelocity = Vec3d.ZERO;
            return;
        }
        double dt = Math.min(0.1, (now - lastFrameTime) / 1.0e9);
        lastFrameTime = now;

        if (MotionCamera.mode.is("Classic")) {
            smoothedPos = classicFollow(target, dt);
        } else {
            smoothedPos = bounceFollow(target, dt);
        }

        this.pos = smoothedPos;
    }

    /** Classic：每轴独立插值跟随，speed 为 60fps 基准的每帧比例，按帧时间归一 */
    @Unique
    private Vec3d classicFollow(Vec3d target, double dt) {
        double frames = dt * 60.0;
        double fx = followFactor(MotionCamera.speedX.getValue(), frames);
        double fy = followFactor(MotionCamera.speedY.getValue(), frames);
        double fz = followFactor(MotionCamera.speedZ.getValue(), frames);

        return new Vec3d(
            smoothedPos.x + (target.x - smoothedPos.x) * fx,
            smoothedPos.y + (target.y - smoothedPos.y) * fy,
            smoothedPos.z + (target.z - smoothedPos.z) * fz
        );
    }

    @Unique
    private double followFactor(double speedPerFrame, double frames) {
        return 1.0 - Math.pow(1.0 - speedPerFrame, frames);
    }

    /** Bounce：弹簧物理，欠阻尼时转向会过冲回弹 */
    @Unique
    private Vec3d bounceFollow(Vec3d target, double dt) {
        double k = MotionCamera.stiffness.getValue();
        double d = MotionCamera.damping.getValue();

        Vec3d accel = target.subtract(smoothedPos).multiply(k);
        springVelocity = springVelocity.add(accel.multiply(dt));
        springVelocity = springVelocity.multiply(Math.exp(-d * dt));
        Vec3d newPos = smoothedPos.add(springVelocity.multiply(dt));

        // 限制最大滞后距离，防止跑图时相机被甩太远
        double maxDist = MotionCamera.maxDistance.getValue();
        double dist = newPos.distanceTo(target);
        if (dist > maxDist) {
            newPos = target.add(newPos.subtract(target).normalize().multiply(maxDist));
        }
        return newPos;
    }
}
