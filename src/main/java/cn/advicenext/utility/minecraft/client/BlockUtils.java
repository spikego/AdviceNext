package cn.advicenext.utility.minecraft.client;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public static boolean isValidBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem && 
               Block.getBlockFromItem(stack.getItem()).getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN);
    }
    
    public static int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValidBlock(stack)) {
                return i;
            }
        }
        return -1;
    }
    
    public static boolean canPlace(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable() && 
               mc.world.canPlace(mc.world.getBlockState(pos), pos, null);
    }
    
    public static Direction getPlaceDirection(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
                return direction.getOpposite();
            }
        }
        return Direction.UP;
    }
    
    public static Vec3d getHitVec(BlockPos pos, Direction direction) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d offset = Vec3d.of(direction.getVector()).multiply(0.5);
        return center.add(offset);
    }
    
    public static void placeBlock(BlockPos pos, Direction direction) {
        Vec3d hitVec = getHitVec(pos.offset(direction), direction.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(hitVec, direction.getOpposite(), pos.offset(direction), false);
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }
}