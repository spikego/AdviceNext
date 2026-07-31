package cn.advicenext.utility.minecraft.world;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ================ Basic Block Queries ================

    public static Block getBlock(BlockPos pos) {
        World world = mc.world;
        if (world == null) return Blocks.AIR;
        return world.getBlockState(pos).getBlock();
    }

    public static BlockState getBlockState(BlockPos pos) {
        World world = mc.world;
        if (world == null) return Blocks.AIR.getDefaultState();
        return world.getBlockState(pos);
    }

    public static Block getBlock(double x, double y, double z) {
        return getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))).getBlock();
    }

    public static Block getBlock(Vec3d position) {
        return getBlockState(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z))).getBlock();
    }

    public static boolean isReplaceable(BlockPos pos) {
        World world = mc.world;
        if (world == null) return true;
        return world.getBlockState(pos).isReplaceable();
    }

    public static boolean isAir(BlockPos pos) {
        World world = mc.world;
        if (world == null) return false;
        return world.getBlockState(pos).isAir();
    }

    public static boolean isSolid(BlockPos pos) {
        World world = mc.world;
        if (world == null) return false;
        return world.getBlockState(pos).isSolid();
    }

    public static boolean isBlockPosEqual(BlockPos a, BlockPos b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    public static boolean isSamePos(BlockPos a, BlockPos b) {
        return a == b || (a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ());
    }

    public static boolean check(BlockPos pos, Block block) {
        return getBlock(pos) == block;
    }

    public static boolean replaceable(BlockPos pos) {
        if (mc.world == null || mc.player == null) return true;
        return getBlockState(pos).isReplaceable();
    }

    public static BlockPos pos(double x, double y, double z) {
        return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    // ================ Block Classification ================

    public static boolean isNormalBlock(Block block) {
        return block == Blocks.GLASS || (block.getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN)
                && block != Blocks.GRAVEL
                && block != Blocks.SAND
                && block != Blocks.SOUL_SAND
                && block != Blocks.TNT
                && block != Blocks.CRAFTING_TABLE
                && block != Blocks.FURNACE
                && block != Blocks.DISPENSER
                && block != Blocks.DROPPER
                && block != Blocks.NOTE_BLOCK
                && block != Blocks.COMMAND_BLOCK);
    }

    public static boolean notFullBlock(Block block) {
        return block instanceof FenceGateBlock
                || block instanceof LadderBlock
                || block instanceof FlowerPotBlock
                || block instanceof AbstractPressurePlateBlock
                || isFluid(block)
                || block instanceof FenceBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock
                || block instanceof ChestBlock;
    }

    public static boolean isFluid(Block block) {
        return block == Blocks.WATER || block == Blocks.LAVA
                || block.getDefaultState().getFluidState().isOf(Fluids.WATER)
                || block.getDefaultState().getFluidState().isOf(Fluids.LAVA);
    }

    public static boolean isFluid(BlockPos pos) {
        World world = mc.world;
        if (world == null) return false;
        return world.getBlockState(pos).getFluidState().isStill()
                || world.getBlockState(pos).getFluidState().isOf(Fluids.WATER)
                || world.getBlockState(pos).getFluidState().isOf(Fluids.LAVA);
    }

    public static boolean isInteractable(Block block) {
        return block instanceof TrapdoorBlock
                || block instanceof DoorBlock
                || block instanceof BlockWithEntity
                || block instanceof JukeboxBlock
                || block instanceof FenceGateBlock
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof EnchantingTableBlock
                || block instanceof BrewingStandBlock
                || block instanceof BedBlock
                || block instanceof DropperBlock
                || block instanceof DispenserBlock
                || block instanceof HopperBlock
                || block instanceof AnvilBlock
                || block instanceof NoteBlock
                || block instanceof CraftingTableBlock;
    }

    public static boolean isInteractable(BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos() == null) {
            return false;
        }
        if (mc.player == null) return false;
        if (!mc.player.isSneaking() || mc.player.getMainHandStack() == null) {
            return isInteractable(getBlock(hit.getBlockPos()));
        }
        return false;
    }

    // ================ Block Hardness & Dig Efficiency ================

    public static float getBlockHardness(Block block, ItemStack stack, boolean ignoreSlow, boolean ignoreGround) {
        if (mc.world == null) return 0f;
        float hardness = block.getDefaultState().getHardness(mc.world, BlockPos.ORIGIN);
        if (hardness < 0f) {
            return 0f;
        }
        boolean toolNotRequired = block.getDefaultState().isToolRequired() == false;
        if (toolNotRequired || (stack != null && stack.isSuitableFor(block.getDefaultState()))) {
            return getToolDigEfficiency(stack, block, ignoreSlow, ignoreGround) / hardness / 30f;
        }
        return getToolDigEfficiency(stack, block, ignoreSlow, ignoreGround) / hardness / 100f;
    }

    public static float getBlockHardness(BlockPos pos, ItemStack stack, boolean ignoreSlow, boolean ignoreGround) {
        return getBlockHardness(getBlock(pos), stack, ignoreSlow, ignoreGround);
    }

    public static float maxDigRateAcrossSlots(Block block, int slotCount) {
        if (mc.player == null || slotCount <= 0) return 0f;
        int n = Math.min(slotCount, mc.player.getInventory().size());
        float best = 0f;
        for (int i = 0; i < n; i++) {
            float h = getBlockHardness(block, mc.player.getInventory().getStack(i), false, false);
            if (h > best) {
                best = h;
            }
        }
        return best;
    }

    public static float getToolDigEfficiency(ItemStack stack, Block block, boolean ignoreSlow, boolean ignoreGround) {
        if (mc.player == null || mc.world == null) return 1f;
        float n = (stack == null) ? 1f : stack.getMiningSpeedMultiplier(block.getDefaultState());
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            n *= 1f + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2f;
        }
        if (!ignoreSlow) {
            if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                float n2;
                switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                    case 0:  n2 = 0.3f; break;
                    case 1:  n2 = 0.09f; break;
                    case 2:  n2 = 0.0027f; break;
                    default: n2 = 8.1E-4f; break;
                }
                n *= n2;
            }
            if (!mc.player.isOnGround() && !ignoreGround) {
                n /= 5f;
            }
        }
        return n;
    }

    public static float getFistBreakTicks(Block block) {
        if (mc.world == null) return Float.MAX_VALUE;
        float hardness = block.getDefaultState().getHardness(mc.world, BlockPos.ORIGIN);
        if (hardness < 0) return Float.MAX_VALUE;
        if (hardness == 0) return 0;
        return hardness * (block.getDefaultState().isToolRequired() ? 100f : 30f);
    }

    public static float getFistBreakTicks(BlockPos pos) {
        return getFistBreakTicks(getBlock(pos));
    }

    // ================ Bounding Boxes ================

    public static Box getBlockSelectionBox(BlockPos pos) {
        if (mc.world == null) return null;
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        }
        return shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Box getCollisionOrSelectionBox(BlockPos pos) {
        if (mc.world == null) return null;
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(mc.world, pos);
        if (shape.isEmpty()) {
            shape = state.getOutlineShape(mc.world, pos);
        }
        if (shape.isEmpty()) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        }
        return shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Box getCollisionOrSelectedOnly(BlockPos pos) {
        if (mc.world == null) return null;
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(mc.world, pos);
        if (shape.isEmpty()) {
            shape = state.getOutlineShape(mc.world, pos);
        }
        if (shape.isEmpty()) return null;
        return shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Box unionBlockBounds(BlockPos a, BlockPos b) {
        Box ua = getCollisionOrSelectionBox(a);
        Box ub = getCollisionOrSelectionBox(b);
        if (ua == null || ub == null) return null;
        return ua.union(ub);
    }

    // ================ Placement & Neighbor Checks ================

    public static boolean canPlaceBlockAt(BlockPos pos) {
        World world = mc.world;
        if (world == null) return false;
        BlockState state = world.getBlockState(pos);
        return state.isReplaceable();
    }

    public static boolean canPlaceBlockOnSide(ItemStack stack, BlockPos pos, Direction side) {
        if (stack == null || !(stack.getItem() instanceof BlockItem)) return false;
        if (mc.world == null || mc.player == null) return false;
        BlockPos neighbor = pos.offset(side);
        BlockState neighborState = mc.world.getBlockState(neighbor);
        return !neighborState.isReplaceable() && !neighborState.isAir()
                && neighborState.getBlock() != Blocks.WATER
                && neighborState.getBlock() != Blocks.LAVA;
    }

    public static Direction getPlaceableSide(BlockPos pos) {
        World world = mc.world;
        if (world == null) return null;

        Direction[] priority = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction dir : priority) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighbor);
            if (!neighborState.isReplaceable() && !neighborState.isAir()
                    && neighborState.getBlock() != Blocks.WATER
                    && neighborState.getBlock() != Blocks.LAVA) {
                return dir;
            }
        }
        return null;
    }

    public static boolean hasAirNeighbor(BlockPos pos, BlockPos... exclude) {
        if (mc.world == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isAir()) continue;
            boolean excluded = false;
            for (BlockPos ex : exclude) {
                if (neighbor.equals(ex)) { excluded = true; break; }
            }
            if (!excluded) return true;
        }
        return false;
    }

    public static boolean isAdjacentToBed(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (getBlock(pos.offset(dir)) instanceof BedBlock) return true;
        }
        return false;
    }

    // ================ BlockHitResult Helpers ================

    public static BlockHitResult getPlaceResult(BlockPos pos, Direction side) {
        BlockPos neighbor = pos.offset(side);
        Direction face = side.getOpposite();
        Vec3d hit = new Vec3d(
                neighbor.getX() + 0.5 + face.getOffsetX() * 0.5,
                neighbor.getY() + 0.5 + face.getOffsetY() * 0.5,
                neighbor.getZ() + 0.5 + face.getOffsetZ() * 0.5
        );
        return new BlockHitResult(hit, face, neighbor, false);
    }

    public static BlockHitResult findPlaceResult(BlockPos placePos) {
        Direction side = getPlaceableSide(placePos);
        if (side == null) return null;
        return getPlaceResult(placePos, side);
    }

    public static BlockPos offsetPos(BlockHitResult hit) {
        return hit.getBlockPos().offset(hit.getSide());
    }

    // ================ Ray Tracing ================

    public static BlockHitResult rayTraceBlock(BlockPos targetPos) {
        if (mc.player == null || mc.world == null) return null;
        double reach = mc.player.getBlockInteractionRange();
        Vec3d eyes = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVecClient();
        Vec3d end = eyes.add(look.x * reach, look.y * reach, look.z * reach);
        BlockHitResult result = mc.world.raycast(new RaycastContext(eyes, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        if (result.getType() == HitResult.Type.BLOCK
                && result.getBlockPos().equals(targetPos)) {
            return result;
        }
        return null;
    }

    public static boolean canSeeVecBlock(BlockPos pos, Vec3d eyePos, Vec3d blockPoint) {
        if (mc.world == null) return true;
        BlockHitResult result = mc.world.raycast(new RaycastContext(eyePos, blockPoint,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        if (result == null) return true;
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = result.getBlockPos();
            if (hitPos.getX() == pos.getX() && hitPos.getY() == pos.getY() && hitPos.getZ() == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBlockBeSeen(BlockPos pos) {
        if (mc.player == null) return false;
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (double offsetY = 0.0; offsetY <= 0.5; offsetY += 0.5) {
            double y = pos.getY() + offsetY;
            Vec3d blockPoint = new Vec3d(pos.getX() + 1, y, pos.getZ() + 0.5);
            if (canSeeVecBlock(pos, eyePos, blockPoint)) return true;
            blockPoint = new Vec3d(pos.getX(), y, pos.getZ() + 0.5);
            if (canSeeVecBlock(pos, eyePos, blockPoint)) return true;
            blockPoint = new Vec3d(pos.getX() + 0.5, y, pos.getZ() + 1);
            if (canSeeVecBlock(pos, eyePos, blockPoint)) return true;
            blockPoint = new Vec3d(pos.getX() + 0.5, y, pos.getZ());
            if (canSeeVecBlock(pos, eyePos, blockPoint)) return true;
        }
        return false;
    }

    public static BlockHitResult traverseBlocksAlongRay(Vec3d start, Vec3d end, boolean wantBed, boolean wantAdjacent) {
        if (mc.world == null) return null;
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)) return null;
        if (Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z)) return null;

        int destX = MathHelper.floor(end.x);
        int destY = MathHelper.floor(end.y);
        int destZ = MathHelper.floor(end.z);
        int curX = MathHelper.floor(start.x);
        int curY = MathHelper.floor(start.y);
        int curZ = MathHelper.floor(start.z);

        BlockHitResult firstHit = null;

        BlockHitResult candidate = getBlockCollisionHit(curX, curY, curZ, start, end);
        if (candidate != null) {
            if (isBedOrAdjacentMatch(candidate.getBlockPos(), wantBed, wantAdjacent)) return candidate;
            firstHit = candidate;
        }

        Vec3d tracePos = start;
        int remaining = 200;

        while (remaining-- >= 0) {
            if (Double.isNaN(tracePos.x) || Double.isNaN(tracePos.y) || Double.isNaN(tracePos.z))
                return firstHit;
            if (curX == destX && curY == destY && curZ == destZ)
                return firstHit;

            boolean crossX = true, crossY = true, crossZ = true;
            double boundX = 999.0, boundY = 999.0, boundZ = 999.0;
            if (destX > curX) boundX = (double) curX + 1.0;
            else if (destX < curX) boundX = (double) curX;
            else crossX = false;
            if (destY > curY) boundY = (double) curY + 1.0;
            else if (destY < curY) boundY = (double) curY;
            else crossY = false;
            if (destZ > curZ) boundZ = (double) curZ + 1.0;
            else if (destZ < curZ) boundZ = (double) curZ;
            else crossZ = false;

            double dx = end.x - tracePos.x;
            double dy = end.y - tracePos.y;
            double dz = end.z - tracePos.z;
            double tX = 999.0, tY = 999.0, tZ = 999.0;
            if (crossX) tX = (boundX - tracePos.x) / dx;
            if (crossY) tY = (boundY - tracePos.y) / dy;
            if (crossZ) tZ = (boundZ - tracePos.z) / dz;
            if (tX == -0.0) tX = -1.0E-4;
            if (tY == -0.0) tY = -1.0E-4;
            if (tZ == -0.0) tZ = -1.0E-4;

            Direction face;
            if (tX < tY && tX < tZ) {
                face = destX > curX ? Direction.WEST : Direction.EAST;
                tracePos = new Vec3d(boundX, tracePos.y + dy * tX, tracePos.z + dz * tX);
            } else if (tY < tZ) {
                face = destY > curY ? Direction.DOWN : Direction.UP;
                tracePos = new Vec3d(tracePos.x + dx * tY, boundY, tracePos.z + dz * tY);
            } else {
                face = destZ > curZ ? Direction.NORTH : Direction.SOUTH;
                tracePos = new Vec3d(tracePos.x + dx * tZ, tracePos.y + dy * tZ, boundZ);
            }

            curX = MathHelper.floor(tracePos.x) - (face == Direction.EAST ? 1 : 0);
            curY = MathHelper.floor(tracePos.y) - (face == Direction.UP ? 1 : 0);
            curZ = MathHelper.floor(tracePos.z) - (face == Direction.SOUTH ? 1 : 0);

            candidate = getBlockCollisionHit(curX, curY, curZ, start, end);
            if (candidate != null) {
                if (isBedOrAdjacentMatch(candidate.getBlockPos(), wantBed, wantAdjacent)) return candidate;
                if (firstHit == null) firstHit = candidate;
            }
        }
        return firstHit;
    }

    private static BlockHitResult getBlockCollisionHit(int x, int y, int z, Vec3d start, Vec3d end) {
        if (mc.world == null) return null;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getRaycastShape(mc.world, pos);
        if (shape.isEmpty()) return null;
        return shape.raycast(start, end, pos);
    }

    private static boolean isBedOrAdjacentMatch(BlockPos pos, boolean wantBed, boolean wantAdjacent) {
        Block block = getBlock(pos);
        boolean isBed = block instanceof BedBlock;
        if (wantBed && isBed) return true;
        if (wantAdjacent && !isBed && isAdjacentToBed(pos)) return true;
        return false;
    }

    // ================ Direction & Face Helpers ================

    public static Direction facingFromBlockCenterToPoint(BlockPos pos, Vec3d hit) {
        double px = hit.x - (pos.getX() + 0.5);
        double py = hit.y - (pos.getY() + 0.5);
        double pz = hit.z - (pos.getZ() + 0.5);
        double ax = Math.abs(px);
        double ay = Math.abs(py);
        double az = Math.abs(pz);
        if (ax > ay && ax > az) {
            return px > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay > az) {
            return py > 0 ? Direction.UP : Direction.DOWN;
        }
        return pz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static Vec3d getFaceCenter(BlockPos block, Direction face) {
        double eps = 1e-3;
        double cx = block.getX() + 0.5;
        double cy = block.getY() + 0.5;
        double cz = block.getZ() + 0.5;
        switch (face) {
            case UP:    return new Vec3d(cx, block.getY() + 1 - eps, cz);
            case DOWN:  return new Vec3d(cx, block.getY() + eps, cz);
            case NORTH: return new Vec3d(cx, cy, block.getZ() + eps);
            case SOUTH: return new Vec3d(cx, cy, block.getZ() + 1 - eps);
            case EAST:  return new Vec3d(block.getX() + 1 - eps, cy, cz);
            case WEST:  return new Vec3d(block.getX() + eps, cy, cz);
            default:    return new Vec3d(cx, cy, cz);
        }
    }

    public static Direction[] getVisibleFaces(Vec3d eye, BlockPos block) {
        Direction yFace = Math.abs(eye.y - (block.getY() + 1)) < Math.abs(eye.y - block.getY())
                ? Direction.UP : Direction.DOWN;
        Direction zFace = Math.abs(eye.z - (block.getZ() + 1)) < Math.abs(eye.z - block.getZ())
                ? Direction.SOUTH : Direction.NORTH;
        Direction xFace = Math.abs(eye.x - (block.getX() + 1)) < Math.abs(eye.x - block.getX())
                ? Direction.EAST : Direction.WEST;
        return new Direction[]{yFace, zFace, xFace};
    }

    public static boolean containsFace(Direction[] faces, Direction face) {
        for (Direction f : faces) if (f == face) return true;
        return false;
    }

    // ================ Distance Helpers ================

    public static double getCenterDistToBlock(BlockPos pos) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return Double.MAX_VALUE;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        return player.squaredDistanceTo(cx, cy, cz);
    }

    public static double dist2PointAABB(Vec3d p, BlockPos b) {
        double cx = Math.max(b.getX(), Math.min(b.getX() + 1, p.x));
        double cy = Math.max(b.getY(), Math.min(b.getY() + 1, p.y));
        double cz = Math.max(b.getZ(), Math.min(b.getZ() + 1, p.z));
        double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3d getBlockCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static boolean isBlockInReach(BlockPos pos) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return false;
        Vec3d center = getBlockCenter(pos);
        double reach = 4.5;
        return player.getEyePos().squaredDistanceTo(center) <= reach * reach;
    }

    // ================ Hotbar / Slot ================

    public static int findBlockSlot() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return -1;

        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof BlockItem) {
            return currentSlot;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    public static int findBestBlockSlot() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return -1;

        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof BlockItem && isValidBlock(((BlockItem) mainHand.getItem()).getBlock())) {
            return currentSlot;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem && isValidBlock(((BlockItem) stack.getItem()).getBlock())) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isValidBlock(Block block) {
        return !(block instanceof FallingBlock)
                && !(block instanceof FluidBlock)
                && block != Blocks.FIRE
                && block != Blocks.SOUL_FIRE
                && block != Blocks.COBWEB
                && block != Blocks.SWEET_BERRY_BUSH
                && block != Blocks.POWDER_SNOW;
    }

    public static int getBlockCount() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem && isValidBlock(((BlockItem) stack.getItem()).getBlock())) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffHandStack();
        if (offhand.getItem() instanceof BlockItem && isValidBlock(((BlockItem) offhand.getItem()).getBlock())) {
            count += offhand.getCount();
        }
        return count;
    }

    public static void swapToSlot(int slot) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        player.getInventory().setSelectedSlot(slot);
    }

    public static void silentSwapToSlot(int slot) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    // ================ Nearby Blocks ================

    public static List<BlockPos> getNearbyBlocks(BlockPos center, int range) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    blocks.add(center.add(x, y, z));
                }
            }
        }
        return blocks;
    }
}