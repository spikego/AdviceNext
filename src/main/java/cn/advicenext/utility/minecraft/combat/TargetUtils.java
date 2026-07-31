package cn.advicenext.utility.minecraft.combat;

import cn.advicenext.features.module.impl.combat.AntiBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TargetUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum TargetPriority {
        DISTANCE, HEALTH, ANGLE, HURT_TIME, ARMOR
    }

    public enum TargetFilter {
        PLAYERS, MOBS, ANIMALS, ALL
    }

    // ==================== 核心获取 ====================

    public static List<LivingEntity> getTargets(double range, TargetFilter filter) {
        if (mc.world == null || mc.player == null) return List.of();

        Box searchBox = mc.player.getBoundingBox().expand(range);
        return mc.world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> {
            if (!isValidTarget(entity)) return false;
            if (mc.player.distanceTo(entity) > range) return false;
            if (entity instanceof PlayerEntity p && AntiBot.isBotStatic(p)) return false;
            return matchesFilter(entity, filter);
        });
    }

    public static List<LivingEntity> getTargets(double range, TargetFilter filter, Predicate<LivingEntity> extra) {
        return getTargets(range, filter).stream().filter(extra).collect(Collectors.toList());
    }

    public static LivingEntity getBestTarget(double range, TargetFilter filter, TargetPriority priority) {
        return getBestTarget(range, filter, priority, e -> true);
    }

    public static LivingEntity getBestTarget(double range, TargetFilter filter, TargetPriority priority, Predicate<LivingEntity> extra) {
        List<LivingEntity> targets = getTargets(range, filter, extra);
        if (targets.isEmpty()) return null;

        return switch (priority) {
            case DISTANCE -> targets.stream().min(Comparator.comparingDouble(e -> mc.player.distanceTo(e))).orElse(null);
            case HEALTH -> targets.stream().min(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            case ANGLE -> targets.stream().min(Comparator.comparingDouble(TargetUtils::getAngleToEntity)).orElse(null);
            case HURT_TIME -> targets.stream().max(Comparator.comparingInt(e -> e.hurtTime)).orElse(null);
            case ARMOR -> targets.stream().min(Comparator.comparingInt(e -> e instanceof PlayerEntity p ? p.getArmor() : 20)).orElse(null);
        };
    }

    // ==================== 玩家目标 ====================

    public static List<PlayerEntity> getPlayers(double range) {
        if (mc.world == null || mc.player == null) return List.of();
        return mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && p.isAlive())
            .filter(p -> mc.player.distanceTo(p) <= range)
            .filter(p -> !p.isInvisible())
            .filter(p -> !AntiBot.isBotStatic(p))
            .collect(Collectors.toList());
    }

    public static List<PlayerEntity> getEnemyPlayers(double range) {
        return getPlayers(range).stream()
            .filter(p -> !isTeammate(p) && !isFriend(p))
            .collect(Collectors.toList());
    }

    public static PlayerEntity getClosestPlayer(double range) {
        return getEnemyPlayers(range).stream()
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    // ==================== FOV / 角度 ====================

    public static double getAngleToEntity(Entity entity) {
        if (mc.player == null) return Double.MAX_VALUE;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
        Vec3d direction = entityPos.subtract(playerPos).normalize();

        Vec3d playerLook = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();

        double dot = playerLook.dotProduct(direction);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }

    public static boolean isInFOV(Entity entity, float fov) {
        return getAngleToEntity(entity) <= fov / 2.0;
    }

    // ==================== 视线 / 墙体 ====================

    public static boolean hasLineOfSight(Entity entity) {
        if (mc.world == null || mc.player == null) return false;

        Vec3d start = mc.player.getEyePos();
        Vec3d end = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());

        return mc.world.raycast(new RaycastContext(
            start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    public static boolean canSee(Entity entity) {
        return hasLineOfSight(entity);
    }

    // ==================== 预测 / 追踪 ====================

    public static Vec3d getPredictedPosition(LivingEntity entity, int ticksAhead) {
        Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d velocity = entity.getVelocity();
        return pos.add(velocity.multiply(ticksAhead * 0.05));
    }

    public static Vec3d getPredictedHitPos(LivingEntity entity, int ticksAhead) {
        Vec3d base = getPredictedPosition(entity, ticksAhead);
        return new Vec3d(base.x, base.y + entity.getHeight() * 0.6, base.z);
    }

    // ==================== 条件判断 ====================

    public static boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive() || entity.isRemoved()) return false;
        return entity instanceof LivingEntity;
    }

    public static boolean canAttack(LivingEntity entity) {
        if (!isValidTarget(entity)) return false;
        if (entity instanceof PlayerEntity p && (isTeammate(p) || isFriend(p))) return false;
        if (entity instanceof TameableEntity t && t.getOwner() == mc.player) return false;
        return true;
    }

    public static boolean isTeammate(PlayerEntity player) {
        if (mc.player == null) return false;
        return mc.player.isTeammate(player);
    }

    public static boolean isFriend(PlayerEntity player) {
        return false;
    }

    public static boolean isInRange(Entity entity, double range) {
        return mc.player != null && mc.player.distanceTo(entity) <= range;
    }

    public static boolean isInAttackRange(Entity entity) {
        return mc.player != null && mc.player.distanceTo(entity) <= mc.player.getEntityInteractionRange();
    }

    public static boolean isMoving(LivingEntity entity) {
        return entity.getVelocity().lengthSquared() > 0.001;
    }

    public static boolean isLowHealth(LivingEntity entity, float threshold) {
        return entity.getHealth() / entity.getMaxHealth() * 100f <= threshold;
    }

    public static double getHealthPercentage(LivingEntity entity) {
        return entity.getHealth() / entity.getMaxHealth() * 100.0;
    }

    // ==================== 实体类型判断 ====================

    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity && entity != mc.player;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof Monster;
    }

    public static boolean isAnimal(Entity entity) {
        return entity instanceof AnimalEntity;
    }

    private static boolean matchesFilter(Entity entity, TargetFilter filter) {
        return switch (filter) {
            case PLAYERS -> entity instanceof PlayerEntity;
            case MOBS -> entity instanceof Monster;
            case ANIMALS -> entity instanceof AnimalEntity;
            case ALL -> true;
        };
    }

    // ==================== 排序 ====================

    public static List<LivingEntity> sortByDistance(List<LivingEntity> entities) {
        List<LivingEntity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        return sorted;
    }

    public static List<LivingEntity> sortByHealth(List<LivingEntity> entities) {
        List<LivingEntity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        return sorted;
    }

    public static List<LivingEntity> sortByAngle(List<LivingEntity> entities) {
        List<LivingEntity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingDouble(TargetUtils::getAngleToEntity));
        return sorted;
    }

    public static List<LivingEntity> sortByHurtTime(List<LivingEntity> entities) {
        List<LivingEntity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingInt((LivingEntity e) -> e.hurtTime).reversed());
        return sorted;
    }
}