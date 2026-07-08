package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;

import java.util.List;

public class Sprint extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Sprint", "Legit", List.of("Legit"));

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
