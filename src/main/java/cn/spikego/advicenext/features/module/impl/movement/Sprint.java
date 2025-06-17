package cn.spikego.advicenext.features.module.impl.movement;

import cn.spikego.advicenext.event.impl.TickEvent;
import cn.spikego.advicenext.features.module.Category;
import cn.spikego.advicenext.features.module.Module;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Auto sprint", Category.MOVEMENT);
        this.enabled = false;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player != null && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }
}
