package cn.advicenext.utility.minecraft.client;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class RotateUtils {
    
    public static class Rotation {
        public float yaw;
        public float pitch;
        
        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
    
    public static Rotation getRotationToEntity(Entity entity, Vec3d playerPos) {
        Vec3d entityPos = entity.getPos();
        double deltaX = entityPos.x - playerPos.x;
        double deltaY = entityPos.y - playerPos.y;
        double deltaZ = entityPos.z - playerPos.z;
        
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, distance) * 180.0 / Math.PI);
        
        return new Rotation(yaw, pitch);
    }
    
    public static Rotation getRotationToPos(Vec3d targetPos, Vec3d playerPos) {
        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;
        
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, distance) * 180.0 / Math.PI);
        
        return new Rotation(yaw, pitch);
    }
    
    public static float normalizeAngle(float angle) {
        while (angle > 180.0f) angle -= 360.0f;
        while (angle < -180.0f) angle += 360.0f;
        return angle;
    }
    
    public static Rotation smoothRotation(Rotation current, Rotation target, float speed) {
        float yawDiff = normalizeAngle(target.yaw - current.yaw);
        float pitchDiff = normalizeAngle(target.pitch - current.pitch);
        
        float newYaw = current.yaw + yawDiff * speed;
        float newPitch = current.pitch + pitchDiff * speed;
        
        return new Rotation(newYaw, Math.max(-90.0f, Math.min(90.0f, newPitch)));
    }
    
    private static Rotation serverRotation;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public static void setSilentRotation(Rotation rotation, boolean movementFix) {
        if (mc.player == null) return;
        
        serverRotation = rotation;
        
        // 发送服务端旋转数据包
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
            rotation.yaw, rotation.pitch, mc.player.isOnGround(), mc.player.isOnGround()));
        
        if (movementFix) {
            applyMovementFix(rotation);
        }
    }
    
    private static void applyMovementFix(Rotation rotation) {
        if (mc.player == null) return;
        
        // 获取当前移动输入
        float forward = 0.0f;
        float strafe = 0.0f;
        
        if (mc.options.forwardKey.isPressed()) forward += 1.0f;
        if (mc.options.backKey.isPressed()) forward -= 1.0f;
        if (mc.options.leftKey.isPressed()) strafe += 1.0f;
        if (mc.options.rightKey.isPressed()) strafe -= 1.0f;
        
        if (forward == 0.0f && strafe == 0.0f) return;
        
        // 计算角度差异
        float yawDiff = normalizeAngle(rotation.yaw - mc.player.getYaw());
        double radians = Math.toRadians(yawDiff);
        
        // 修正移动方向
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        
        double newForward = forward * cos - strafe * sin;
        double newStrafe = forward * sin + strafe * cos;
        
        // 应用修正后的移动
        Vec3d velocity = mc.player.getVelocity();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        
        if (speed > 0) {
            double motionX = newForward * speed * -Math.sin(Math.toRadians(mc.player.getYaw()));
            double motionZ = newForward * speed * Math.cos(Math.toRadians(mc.player.getYaw()));
            
            mc.player.setVelocity(motionX, velocity.y, motionZ);
        }
    }
    
    public static Rotation getServerRotation() {
        return serverRotation != null ? serverRotation : 
            new Rotation(mc.player.getYaw(), mc.player.getPitch());
    }
    
    public static void resetSilentRotation() {
        serverRotation = null;
    }
}