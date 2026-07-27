package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.combat.KillAura;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.HUDEditScreen;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class HUD extends Module {

    public final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);
    public StringSetting WaterMarkText = new StringSetting("WaterMarkText", "WaterMarkText", "AdviceNext");
    public final BooleanSetting ArrayList = new BooleanSetting("ArrayList", "Shows enabled modules", true);
    public final BooleanSetting Notification = new BooleanSetting("Notifications", "Shows notifications", true);
    public final BooleanSetting TargetInfo = new BooleanSetting("TargetInfo", "Shows target player info", true);
    public final BooleanSetting HudEdit = new BooleanSetting("HudEdit", "Opens the HUD editor", false);
    public int watermarkX = 10, watermarkY = 10;
    public int arrayListX = -5, arrayListY = 10;
    public int targetInfoX = 520, targetInfoY = 150;

    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float animationProgress = 0f;
    private float targetHealth = 0f;
    private float displayHealth = 0f;

    public HUD() {
        super("HUD", "Render HUD", Category.RENDER);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (WaterMark.getValue()) {
            renderWatermark(event);
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

    private void renderWatermark(Render2DEvent event) {
        int x = watermarkX < 0 ? mc.getWindow().getScaledWidth() + watermarkX : watermarkX;
        int y = watermarkY < 0 ? mc.getWindow().getScaledHeight() + watermarkY : watermarkY;
        String text = WaterMarkText.getValue();

        event.getContext().drawText(mc.textRenderer, text, x, y, Colors.currentColor().getRGB(), true);
    }

    private void renderArrayList(Render2DEvent event) {
        int y = arrayListY < 0 ? mc.getWindow().getScaledHeight() + arrayListY : arrayListY;
        int screenWidth = mc.getWindow().getScaledWidth();

        java.util.List<Module> enabledModules = ModuleManager.getModules().stream()
                .filter(Module::getEnabled)
                .sorted((m1, m2) -> Integer.compare(getDisplayWidth(m2), getDisplayWidth(m1)))
                .toList();

        int i = 0;
        for (Module module : enabledModules) {
            String name = module.getName();
            String value = module.getDisplayValue();
            int totalWidth = getDisplayWidth(module);
            int x = arrayListX < 0 ? screenWidth + arrayListX - totalWidth : arrayListX;

            int color = Colors.gradientColor(i, enabledModules.size()).getRGB();
            event.getContext().drawText(mc.textRenderer, name, x, y, color, true);

            if (value != null && !value.isEmpty()) {
                int nameWidth = mc.textRenderer.getWidth(name);
                event.getContext().drawText(mc.textRenderer, " [" + value + "]", x + nameWidth, y, 0xFF808080, true);
            }

            y += 10;
            i++;
        }
    }

    private int getDisplayWidth(Module module) {
        int width = mc.textRenderer.getWidth(module.getName());
        String value = module.getDisplayValue();
        if (value != null && !value.isEmpty()) {
            width += mc.textRenderer.getWidth(" [" + value + "]");
        }
        return width;
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

        int centerX = targetInfoX >= 0 ? targetInfoX + 80 : mc.getWindow().getScaledWidth() / 2;
        int centerY = targetInfoY >= 0 ? targetInfoY : mc.getWindow().getScaledHeight() / 2;
        int width = 160;
        int height = 50;

        int x = centerX - width / 2;
        int y = (int) (centerY + 25 + (1 - easeProgress) * 20);
        int alpha = (int) (easeProgress * 255);

        String name = currentTarget.getName().getString();
        float health = currentTarget.getHealth();
        float maxHealth = currentTarget.getMaxHealth();

        targetHealth = health;
        displayHealth = lerp(displayHealth, targetHealth, 0.15f);
        int healthPercent = (int) ((displayHealth / maxHealth) * 100);

        int bgColor = (Math.min(100, alpha) << 24) | (17 * 0x10000 + 17 * 0x100 + 17);
        int borderColor = Colors.currentColor().getRGB();

        event.getContext().fill(x, y, x + width, y + height, bgColor);
        event.getContext().fill(x, y, x + width, y + 1, borderColor);
        event.getContext().fill(x, y + height - 1, x + width, y + height, borderColor);
        event.getContext().fill(x, y, x + 1, y + height, borderColor);
        event.getContext().fill(x + width - 1, y, x + width, y + height, borderColor);

        int nameColor = -1;
        int nameX = x + (width - mc.textRenderer.getWidth(name)) / 2;
        event.getContext().drawText(mc.textRenderer, name, nameX, y + 8, nameColor, false);

        int barX = x + 12;
        int barY = y + 25;
        int barWidth = width - 24;
        int barHeight = 4;

        event.getContext().fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF202020);

        int healthWidth = (int) ((displayHealth / maxHealth) * barWidth);
        if (healthWidth > 0) {
            int healthColor = getHealthColor(healthPercent);
            event.getContext().fill(barX, barY, barX + healthWidth, barY + barHeight, healthColor);
        }

        String hpText = String.format("%.0f HP", displayHealth);
        int hpX = x + (width - mc.textRenderer.getWidth(hpText)) / 2;
        event.getContext().drawText(mc.textRenderer, hpText, hpX, y + 35, 0xFFCCCCCC, false);
    }

    private int getHealthColor(int healthPercent) {
        if (healthPercent > 80) return 0xFF4CAF50;
        if (healthPercent > 60) return 0xFF8BC34A;
        if (healthPercent > 40) return 0xFFFFC107;
        if (healthPercent > 20) return 0xFFFF9800;
        return 0xFFF44336;
    }

    private void updateTarget() {
        // 优先 KillAura 的目标
        if (KillAura.INSTANCE != null && KillAura.INSTANCE.getEnabled() && KillAura.INSTANCE.getTarget() != null) {
            LivingEntity kaTarget = KillAura.INSTANCE.getTarget();
            if (kaTarget != currentTarget) {
                currentTarget = kaTarget;
                displayHealth = kaTarget.getHealth();
            }
            lastAttackTime = System.currentTimeMillis();
            return;
        }

        if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof PlayerEntity target) {
                if (target != mc.player) {
                    if (currentTarget != target) {
                        currentTarget = target;
                        displayHealth = target.getHealth();
                    }
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

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
}
