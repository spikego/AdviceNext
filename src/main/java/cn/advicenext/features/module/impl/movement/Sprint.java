package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;

import java.util.List;

public class Sprint extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Sprint", "Legit", List.of("Legit","Omni"));

    public Sprint() {
        super("Sprint", "Auto sprint", Category.MOVEMENT);
        this.enabled = false;
        this.settings.add(mode);
    }

    @Override
    public void onTick(TickEvent event) {
        // 检查模块是否启用
        if (!this.enabled) return;

        // 检查玩家和世界是否存在
        if (mc.player != null && mc.world != null) {
            // 检查玩家是否可以冲刺（前进键被按下，且不是在潜行，不是饥饿状态等）
            boolean canSprint = mc.options.forwardKey.isPressed() &&
                    !mc.player.isSneaking() &&
                    !mc.player.isUsingItem() &&
                    mc.player.getHungerManager().getFoodLevel() > 6 &&
                    !mc.player.isTouchingWater();

            // 设置冲刺状态
            mc.player.setSprinting(canSprint);
        }

        if(mode.is("Omni")){
            RotationUtils.resetSilentRotation();
            if(mc.options.forwardKey.isPressed()){
                mc.player.setSprinting(true);
            }
            if(mc.options.backKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw-180, mc.player.getPitch()), MovementCorrection.Mode.STRICT);
            }
            if(mc.options.leftKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw-90, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
            if(mc.options.rightKey.isPressed()) {
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw+90, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
            if(mc.options.forwardKey.isPressed() && mc.options.rightKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw+45, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
            if(mc.options.forwardKey.isPressed() && mc.options.leftKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw-45, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
            if(mc.options.backKey.isPressed() && mc.options.rightKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw+135, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
            if(mc.options.backKey.isPressed() && mc.options.leftKey.isPressed()){
                RotationUtils.setSilentRotation(new RotationUtils.Rotation(mc.player.headYaw-135, mc.player.getPitch()), cn.advicenext.utility.minecraft.movement.MovementCorrection.Mode.STRICT);
            }
        }
    }

    @Override
    public void onDisable() {
        // 当模块禁用时，确保停止冲刺
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }
    
    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}
