package cn.advicenext.features.module.impl.player;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.impl.combat.AntiBot;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.world.BlockUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GhostHand extends Module {

    public static GhostHand INSTANCE;

    private final BooleanSetting throughNonPlayer = new BooleanSetting("NonPlayer", "Interact through non-player entities", true);
    private final BooleanSetting throughBots = new BooleanSetting("Bots", "Interact through bots", false);
    private final BooleanSetting throughFriendlies = new BooleanSetting("Friendlies", "Interact through friendlies", false);
    private final BooleanSetting throughEnemies = new BooleanSetting("Enemies", "Interact through enemies", true);

    private final BooleanSetting priorityEverything = new BooleanSetting("Everything", "Prioritize everything", true);
    private final BooleanSetting priorityBed = new BooleanSetting("Bed", "Prioritize bed", false);
    private final BooleanSetting priorityBedAdjacent = new BooleanSetting("BedAdjacent", "Prioritize next to bed", false);

    private final BooleanSetting useSword = new BooleanSetting("Sword", "Allow while holding sword", false);
    private final BooleanSetting useTool = new BooleanSetting("Tool", "Allow while holding tool", true);
    private final BooleanSetting useFists = new BooleanSetting("Fists", "Allow while holding fists", true);
    private final BooleanSetting useBucket = new BooleanSetting("Bucket", "Allow while holding bucket", true);
    private final BooleanSetting useFlintSteel = new BooleanSetting("Flint&Steel", "Allow while holding flint and steel", true);
    private final BooleanSetting useCobweb = new BooleanSetting("Cobweb", "Allow while holding cobweb", true);
    private final BooleanSetting useOther = new BooleanSetting("Other", "Allow while holding other items", true);

    private final BooleanSetting requireLmb = new BooleanSetting("RequireLMB", "Require left mouse button", false);
    private final BooleanSetting requireRmb = new BooleanSetting("RequireRMB", "Require right mouse button", false);
    private final BooleanSetting notSword = new BooleanSetting("NotSword", "Not holding a sword", false);

    public GhostHand() {
        super("GhostHand", "Interact with blocks through entities", Category.PLAYER);
        INSTANCE = this;
        this.settings.add(throughNonPlayer);
        this.settings.add(throughBots);
        this.settings.add(throughFriendlies);
        this.settings.add(throughEnemies);
        this.settings.add(priorityEverything);
        this.settings.add(priorityBed);
        this.settings.add(priorityBedAdjacent);
        this.settings.add(useSword);
        this.settings.add(useTool);
        this.settings.add(useFists);
        this.settings.add(useBucket);
        this.settings.add(useFlintSteel);
        this.settings.add(useCobweb);
        this.settings.add(useOther);
        this.settings.add(requireLmb);
        this.settings.add(requireRmb);
        this.settings.add(notSword);
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.getEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return false;
        if (mc.getCameraEntity() == null) return false;
        if (notSword.getValue() && isHoldingSword()) return false;
        if (requireLmb.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) return false;
        if (requireRmb.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) return false;
        return heldItemAllowed();
    }

    public HitResult modifyCrosshairTarget(HitResult original, float tickDelta) {
        if (!shouldOverrideMouseOver()) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        Entity viewEntity = mc.getCameraEntity();
        if (viewEntity == null) return null;

        double reach = mc.player.getBlockInteractionRange();
        Vec3d eyes = viewEntity.getEyePos();
        Vec3d look = viewEntity.getRotationVec(tickDelta);
        Vec3d rayEnd = eyes.add(look.multiply(reach));

        BlockHitResult blockHit = BlockUtils.traverseBlocksAlongRay(eyes, rayEnd,
                priorityBed.getValue(), priorityBedAdjacent.getValue());
        if (blockHit == null) return null;

        boolean isBed = BlockUtils.getBlock(blockHit.getBlockPos()) instanceof BedBlock;
        boolean isAdjacent = !isBed && BlockUtils.isAdjacentToBed(blockHit.getBlockPos());
        boolean priorityOverride = (priorityBed.getValue() && isBed)
                || (priorityBedAdjacent.getValue() && isAdjacent);

        if (!priorityEverything.getValue() && !priorityOverride) return null;

        if (!priorityOverride) {
            Vec3d blockHitVec = blockHit.getPos();
            double blockDist = eyes.distanceTo(blockHitVec);

            Box scanBox = new Box(
                    Math.min(eyes.x, blockHitVec.x) - 1.0,
                    Math.min(eyes.y, blockHitVec.y) - 1.0,
                    Math.min(eyes.z, blockHitVec.z) - 1.0,
                    Math.max(eyes.x, blockHitVec.x) + 1.0,
                    Math.max(eyes.y, blockHitVec.y) + 1.0,
                    Math.max(eyes.z, blockHitVec.z) + 1.0);

            List<Entity> candidates = new ArrayList<>();
            for (Entity e : mc.world.getEntities()) {
                if (e == viewEntity) continue;
                if (e.isSpectator()) continue;
                if (!e.canHit()) continue;
                if (!scanBox.intersects(e.getBoundingBox())) continue;
                candidates.add(e);
            }

            Entity closest = null;
            double closestDist = Double.MAX_VALUE;

            for (Entity e : candidates) {
                Box bb = e.getBoundingBox().expand(e.getTargetingMargin());
                Vec3d intercept = bb.raycast(eyes, blockHitVec).orElse(null);
                boolean inside = bb.contains(eyes);
                if (!inside && intercept == null) continue;
                double dist = inside ? 0.0 : eyes.distanceTo(intercept);
                if (dist >= blockDist) continue;
                if (e == viewEntity.getVehicle()) continue;
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }

            if (closest == null || !obstructionAllowed(closest)) return null;
        }

        return blockHit;
    }

    private boolean obstructionAllowed(Entity e) {
        if (!(e instanceof PlayerEntity)) return throughNonPlayer.getValue();
        PlayerEntity player = (PlayerEntity) e;
        if (AntiBot.isBotStatic(player)) return throughBots.getValue();
        if (TargetUtils.isTeammate(player)) return throughFriendlies.getValue();
        return throughEnemies.getValue();
    }

    private boolean heldItemAllowed() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) return useFists.getValue();
        if (held.isIn(ItemTags.SWORDS)) return useSword.getValue();
        if (held.isIn(ItemTags.PICKAXES) || held.isIn(ItemTags.AXES)
                || held.isIn(ItemTags.SHOVELS) || held.isIn(ItemTags.HOES)
                || held.getItem() instanceof ShearsItem) return useTool.getValue();
        if (held.getItem() instanceof BucketItem) return useBucket.getValue();
        if (held.getItem() instanceof FlintAndSteelItem) return useFlintSteel.getValue();
        if (held.getItem() instanceof BlockItem && ((BlockItem) held.getItem()).getBlock() == Blocks.COBWEB) return useCobweb.getValue();
        return useOther.getValue();
    }

    private boolean isHoldingSword() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
    }
}