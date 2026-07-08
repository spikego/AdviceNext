package cn.advicenext.features.module;

import cn.advicenext.AdviceNext;
import cn.advicenext.event.impl.*;
import cn.advicenext.features.module.impl.client.ClientTheme;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.utility.minecraft.client.SoundUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

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

    public void onPacket(PacketEvent event) {}

    public void onRender3D(Render3DEvent event){}

    public void onMovement(MovementEvent event) {}

    public void enable() {
        this.enabled = true;
        onEnable();
        NotificationManager.getInstance().addNotification(
                "Module",
                this.name + " has been enabled",
                NotificationManager.NotificationType.INFO,
                3000 // 3秒
        );
        SoundUtils.playModuleSound(ClientTheme.INSTANCE.themeSoundStyles.getValue().toLowerCase(), true);
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
        SoundUtils.playModuleSound(ClientTheme.INSTANCE.themeSoundStyles.getValue().toLowerCase(), false);

    }

    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    private void playSound() {
        SoundEvent soundEvent = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        mc.player.playSound(soundEvent, 1.0f, 1.0f);
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
    
    /**
     * 获取模块在ArrayList中显示的value
     * 子类可以重写此方法来显示特定的值
     * @return 显示的value，null或空字符串表示不显示
     */
    public String getDisplayValue() {
        return null;
    }
}