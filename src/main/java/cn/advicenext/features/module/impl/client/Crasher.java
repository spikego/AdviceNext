package cn.advicenext.features.module.impl.client;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;

import java.util.List;

public class Crasher extends Module{
    private final ModeSetting mode = new ModeSetting("Mode", "Crash", "Crash", List.of("Packet"));

    public Crasher() {
        super("Crasher", "Crasher", Category.CLIENT);
        this.settings.add(mode);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mode.getValue().equals("Packet")) {
            for (int i = 0; i < 100; i++) {
                mc.player.networkHandler.sendChatMessage("/crash" + i);;
            };
        }
    }
}
