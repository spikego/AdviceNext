package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("NoJumpDelay", "Prevent jump delay", Category.PLAYER);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        mc.player.jumpingCooldown = 0;
    }
}