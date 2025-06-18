package cn.advicenext.features.module;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.AbstractSetting;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CopyOnWriteArrayList;

public class Module {
    protected String name;
    protected String description;
    protected Category category;
    protected boolean enabled = false;
    protected int key = -1;

    public final CopyOnWriteArrayList<AbstractSetting<?>> settings = new CopyOnWriteArrayList<>();

    protected MinecraftClient mc = MinecraftClient.getInstance();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public void onEnable() {}
    public void onDisable() {}

    public void onTick(TickEvent event) {}

    public void onRender2D(Render2DEvent event) {}

    public void enable() {
        this.enabled = true;
        onEnable();
        NotificationManager.getInstance().addNotification(
                "Module",
                this.name + " has been enabled",
                NotificationManager.NotificationType.INFO,
                3000 // 3秒
        );
    }

    public void disable() {
        this.enabled = false;
        onDisable();
        NotificationManager.getInstance().addNotification(
                "Module",
                this.name + " has been disabled",
                NotificationManager.NotificationType.INFO,
                3000 // 3秒
        );
    }

    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    public void bindKey(int key) {
        this.key = key;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public int getKey() {
        return key;
    }

    public Category getCategory() {
        return category;
    }
}