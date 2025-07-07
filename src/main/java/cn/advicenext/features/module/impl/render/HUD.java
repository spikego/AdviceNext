package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.HUDEditScreen;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module{

    private final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);
    private final BooleanSetting ArrayList = new BooleanSetting("ArrayList", "Shows enabled modules", true);
    private final BooleanSetting Notification = new BooleanSetting("Notifications", "Shows notifications", true);
    private final BooleanSetting TargetInfo = new BooleanSetting("TargetInfo", "Shows target player info", true);
    private final BooleanSetting HudEdit = new BooleanSetting("HudEdit", "Opens the HUD editor", false);
    public int watermarkX = 10, watermarkY = 10;
    public int arrayListX = -1, arrayListY = 10;
    public int targetInfoX = 10, targetInfoY = -80;

    public HUD() {
        super("HUD", "Render HUD", Category.RENDER);
        this.settings.add(WaterMark);
        this.settings.add(ArrayList);
        this.settings.add(TargetInfo);
        this.settings.add(HudEdit);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (WaterMark.getValue()) {
            int x = watermarkX < 0 ? mc.getWindow().getScaledWidth() + watermarkX : watermarkX;
            int y = watermarkY < 0 ? mc.getWindow().getScaledHeight() + watermarkY : watermarkY;
            event.getContext().drawText(mc.textRenderer, "AdviceNext", x, y, Colors.currentColor().getRGB(), true);
        }

        if (ArrayList.getValue()) {
            renderArrayList(event);
        }

        if(Notification.getValue()) {
            NotificationManager.getInstance().render(event);
        }
        
        if(TargetInfo.getValue()) {
            renderTargetInfo(event);
        }

        if(HudEdit.getValue()) {
            mc.setScreen(new HUDEditScreen());
            HudEdit.setValue(false);
        }
    }

    private void renderArrayList(Render2DEvent event) {
        List<Module> enabledModules = ModuleManager.getModules().stream()
                .filter(Module::getEnabled)
                .sorted(Comparator.comparing(m -> -mc.textRenderer.getWidth(m.getName())))
                .collect(Collectors.toList());

        int startY = arrayListY < 0 ? mc.getWindow().getScaledHeight() + arrayListY : arrayListY;
        int y = startY;
        int screenWidth = mc.getWindow().getScaledWidth();

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            String name = module.getName();
            int width = mc.textRenderer.getWidth(name);
            int x = arrayListX < 0 ? screenWidth + arrayListX - width : arrayListX;

            int color = Colors.gradientColor(i, enabledModules.size()).getRGB();
            event.getContext().drawText(mc.textRenderer, name, x, y, color, true);
            y += 10;
        }
    }

    private void renderTargetInfo(Render2DEvent event) {
        PlayerEntity target = getClosestPlayer();
        if (target == null) return;
        
        int x = targetInfoX < 0 ? mc.getWindow().getScaledWidth() + targetInfoX : targetInfoX;
        int y = targetInfoY < 0 ? mc.getWindow().getScaledHeight() + targetInfoY : targetInfoY;
        int width = 150;
        int height = 60;
        
        event.getContext().fill(x, y, x + width, y + height, 0x80000000);
        
        int borderColor = Colors.currentColor().getRGB();
        event.getContext().fill(x, y, x + width, y + 1, borderColor);
        event.getContext().fill(x, y, x + 1, y + height, borderColor);
        event.getContext().fill(x + width - 1, y, x + width, y + height, borderColor);
        event.getContext().fill(x, y + height - 1, x + width, y + height, borderColor);
        
        String name = target.getName().getString();
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        String distance = String.format("%.1f", mc.player.distanceTo(target));
        
        event.getContext().drawText(mc.textRenderer, name, x + 5, y + 5, 0xFFFFFFFF, true);
        
        int barX = x + 5;
        int barY = y + 18;
        int barWidth = width - 10;
        int barHeight = 8;
        event.getContext().fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        
        int healthWidth = (int) ((health / maxHealth) * barWidth);
        int healthColor = health > maxHealth * 0.6 ? 0xFF00FF00 : health > maxHealth * 0.3 ? 0xFFFFFF00 : 0xFFFF0000;
        event.getContext().fill(barX, barY, barX + healthWidth, barY + barHeight, healthColor);
        
        String healthText = String.format("%.1f / %.1f", health, maxHealth);
        event.getContext().drawText(mc.textRenderer, healthText, x + 5, y + 30, 0xFFFFFFFF, true);
        
        event.getContext().drawText(mc.textRenderer, distance + "m", x + 5, y + 45, 0xFFAAAAAA, true);
    }
    
    private PlayerEntity getClosestPlayer() {
        PlayerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            double distance = mc.player.distanceTo(player);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }
        
        return closest;
    }
}