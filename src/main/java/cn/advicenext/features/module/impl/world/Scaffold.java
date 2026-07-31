package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import cn.advicenext.utility.minecraft.world.BlockUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends Module {

    public static Scaffold INSTANCE;

    private final ModeSetting technique = new ModeSetting("Technique", "Scaffold technique", "Normal",
            List.of("Normal", "Telly", "Expand"));
    private final ModeSetting rotation = new ModeSetting("Rotation", "Rotation mode", "Vanilla",
            List.of("Vanilla", "Smooth", "Snap", "None"));
    private final DoubleSetting rotationSpeed = new DoubleSetting("RotSpeed", "Smooth rotation speed", 60.0, 100.0, 10.0, 1.0,
            () -> rotation.is("Smooth"));
    private final ModeSetting moveFix = new ModeSetting("MoveFix", "Movement correction", "Silent",
            List.of("Off", "Silent", "Strict"), () -> !rotation.is("None"));
    private final BooleanSetting tower = new BooleanSetting("Tower", "Tower mode (hold jump)", true);
    private final BooleanSetting down = new BooleanSetting("Down", "Build down when sneaking", true);
    private final BooleanSetting eagle = new BooleanSetting("Eagle", "Sneak at block edge", false);
    private final BooleanSetting sprint = new BooleanSetting("Sprint", "Keep sprinting", true);
    private final BooleanSetting swing = new BooleanSetting("Swing", "Swing hand", true);
    private final IntSetting placeDelay = new IntSetting("Delay", "Place delay (ticks)", 0, 10, 0, 1);
    private final BooleanSetting sameY = new BooleanSetting("SameY", "Keep same Y level", false);

    private final BooleanSetting tellyRotate = new BooleanSetting("TellyRotate", "Rotate 180 when telly", true,
            () -> technique.is("Telly"));
    private final IntSetting tellyJumpTicks = new IntSetting("TellyJumpTicks", "Ticks before turning", 3, 1, 10, 1,
            () -> technique.is("Telly"));

    private RotationUtils.Rotation lastRotation;
    private int delayTicks = 0;
    private int tellyTicksOnGround = 0;
    private BlockPos lastPlacePos = null;

    public Scaffold() {
        super("Scaffold", "Automatically bridges blocks", Category.WORLD);
        INSTANCE = this;
        this.settings.add(technique);
        this.settings.add(rotation);
        this.settings.add(rotationSpeed);
        this.settings.add(moveFix);
        this.settings.add(tower);
        this.settings.add(down);
        this.settings.add(eagle);
        this.settings.add(sprint);
        this.settings.add(swing);
        this.settings.add(placeDelay);
        this.settings.add(sameY);
        this.settings.add(tellyRotate);
        this.settings.add(tellyJumpTicks);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (delayTicks > 0) {
            delayTicks--;
        }

        if (sprint.getValue()) {
            mc.player.setSprinting(true);
        }

        if (eagle.getValue()) {
            BlockPos below = mc.player.getBlockPos().down();
            if (mc.world.getBlockState(below).isAir()) {
                mc.options.sneakKey.setPressed(true);
            }
        }

        if (mc.player.isOnGround()) {
            tellyTicksOnGround++;
        } else {
            tellyTicksOnGround = 0;
        }

        BlockPos placePos = findPlacePos();
        if (placePos == null) {
            resetRotation();
            return;
        }

        placeBlock(placePos);

        if (technique.is("Telly") && shouldTellyJump()) {
            doTellyJump();
        }
    }

    private boolean shouldTellyJump() {
        if (!mc.player.isOnGround()) return false;
        if (tellyTicksOnGround < tellyJumpTicks.getValue()) return false;

        boolean moving = mc.player.input.playerInput.forward() || mc.player.input.playerInput.backward()
                || mc.player.input.playerInput.left() || mc.player.input.playerInput.right();
        return moving && BlockUtils.getBlockCount() > 0;
    }

    private void doTellyJump() {
        if (tellyRotate.getValue()) {
            RotationUtils.Rotation reverse = new RotationUtils.Rotation(
                    RotationUtils.normalizeAngle(mc.player.getYaw() + 180), 45.0F);
            RotationUtils.setSilentRotation(reverse, getCorrectionMode());
        }
        mc.player.jump();
        tellyTicksOnGround = 0;
    }

    private void placeBlock(BlockPos placePos) {
        BlockHitResult hitResult = BlockUtils.findPlaceResult(placePos);
        if (hitResult == null) {
            resetRotation();
            return;
        }

        int blockSlot = BlockUtils.findBestBlockSlot();
        if (blockSlot == -1) {
            resetRotation();
            return;
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        boolean swapped = currentSlot != blockSlot;
        if (swapped) {
            BlockUtils.silentSwapToSlot(blockSlot);
            BlockUtils.swapToSlot(blockSlot);
        }

        Vec3d hitPos = hitResult.getPos();
        RotationUtils.Rotation targetRotation = RotationUtils.getRotationToPos(hitPos, mc.player.getEyePos());
        applyRotation(targetRotation);

        if (delayTicks <= 0) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            if (swing.getValue()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            delayTicks = placeDelay.getValue();
            lastPlacePos = placePos;
        }

        if (swapped) {
            BlockUtils.swapToSlot(currentSlot);
            BlockUtils.silentSwapToSlot(currentSlot);
        }
    }

    private BlockPos findPlacePos() {
        double posX = mc.player.getX();
        double posY = mc.player.getY();
        double posZ = mc.player.getZ();
        Vec3d vel = mc.player.getVelocity();

        double baseY = posY - 1.0;

        if (tower.getValue() && mc.options.jumpKey.isPressed()) {
            baseY = posY - 1.0;
        }
        if (down.getValue() && mc.options.sneakKey.isPressed() && !eagle.getValue()) {
            baseY = posY - 2.0;
        }
        if (sameY.getValue() && lastPlacePos != null) {
            baseY = lastPlacePos.getY();
        }

        BlockPos playerBlockPos = BlockPos.ofFloored(posX, baseY, posZ);

        List<BlockPos> candidates = getCandidates(playerBlockPos, vel);

       final double fx = posX, fy = baseY, fz = posZ;
        return candidates.stream()
                .filter(pos -> BlockUtils.isReplaceable(pos))
                .filter(pos -> BlockUtils.getPlaceableSide(pos) != null)
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(fx, fy, fz)))
                .orElse(null);
    }

    private List<BlockPos> getCandidates(BlockPos base, Vec3d vel) {
        List<BlockPos> candidates = new ArrayList<>();

        candidates.add(base);

        candidates.add(base.add(0, 1, 0));

        if (technique.is("Expand")) {
            double absX = Math.abs(vel.x);
            double absZ = Math.abs(vel.z);
            if (absX > 0.05 || absZ > 0.05) {
                int dirX = vel.x > 0.05 ? 1 : (vel.x < -0.05 ? -1 : 0);
                int dirZ = vel.z > 0.05 ? 1 : (vel.z < -0.05 ? -1 : 0);
                candidates.add(base.add(dirX, 0, dirZ));
                if (dirX != 0 && dirZ != 0) {
                    candidates.add(base.add(dirX, 0, 0));
                    candidates.add(base.add(0, 0, dirZ));
                }
            }
        }

        boolean moving = mc.player.input.playerInput.forward() || mc.player.input.playerInput.backward()
                || mc.player.input.playerInput.left() || mc.player.input.playerInput.right();
        if (moving) {
            float yaw = mc.player.getYaw();
            float forward = mc.player.input.getMovementInput().y;
            float sideways = mc.player.input.getMovementInput().x;

            float moveYaw = yaw;
            if (forward > 0) {
                moveYaw = yaw;
            } else if (forward < 0) {
                moveYaw = yaw + 180;
            } else if (sideways > 0) {
                moveYaw = yaw - 90;
            } else if (sideways < 0) {
                moveYaw = yaw + 90;
            }

            double rad = Math.toRadians(moveYaw);
            int dirX = (int) Math.round(-Math.sin(rad));
            int dirZ = (int) Math.round(Math.cos(rad));

            candidates.add(base.add(dirX, 0, dirZ));
            if (dirX != 0) {
                candidates.add(base.add(dirX, 0, 0));
            }
            if (dirZ != 0) {
                candidates.add(base.add(0, 0, dirZ));
            }
        }

        return candidates;
    }

    private void applyRotation(RotationUtils.Rotation targetRotation) {
        String rotMode = rotation.getValue();
        if (rotMode.equals("None")) return;

        RotationUtils.Rotation current = lastRotation != null
                ? lastRotation
                : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());

        RotationUtils.Rotation next;
        if (rotMode.equals("Snap")) {
            next = targetRotation;
        } else if (rotMode.equals("Smooth")) {
            float maxTurn = rotationSpeed.getValue().floatValue() * 0.5F;
            next = RotationUtils.smoothRotationLimited(current, targetRotation, maxTurn);
        } else {
            next = targetRotation;
        }

        RotationUtils.setSilentRotation(next, getCorrectionMode());
        lastRotation = next;
    }

    private void resetRotation() {
        if (lastRotation == null || mc.player == null) return;
        RotationUtils.Rotation visual = new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
        RotationUtils.Rotation next = RotationUtils.smoothRotationLimited(lastRotation, visual, 12.0F);
        if (Math.abs(RotationUtils.normalizeAngle(next.yaw - visual.yaw)) <= 0.5F
                && Math.abs(next.pitch - visual.pitch) <= 0.5F) {
            lastRotation = null;
            return;
        }
        RotationUtils.setSilentRotation(next, getCorrectionMode());
        lastRotation = next;
    }

    private MovementCorrection.Mode getCorrectionMode() {
        String val = moveFix.getValue();
        if (val.equals("Silent")) return MovementCorrection.Mode.SILENT;
        if (val.equals("Strict")) return MovementCorrection.Mode.STRICT;
        return MovementCorrection.Mode.OFF;
    }

    public static int getBlockCount() {
        return BlockUtils.getBlockCount();
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastRotation = null;
        delayTicks = 0;
        tellyTicksOnGround = 0;
        lastPlacePos = null;
    }

    @Override
    public String getDisplayValue() {
        if (technique.is("Telly")) return "Telly";
        return String.valueOf(getBlockCount());
    }
}