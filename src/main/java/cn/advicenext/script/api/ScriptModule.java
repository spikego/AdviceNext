package cn.advicenext.script.api;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;

public abstract class ScriptModule extends Module {
    
    public ScriptModule(String name, String description) {
        super(name, description, Category.MISC);
    }
    
    // 脚本模块的基础实现
    public abstract void onScriptEnable();
    public abstract void onScriptDisable();
    
    @Override
    public void onEnable() {
        onScriptEnable();
    }
    
    @Override
    public void onDisable() {
        onScriptDisable();
    }
}