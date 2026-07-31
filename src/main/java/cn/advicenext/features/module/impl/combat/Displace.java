package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.*;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;

import java.util.*;

public class Displace extends Module {

    public static Displace INSTANCE;

    private static final int DISPLACE_WINDOW_TICKS = 10;
    private static final int VOID_SCAN_DIRECTIONS = 32;
    private static final int VOID_SCAN_RINGS = 12;
    private static final int VOID_SCAN_DEPTH = 10;
    private static final double VOID_SCAN_STEP = 0.5;
    private static final double DYNAMIC_SCAN_STEP = 0.5;
    private static final double DYNAMIC_SCAN_DISTANCE = 6.0;
    private static final double DYNAMIC_SCAN_SIDE_STEP = 0.45;
    private static final double DYNAMIC_WALL_CHECK_STEP = 0.25;
    private static final double DYNAMIC_COLLISION_INSET = 0.03;
    private static final long ARROW_FADE_MS = 250;
    private static final double ARROW_FORWARD_GAP = 0.24;
    private static final double ARROW_BODY_LENGTH = 0.74;
    private static final double ARROW_BODY_HALF_HEIGHT = 0.08;
    private static final double ARROW_HEAD_BACKSET = 0.18;
    private static final double ARROW_HEAD_LENGTH = 0.52;
    private static final double ARROW_HEAD_HALF_HEIGHT = 0.30;

    private static final double[] VOID_SCAN_X = new double[VOID_SCAN_DIRECTIONS];
    private static final double[] VOID_SCAN_Z = new double[VOID_SCAN_DIRECTIONS];

