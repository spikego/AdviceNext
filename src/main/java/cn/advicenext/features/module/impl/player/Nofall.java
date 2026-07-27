package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class Nofall extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "NoFall mode", "Packet", List.of("Packet", "NoGround","Vulcan"));

    public Nofall() {
        super("Nofall", "Prevents fall damage", Category.PLAYER);
        this.settings.add(mode);
    }

    @Override
    public void onTick(TickEvent event){
        if (mc.player == null) return;

        if(mode.getValue().equals("NoGround")){
            if(mc.player.fallDistance > 3){
                mc.player.setOnGround(true);
            }
        }

        if(mode.getValue().equals("Packet")){
            if(mc.player.fallDistance > 3){
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true,mc.player.horizontalCollision));
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if(mode.is("Vulcan")) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket packet && mc.player.fallDistance > 7.0) {
                // 使用反射设置onGround为true
                try {
                    java.lang.reflect.Field onGroundField = PlayerMoveC2SPacket.class.getDeclaredField("onGround");
                    onGroundField.setAccessible(true);
                    onGroundField.setBoolean(packet, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mc.player.fallDistance = 0.0f;
                mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
            }
        }


    }

    /**
     * Check if the player is above void (no blocks below until world bottom)
     */
    private boolean isAboveVoid() {
        if (mc.player == null) return false;
        
        BlockPos playerPos = mc.player.getBlockPos();
        Box playerBox = mc.player.getBoundingBox();
        
        // Check from player Y down to world bottom (typically Y=-64 or lower)
        int minY = mc.world.getBottomY() - 10;
        
        for (int y = playerPos.getY() - 1; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (!mc.world.getBlockState(checkPos).isAir()) {
                return false; // Found a block below, not above void
            }
        }
        
        return true; // No blocks found below, above void
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}