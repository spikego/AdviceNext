package cn.spikego.advicenext.features.module.impl.world;

import cn.spikego.advicenext.features.module.Module;
import cn.spikego.advicenext.features.module.Category;
import cn.spikego.advicenext.features.module.ModuleManager;
import cn.spikego.advicenext.features.value.slider.IntSetting;

public class FastPlace extends Module{
    private static final IntSetting Dealay = new IntSetting("Delay", "Block place delay in ticks", 1, 10, 0,1);

    // 静态实例，便于访问
    private static FastPlace INSTANCE;

    public FastPlace() {
        super("FastPlace", "Place blocks faster", Category.WORLD);
        this.settings.add(Dealay);
        INSTANCE = this;
    }

    /**
     * 静态方法获取模块是否启用 - 使用不同的名称
     */
    public static boolean isEnabled() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    /**
     * 获取延迟设置值
     */
    public static int getDelay() {
        return Dealay.getValue();
    }
}
