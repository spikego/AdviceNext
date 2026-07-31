package cn.advicenext.utility.minecraft.network.lag;

import cn.advicenext.features.module.Module;

public class ModuleTimeout extends LagTimeout {
    private final Module module;
    private boolean hasModuleDisabled = false;

    public ModuleTimeout(Module module) {
        this.module = module;
        if (!module.getEnabled()) {
            hasModuleDisabled = true;
        }
    }

    @Override
    protected boolean shouldHaveTimedOut() {
        if (!module.getEnabled()) {
            hasModuleDisabled = true;
        }
        return hasModuleDisabled;
    }
}