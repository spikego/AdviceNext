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
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {

    private final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);
    private final BooleanSetting ArrayList = new BooleanSetting("ArrayList", "Shows enabled modules", true);
    private final BooleanSetting Notification = new BooleanSetting("Notifications", "Shows notifications", true);
    private final BooleanSetting TargetInfo = new BooleanSetting("TargetInfo", "Shows target player info", true);
    private final BooleanSetting HudEdit = new BooleanSetting("HudEdit", "Opens the HUD editor", false);
    public int watermarkX = 10, watermarkY = 10;
    public int arrayListX = -5, arrayListY = 10;
    public int targetInfoX = 520, targetInfoY = 150;

    private PlayerEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float animationProgress = 0f;
    private float targetHealth = 0f;
    private float displayHealth = 0f;

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

        if (Notification.getValue()) {
            NotificationManager.getInstance().render(event);
        }

        if (TargetInfo.getValue()) {
            renderTargetInfo(event);
        }

        if (HudEdit.getValue()) {
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
        updateTarget();

        if (currentTarget == null || System.currentTimeMillis() - lastAttackTime > 3000) {
            animationProgress = Math.max(0, animationProgress - 0.08f);
            if (animationProgress <= 0) {
                currentTarget = null;
                return;
            }
        } else {
            animationProgress = Math.min(1, animationProgress + 0.12f);
        }

        float easeProgress = easeInOutCubic(animationProgress);

        int centerX = mc.getWindow().getScaledWidth() / 2;
        int centerY = mc.getWindow().getScaledHeight() / 2;
        int width = 160;
        int height = 50;

        int x = centerX - width / 2;
        int y = (int) (centerY + 25 + (1 - easeProgress) * 20);
        int alpha = (int) (easeProgress * 120); // 半透明

        String name = currentTarget.getName().getString();
        float health = currentTarget.getHealth();
        float maxHealth = currentTarget.getMaxHealth();

        targetHealth = health;
        displayHealth = lerp(displayHealth, targetHealth, 0.15f);
        int healthPercent = (int) ((displayHealth / maxHealth) * 100);

        int bgColor = (alpha << 24) | 0x1E1E1E;
        event.getContext().fill(x, y, x + width, y + height, bgColor);

        // 顶部彩色条
        int accentColor = (alpha << 24) | (Colors.currentColor().getRGB() & 0xFFFFFF);
        event.getContext().fill(x, y, x + width, y + 3, accentColor);

        int nameColor = (alpha << 24) | 0xFFFFFF;
        int nameX = x + (width - mc.textRenderer.getWidth(name)) / 2;
        event.getContext().drawText(mc.textRenderer, name, nameX, y + 8, nameColor, false);

        int barX = x + 12;
        int barY = y + 25;
        int barWidth = width - 24;
        int barHeight = 4;

        int barBgColor = (alpha << 24) | 0x404040;
        event.getContext().fill(barX, barY, barX + barWidth, barY + barHeight, barBgColor);

        int healthWidth = (int) ((displayHealth / maxHealth) * barWidth);
        if (healthWidth > 0) {
            int healthColor = (alpha << 24) | (getOpaiHealthColor(healthPercent) & 0xFFFFFF);
            event.getContext().fill(barX, barY, barX + healthWidth, barY + barHeight, healthColor);
        }

        String hpText = String.format("%.0f HP", displayHealth);
        int hpX = x + (width - mc.textRenderer.getWidth(hpText)) / 2;
        int hpColor = (alpha << 24) | 0xCCCCCC;
        event.getContext().drawText(mc.textRenderer, hpText, hpX, y + 35, hpColor, false);
    }

    private int getOpaiHealthColor(int healthPercent) {
        if (healthPercent > 80) return 0x4CAF50; // 绿色
        if (healthPercent > 60) return 0x8BC34A; // 浅绿
        if (healthPercent > 40) return 0xFFC107; // 黄色
        if (healthPercent > 20) return 0xFF9800; // 橙色
        return 0xF44336; // 红色
    }

    private int getHealthColor(int healthPercent) {
        if (healthPercent > 75) return 0xFF00FF00; // 绿色
        if (healthPercent > 50) return 0xFFFFFF00; // 黄色
        if (healthPercent > 25) return 0xFFFF8000; // 橙色
        return 0xFFFF0000; // 红色
    }

    private void updateTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            if (entityHit.getEntity() instanceof PlayerEntity) {
                PlayerEntity target = (PlayerEntity) entityHit.getEntity();
                if (target != mc.player) {
                    if (currentTarget != target) {
                        currentTarget = target;
                        displayHealth = target.getHealth();
                    }
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        // 检查攻击事件
        if (mc.options.attackKey.isPressed() && currentTarget != null) {
            lastAttackTime = System.currentTimeMillis();
        }
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private float lerp(float start, float end, float factor) {
        return start + factor * (end - start);
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