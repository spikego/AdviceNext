package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.player.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class Clutch extends Module {
    private final IntSetting placeDelay = new IntSetting("Place Delay", "Place block dealy.", 0, 4, 0, 1);
    private final IntSetting chance = new IntSetting("Chance", "Chance to place block.", 0, 100, 0, 1);
    private final IntSetting maxBlocks = new IntSetting("Max Blocks", "Max blocks to place.", 6, 8, 1, 1);
    private final BooleanSetting SilentRotation = new BooleanSetting("Silent Rotation", "Silent rotation.", true);
    private final BooleanSetting movementFix = new BooleanSetting("movementFix", "fix movement.", true);
    private final ModeSetting switchMode = new ModeSetting("Switch Mode", "Switch mode.", "Silent", List.of("Auto", "Silent", "None"));
    
    private boolean isFalling = false;
    private int blocksPlaced = 0;
    private long lastPlaceTime = 0;
    private int originalSlot = -1;
    private BlockPos lastPlacedPos = null;
    private final Random random = new Random();
    
    public Clutch() {
        super("Clutch", "Automatically places blocks under you when falling", Category.WORLD);
        this.settings.add(placeDelay);
        this.settings.add(chance);
        this.settings.add(maxBlocks);
        this.settings.add(SilentRotation);
        this.settings.add(movementFix);
        this.settings.add(switchMode);
    }
    
    @Override
    public void onEnable() {
        isFalling = false;
        blocksPlaced = 0;
        lastPlaceTime = 0;
        originalSlot = -1;
        lastPlacedPos = null;
        RotationUtils.resetSilentRotation();
    }
    
    @Override
    public void onDisable() {
        // 如果是Silent模式，恢复原来的物品栏选择
        if (originalSlot != -1 && mc.player != null && switchMode.getValue().equals("Silent")) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        
        isFalling = false;
        blocksPlaced = 0;
        lastPlacedPos = null;
        RotationUtils.resetSilentRotation();
    }
    
    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        // 检测玩家是否在下落
        boolean currentlyFalling = mc.player.fallDistance > 2.0f && !mc.player.isOnGround() && 
                                  !mc.player.isUsingItem() && !mc.player.isClimbing() && !mc.player.isTouchingWater();
        
        // 检测是否在虚空中
        boolean inVoid = mc.player.getY() < mc.world.getBottomY();
        
        // 如果玩家开始下落或在虚空中
        if ((currentlyFalling || inVoid) && !isFalling) {
            isFalling = true;
            blocksPlaced = 0;
            lastPlaceTime = System.currentTimeMillis();
        }
        
        // 如果玩家停止下落
        if (!currentlyFalling && !inVoid && isFalling) {
            resetClutchState();
            return;
        }
        
        // 如果玩家正在下落，尝试放置方块
        if (isFalling) {
            // 检查随机几率
            if (chance.getValue() < 100 && random.nextInt(100) >= chance.getValue()) {
                return;
            }
            
            // 检查延迟
            if (System.currentTimeMillis() - lastPlaceTime < placeDelay.getValue() * 50) {
                return;
            }
            
            // 检查已放置方块数量
            if (blocksPlaced >= maxBlocks.getValue()) {
                resetClutchState();
                return;
            }
            
            // 尝试放置方块 - 始终使用方块自救
            tryPlaceBlock();
        }
    }
    
    private void resetClutchState() {
        isFalling = false;
        blocksPlaced = 0;
        lastPlacedPos = null;
        
        // 如果是Silent模式，恢复原来的物品栏选择
        if (originalSlot != -1 && switchMode.getValue().equals("Silent")) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        
        RotationUtils.resetSilentRotation();
    }
    
    private void tryPlaceBlock() {
        // 查找可放置的方块位置
        BlockPos targetPos = findPlacePosition();
        if (targetPos == null) return;
        
        // 查找可用的方块 - 必须有方块才能自救
        int blockSlot = findBlockInHotbar();
        if (blockSlot == -1) return;
        
        // 保存原始选择的物品栏
        String currentMode = switchMode.getValue();
        if (currentMode.equals("Silent") && originalSlot == -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }
        
        // 切换到方块
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (currentMode.equals("Auto") || currentMode.equals("Silent")) {
            mc.player.getInventory().setSelectedSlot(blockSlot);
        } else if (currentMode.equals("None") && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            // None模式下不切换物品栏，但必须有方块
            return;
        }
        
        // 计算放置方向
        Direction direction = getPlaceDirection(targetPos);
        if (direction == null) return;
        
        // 计算目标方块的相邻方块
        BlockPos neighborPos = targetPos.offset(direction);
        
        // 应用旋转 - 使用RotateUtils.setSilentRotation方法
        if (SilentRotation.getValue()) {
            RotationUtils.Rotation rotation = calculateRotation(neighborPos, direction);
            RotationUtils.setSilentRotation(rotation);
        }
        
        // 应用移动修复 - 只有在启用movementFix时才考虑移动合法性
        if (movementFix.getValue() && !SilentRotation.getValue()) {
            // 停止水平移动
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            
            // 将玩家移动到方块中心
            double centerX = targetPos.getX() + 0.5;
            double centerZ = targetPos.getZ() + 0.5;
            mc.player.updatePosition(centerX, mc.player.getY(), centerZ);
        }
        
        // 创建方块放置结果
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(neighborPos.getX() + 0.5, neighborPos.getY() + 0.5, neighborPos.getZ() + 0.5),
                direction.getOpposite(),
                neighborPos,
                false
        );
        
        // 放置方块
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        // 记录放置时间和位置
        lastPlaceTime = System.currentTimeMillis();
        lastPlacedPos = targetPos;
        blocksPlaced++;
        
        // 验证方块是否成功放置
        verifyBlockPlacement(targetPos);
        
        // 如果是Silent模式，立即切换回原来的物品栏
        if (currentMode.equals("Silent")) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }
    
    private void verifyBlockPlacement(BlockPos pos) {
        // 等待一个游戏刻以确保方块状态更新
        mc.execute(() -> {
            if (mc.world != null) {
                BlockState state = mc.world.getBlockState(pos);
                if (state.isAir()) {
                    // 方块放置失败，尝试再次放置
                    if (blocksPlaced > 0) blocksPlaced--;
                }
            }
        });
    }
    
    private BlockPos findPlacePosition() {
        if (mc.player == null) return null;
        
        // 获取玩家脚下的方块位置
        BlockPos playerPos = mc.player.getBlockPos().down();
        
        // 检查该位置是否可以放置方块
        if (canPlaceBlockAt(playerPos)) {
            return playerPos;
        }
        
        // 如果玩家脚下不能放置，检查周围的方块
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue; // 不检查上方
            
            BlockPos offsetPos = playerPos.offset(direction);
            if (canPlaceBlockAt(offsetPos)) {
                return offsetPos;
            }
        }
        
        return null;
    }
    
    private boolean canPlaceBlockAt(BlockPos pos) {
        if (mc.world == null) return false;
        
        // 检查位置是否已经有方块
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        // 检查是否有相邻方块可以依附
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighborPos);
            
            if (!neighborState.isAir() && !neighborState.getBlock().equals(Blocks.FIRE) && 
                !neighborState.getBlock().equals(Blocks.LAVA) && !neighborState.getBlock().equals(Blocks.WATER)) {
                return true;
            }
        }
        
        return false;
    }
    
    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.world == null) return null;
        
        // 检查每个方向是否有可以依附的方块
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighborPos);
            
            if (!neighborState.isAir() && !neighborState.getBlock().equals(Blocks.FIRE) && 
                !neighborState.getBlock().equals(Blocks.LAVA) && !neighborState.getBlock().equals(Blocks.WATER)) {
                return direction;
            }
        }
        
        return null;
    }
    
    private int findBlockInHotbar() {
        if (mc.player == null) return -1;
        
        // 检查主手是否已经持有方块
        ItemStack mainHandStack = mc.player.getMainHandStack();
        if (mainHandStack.getItem() instanceof BlockItem && isValidBlock(((BlockItem) mainHandStack.getItem()).getBlock())) {
            return mc.player.getInventory().getSelectedSlot();
        }
        
        // 检查快捷栏中的方块
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem && isValidBlock(((BlockItem) stack.getItem()).getBlock())) {
                return i;
            }
        }
        
        return -1;
    }
    
    private boolean isValidBlock(Block block) {
        // 检查方块是否适合放置（排除不稳定的方块）
        return !block.equals(Blocks.SAND) && 
               !block.equals(Blocks.GRAVEL) && 
               !block.equals(Blocks.ANVIL) && 
               !block.equals(Blocks.DRAGON_EGG) && 
               !block.equals(Blocks.SCAFFOLDING);
    }
    
    private RotationUtils.Rotation calculateRotation(BlockPos pos, Direction direction) {
        // 计算目标位置
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = new Vec3d(
                pos.getX() + 0.5 + direction.getOpposite().getOffsetX() * 0.5,
                pos.getY() + 0.5 + direction.getOpposite().getOffsetY() * 0.5,
                pos.getZ() + 0.5 + direction.getOpposite().getOffsetZ() * 0.5
        );
        
        // 使用RotateUtils的getRotationToPos方法计算旋转
        return RotationUtils.getRotationToPos(targetPos, eyePos);
    }
}