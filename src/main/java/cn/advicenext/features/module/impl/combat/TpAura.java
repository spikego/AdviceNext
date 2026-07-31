package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.minecraft.combat.AttackUtils;
import cn.advicenext.utility.minecraft.combat.TargetUtils;
import cn.advicenext.utility.minecraft.network.PacketUtils;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class TpAura extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Teleport mode", "Immediate",
            List.of("Immediate", "AStar"));

    private final DoubleSetting attackRange = new DoubleSetting("AttackRange", "Attack range", 4.2, 6.0, 2.0, 0.1);
    private final IntSetting maxDistance = new IntSetting("MaxDistance", "Max teleport distance", 95, 250, 10, 5,
            () -> mode.is("AStar"));
    private final IntSetting tickDistance = new IntSetting("TickDistance", "Blocks per tick", 3, 7, 1, 1,
            () -> mode.is("AStar"));
    private final BooleanSetting tpBack = new BooleanSetting("TpBack", "Teleport back after attack", true,
            () -> mode.is("AStar"));
    private final BooleanSetting renderPath = new BooleanSetting("RenderPath", "Render teleport path", true);

    private int tickCounter = 0;
    private Vec3d desyncPosition = null;
    private Vec3d originalPosition = null;
    private boolean isTeleporting = false;
    private List<Vec3d> currentPath = new ArrayList<>();

    private static final int[][] ASTAR_DIRECTIONS = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}
    };

    public TpAura() {
        super("TpAura", "Teleports to enemies and attacks them", Category.COMBAT);
        this.settings.add(mode);
        this.settings.add(attackRange);
        this.settings.add(maxDistance);
        this.settings.add(tickDistance);
        this.settings.add(tpBack);
        this.settings.add(renderPath);
    }

    @Override
    public void onDisable() {
        desyncPosition = null;
        originalPosition = null;
        isTeleporting = false;
        currentPath.clear();
        tickCounter = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Immediate")) {
            handleImmediate();
        } else if (mode.is("AStar")) {
            handleAStar();
        }
    }

    private void handleImmediate() {
        if (isTeleporting) {
            tickCounter++;
            if (tickCounter >= 2) {
                travelBack();
            }
            return;
        }

        LivingEntity target = TargetUtils.getBestTarget(attackRange.getValue(), TargetUtils.TargetFilter.PLAYERS,
                TargetUtils.TargetPriority.DISTANCE);
        if (target == null) return;

        originalPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        travelTo(targetPos);
        desyncPosition = targetPos;

        AttackUtils.attack(target, false, true);

        isTeleporting = true;
        tickCounter = 0;
    }

    private void handleAStar() {
        if (isTeleporting) {
            if (currentPath.isEmpty()) {
                travelBack();
                return;
            }

            tickCounter++;
            if (tickCounter >= tickDistance.getValue()) {
                tickCounter = 0;
                if (!currentPath.isEmpty()) {
                    Vec3d nextPos = currentPath.remove(currentPath.size() - 1);
                    travelTo(nextPos);
                    desyncPosition = nextPos;
                }
            }
            return;
        }

        LivingEntity target = TargetUtils.getBestTarget(maxDistance.getValue(), TargetUtils.TargetFilter.PLAYERS,
                TargetUtils.TargetPriority.DISTANCE);
        if (target == null) return;

        originalPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        BlockPos start = mc.player.getBlockPos();
        BlockPos end = target.getBlockPos();

        List<BlockPos> path = findAStarPath(start, end);
        if (path.isEmpty()) {
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            travelTo(targetPos);
            desyncPosition = targetPos;
            AttackUtils.attack(target, false, true);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            travelBack();
            return;
        }

        currentPath = new ArrayList<>();
        for (BlockPos bp : path) {
            currentPath.add(new Vec3d(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5));
        }

        AttackUtils.attack(target, false, true);

        isTeleporting = true;
        tickCounter = 0;
    }

    private void travelTo(Vec3d pos) {
        PacketUtils.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z, mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    private void travelBack() {
        if (originalPosition != null) {
            PacketUtils.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    originalPosition.x, originalPosition.y, originalPosition.z,
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        }
        isTeleporting = false;
        desyncPosition = null;
        originalPosition = null;
        currentPath.clear();
        tickCounter = 0;
    }

    private List<BlockPos> findAStarPath(BlockPos start, BlockPos end) {
        int maxDist = maxDistance.getValue();
        if (start.getSquaredDistance(end) > maxDist * maxDist) {
            return Collections.emptyList();
        }

        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Integer> gScore = new HashMap<>();
        Map<BlockPos, Integer> fScore = new HashMap<>();

        PriorityQueue<BlockPos> openSet = new PriorityQueue<>(
                Comparator.comparingInt(pos -> fScore.getOrDefault(pos, Integer.MAX_VALUE)));

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, end));
        openSet.add(start);

        int maxIterations = 500;
        int iterations = 0;

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            BlockPos current = openSet.poll();
            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }

            for (int[] dir : ASTAR_DIRECTIONS) {
                BlockPos neighbor = current.add(dir[0], dir[1], dir[2]);

                if (neighbor.getSquaredDistance(start) > maxDist * maxDist) continue;
                if (!isWalkable(neighbor)) continue;

                int tentativeG = gScore.getOrDefault(current, Integer.MAX_VALUE) + 1;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristic(neighbor, end));
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private int heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos current) {
        List<BlockPos> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }
        return path;
    }

    private boolean isWalkable(BlockPos pos) {
        if (mc.world == null) return false;
        return !mc.world.getBlockState(pos).isSolidBlock(mc.world, pos)
                && !mc.world.getBlockState(pos.up()).isSolidBlock(mc.world, pos.up());
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!renderPath.getValue()) return;
        if (mc.player == null) return;

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());
        int lineColor = 0xDD242093;

        if (desyncPosition != null && originalPosition != null) {
            Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
            Vec3d targetPos = desyncPosition.add(0, 1, 0);
            Render3DEngine.drawLine3D(event.getMatrices(), vertexConsumer,
                    event.getCameraRenderState(), playerPos, targetPos, lineColor, 1.5F);
        }

        if (!currentPath.isEmpty()) {
            Vec3d prev = new Vec3d(mc.player.getX(), mc.player.getY() + 0.5, mc.player.getZ());
            for (Vec3d point : currentPath) {
                Vec3d pt = point.add(0, 0.5, 0);
                Render3DEngine.drawLine3D(event.getMatrices(), vertexConsumer,
                        event.getCameraRenderState(), prev, pt, lineColor, 1.0F);
                prev = pt;
            }
        }
    }
}