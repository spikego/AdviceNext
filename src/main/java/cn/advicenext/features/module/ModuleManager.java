package cn.advicenext.features.module;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.advicenext.AdviceNext;
import cn.advicenext.features.module.impl.combat.*;
import cn.advicenext.features.module.impl.movement.*;
import cn.advicenext.features.module.impl.render.*;
import cn.advicenext.features.module.impl.misc.*;
import cn.advicenext.features.module.impl.world.*;
import cn.advicenext.features.module.impl.client.*;
import cn.advicenext.features.module.impl.player.*;
import cn.advicenext.features.value.AbstractSetting;

public final class ModuleManager {
    public static final CopyOnWriteArrayList<Module> modules = new CopyOnWriteArrayList<>();
    private ModuleManager() {}

    public static void initialize() {
        AdviceNext.LOGGER.info("ModuleManager is initializing!");
        addModule(new Sprint());
        addModule(new HUD());
        addModule(new ClickGui());
        addModule(new AutoClicker());
        addModule(new FastPlace());
        addModule(new Timer());
        addModule(new Speed());
        addModule(new Crasher());
        addModule(new ESP());
        addModule(new MotionCamera());
        addModule(new Blink());
        addModule(new NoHurtCam());
        addModule(new ClientTheme());
        addModule(new KeepSprint());
        addModule(new Fly());
    }

    private static void addModule(Module module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object obj = field.get(module);
                if (obj instanceof AbstractSetting<?>) {
                    module.settings.add((AbstractSetting<?>) obj);
                }
            } catch (IllegalAccessException ignored) {}
        }
        modules.add(module);
    }

    public static List<Module> getModules() {
        return modules;
    }
}