    private final DoubleSetting yawOffset = new DoubleSetting("YawOffset", "Yaw offset angle", 90.0, 180.0, 0.0, 1.0);
    private final IntSetting delay = new IntSetting("Delay", "Delay between displaces (ms)", 0, 500, 0, 50);
    private final ModeSetting displaceMode = new ModeSetting("DisplaceMode", "Displace direction mode",
            "Left", List.of("Left", "Right"));
    private final ModeSetting angleMode = new ModeSetting("AngleMode", "How to find displace angle",
            "Static", List.of("Static", "Dynamic"));
    private final BooleanSetting showDirection = new BooleanSetting("ShowDirection", "Show displace arrow", true);
    private final BooleanSetting findVoid = new BooleanSetting("FindVoid", "Find void positions", false);
    private final BooleanSetting blink = new BooleanSetting("Blink", "Blink during displace", false);
    private final BooleanSetting ignoreTeammates = new BooleanSetting("IgnoreTeammates", "Ignore teammates", true);
    private final BooleanSetting hasKnockback = new BooleanSetting("HasKnockback", "Require knockback item", false);

    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean blinkActive = false;
    private Float dynamicVoidYaw = null;
    private Float renderDisplaceYaw = null;
    private PlayerEntity renderTarget = null;
    private Float fadingDisplaceYaw = null;
    private PlayerEntity fadingTarget = null;
    private long arrowFadeStartMs = 0;
    private Float lastRenderedDisplaceYaw = null;
    private PlayerEntity lastRenderedTarget = null;
    private long lastRenderedArrowMs = 0;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    static {
        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double angle = Math.PI * 2.0 * i / VOID_SCAN_DIRECTIONS;
            VOID_SCAN_X[i] = Math.cos(angle);
            VOID_SCAN_Z[i] = Math.sin(angle);
        }
    }

    public Displace() {
        super("Displace", "Displaces you to the side of your target", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(angleMode);
        this.settings.add(yawOffset);
        this.settings.add(delay);
        this.settings.add(displaceMode);
        this.settings.add(showDirection);
        this.settings.add(findVoid);
        this.settings.add(blink);
        this.settings.add(ignoreTeammates);
        this.settings.add(hasKnockback);
    }

    public String getInfo() {
        int ms = delay.getValue();
        return ms + "ms";
    }

    @Override
    public void onEnable() {
        displaceThisTick = false;
        active = false;
        hasKB = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        blinkActive = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
        clearArrowState();
        tickCounter = 0;
        targetWindowStartTicks.clear();
        RotationUtils.resetSilentRotation();
    }

    @Override
    public void onDisable() {
        active = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        blinkActive = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
        clearArrowState();
        targetWindowStartTicks.clear();
        RotationUtils.resetSilentRotation();
    }

    private static int msToTicks(double ms) {
        if (ms <= 0.0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }

    private boolean anyMovementKey() {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    private boolean isDynamicAngle() {
        return angleMode.getValue().equals("Dynamic");
    }

    private Float findStaticVoidYaw(PlayerEntity target) {
        if (target == null || mc.player == null || mc.world == null) return null;

        double bestX = 0.0, bestZ = 0.0;
        double bestScore = Double.MAX_VALUE;

        for (int ring = 1; ring <= VOID_SCAN_RINGS; ring++) {
            double radius = ring * VOID_SCAN_STEP;
            boolean foundInRing = false;

            for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
                double x = target.getX() + VOID_SCAN_X[i] * radius;
                double z = target.getZ() + VOID_SCAN_Z[i] * radius;
                if (!isVoidColumn(x, target.getY(), z)) continue;

                double playerDx = x - mc.player.getX();
                double playerDz = z - mc.player.getZ();
                double playerDistSq = playerDx * playerDx + playerDz * playerDz;
                double score = radius * radius * 1000.0 + playerDistSq;
                if (score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                    foundInRing = true;
                }
            }

            if (foundInRing) break;
        }

        if (bestScore == Double.MAX_VALUE) return null;

        updateDisplaceSide(target, bestX, bestZ);

        double dx = bestX - target.getX();
        double dz = bestZ - target.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return null;

        double aimRadius = Math.min(dist, Math.max(0.35, target.getWidth() * 0.5 + 0.15));
        double aimX = target.getX() + dx / dist * aimRadius;
        double aimZ = target.getZ() + dz / dist * aimRadius;
        Vec3d eyes = mc.player.getEyePos();
        return RotationUtils.getRotationToPos(new Vec3d(aimX, target.getY() + target.getEyeHeight(target.getPose()) * 0.5, aimZ), eyes).yaw;
    }

    private Float findDynamicVoidYaw(PlayerEntity target) {
        if (target == null || mc.player == null || mc.world == null) return null;

        double bestForwardX = 0.0, bestForwardZ = 0.0;
        double bestScore = 0.0;

        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double forwardX = VOID_SCAN_X[i];
            double forwardZ = VOID_SCAN_Z[i];
            double score = scoreVoidPath(target, forwardX, forwardZ);

            if (score > bestScore) {
                bestScore = score;
                bestForwardX = forwardX;
                bestForwardZ = forwardZ;
            }
        }

        if (bestScore <= 0.0) return null;

        updateDisplaceSide(target, target.getX() + bestForwardX, target.getZ() + bestForwardZ);
        return yawFromForward(bestForwardX, bestForwardZ);
    }

    private float yawFromForward(double forwardX, double forwardZ) {
        return (float) (Math.toDegrees(Math.atan2(forwardZ, forwardX)) - 90.0);
    }

    private double scoreVoidPath(PlayerEntity target, double forwardX, double forwardZ) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double score = 0.0;
        double checkedForward = 0.0;
        int consecutiveCenterVoid = 0;
        Box baseCollisionBox = target.getBoundingBox().contract(DYNAMIC_COLLISION_INSET, 0.0, DYNAMIC_COLLISION_INSET);

        for (int step = 1; step <= (int) (DYNAMIC_SCAN_DISTANCE / DYNAMIC_SCAN_STEP); step++) {
            double forward = step * DYNAMIC_SCAN_STEP;
            if (!isDynamicPathClear(target, baseCollisionBox, forwardX, forwardZ, checkedForward, forward)) break;
            checkedForward = forward;

            boolean centerVoid = false;

            for (int side = -1; side <= 1; side++) {
                double sideOffset = side * DYNAMIC_SCAN_SIDE_STEP;
                double x = target.getX() + forwardX * forward + sideX * sideOffset;
                double z = target.getZ() + forwardZ * forward + sideZ * sideOffset;
                if (isVoidColumn(x, target.getY(), z)) {
                    double laneWeight = side == 0 ? 1.4 : 1.0;
                    score += laneWeight * (DYNAMIC_SCAN_DISTANCE + DYNAMIC_SCAN_STEP - forward);
                    centerVoid |= side == 0;
                }
            }

            if (centerVoid) {
                consecutiveCenterVoid++;
                score += consecutiveCenterVoid * 2.0;
            } else {
                consecutiveCenterVoid = 0;
            }
        }

        return score;
    }

    private boolean isDynamicPathClear(PlayerEntity target, Box baseCollisionBox, double forwardX, double forwardZ, double fromForward, double toForward) {
        for (double forward = fromForward + DYNAMIC_WALL_CHECK_STEP; forward <= toForward + 1.0E-4; forward += DYNAMIC_WALL_CHECK_STEP) {
            Box checkBox = baseCollisionBox.offset(forwardX * forward, 0.0, forwardZ * forward);
            if (hasBlockCollision(checkBox)) return false;
        }
        return true;
    }

    private boolean hasBlockCollision(Box box) {
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX + 1.0);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY + 1.0);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ + 1.0);

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        for (int blockX = minX; blockX < maxX; blockX++) {
            for (int blockZ = minZ; blockZ < maxZ; blockZ++) {
                if (!mc.world.isChunkLoaded(blockX >> 4, blockZ >> 4)) return true;

                for (int blockY = minY; blockY < maxY; blockY++) {
                    if (blockY < mc.world.getBottomY() || blockY > mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, blockX, blockZ)) return true;

                    blockPos.set(blockX, blockY, blockZ);
                    VoxelShape shape = mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos);
                    if (!shape.isEmpty()) {
                        Box offset = box.offset(-blockX, -blockY, -blockZ);
                        if (shape.getBoundingBoxes().stream().anyMatch(s -> s.intersects(offset))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isVoidColumn(double x, double y, double z) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = MathHelper.floor(y) - 1;
        int endY = Math.max(mc.world.getBottomY(), startY - VOID_SCAN_DEPTH);

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int blockY = startY; blockY >= endY; blockY--) {
            pos.set(blockX, blockY, blockZ);
            if (!mc.world.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    private void updateDisplaceSide(PlayerEntity target, double voidX, double voidZ) {
        double targetDx = target.getX() - mc.player.getX();
        double targetDz = target.getZ() - mc.player.getZ();
        double voidDx = voidX - mc.player.getX();
        double voidDz = voidZ - mc.player.getZ();
        double cross = targetDx * voidDz - targetDz * voidDx;
        displaceLeft = cross < 0.0;
    }

    private float getFixedDisplaceYaw() {
        float baseYaw = RotationUtils.getServerYaw();
        float offset = yawOffset.getValue().floatValue();
        return displaceLeft ? baseYaw - offset : baseYaw + offset;
    }

    private void clearActiveState() {
        startArrowFade();
        active = false;
        displaceThisTick = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        blinkActive = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
        RotationUtils.resetSilentRotation();
    }

    private void clearFadingArrow() {
        fadingDisplaceYaw = null;
        fadingTarget = null;
        arrowFadeStartMs = 0;
    }

    private void clearArrowState() {
        clearFadingArrow();
        lastRenderedDisplaceYaw = null;
        lastRenderedTarget = null;
        lastRenderedArrowMs = 0;
    }

    private void startArrowFade() {
        long nowMs = System.currentTimeMillis();
        if (lastRenderedDisplaceYaw != null && lastRenderedTarget != null && !lastRenderedTarget.isDead()
                && nowMs - lastRenderedArrowMs <= ARROW_FADE_MS) {
            fadingDisplaceYaw = lastRenderedDisplaceYaw;
            fadingTarget = lastRenderedTarget;
            arrowFadeStartMs = nowMs;
        }
        lastRenderedDisplaceYaw = null;
        lastRenderedTarget = null;
        lastRenderedArrowMs = 0;
    }

    private void pruneTargetDelayStates() {
        if (mc.world == null) {
            targetWindowStartTicks.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = targetWindowStartTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            if (mc.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(PlayerEntity target, int currentTick) {
        if (target == null) return true;

        int targetId = target.getId();
        Integer windowStartTick = targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }

        int delayTicks = msToTicks(delay.getValue());
        if (delayTicks <= 0) return true;

        int elapsed = currentTick - windowStartTick;
        return elapsed >= delayTicks;
    }

    private boolean hasKnockbackEnchant() {
        ItemStack stack = mc.player.getMainHandStack();
        return EnchantmentHelper.getLevel(
                mc.world.getRegistryManager()
                        .getOrThrow(RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.KNOCKBACK),
                stack) > 0;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            clearActiveState();
            return;
        }

        tickCounter++;
        int currentTick = tickCounter;
        pruneTargetDelayStates();

        boolean passesItemCondition = true;
        if (hasKnockback.getValue()) {
            passesItemCondition = hasKnockbackEnchant() || anyMovementKey();
        }
        if (!passesItemCondition) {
            clearActiveState();
            return;
        }

        PlayerEntity target = null;
        boolean attacking = mc.options.attackKey.isPressed()
                || (KillAura.INSTANCE != null && KillAura.INSTANCE.getEnabled() && KillAura.INSTANCE.getTarget() != null);
        if (attacking) {
            target = TargetUtils.getEnemyPlayers(9.0).stream()
                    .filter(p -> !ignoreTeammates.getValue() || !isTeammate(p))
                    .findFirst().orElse(null);
        }

        boolean hasKBEnchant = hasKnockbackEnchant();
        active = target != null;
        if (!active) {
            clearActiveState();
            return;
        }

        dynamicVoidYaw = isDynamicAngle()
                ? findDynamicVoidYaw(target)
                : findVoid.getValue() ? findStaticVoidYaw(target) : null;
        if (dynamicVoidYaw == null && !isDynamicAngle()) {
            displaceLeft = displaceMode.getValue().equals("Left");
        }
        renderDisplaceYaw = dynamicVoidYaw != null ? dynamicVoidYaw : isDynamicAngle() ? null : getFixedDisplaceYaw();
        renderTarget = renderDisplaceYaw != null ? target : null;
        if (renderDisplaceYaw == null) {
            clearActiveState();
            return;
        }

        hasKB = hasKBEnchant;
        displaceThisTick = !displaceThisTick;
        if (displaceThisTick && !shouldDisplaceInCurrentWindow(target, currentTick)) {
            startArrowFade();
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            blinkActive = false;
            dynamicVoidYaw = null;
            renderDisplaceYaw = null;
            renderTarget = null;
            RotationUtils.resetSilentRotation();
            return;
        }

        wasDisplacingLastTick = displaceThisTick;

        if (!displaceThisTick || renderDisplaceYaw == null) {
            RotationUtils.resetSilentRotation();
            return;
        }

        RotationUtils.setSilentRotation(
                new RotationUtils.Rotation(renderDisplaceYaw, mc.player.getPitch()),
                MovementCorrection.Mode.SILENT);
        blinkActive = blink.getValue();
    }

    private boolean isTeammate(PlayerEntity player) {
        if (mc.player == null) return false;
        if (mc.player.isTeammate(player)) return true;
        String playerName = player.getName().getString().toLowerCase();
        String selfName = mc.player.getName().getString().toLowerCase();
        return playerName.equals(selfName);
    }

    @Override
    public void onMovement(MovementEvent event) {
        if (!active) {
            compensateNextTick = false;
            return;
        }

        if (compensateNextTick && !displaceThisTick) {
            compensateNextTick = false;
            if (displaceLeft) {
                event.setStrafe(-1);
            } else {
                event.setStrafe(1);
            }
            return;
        }

        if (!displaceThisTick || hasKB) return;
        if (!anyMovementKey()) return;

        event.setForward(1);
        compensateNextTick = true;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!blinkActive || !displaceThisTick) return;
        if (event.getOrigin() != PacketEvent.TransferOrigin.SEND) return;
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket)) return;

        event.cancelled = true;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!showDirection.getValue()) {
            clearArrowState();
            return;
        }

        long nowMs = System.currentTimeMillis();
        boolean activeArrow = active && renderDisplaceYaw != null && renderTarget != null && !renderTarget.isDead();
        Float arrowYaw = renderDisplaceYaw;
        PlayerEntity arrowTarget = renderTarget;
        float alpha = 1.0F;

        if (activeArrow) {
            clearFadingArrow();
        } else {
            if (fadingDisplaceYaw == null || fadingTarget == null || fadingTarget.isDead()) {
                clearFadingArrow();
                return;
            }

            long fadeElapsedMs = nowMs - arrowFadeStartMs;
            if (fadeElapsedMs >= ARROW_FADE_MS) {
                clearFadingArrow();
                return;
            }

            arrowYaw = fadingDisplaceYaw;
            arrowTarget = fadingTarget;
            alpha = 1.0F - (float) fadeElapsedMs / (float) ARROW_FADE_MS;
        }

        float partialTicks = event.getTickDelta();
        double centerX = MathHelper.lerp(partialTicks, arrowTarget.lastRenderX, arrowTarget.getX());
        double centerY = MathHelper.lerp(partialTicks, arrowTarget.lastRenderY, arrowTarget.getY()) + arrowTarget.getHeight() * 0.5;
        double centerZ = MathHelper.lerp(partialTicks, arrowTarget.lastRenderZ, arrowTarget.getZ());

        double yawRad = Math.toRadians(arrowYaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        double baseOffset = arrowTarget.getWidth() * 0.5 + ARROW_FORWARD_GAP;
        double tailX = centerX + forwardX * baseOffset;
        double tailZ = centerZ + forwardZ * baseOffset;
        double bodyEndX = tailX + forwardX * ARROW_BODY_LENGTH;
        double bodyEndZ = tailZ + forwardZ * ARROW_BODY_LENGTH;
        double headBackX = tailX + forwardX * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double headBackZ = tailZ + forwardZ * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double tipX = bodyEndX + forwardX * ARROW_HEAD_LENGTH;
        double tipZ = bodyEndZ + forwardZ * ARROW_HEAD_LENGTH;

        CameraRenderState cameraRenderState = event.getCameraRenderState();
        if (cameraRenderState == null) return;
        Vec3d camPos = cameraRenderState.pos;
        double viewerX = camPos.x;
        double viewerY = camPos.y;
        double viewerZ = camPos.z;

        drawFilledArrow(event, centerY, tailX, tailZ, bodyEndX, bodyEndZ, headBackX, headBackZ, tipX, tipZ,
                viewerX, viewerY, viewerZ, alpha);

        if (activeArrow) {
            lastRenderedDisplaceYaw = arrowYaw;
            lastRenderedTarget = arrowTarget;
            lastRenderedArrowMs = nowMs;
        }
    }

    private void drawFilledArrow(Render3DEvent event, double centerY,
                                   double tailX, double tailZ,
                                   double bodyEndX, double bodyEndZ,
                                   double headBackX, double headBackZ,
                                   double tipX, double tipZ,
                                   double viewerX, double viewerY, double viewerZ, float alpha) {
        float halfBody = (float) ARROW_BODY_HALF_HEIGHT;
        float halfHead = (float) ARROW_HEAD_HALF_HEIGHT;

        int color = ((int)(255 * 0.85f * alpha) << 24) | 0xFFFFFF;

        VertexConsumer vc = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        MatrixStack matrices = event.getMatrices();
        CameraRenderState cam = event.getCameraRenderState();

        int steps = 60;
        for (int s = 0; s <= steps; s++) {
            float t = (float) s / steps;
            float yOffset = -halfHead + t * halfHead * 2.0f;
            float absOff = Math.abs(yOffset);

            double leftX, leftZ;
            if (absOff <= halfBody) {
                float bodyWeight = halfBody > 0.001f ? (halfBody - absOff) / halfBody : 0.0f;
                leftX = tailX + (bodyEndX - tailX) * (1.0 - bodyWeight);
                leftZ = tailZ + (bodyEndZ - tailZ) * (1.0 - bodyWeight);
            } else {
                leftX = bodyEndX;
                leftZ = bodyEndZ;
            }

            float headWeight = halfHead > 0.001f ? (halfHead - absOff) / halfHead : 0.0f;
            double rightX = headBackX + (tipX - headBackX) * headWeight;
            double rightZ = headBackZ + (tipZ - headBackZ) * headWeight;

            Vec3d a = new Vec3d(leftX, centerY + yOffset, leftZ);
            Vec3d b = new Vec3d(rightX, centerY + yOffset, rightZ);
            Render3DEngine.drawLine3D(matrices, vc, cam, a, b, color, 1.0F);
        }
    }

    public boolean isDisplacing() {
        return this.getEnabled() && active && displaceThisTick;
    }

    @Override
    public String getDisplayValue() {
        return yawOffset.getValue()+" deg";
    }
}