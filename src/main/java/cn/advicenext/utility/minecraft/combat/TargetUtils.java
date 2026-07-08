package cn.advicenext.utility.minecraft.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TargetUtils {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public static List<LivingEntity> getTargetsInRange(double range) {
        if (mc.world == null || mc.player == null) return List.of();
        
        Box searchBox = mc.player.getBoundingBox().expand(range);
        return mc.world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> 
            isValidTarget(entity) && mc.player.distanceTo(entity) <= range
        );
    }
    
    public static LivingEntity getClosestTarget(double range) {
        return getTargetsInRange(range).stream()
            .min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
            .orElse(null);
    }
    
    public static LivingEntity getBestTarget(double range, String priority) {
        List<LivingEntity> targets = getTargetsInRange(range);
        if (targets.isEmpty()) return null;
        
        return switch (priority.toLowerCase()) {
            case "distance" -> targets.stream()
                .min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
                .orElse(null);
            case "health" -> targets.stream()
                .min(Comparator.comparingDouble(LivingEntity::getHealth))
                .orElse(null);
            case "angle" -> targets.stream()
                .min(Comparator.comparingDouble(TargetUtils::getAngleToEntity))
                .orElse(null);
            default -> getClosestTarget(range);
        };
    }
    
    public static List<PlayerEntity> getPlayersInRange(double range) {
        if (mc.world == null || mc.player == null) return List.of();
        
        return mc.world.getPlayers().stream()
            .filter(player -> player != mc.player)
            .filter(player -> mc.player.distanceTo(player) <= range)
            .filter(TargetUtils::isValidTarget)
            .collect(Collectors.toList());
    }
    
    public static PlayerEntity getClosestPlayer(double range) {
        return getPlayersInRange(range).stream()
            .min(Comparator.comparingDouble(player -> mc.player.distanceTo(player)))
            .orElse(null);
    }
    
    public static boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive() || entity.isRemoved()) return false;
        if (entity.isInvisible() && !canSeeInvisible()) return false;
        
        return entity instanceof LivingEntity;
    }
    
    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity && entity != mc.player;
    }
    
    public static boolean isMob(Entity entity) {
        return entity instanceof Monster;
    }
    
    public static boolean isAnimal(Entity entity) {
        return entity instanceof AnimalEntity;
    }
    
    public static boolean isTeammate(PlayerEntity player) {
        if (mc.player == null) return false;
        return mc.player.isTeammate(player);
    }
    
    public static boolean isFriend(PlayerEntity player) {
        // 可以扩展为朋友系统
        return false;
    }
    
    public static boolean canAttack(LivingEntity entity) {
        if (!isValidTarget(entity)) return false;
        if (entity instanceof PlayerEntity player && (isTeammate(player) || isFriend(player))) return false;
        
        return true;
    }
    
    public static double getAngleToEntity(Entity entity) {
        if (mc.player == null) return Double.MAX_VALUE;
        
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        Vec3d direction = entityPos.subtract(playerPos).normalize();
        
        Vec3d playerLook = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();
        
        double dot = playerLook.dotProduct(direction);
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * 180.0 / Math.PI;
    }
    
    public static boolean isInFOV(Entity entity, float fov) {
        return getAngleToEntity(entity) <= fov / 2.0;
    }
    
    public static boolean hasLineOfSight(Entity entity) {
        if (mc.world == null || mc.player == null) return false;
        
        Vec3d start = mc.player.getEyePos();
        Vec3d end = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
            start, end,
            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }
    
    public static Vec3d getPredictedPosition(LivingEntity entity, double time) {
        Vec3d velocity = entity.getVelocity();
        return entity.getPos().add(velocity.multiply(time));
    }
    
    public static boolean isMoving(LivingEntity entity) {
        Vec3d velocity = entity.getVelocity();
        return velocity.lengthSquared() > 0.01;
    }
    
    public static double getHealthPercentage(LivingEntity entity) {
        return (entity.getHealth() / entity.getMaxHealth()) * 100.0;
    }
    
    public static boolean isLowHealth(LivingEntity entity, float threshold) {
        return getHealthPercentage(entity) <= threshold;
    }
    
    public static boolean isInRange(Entity entity, double range) {
        return mc.player != null && mc.player.distanceTo(entity) <= range;
    }
    
    public static boolean isInAttackRange(Entity entity) {
        return isInRange(entity, mc.player.getEntityInteractionRange());
    }
    
    private static boolean canSeeInvisible() {
        // 可以扩展为透视功能
        return false;
    }
    
    public static List<LivingEntity> sortByDistance(List<LivingEntity> entities) {
        return entities.stream()
            .sorted(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
            .collect(Collectors.toList());
    }
    
    public static List<LivingEntity> sortByHealth(List<LivingEntity> entities) {
        return entities.stream()
            .sorted(Comparator.comparingDouble(LivingEntity::getHealth))
            .collect(Collectors.toList());
    }
    
    public static List<LivingEntity> sortByAngle(List<LivingEntity> entities) {
        return entities.stream()
            .sorted(Comparator.comparingDouble(TargetUtils::getAngleToEntity))
            .collect(Collectors.toList());
    }
}