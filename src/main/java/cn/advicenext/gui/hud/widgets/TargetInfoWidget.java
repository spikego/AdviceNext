package cn.advicenext.gui.hud.widgets;

import cn.advicenext.features.module.impl.combat.KillAura;
import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class TargetInfoWidget extends Widget {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float animationProgress = 0f;
    private float targetHealth = 0f;
    private float displayHealth = 0f;

    private final List<TargetEntry> nearbyTargets = new ArrayList<>();

    public TargetInfoWidget() {
        super("targetinfo", 520, 150, 160, 50);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        HUD hud = HUD.getHudInstance();
        boolean isNewMode = hud != null && hud.TargetInfoMode.is("New");

        if (isNewMode) {
            renderNewMode(context, delta);
        } else {
            renderDefaultMode(context, delta);
        }
    }

    private void renderDefaultMode(DrawContext context, float delta) {
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

        HUD hud = HUD.getHudInstance();
        boolean blur = hud != null && hud.TIBlur.getValue();

        float easeProgress = easeInOutCubic(animationProgress);

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        float centerX = x >= 0 ? x + width / 2 : screenW / 2f;
        float centerY = y >= 0 ? y : screenH / 2f;

        float rx = centerX - width / 2;
        float ry = centerY + 25 + (1 - easeProgress) * 20;
        int alpha = (int) (easeProgress * 255);

        String name = currentTarget.getName().getString();
        float health = currentTarget.getHealth();
        float maxHealth = currentTarget.getMaxHealth();

        targetHealth = health;
        displayHealth = lerp(displayHealth, targetHealth, 0.15f);
        int healthPercent = (int) ((displayHealth / maxHealth) * 100);

        boolean isAttacking = KillAura.INSTANCE != null
            && KillAura.INSTANCE.getEnabled()
            && KillAura.INSTANCE.getTarget() == currentTarget;

        if (isAttacking) {
            int glowColor = Colors.currentColor().getRGB();
            float glowPad = 3;
            SkijaUIRenderer.drawRoundedRect("ti_default_glow", rx - glowPad, ry - glowPad,
                width + glowPad * 2, height + glowPad * 2, 10, (60 << 24) | (glowColor & 0x00FFFFFF));
        }

        if (blur) {
            drawBlurredBackground(rx, ry, width, height, 8);
        }

        int bgColor = (Math.min(100, alpha) << 24) | 0x111111;
        SkijaUIRenderer.drawRoundedRect("ti_bg", rx, ry, width, height, 8, bgColor);

        int nameX = (int) (rx + (width - Fonts.inter.get(12).getStringWidth(name)) / 2);
        Fonts.inter.get(12).drawString(name, nameX, ry + 8, -1);

        float barX = rx + 12;
        float barY = ry + 25;
        float barW = width - 24;
        float barH = 4;

        SkijaUIRenderer.drawRoundedRect(barX, barY, barW, barH, 2, 0xFF202020);

        float healthW = (displayHealth / maxHealth) * barW;
        if (healthW > 0) {
            int healthColor = getHealthColor(healthPercent);
            SkijaUIRenderer.drawRoundedRect(barX, barY, healthW, barH, 2, healthColor);
        }

        String hpText = String.format("%.0f HP", displayHealth);
        int hpX = (int) (rx + (width - Fonts.roboto.get(8).getStringWidth(hpText)) / 2);
        Fonts.roboto.get(8).drawString(hpText, hpX, ry + 35, 0xFFCCCCCC);
    }

    private void renderNewMode(DrawContext context, float delta) {
        updateTargetNew();
        if (currentTarget == null) return;

        HUD hud = HUD.getHudInstance();
        boolean followTarget = hud != null && hud.TIFollowTarget.getValue();
        boolean blur = hud != null && hud.TIBlur.getValue();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        boolean isAttacking = KillAura.INSTANCE != null
            && KillAura.INSTANCE.getEnabled()
            && KillAura.INSTANCE.getTarget() == currentTarget;

        float panelW = followTarget ? 140 : 180;
        float panelH = followTarget ? 44 : 56;
        float avatarSize = followTarget ? 28 : 36;
        float panelX, panelY;

        if (followTarget) {
            Vec3d projected = Render3DEngine.worldToScreen(
                currentTarget.getX(),
                currentTarget.getY() + currentTarget.getHeight() + 0.3,
                currentTarget.getZ()
            );

            if (projected != null && projected.z > 0) {
                panelX = (float) projected.x - panelW / 2;
                panelY = (float) projected.y - panelH - 6;
            } else {
                panelX = screenW / 2f - panelW / 2;
                panelY = screenH / 2f - 80;
            }
        } else {
            panelX = x >= 0 ? x : screenW / 2f - panelW / 2;
            panelY = y >= 0 ? y : screenH / 2f - 80;
        }

        panelX = clamp(panelX, 10, screenW - panelW - 10);
        panelY = clamp(panelY, 10, screenH - panelH - 10);

        int bgAlpha = isAttacking ? 140 : 100;
        int bgColor = (bgAlpha << 24) | 0x111111;

        if (isAttacking) {
            int glowColor = Colors.currentColor().getRGB();
            float glowPad = 2;
            SkijaUIRenderer.drawRoundedRect("ti_new_glow", panelX - glowPad, panelY - glowPad,
                panelW + glowPad * 2, panelH + glowPad * 2, 12, (60 << 24) | (glowColor & 0x00FFFFFF));
        }

        if (blur) {
            drawBlurredBackground(panelX, panelY, panelW, panelH, 10);
        }

        SkijaUIRenderer.drawRoundedRect("ti_new_bg", panelX, panelY, panelW, panelH, 10, bgColor);

        float avatarX = panelX + 10;
        float avatarY = panelY + (panelH - avatarSize) / 2;

        if (currentTarget instanceof PlayerEntity player) {
            SkijaUIRenderer.drawPlayerHeadRound(
                player.getGameProfile(),
                avatarX, avatarY, avatarSize, avatarSize / 2);
        } else {
            SkijaUIRenderer.drawCircle("ti_avatar_circle", avatarX + avatarSize / 2,
                avatarY + avatarSize / 2, avatarSize, 0xFF555555);
        }

        float textX = avatarX + avatarSize + 10;
        float textY = avatarY + 4;
        float nameFontSize = followTarget ? 10 : 12;
        float smallFontSize = followTarget ? 7 : 8;

        String name = currentTarget.getName().getString();
        Fonts.inter.get(nameFontSize).drawString(name, textX, textY, -1);

        float health = currentTarget.getHealth();
        float maxHealth = currentTarget.getMaxHealth();
        targetHealth = health;
        displayHealth = lerp(displayHealth, targetHealth, 0.15f);
        int healthPercent = (int) ((displayHealth / maxHealth) * 100);

        float barX = textX;
        float barY = textY + 16;
        float barW = panelW - textX - 10;
        float barH = 4;

        SkijaUIRenderer.drawRoundedRect(barX, barY, barW, barH, 2, 0xFF202020);
        float healthW = (displayHealth / maxHealth) * barW;
        if (healthW > 0) {
            int healthColor = getHealthColor(healthPercent);
            SkijaUIRenderer.drawRoundedRect(barX, barY, healthW, barH, 2, healthColor);
        }

        String hpText = String.format("%.0f / %.0f", displayHealth, maxHealth);
        float hpY = barY + 10;
        Fonts.roboto.get(smallFontSize).drawString(hpText, textX, hpY, 0xFFCCCCCC);

        String distText = String.format("%.1f blocks", mc.player.distanceTo(currentTarget));
        float distW = Fonts.roboto.get(smallFontSize).getStringWidth(distText);
        Fonts.roboto.get(smallFontSize).drawString(distText, panelX + panelW - distW - 10, hpY, 0xFF888888);
    }

    private void updateTargetNew() {
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
            if (entityHit.getEntity() instanceof LivingEntity target) {
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

    private int getHealthColor(int healthPercent) {
        if (healthPercent > 80) return 0xFF4CAF50;
        if (healthPercent > 60) return 0xFF8BC34A;
        if (healthPercent > 40) return 0xFFFFC107;
        if (healthPercent > 20) return 0xFFFF9800;
        return 0xFFF44336;
    }

    private void updateTarget() {
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

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private float lerp(float start, float end, float factor) {
        return start + factor * (end - start);
    }

    private void drawBlurredBackground(float x, float y, float w, float h, float radius) {
        // 使用多层半透明叠加模拟高斯模糊效果
        float pad = 4;
        int[] alphas = {18, 22, 28, 35};
        float[] expansions = {pad, pad * 0.65f, pad * 0.35f, pad * 0.1f};

        for (int i = 0; i < alphas.length; i++) {
            float expand = expansions[i];
            SkijaUIRenderer.drawRoundedRect(
                "ti_blur_" + i,
                x - expand, y - expand,
                w + expand * 2, h + expand * 2,
                radius + expand,
                (alphas[i] << 24) | 0x000000
            );
        }
    }

    private static class TargetEntry {
        final LivingEntity entity;
        final float health;
        final float distance;

        TargetEntry(LivingEntity entity, float health, float distance) {
            this.entity = entity;
            this.health = health;
            this.distance = distance;
        }
    }
}