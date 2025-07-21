package cn.advicenext.features.module.impl.world;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.utility.minecraft.client.RotateUtils;
import cn.advicenext.utility.minecraft.client.RotateUtils.Rotation;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class LegitScaffold extends Module {
    // 基础设置
    private final IntSetting sneakDelay = new IntSetting("Sneak Delay", "Delay before sneaking (ms)", 0, 500, 0, 10);
    private final IntSetting unsneakDelay = new IntSetting("Unsneak Delay", "Delay before unsneaking (ms)", 0, 500, 0, 10);
    
    // 辅助设置
    private final BooleanSetting bridgeAssist = new BooleanSetting("Bridge Assist", "Automatically aim at block edge center", false);
    private final DoubleSetting rotationDelay = new DoubleSetting("Rotation Delay", "Delay before rotation (seconds)", 0.0, 1.0, 0.0, 0.05);
    private final BooleanSetting silent = new BooleanSetting("Silent", "Use silent rotation", true);
    
    // 自动放置
    private final BooleanSetting autoPlace = new BooleanSetting("Auto Place", "Automatically place blocks when looking at edge", false);
    
    // 状态变量
    private boolean isSneaking = false;
    private boolean isOnEdge = false;
    private long sneakTime = 0;
    private long unsneakTime = 0;
    private long lastPlaceTime = 0;
    private BlockPos targetPos = null;
    private Direction targetDirection = null;
    
    public LegitScaffold() {
        super("LegitScaffold", "Automatically sneaks at block edges for legit bridging", Category.WORLD);
        this.settings.add(sneakDelay);
        this.settings.add(unsneakDelay);
        this.settings.add(bridgeAssist);
        if(bridgeAssist.getValue()) {
            this.settings.add(rotationDelay);
            this.settings.add(silent);
        }
        this.settings.add(autoPlace);
    }
    
    @Override
    public void onEnable() {
        isSneaking = false;
        isOnEdge = false;
        sneakTime = 0;
        unsneakTime = 0;
        lastPlaceTime = 0;
        targetPos = null;
        targetDirection = null;
    }
    
    @Override
    public void onDisable() {
        // 如果正在蹲下，则取消蹲下
        if (isSneaking && mc.player != null) {
            mc.options.sneakKey.setPressed(false);
            isSneaking = false;
        }
        
        // 重置旋转
        if (bridgeAssist.getValue()) {
            RotateUtils.resetSilentRotation();
        }
    }
    
    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        // 检测玩家是否在方块边缘
        checkEdgeStatus();
        
        // 处理蹲起逻辑
        handleSneaking();
        
        // 处理辅助瞄准
        if (bridgeAssist.getValue() && isOnEdge) {
            handleBridgeAssist();
        }
        
        // 处理自动放置
        if (autoPlace.getValue() && isSneaking && isOnEdge) {
            handleAutoPlace();
        }
    }
    
    private void checkEdgeStatus() {
        // 获取玩家脚下的方块位置
        BlockPos playerPos = mc.player.getBlockPos();
        
        // 获取玩家朝向
        Direction facing = mc.player.getHorizontalFacing();
        
        // 获取玩家前方的方块位置
        BlockPos frontPos = playerPos.offset(facing);
        
        // 检查前方方块是否为空气
        boolean frontAir = mc.world.getBlockState(frontPos).isAir();
        
        // 检查脚下方块是否为实体方块
        boolean standingOnBlock = !mc.world.getBlockState(playerPos.down()).isAir();
        
        // 获取玩家在方块内的位置
        double xInBlock = mc.player.getX() - playerPos.getX();
        double zInBlock = mc.player.getZ() - playerPos.getZ();
        
        // 判断是否在边缘
        boolean onEdge = false;
        
        if (standingOnBlock && frontAir) {
            switch (facing) {
                case NORTH: // Z-
                    onEdge = zInBlock < 0.3;
                    break;
                case SOUTH: // Z+
                    onEdge = zInBlock > 0.7;
                    break;
                case WEST: // X-
                    onEdge = xInBlock < 0.3;
                    break;
                case EAST: // X+
                    onEdge = xInBlock > 0.7;
                    break;
                default:
                    break;
            }
        }
        
        // 更新状态
        isOnEdge = onEdge;
        
        // 保存目标位置和方向
        if (isOnEdge) {
            targetPos = frontPos.down();
            targetDirection = facing;
        } else {
            targetPos = null;
            targetDirection = null;
        }
    }
    
    private void handleSneaking() {
        long currentTime = System.currentTimeMillis();
        
        // 如果在边缘且未蹲下
        if (isOnEdge && !isSneaking) {
            if (sneakTime == 0) {
                sneakTime = currentTime;
            }
            
            // 检查延迟
            if (currentTime - sneakTime >= sneakDelay.getValue()) {
                mc.options.sneakKey.setPressed(true);
                isSneaking = true;
                unsneakTime = 0;
            }
        } 
        // 如果不在边缘且正在蹲下
        else if (!isOnEdge && isSneaking) {
            if (unsneakTime == 0) {
                unsneakTime = currentTime;
            }
            
            // 检查延迟
            if (currentTime - unsneakTime >= unsneakDelay.getValue()) {
                mc.options.sneakKey.setPressed(false);
                isSneaking = false;
                sneakTime = 0;
            }
        }
        // 重置计时器
        else if (!isOnEdge) {
            sneakTime = 0;
        } else if (isOnEdge) {
            unsneakTime = 0;
        }
    }
    
    private void handleBridgeAssist() {
        if (targetPos == null || targetDirection == null) return;
        
        // 计算目标旋转位置
        Vec3d targetVec = calculateTargetPosition();
        if (targetVec == null) return;
        
        // 计算玩家眼睛位置
        Vec3d eyePos = mc.player.getEyePos();
        
        // 计算旋转
        Rotation rotation = RotateUtils.getRotationToPos(targetVec, eyePos);
        
        // 应用旋转
        if (rotationDelay.getValue() > 0) {
            // 使用平滑旋转
            Rotation currentRot = new Rotation(mc.player.getYaw(), mc.player.getPitch());
            float speed = (float)(1.0 / (rotationDelay.getValue() * 20)); // 转换为每tick的速度
            Rotation smoothRot = RotateUtils.smoothRotation(currentRot, rotation, speed);
            
            // 应用旋转
            RotateUtils.setSilentRotation(smoothRot, false);
        } else {
            // 直接旋转
            RotateUtils.setSilentRotation(rotation, false);
        }
    }
    
    private Vec3d calculateTargetPosition() {
        if (targetPos == null || targetDirection == null) return null;
        
        // 计算目标位置
        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 1.0;
        double z = targetPos.getZ() + 0.5;
        
        // 根据方向调整位置，使其指向方块边缘中心
        switch (targetDirection) {
            case NORTH:
                z += 0.5;
                break;
            case SOUTH:
                z -= 0.5;
                break;
            case WEST:
                x += 0.5;
                break;
            case EAST:
                x -= 0.5;
                break;
            default:
                break;
        }
        
        return new Vec3d(x, y, z);
    }
    
    private void handleAutoPlace() {
        if (targetPos == null || targetDirection == null) return;
        
        // 检查是否有方块在手中
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        
        // 获取玩家视线
        BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
        if (hitResult == null) return;
        
        // 检查是否看向有效位置
        BlockPos hitPos = hitResult.getBlockPos();
        Direction hitFace = hitResult.getSide();
        
        // 检查是否是有效的放置位置
        if (!isValidPlacement(hitPos, hitFace)) return;
        
        // 放置方块
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastPlaceTime = System.currentTimeMillis();
    }
    
    private boolean isValidPlacement(BlockPos pos, Direction face) {
        // 检查目标位置是否为空气
        BlockState state = mc.world.getBlockState(pos.offset(face));
        if (!state.isAir()) return false;
        
        // 检查是否是向下放置
        return face == Direction.DOWN || pos.offset(face).equals(targetPos);
    }
}