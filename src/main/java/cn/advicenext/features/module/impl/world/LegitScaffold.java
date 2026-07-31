package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.MovementEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.world.BlockUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Random;

public class LegitScaffold extends Module {

    public static LegitScaffold INSTANCE;

    private final DoubleSetting minEdgeDistance = new DoubleSetting("MinEdgeDist", "Minimum edge distance", 0.4, 1.3, 0.01, 0.01);
    private final DoubleSetting maxEdgeDistance = new DoubleSetting("MaxEdgeDist", "Maximum edge distance", 0.6, 1.3, 0.01, 0.01);
    private final ModeSetting conditions = new ModeSetting("Conditions", "Conditions to activate",
            "OnGround", List.of("OnGround", "HoldingBlocks", "OnGround+HoldingBlocks", "Always"));

    private final Random random = new Random();
    private float currentEdgeDistance;
    private boolean wasSneaking;
    private boolean sneakCaptured;

    public LegitScaffold() {
        super("LegitScaffold", "Eagle sneaking at block edges (legit)", Category.WORLD);
        INSTANCE = this;
        currentEdgeDistance = getRandomEdgeDistance();
        this.settings.add(minEdgeDistance);
        this.settings.add(maxEdgeDistance);
        this.settings.add(conditions);
    }

    private float getRandomEdgeDistance() {
        float min = minEdgeDistance.getValue().floatValue();
        float max = maxEdgeDistance.getValue().floatValue();
        if (min >= max) return min;
        return min + random.nextFloat() * (max - min);
    }

    @Override
    public void onDisable() {
        wasSneaking = false;
        sneakCaptured = false;
        super.onDisable();
    }

    @Override
    public void onMovement(MovementEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.getAbilities().flying) return;

        boolean conditionsMet = checkConditions();
        boolean originalSneak = event.isOriginalSneak();
        boolean isActive = shouldActivateEagle(event, conditionsMet);

        updateSneakCapture(originalSneak, isActive);

        if (conditionsMet) {
            if (sneakCaptured) {
                event.setSneak(true);
            } else {
                event.setSneak(originalSneak || isActive);
            }
        }

        updateSneakState(event.isSneak());
    }

    private boolean checkConditions() {
        String cond = conditions.getValue();
        boolean onGround = mc.player.isOnGround();
        boolean holdingBlocks = isHoldingBlocks();

        return switch (cond) {
            case "OnGround" -> onGround;
            case "HoldingBlocks" -> holdingBlocks;
            case "OnGround+HoldingBlocks" -> onGround && holdingBlocks;
            case "Always" -> true;
            default -> onGround;
        };
    }

    private boolean isHoldingBlocks() {
        return mc.player.getMainHandStack().getItem() instanceof BlockItem
                && BlockUtils.isValidBlock(((BlockItem) mc.player.getMainHandStack().getItem()).getBlock());
    }

    private boolean shouldActivateEagle(MovementEvent event, boolean conditionsMet) {
        if (!conditionsMet) return false;

        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return false;

        return isCloseToEdge(forward, strafe, currentEdgeDistance);
    }

    private boolean isCloseToEdge(float forward, float strafe, float edgeDistance) {
        Box box = mc.player.getBoundingBox();
        double minX = box.minX;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxZ = box.maxZ;

        double blockMinX = Math.floor(minX);
        double blockMinZ = Math.floor(minZ);
        double blockMaxX = Math.floor(maxX);
        double blockMaxZ = Math.floor(maxZ);

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos below = playerPos.down();

        if (mc.world.getBlockState(below).isAir()) return false;

        double epsilon = 0.001;
        double dist = edgeDistance;

        if (forward > 0) {
            if (Math.abs(maxZ - (blockMaxZ + 1.0)) < dist + epsilon) return true;
        } else if (forward < 0) {
            if (Math.abs(minZ - blockMinZ) < dist + epsilon) return true;
        }

        if (strafe > 0) {
            if (Math.abs(minX - blockMinX) < dist + epsilon) return true;
        } else if (strafe < 0) {
            if (Math.abs(maxX - (blockMaxX + 1.0)) < dist + epsilon) return true;
        }

        return false;
    }

    private void updateSneakCapture(boolean originalSneak, boolean active) {
        if (!conditions.is("Always") && !conditions.is("OnGround+HoldingBlocks")) {
            sneakCaptured = false;
            return;
        }

        if (!sneakCaptured && active && originalSneak) {
            sneakCaptured = true;
        } else if (sneakCaptured && !originalSneak) {
            sneakCaptured = false;
        }
    }

    private void updateSneakState(boolean isSneaking) {
        if (isSneaking) {
            wasSneaking = true;
            return;
        }

        if (wasSneaking) {
            currentEdgeDistance = getRandomEdgeDistance();
            wasSneaking = false;
        }
    }
}