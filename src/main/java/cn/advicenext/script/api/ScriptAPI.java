package cn.advicenext.script.api;

import cn.advicenext.event.EventBus;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.notification.NotificationManager.NotificationType;
import net.minecraft.client.MinecraftClient;

public class ScriptAPI {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    
    // 通知API
    public static void notify(String title, String message) {
        NotificationManager.getInstance().addNotification(title, message, NotificationType.INFO, 3000);
    }
    
    public static void notifySuccess(String title, String message) {
        NotificationManager.getInstance().addNotification(title, message, NotificationType.SUCCESS, 3000);
    }
    
    public static void notifyWarning(String title, String message) {
        NotificationManager.getInstance().addNotification(title, message, NotificationType.WARNING, 3000);
    }
    
    public static void notifyError(String title, String message) {
        NotificationManager.getInstance().addNotification(title, message, NotificationType.ERROR, 3000);
    }
    
    // 模块API
    public static Module getModule(String name) {
        return ModuleManager.getModules().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    public static void toggleModule(String name) {
        Module module = getModule(name);
        if (module != null) {
            module.toggle();
        }
    }
    
    public static void enableModule(String name) {
        Module module = getModule(name);
        if (module != null) {
            module.enable();
        }
    }
    
    public static void disableModule(String name) {
        Module module = getModule(name);
        if (module != null) {
            module.disable();
        }
    }
    
    // 事件API
    public static void registerEvent(Object listener) {
        EventBus.register(listener);
    }
    
    public static void unregisterEvent(Object listener) {
        EventBus.unregister(listener);
    }
}