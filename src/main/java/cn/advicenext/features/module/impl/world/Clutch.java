package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import cn.advicenext.utility.minecraft.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Clutch extends Module {
    private final DoubleSetting reach = new DoubleSetting("Reach", "Reach distance", 4.5, 0.5, 4.5, 0.1);
    private final IntSetting speed = new IntSetting("Speed", "Rotation speed", 8, 0, 100, 1);
    private final IntSetting snapbackSpeed = new IntSetting("Snapback Speed", "Speed to snap back", 12, 0, 100, 1);
    private final IntSetting maxDistance = new IntSetting("Max Distance", "Max blocks to place", 10, 0, 20, 1);
    private final IntSetting rotationTolerance = new IntSetting("Rotation Tolerance", "Tolerance for placement", 25, 20, 100, 1);
    private final ModeSetting switchBlock = new ModeSetting("Switch Block", "Block switching mode", "Client",
            List.of("Client", "Server"));
    private final BooleanSetting simulateFuturePosition = new BooleanSetting("Simulate Future", "Simulate future position", true);
    private final BooleanSetting autoClutch = new BooleanSetting("Auto Clutch", "Auto clutch when knocked off", false);
    private final IntSetting minimumFallDistance = new IntSetting("Min Fall Distance", "Min fall distance for auto", 10, 3, 20, 1);
    private final ModeSetting activationMode = new ModeSetting("Activation", "How to activate clutch",
            "Toggle", List.of("Toggle", "Auto", "KeyHold"));
    private final IntSetting holdKey = new IntSetting("HoldKey", "Key to hold for clutch (KeyHold mode)", GLFW.GLFW_KEY_G, 0, 350, 1,
            () -> activationMode.is("KeyHold"));

    private static final double HALF_WIDTH = 0.3;
    private static final double[][] CORNERS = {{-HALF_WIDTH, -HALF_WIDTH}, {HALF_WIDTH, -HALF_WIDTH}, {-HALF_WIDTH, HALF_WIDTH}, {HALF_WIDTH, HALF_WIDTH}};

    private BlockPos targetHitPos;
    private Direction targetSide;
    private float aimYaw;
    private float aimPitch;
    private boolean hasAim;
    private boolean resetting;
    private boolean slotWasSwapped;
    private int prevSlot = -1;
    private int plannedSlot = -1;
    private int originalSlot = -1;
    private int serverSlot = -1;
    private BlockPos lastPlaced;
    private int clutchBlocksPlaced;
    private boolean autoClutchActive;
    private boolean autoClutchChecking;
    private int autoClutchCheckCounter;
    private boolean autoClutchLandedGuard;
    private int autoClutchLandedTick;
    private int prevHurtTime = -1;

    public Clutch() {
        super("Clutch", "Automatically places blocks under you when falling", Category.WORLD);
        this.settings.add(reach);
        this.settings.add(speed);
        this.settings.add(snapbackSpeed);
        this.settings.add(maxDistance);
        this.settings.add(rotationTolerance);
        this.settings.add(switchBlock);
        this.settings.add(simulateFuturePosition);
        this.settings.add(autoClutch);
        this.settings.add(minimumFallDistance);
        this.settings.add(activationMode);
        this.settings.add(holdKey);
    }

    @Override
    public void onEnable() {
        hasAim = false;
        resetting = false;
        clutchBlocksPlaced = 0;
        autoClutchActive = false;
        autoClutchChecking = false;
        autoClutchCheckCounter = 0;
        autoClutchLandedGuard = false;
        autoClutchLandedTick = 0;
        prevHurtTime = -1;
        prevSlot = -1;
        plannedSlot = -1;
        originalSlot = -1;
        serverSlot = -1;
        lastPlaced = null;
        RotationUtils.resetSilentRotation();
    }

    @Override
    public void onDisable() {
        if (switchBlock.is("Server") && originalSlot != -1 && mc.player != null) {
            BlockUtils.silentSwapToSlot(originalSlot);
            originalSlot = -1;
            serverSlot = -1;
        }
        clearAim(true);
        slotWasSwapped = false;
        prevSlot = -1;
        plannedSlot = -1;
        autoClutchActive = false;
        autoClutchChecking = false;
        autoClutchLandedGuard = false;
        RotationUtils.resetSilentRotation();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround()) {
            clutchBlocksPlaced = 0;
        }

        updateAutoClutch();

        boolean active = isActivated();
        if (mc.currentScreen != null || !active) {
            clearAim(true);
            return;
        }

        BlockPos below = mc.player.getBlockPos().down();
        if (!canPlaceThrough(below)) {
            clearAim(false);
            return;
        }

        int blockSlot = BlockUtils.findBestBlockSlot();
        if (blockSlot == -1) {
            clearAim(false);
            return;
        }

        plannedSlot = blockSlot;

        AimResult aim = clutchAim();
        if (aim != null) {
            targetHitPos = aim.ray.getBlockPos();
            targetSide = aim.ray.getSide();
            aimYaw = aim.yaw;
            aimPitch = aim.pitch;
            hasAim = true;
            resetting = false;
        }

        if (hasAim && !resetting) {
            applyRotation();
            equipPlannedSlot();
            tryPlaceBlock();
        }
    }

    private void updateAutoClutch() {
        if (!autoClutch.getValue()) {
            autoClutchActive = false;
            autoClutchChecking = false;
            autoClutchLandedGuard = false;
            prevHurtTime = mc.player.hurtTime;
            return;
        }

        int curHurtTime = mc.player.hurtTime;
        if (curHurtTime > prevHurtTime) {
            autoClutchChecking = true;
            autoClutchCheckCounter = 0;
            autoClutchLandedGuard = false;
        }
        prevHurtTime = curHurtTime;

        if (autoClutchChecking && !autoClutchActive && !autoClutchLandedGuard) {
            if (autoClutchCheckCounter == 0 || autoClutchCheckCounter % 3 == 0) {
                if (willFallFar(minimumFallDistance.getValue())) {
                    autoClutchActive = true;
                }
            }
            autoClutchCheckCounter++;
        }

        if (autoClutchLandedGuard) {
            int ticksExisted = mc.player.age;
            boolean expired = ticksExisted - autoClutchLandedTick >= 10;
            boolean jumped = mc.options.jumpKey.isPressed();
            boolean airborneUp = !mc.player.isOnGround() && mc.player.getVelocity().y > 0;
            if (expired || jumped || airborneUp) {
                autoClutchActive = false;
                autoClutchChecking = false;
                autoClutchLandedGuard = false;
            }
        }

        if (autoClutchActive && mc.player.isOnGround() && mc.player.hurtTime < mc.player.maxHurtTime - 2) {
            if (!autoClutchLandedGuard) {
                autoClutchLandedGuard = true;
                autoClutchLandedTick = mc.player.age;
                if (!willFallSoon()) {
                    autoClutchActive = false;
                    autoClutchChecking = false;
                    autoClutchLandedGuard = false;
                }
            }
        }

        if (!autoClutchActive && !autoClutchLandedGuard && mc.player.isOnGround() && mc.player.hurtTime == 0) {
            autoClutchChecking = false;
            autoClutchCheckCounter = 0;
        }
    }

    private boolean isActivated() {
        return switch (activationMode.getValue()) {
            case "Toggle" -> true;
            case "Auto" -> autoClutchActive;
            case "KeyHold" -> {
                long window = mc.getWindow().getHandle();
                yield org.lwjgl.glfw.GLFW.glfwGetKey(window, holdKey.getValue()) == GLFW.GLFW_PRESS;
            }
            default -> true;
        };
    }

    private void applyRotation() {
        if (resetting) {
            float currentYaw = RotationUtils.getServerYaw();
            float currentPitch = RotationUtils.getServerPitch();
            float[] smoothed = getRotationsSmoothed(currentYaw, currentPitch, mc.player.getYaw(), mc.player.getPitch(), true);
            RotationUtils.setSilentRotation(new RotationUtils.Rotation(smoothed[0], smoothed[1]));

            if (Math.abs(MathHelper.wrapDegrees(smoothed[0] - mc.player.getYaw())) < 0.5f
                    && Math.abs(smoothed[1] - mc.player.getPitch()) < 0.5f) {
                resetting = false;
                hasAim = false;
            }
            return;
        }

        float currentYaw = RotationUtils.getServerYaw();
        float currentPitch = RotationUtils.getServerPitch();
        float[] smoothed = getRotationsSmoothed(currentYaw, currentPitch, aimYaw, aimPitch, false);
        RotationUtils.setSilentRotation(new RotationUtils.Rotation(smoothed[0], smoothed[1]));
    }

    private void tryPlaceBlock() {
        if (targetHitPos == null || targetSide == null) return;

        int maxBlocks = maxDistance.getValue();
        if (maxBlocks > 0 && clutchBlocksPlaced >= maxBlocks) return;

        float servYaw = RotationUtils.getServerYaw();
        float servPitch = RotationUtils.getServerPitch();
        BlockHitResult mop = rayCastBlock(reach.getValue(), servYaw, servPitch);

        if (mop != null && targetHitPos.equals(mop.getBlockPos()) && targetSide == mop.getSide()) {
            double tolerance = rotationTolerance.getValue();
            if (Math.abs(MathHelper.wrapDegrees(servYaw - aimYaw)) <= tolerance
                    && Math.abs(servPitch - aimPitch) <= tolerance) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, mop);
                mc.player.swingHand(Hand.MAIN_HAND);

                if (mop.getSide() != Direction.UP) {
                    clutchBlocksPlaced++;
                }
                lastPlaced = targetHitPos;
            }
        }
    }

    private void equipPlannedSlot() {
        if (plannedSlot < 0 || plannedSlot > 8) return;
        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (switchBlock.is("Server")) {
            if (originalSlot == -1) originalSlot = currentSlot;
            if (serverSlot != plannedSlot) {
                BlockUtils.silentSwapToSlot(plannedSlot);
                serverSlot = plannedSlot;
                slotWasSwapped = true;
            }
        } else {
            if (prevSlot == -1) prevSlot = currentSlot;
            if (currentSlot != plannedSlot) {
                BlockUtils.swapToSlot(plannedSlot);
                slotWasSwapped = true;
            }
        }
    }

    private void clearAim(boolean allowSnapback) {
        if (slotWasSwapped && prevSlot != -1 && prevSlot != mc.player.getInventory().getSelectedSlot()) {
            if (switchBlock.is("Server")) {
                BlockUtils.silentSwapToSlot(prevSlot);
                serverSlot = prevSlot;
            } else {
                BlockUtils.swapToSlot(prevSlot);
            }
            slotWasSwapped = false;
        }
        targetHitPos = null;
        targetSide = null;
        lastPlaced = null;
        clutchBlocksPlaced = 0;
        if (allowSnapback && hasAim) {
            resetting = true;
        }
        hasAim = false;
        prevSlot = -1;
        plannedSlot = -1;
    }

    private float[] getRotationsSmoothed(float currentYaw, float currentPitch, float targetYaw, float targetPitch, boolean isSnapback) {
        float spd = isSnapback ? snapbackSpeed.getValue() : speed.getValue();
        float maxTurn = 1.5F + MathHelper.clamp(spd, 0.0F, 100.0F) * 0.585F;

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = MathHelper.clamp(targetPitch, -90.0F, 90.0F) - currentPitch;

        float stepYaw = Math.copySign(Math.min(Math.abs(yawDiff), maxTurn), yawDiff);
        float stepPitch = Math.copySign(Math.min(Math.abs(pitchDiff), maxTurn * 0.7F), pitchDiff);

        return new float[]{
            currentYaw + stepYaw,
            MathHelper.clamp(currentPitch + stepPitch, -90.0F, 90.0F)
        };
    }

    private BlockHitResult rayCastBlock(double reachVal, float yaw, float pitch) {
        Vec3d eye = mc.player.getEyePos();
        float radPitch = pitch * 0.017453292F;
        float radYaw = -yaw * 0.017453292F;
        float cosPitch = MathHelper.cos(radPitch);
        float sinPitch = MathHelper.sin(radPitch);
        float cosYaw = MathHelper.cos(radYaw);
        float sinYaw = MathHelper.sin(radYaw);
        Vec3d look = new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3d end = eye.add(look.x * reachVal, look.y * reachVal, look.z * reachVal);

        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                eye, end,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    private boolean canPlaceThrough(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isAir() || state.isLiquid() || state.isReplaceable();
    }

    private boolean willFallFar(double minFall) {
        double startY = mc.player.getY();
        PredictionState pred = new PredictionState().fromPlayer();
        for (int t = 0; t < 60; t++) {
            pred.tick(false);
            if (pred.onGround) return false;
            double fall = startY - pred.posY;
            if (fall > minFall) return true;
        }
        return false;
    }

    private boolean willFallSoon() {
        PredictionState pred = new PredictionState().fromPlayer();
        for (int t = 0; t < 10; t++) {
            pred.tick(true);
            if (!pred.onGround && pred.motionY < 0) return true;
        }
        return false;
    }

    private AimResult clutchAim() {
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d eye = mc.player.getEyePos();

        Vec3d futurePos = playerPos;
        if (simulateFuturePosition.getValue()) {
            PredictionState pred = new PredictionState().fromPlayer();
            for (int t = 0; t < 20; t++) {
                pred.tick(false);
                if (pred.posY < playerPos.y - 2 || pred.onGround) break;
            }
            futurePos = pred.getPos();
        }

        int feetX = MathHelper.floor(playerPos.x);
        int feetZ = MathHelper.floor(playerPos.z);
        int feetY = MathHelper.floor(playerPos.y);
        int minX = feetX - 5;
        int maxX = feetX + 4;
        int minZ = feetZ - 5;
        int maxZ = feetZ + 4;
        int maxY = feetY - 1;
        int minY = feetY - 4;

        List<BlockCandidate> candidates = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (canPlaceThrough(pos)) continue;

                    double currentDist = dist2PointAABB(playerPos, pos);
                    double futureDist = dist2PointAABB(futurePos, pos);
                    double score = simulateFuturePosition.getValue() ? (currentDist * 0.3 + futureDist * 0.7) : currentDist;
                    if (pos.equals(lastPlaced)) score *= 0.95;
                    candidates.add(new BlockCandidate(score, pos));
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(a.score, b.score));

        ItemStack held = plannedSlot >= 0 && plannedSlot <= 8
                ? mc.player.getInventory().getStack(plannedSlot) : null;

        for (BlockCandidate candidate : candidates) {
            boolean underPlayer = isBlockUnderPlayer(candidate.pos, playerPos);
            AimResult result = getBestRotationsToBlock(held, candidate.pos, eye, reach.getValue(), underPlayer);
            if (result != null) return result;
        }
        return null;
    }

    private boolean isBlockUnderPlayer(BlockPos pos, Vec3d playerPos) {
        if (pos.getY() >= MathHelper.floor(playerPos.y)) return false;
        for (double[] corner : CORNERS) {
            int cx = MathHelper.floor(playerPos.x + corner[0]);
            int cz = MathHelper.floor(playerPos.z + corner[1]);
            if (pos.getX() == cx && pos.getZ() == cz) return true;
        }
        return false;
    }

    private AimResult getBestRotationsToBlock(ItemStack held, BlockPos pos, Vec3d eye, double reachVal, boolean underPlayer) {
        double inset = 0.05;
        double step = 0.2;
        double jitter = step * 0.1;
        boolean faceSouth = Math.abs(eye.z - (pos.getZ() + 1)) < Math.abs(eye.z - pos.getZ());
        boolean faceEast = Math.abs(eye.x - (pos.getX() + 1)) < Math.abs(eye.x - pos.getX());
        float baseYaw = RotationUtils.getServerYaw();
        float basePitch = RotationUtils.getServerPitch();
        int n = (int) Math.round(1 / step);

        List<RotationCandidate> candidates = new ArrayList<>();
        candidates.add(new RotationCandidate(0, baseYaw, basePitch));

        for (int row = 0; row <= n; row++) {
            double v = clamp01(row * step + randomRange(-jitter, jitter));
            for (int col = 0; col <= n; col++) {
                double u = clamp01(col * step + randomRange(-jitter, jitter));

                if (underPlayer) {
                    float[] rV = getRotationsWrapped(eye, pos.getX() + u, pos.getY() + 1 - inset, pos.getZ() + v);
                    double costV = Math.abs(wrapYawDelta(baseYaw, rV[0])) + Math.abs(rV[1] - basePitch);
                    candidates.add(new RotationCandidate(costV, rV[0], rV[1]));
                }

                float[] rZ = getRotationsWrapped(eye, pos.getX() + u, pos.getY() + v,
                        faceSouth ? pos.getZ() + 1 - inset : pos.getZ() + inset);
                double costZ = Math.abs(wrapYawDelta(baseYaw, rZ[0])) + Math.abs(rZ[1] - basePitch);
                candidates.add(new RotationCandidate(costZ, rZ[0], rZ[1]));

                float[] rX = getRotationsWrapped(eye,
                        faceEast ? pos.getX() + 1 - inset : pos.getX() + inset,
                        pos.getY() + v, pos.getZ() + u);
                double costX = Math.abs(wrapYawDelta(baseYaw, rX[0])) + Math.abs(rX[1] - basePitch);
                candidates.add(new RotationCandidate(costX, rX[0], rX[1]));
            }
        }

        candidates.sort((a, b) -> Double.compare(a.cost, b.cost));

        for (RotationCandidate candidate : candidates) {
            BlockHitResult mop = rayCastBlock(reachVal, candidate.yaw, candidate.pitch);
            if (mop != null && mop.getBlockPos().equals(pos)) {
                return new AimResult(mop, candidate.yaw, candidate.pitch);
            }
        }
        return null;
    }

    private float[] getRotationsWrapped(Vec3d eye, double x, double y, double z) {
        double dx = x - eye.x;
        double dy = y - eye.y;
        double dz = z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    private double dist2PointAABB(Vec3d p, BlockPos b) {
        double cx = Math.max(b.getX(), Math.min(b.getX() + 1, p.x));
        double cy = Math.max(b.getY(), Math.min(b.getY() + 1, p.y));
        double cz = Math.max(b.getZ(), Math.min(b.getZ() + 1, p.z));
        double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static float wrapYawDelta(float a, float b) {
        return Math.abs(MathHelper.wrapDegrees(a - b));
    }

    private static double clamp01(double val) {
        return Math.max(0, Math.min(1, val));
    }

    private static double randomRange(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private class PredictionState {
        double posX, posY, posZ;
        double motionX, motionY, motionZ;
        boolean onGround;

        PredictionState fromPlayer() {
            PredictionState p = new PredictionState();
            p.posX = mc.player.getX();
            p.posY = mc.player.getY();
            p.posZ = mc.player.getZ();
            p.motionX = mc.player.getVelocity().x;
            p.motionY = mc.player.getVelocity().y;
            p.motionZ = mc.player.getVelocity().z;
            p.onGround = mc.player.isOnGround();
            return p;
        }

        void tick(boolean ignoreGround) {
            if (!ignoreGround && onGround && motionY <= 0) {
                motionY = 0;
                motionX *= 0.6;
                motionZ *= 0.6;
            } else {
                motionY -= 0.08;
                motionY *= 0.98;
                motionX *= 0.91;
                motionZ *= 0.91;
            }
            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            BlockPos below = new BlockPos(MathHelper.floor(posX), MathHelper.floor(posY - 0.2), MathHelper.floor(posZ));
            BlockState state = mc.world.getBlockState(below);
            boolean solid = state.isSolidBlock(mc.world, below);
            onGround = solid && posY - MathHelper.floor(posY) < 0.3;
        }

        Vec3d getPos() {
            return new Vec3d(posX, posY, posZ);
        }
    }

    private static class BlockCandidate {
        double score;
        BlockPos pos;

        BlockCandidate(double score, BlockPos pos) {
            this.score = score;
            this.pos = pos;
        }
    }

    private static class RotationCandidate {
        double cost;
        float yaw;
        float pitch;

        RotationCandidate(double cost, float yaw, float pitch) {
            this.cost = cost;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static class AimResult {
        BlockHitResult ray;
        float yaw;
        float pitch;

        AimResult(BlockHitResult ray, float yaw, float pitch) {
            this.ray = ray;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}