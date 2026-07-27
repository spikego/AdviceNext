package cn.advicenext.utility.minecraft.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AttackUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static long lastAttackTime;
    private static int attackTicks;

    public static class AttackResult {
        public final boolean attacked;
        public final boolean critical;
        public final boolean sprinting;
        public final float damage;

        public AttackResult(boolean attacked, boolean critical, boolean sprinting, float damage) {
            this.attacked = attacked;
            this.critical = critical;
            this.sprinting = sprinting;
            this.damage = damage;
        }
    }

    // ==================== 冷却系统 ====================

    public static float getAttackCooldown() {
        if (mc.player == null) return 0f;
        return mc.player.getAttackCooldownProgress(0.5f);
    }

    public static boolean isAttackReady() {
        return getAttackCooldown() >= 1.0f;
    }

    public static float getAttackCooldown(float partialTicks) {
        if (mc.player == null) return 0f;
        return mc.player.getAttackCooldownProgress(partialTicks);
    }

    // ==================== 攻击执行 ====================

    public static AttackResult attack(LivingEntity entity, boolean critical, boolean sprint) {
        if (mc.player == null || mc.interactionManager == null) {
            return new AttackResult(false, false, false, 0);
        }

        if (!isAttackReady()) {
            return new AttackResult(false, false, false, 0);
        }

        boolean wasSprinting = mc.player.isSprinting();
        if (sprint) mc.player.setSprinting(true);

        boolean didCrit = false;
        if (critical && canCritical()) {
            didCrit = true;
        }

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (!sprint) mc.player.setSprinting(wasSprinting);

        float damage = calculateDamage(entity, didCrit);
        lastAttackTime = System.currentTimeMillis();
        attackTicks = 0;

        return new AttackResult(true, didCrit, sprint, damage);
    }

    public static void attackPacket(LivingEntity entity) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        mc.player.networkHandler.sendPacket(
            PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSprinting())
        );
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ==================== 暴击判断 ====================

    public static boolean canCritical() {
        if (mc.player == null) return false;
        return !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.isInLava()
            && !mc.player.hasVehicle()
            && !mc.player.isGliding()
            && mc.player.fallDistance > 0f;
    }

    public static boolean wouldCritical() {
        return canCritical();
    }

    // ==================== 伤害计算 ====================

    public static float calculateDamage(LivingEntity target, boolean critical) {
        if (mc.player == null) return 0f;

        ItemStack weapon = mc.player.getMainHandStack();
        float damage = (float) mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);

        if (critical) {
            damage *= 1.5f;
        }

        if (weapon.isIn(ItemTags.SWORDS)) {
            damage += 1.0f;
        }

        return damage;
    }

    public static float getEstimatedDamage(LivingEntity target) {
        return calculateDamage(target, canCritical());
    }

    // ==================== 射线检测 ====================

    public static Entity getRaycastEntity(double range) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d start = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(range));

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
            double dist = entityPos.distanceTo(start);
            if (dist > range) continue;

            Vec3d closest = closestPointOnRay(start, end, entityPos);
            if (closest == null) continue;

            double hitDist = closest.distanceTo(entityPos);
            if (hitDist < entity.getWidth() + 0.3) {
                return entity;
            }
        }

        return null;
    }

    public static boolean isEntityInCrosshair(LivingEntity entity) {
        Entity raycast = getRaycastEntity(mc.player.getEntityInteractionRange());
        return raycast == entity;
    }

    // ==================== 武器工具 ====================

    public static boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
    }

    public static boolean isHoldingSword() {
        return isHoldingWeapon();
    }

    public static boolean isHoldingAxe() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.AXES);
    }

    public static ItemStack getWeapon() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getMainHandStack();
    }

    // ==================== 自动格挡 ====================

    public static boolean canBlock() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
    }

    public static void startBlocking() {
        if (mc.player == null || mc.options == null) return;
        mc.options.useKey.setPressed(true);
    }

    public static void stopBlocking() {
        if (mc.player == null || mc.options == null) return;
        mc.options.useKey.setPressed(false);
    }

    // ==================== 重置 ====================

    public static void reset() {
        lastAttackTime = 0;
        attackTicks = 0;
    }

    // ==================== 私有工具 ====================

    private static Vec3d closestPointOnRay(Vec3d rayStart, Vec3d rayEnd, Vec3d point) {
        Vec3d rayDir = rayEnd.subtract(rayStart).normalize();
        double t = point.subtract(rayStart).dotProduct(rayDir);
        if (t < 0 || t > rayStart.distanceTo(rayEnd)) return null;
        return rayStart.add(rayDir.multiply(t));
    }
}