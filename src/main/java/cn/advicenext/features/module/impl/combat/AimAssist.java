package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.client.RotateUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private final DoubleSetting range = new DoubleSetting("Range", "Target range", 4.0, 6.0, 1.0, 0.1);
    private final DoubleSetting speed = new DoubleSetting("Speed", "Aim assist speed", 0.3, 1.0, 0.1, 0.05);
    private final DoubleSetting fov = new DoubleSetting("FOV", "Field of view", 90.0, 180.0, 30.0, 5.0);
    private final BooleanSetting silent = new BooleanSetting("Silent", "Silent aim assist", true);
    private final BooleanSetting onlyAttacking = new BooleanSetting("Only Attacking", "Only assist when attacking", false);
    
    public AimAssist() {
        super("AimAssist", "Assists with aiming at targets", Category.COMBAT);
        this.settings.add(range);
        this.settings.add(speed);
        this.settings.add(fov);
        this.settings.add(silent);
        this.settings.add(onlyAttacking);
    }
    
    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        if (onlyAttacking.getValue() && !mc.options.attackKey.isPressed()) return;
        
        PlayerEntity target = getClosestPlayer();
        if (target == null) return;
        
        Vec3d targetPos = target.getEyePos();
        Vec3d playerEyePos = mc.player.getEyePos();
        
        // Check if target is within FOV
        if (!isWithinFOV(targetPos, playerEyePos)) return;
        
        RotateUtils.Rotation targetRotation = RotateUtils.getRotationToPos(targetPos, playerEyePos);
        RotateUtils.Rotation currentRotation = new RotateUtils.Rotation(mc.player.getYaw(), mc.player.getPitch());
        
        // Smooth rotation towards target
        RotateUtils.Rotation smoothedRotation = RotateUtils.smoothRotation(
            currentRotation, 
            targetRotation, 
            speed.getValue().floatValue()
        );
        
        if (silent.getValue()) {
            // Silent aim - only send rotation packets
            RotateUtils.setSilentRotation(smoothedRotation, true);
        } else {
            // Normal aim - move camera
            mc.player.setYaw(smoothedRotation.yaw);
            mc.player.setPitch(smoothedRotation.pitch);
        }
    }
    
    private PlayerEntity getClosestPlayer() {
        PlayerEntity closest = null;
        double closestDistance = range.getValue();
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player != mc.player) {
                double distance = mc.player.distanceTo(player);
                if (distance < closestDistance) {
                    closest = player;
                    closestDistance = distance;
                }
            }
        }
        
        return closest;
    }
    
    private boolean isWithinFOV(Vec3d targetPos, Vec3d playerEyePos) {
        Vec3d toTarget = targetPos.subtract(playerEyePos).normalize();
        Vec3d playerLook = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();
        
        double dot = toTarget.dotProduct(playerLook);
        double angle = Math.toDegrees(Math.acos(dot));
        
        return angle <= fov.getValue() / 2.0;
    }
    
    @Override
    public void onDisable() {
        if (silent.getValue()) {
            RotateUtils.resetSilentRotation();
        }
    }
}