package cn.advicenext.utility.minecraft.player;

import cn.advicenext.utility.minecraft.movement.MovementUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class RotationUtils {
    
    public static class Rotation {
        public float yaw;
        public float pitch;
        
        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
    
    public static class RotationData {
        private Rotation rotation;
        private MovementUtils.MovementCorrection movementCorrection;
        private boolean silent;
        private float smoothness;
        private long timestamp;
        
        public RotationData(Rotation rotation, MovementUtils.MovementCorrection movementCorrection, boolean silent, float smoothness) {
            this.rotation = rotation;
            this.movementCorrection = movementCorrection;
            this.silent = silent;
            this.smoothness = smoothness;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Rotation getRotation() { return rotation; }
        public MovementUtils.MovementCorrection getMovementCorrection() { return movementCorrection; }
        public boolean isSilent() { return silent; }
        public float getSmoothness() { return smoothness; }
        public long getTimestamp() { return timestamp; }
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
    private static MovementUtils.MovementCorrection currentCorrection = MovementUtils.MovementCorrection.OFF;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private static long lastPacketTime = 0;
    
    public static void setSilentRotation(Rotation rotation, MovementUtils.MovementCorrection movementCorrection) {
        if (mc.player == null) return;
        
        serverRotation = rotation;
        currentCorrection = movementCorrection;
        
        // Limit packet sending to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPacketTime > 50) { // Max 20 packets per second
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                rotation.yaw, rotation.pitch, mc.player.isOnGround(), mc.player.isOnGround()));
            lastPacketTime = currentTime;
        }
        
        if (movementCorrection != MovementUtils.MovementCorrection.OFF) {
            applyMovementFix(rotation);
        }
    }

    private static void applyMovementFix(Rotation rotation) {
        if(MovementUtils.MovementCorrection.valueOf(currentCorrection.name()) == MovementUtils.MovementCorrection.SILENT) {
            if (mc.player == null) return;

            // 获取原始移动输入
            float forward = 0;
            float strafe = 0;

            if (mc.options.forwardKey.isPressed()) forward += 1;
            if (mc.options.backKey.isPressed()) forward -= 1;
            if (mc.options.leftKey.isPressed()) strafe += 1;
            if (mc.options.rightKey.isPressed()) strafe -= 1;
        }
    }


    public static Rotation getServerRotation() {
        return serverRotation;
    }
    
    public static void resetSilentRotation() {
        serverRotation = null;
        currentCorrection = MovementUtils.MovementCorrection.OFF;
    }
    
    public static MovementUtils.MovementCorrection getMovementCorrection() {
        return currentCorrection;
    }
    
    public static Rotation getRotationToEntity(Entity entity) {
        return getRotationToEntity(entity, mc.player.getPos());
    }
    
    public static Rotation getRotationToPos(Vec3d targetPos) {
        return getRotationToPos(targetPos, mc.player.getPos());
    }
    
    public static void setSilentRotation(Rotation rotation) {
        setSilentRotation(rotation, MovementUtils.MovementCorrection.SILENT);
    }
    
    public static void setRotation(RotationData data) {
        if (data.isSilent()) {
            setSilentRotation(data.getRotation(), data.getMovementCorrection());
        } else {
            mc.player.setYaw(data.getRotation().yaw);
            mc.player.setPitch(data.getRotation().pitch);
            if (data.getMovementCorrection() == MovementUtils.MovementCorrection.STRICT) {
                applyMovementFix(data.getRotation());
            }
        }
    }
}