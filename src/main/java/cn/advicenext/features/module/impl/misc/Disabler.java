package cn.advicenext.features.module.impl.misc;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.ModeSetting;

import java.util.List;

public class Disabler extends Module{
    private final ModeSetting anticheat = new ModeSetting("Anti-Cheat", "Select the disabler mode", "Watchdog", List.of("Watchdog","GrimAC"));
    public Disabler() {
        super("Disabler", "Disable some anticheat",Category.MISC);
        this.settings.add(anticheat);
    }



    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
    }
}
