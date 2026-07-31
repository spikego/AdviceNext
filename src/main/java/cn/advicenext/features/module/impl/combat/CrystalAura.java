package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public class CrystalAura extends Module {

    public static CrystalAura INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "Crystal mode", "Smart",
            List.of("Smart", "Simple"));
    private final DoubleSetting placeRange = new DoubleSetting("PlaceRange", "Place range", 4.5, 6.0, 1.0, 0.1);
    private final DoubleSetting breakRange = new DoubleSetting("BreakRange", "Break range", 4.5, 6.0, 1.0, 0.1);
    private final DoubleSetting minDamage = new DoubleSetting("MinDamage", "Minimum damage", 6.0, 20.0, 0.0, 0.5);
    private final DoubleSetting maxSelfDamage = new DoubleSetting("MaxSelfDamage", "Max self damage", 10.0, 20.0, 0.0, 0.5);
    private final DoubleSetting placeDelay = new DoubleSetting("PlaceDelay", "Place delay ms", 0.0, 500.0, 0.0, 10.0);
    private final DoubleSetting breakDelay = new DoubleSetting("BreakDelay", "Break delay ms", 0.0, 500.0, 0.0, 10.0);
    private final IntSetting rotationSpeed = new IntSetting("RotationSpeed", "Aim speed", 60, 100, 10, 1);
    private final BooleanSetting autoPlace = new BooleanSetting("AutoPlace", "Place crystals", true);
    private final BooleanSetting autoBreak = new BooleanSetting("AutoBreak", "Break crystals", true);
    private final BooleanSetting antiSuicide = new BooleanSetting("AntiSuicide", "Prevent self kill", true);
    private final BooleanSetting showRange = new BooleanSetting("RangeCircle", "Show range", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation", "Rotation mode", "Silent",
            List.of("Silent", "None"));

    private RotationUtils.Rotation lastRotation;
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;

    public CrystalAura() {
        super("CrystalAura", "Auto place and break end crystals", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(mode);
        this.settings.add(placeRange);
        this.settings.add(breakRange);
        this.settings.add(minDamage);
        this.settings.add(maxSelfDamage);
        this.settings.add(placeDelay);
        this.settings.add(breakDelay);
        this.settings.add(rotationSpeed);
        this.settings.add(autoPlace);
        this.settings.add(autoBreak);
        this.settings.add(antiSuicide);
        this.settings.add(showRange);
        this.settings.add(rotationMode);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        BlockPos bestPlace = null;
        EndCrystalEntity bestCrystal = null;

        if (autoPlace.getValue() && (now - lastPlaceTime) >= placeDelay.getValue().longValue()) {
            bestPlace = findBestPlacePos();
        }
        if (autoBreak.getValue() && (now - lastBreakTime) >= breakDelay.getValue().longValue()) {
            bestCrystal = findBestCrystal();
        }

        RotationUtils.Rotation targetRotation = null;
        if (bestPlace != null) {
            Vec3d center = new Vec3d(bestPlace.getX() + 0.5, bestPlace.getY() + 1.0, bestPlace.getZ() + 0.5);
            targetRotation = RotationUtils.getRotationToPos(center, mc.player.getEyePos());
        } else if (bestCrystal != null) {
            targetRotation = RotationUtils.getRotationToPos(new Vec3d(bestCrystal.getX(), bestCrystal.getY(), bestCrystal.getZ()), mc.player.getEyePos());
        }

        if (targetRotation != null && rotationMode.is("Silent")) {
            RotationUtils.Rotation current = lastRotation != null
                    ? lastRotation : new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
            RotationUtils.Rotation next = RotationUtils.smoothRotation(current, targetRotation,
                    rotationSpeed.getValue().floatValue());
            RotationUtils.setSilentRotation(next);
            lastRotation = next;
        }

        if (bestPlace != null) {
            placeCrystal(bestPlace);
            lastPlaceTime = now;
            lastBreakTime = now;
        }
        if (bestCrystal != null && bestPlace == null) {
            breakCrystal(bestCrystal);
            lastBreakTime = now;
        }
    }

    private BlockPos findBestPlacePos() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        int range = (int) Math.ceil(placeRange.getValue());
        double bestDamage = 0;
        BlockPos best = null;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!isValidPlacement(pos)) continue;
                    double dist = mc.player.getEyePos().distanceTo(
                            new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5));
                    if (dist > placeRange.getValue()) continue;
                    double damage = calculateDamage(pos);
                    if (damage < minDamage.getValue()) continue;
                    double selfDamage = calculateSelfDamage(pos);
                    if (selfDamage > maxSelfDamage.getValue()) continue;
                    if (antiSuicide.getValue() && selfDamage >= mc.player.getHealth()) continue;
                    if (damage > bestDamage) {
                        bestDamage = damage;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private EndCrystalEntity findBestCrystal() {
        if (mc.world == null || mc.player == null) return null;
        double range = breakRange.getValue();
        double bestDamage = 0;
        EndCrystalEntity best = null;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            double dist = mc.player.getEyePos().distanceTo(new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
            if (dist > range) continue;
            double damage = calculateDamage(entity.getBlockPos());
            double selfDamage = calculateSelfDamage(entity.getBlockPos());
            if (antiSuicide.getValue() && selfDamage >= mc.player.getHealth()) continue;
            if (damage > bestDamage) {
                bestDamage = damage;
                best = crystal;
            }
        }
        return best;
    }

    private boolean isValidPlacement(BlockPos pos) {
        if (mc.world == null) return false;
        BlockPos down = pos.down();
        return mc.world.getBlockState(down).getBlock() == net.minecraft.block.Blocks.BEDROCK
                || mc.world.getBlockState(down).getBlock() == net.minecraft.block.Blocks.OBSIDIAN;
    }

    private double calculateDamage(BlockPos pos) {
        if (mc.world == null) return 0;
        double totalDamage = 0;
        Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) {
                if (player == mc.player || player.isDead()) continue;
                if (AntiBot.isBotStatic(player)) continue;
                double dist = new Vec3d(player.getX(), player.getY(), player.getZ()).distanceTo(crystalPos);
                if (dist > 12.0) continue;
                double exposure = raycastExposure(crystalPos, player);
                double damage = (1.0 - dist / 12.0) * exposure * 6.0;
                if (damage > totalDamage) totalDamage = damage;
            }
        }
        return totalDamage;
    }

    private double calculateSelfDamage(BlockPos pos) {
        if (mc.player == null) return 0;
        Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        double dist = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(crystalPos);
        if (dist > 12.0) return 0;
        double exposure = raycastExposure(crystalPos, mc.player);
        return (1.0 - dist / 12.0) * exposure * 6.0;
    }

    private double raycastExposure(Vec3d from, Entity to) {
        Vec3d toPos = new Vec3d(to.getX(), to.getY() + to.getHeight() * 0.5, to.getZ());
        Vec3d diff = toPos.subtract(from);
        double dist = diff.length();
        if (dist < 0.01) return 1.0;
        Vec3d direction = diff.normalize();
        double step = 0.3;
        int hits = 0;
        int samples = 0;
        for (double d = 0; d <= dist; d += step) {
            samples++;
            Vec3d check = from.add(direction.multiply(d));
            if (mc.world != null && !mc.world.isAir(BlockPos.ofFloored(check))) {
                hits++;
            }
        }
        if (samples == 0) return 1.0;
        return 1.0 - (double) hits / samples;
    }

    private void placeCrystal(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return;
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(crystalSlot);
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private void breakCrystal(EndCrystalEntity crystal) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findCrystalSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (showRange.getValue() && mc.player != null) {
            float range = placeRange.getValue().floatValue();
            Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY() + 0.02, mc.player.getZ());
            net.minecraft.client.render.VertexConsumer vertexConsumer = event.getVertexConsumers()
                    .getBuffer(net.minecraft.client.render.RenderLayers.lines());
            int color = cn.advicenext.gui.colors.Colors.currentColor().getRGB();
            int rangeColor = (130 << 24) | (color & 0x00FFFFFF);
            cn.advicenext.utility.client.render.Render3DEngine.drawCircle3D(
                    event.getMatrices(), vertexConsumer, event.getCameraRenderState(),
                    pos, range, rangeColor, 56, 1.5F);
        }
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
        lastRotation = null;
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}