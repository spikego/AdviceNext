package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.impl.combat.AntiBot;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.Render2DEngine;
import cn.advicenext.utility.client.render.Render3DEngine;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.List;

/**
 * ESP：透视实体。
 *
 * 模式：
 * <ul>
 *   <li><b>Classic</b> — 2D 矩形框 + 可选 Glow 发光。</li>
 *   <li><b>New</b> — 2D 圆角框（Skija 渲染） + 可选 Glow 发光。</li>
 *   <li><b>3D</b> — 真 3D 线框（世界空间渲染，通过 Render3DEvent）。</li>
 *   <li><b>Outline</b> — 模型轮廓（MixinLivingEntityRenderer）。</li>
 * </ul>
 */
public class ESP extends Module {
    public static ESP INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "ESP mode", "Classic", List.of("Classic", "New", "3D", "Outline"));
    private final ModeSetting colorMode = new ModeSetting("Color", "ESP color mode", "Client", List.of("Client", "Team", "White", "Red", "Green"));
    private final BooleanSetting glow = new BooleanSetting("Glow", "Glow effect around boxes", true, () -> mode.is("Classic") || mode.is("New"));
    private final DoubleSetting radius = new DoubleSetting("Corner Radius", "Box corner radius", 4.0, 10.0, 0.0, 0.5, () -> mode.is("Classic") || mode.is("New"));
    private final BooleanSetting filledBox = new BooleanSetting("Filled", "Fill 3D boxes", true, () -> mode.is("3D"));
    private final BooleanSetting showName = new BooleanSetting("Names", "Show entity names", true);
    private final BooleanSetting showHealth = new BooleanSetting("Health", "Show health bar", true);
    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", "Only render players", true);
    private final BooleanSetting self = new BooleanSetting("Self", "Render yourself", false);

    public ESP() {
        super("ESP", "See entities through walls", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }

    // ==================== 渲染 ====================

    @Override
    public void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (mode.is("3D")) {
            return;
        }
        if (mode.is("Outline")) {
            renderOutline(event);
            return;
        }
        if (mode.is("New")) {
            renderNewBoxes(event);
            return;
        }
        if (mode.is("Classic")) {
            renderClassicBoxes(event);
        }
    }

    private void renderClassicBoxes(Render2DEvent event) {
        DrawContext ctx = event.getContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float w = entity.getWidth() / 2.0f;
            float h = entity.getHeight();

            float[] bounds = projectBounds(x, y, z, w, h);
            if (bounds == null) continue;

            float minX = Math.max(0, bounds[0]);
            float minY = Math.max(0, bounds[1]);
            float maxX = Math.min(screenWidth, bounds[2]);
            float maxY = Math.min(screenHeight, bounds[3]);

            if (maxX - minX < 2 || maxY - minY < 2) continue;

            int color = getColor(entity).getRGB();
            int outlineColor = Render2DEngine.withAlpha(color, 230);
            int fillColor = Render2DEngine.withAlpha(color, 45);

            // 填充
            ctx.fill((int) minX, (int) minY, (int) maxX, (int) maxY, fillColor);

            // 边框（横线 + 竖线，LiquidBounce 风格）
            float thickness = 1.0F;
            // 上边
            ctx.fill((int) minX, (int) minY, (int) maxX, (int) (minY + thickness), outlineColor);
            // 下边
            ctx.fill((int) minX, (int) (maxY - thickness), (int) maxX, (int) maxY, outlineColor);
            // 左边
            ctx.fill((int) minX, (int) minY, (int) (minX + thickness), (int) maxY, outlineColor);
            // 右边
            ctx.fill((int) (maxX - thickness), (int) minY, (int) maxX, (int) maxY, outlineColor);

            drawNameAndHealth(event, entity, minX, minY, maxX, maxY);
        }
    }

    private void renderNewBoxes(Render2DEvent event) {
        DrawContext ctx = event.getContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();
        float cornerRadius = radius.getValue().floatValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float w = entity.getWidth() / 2.0f;
            float h = entity.getHeight();

            float[] bounds = projectBounds(x, y, z, w, h);
            if (bounds == null) continue;

            float minX = Math.max(0, bounds[0]);
            float minY = Math.max(0, bounds[1]);
            float maxX = Math.min(screenWidth, bounds[2]);
            float maxY = Math.min(screenHeight, bounds[3]);

            if (maxX - minX < 2 || maxY - minY < 2) continue;

            int color = getColor(entity).getRGB();
            float boxW = maxX - minX;
            float boxH = maxY - minY;

            if (glow.getValue()) {
                Render2DEngine.drawDropShadow(ctx, minX, minY, boxW, boxH, cornerRadius, 6);
                Render2DEngine.drawGlow(ctx, minX, minY, boxW, boxH, cornerRadius, color, 5);
            }

            int fillColor = Render2DEngine.withAlpha(color, 45);
            SkijaUIRenderer.drawRoundedRect("esp_new_fill_" + entity.getId(),
                minX, minY, boxW, boxH, cornerRadius, fillColor);

            int outlineColor = Render2DEngine.withAlpha(color, 230);
            Render2DEngine.drawRoundOutline(ctx, minX, minY, boxW, boxH, cornerRadius, 1.5F, outlineColor);

            drawNameAndHealth(event, entity, minX, minY, maxX, maxY);
        }
    }

    private void renderOutline(Render2DEvent event) {
        DrawContext ctx = event.getContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float w = entity.getWidth() / 2.0f;
            float h = entity.getHeight();

            float[] bounds = projectBounds(x, y, z, w, h);
            if (bounds == null) continue;

            float minX = Math.max(0, bounds[0]);
            float minY = Math.max(0, bounds[1]);
            float maxX = Math.min(screenWidth, bounds[2]);
            float maxY = Math.min(screenHeight, bounds[3]);

            if (maxX - minX < 2 || maxY - minY < 2) continue;

            int color = getColor(entity).getRGB();
            int outlineColor = Render2DEngine.withAlpha(color, 240);
            float thickness = 1.0F;

            ctx.fill((int) minX, (int) minY, (int) maxX, (int) (minY + thickness), outlineColor);
            ctx.fill((int) minX, (int) (maxY - thickness), (int) maxX, (int) maxY, outlineColor);
            ctx.fill((int) minX, (int) minY, (int) (minX + thickness), (int) maxY, outlineColor);
            ctx.fill((int) (maxX - thickness), (int) minY, (int) maxX, (int) maxY, outlineColor);

            drawNameAndHealth(event, entity, minX, minY, maxX, maxY);
        }
    }

    private float[] projectBounds(double x, double y, double z, float w, float h) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean visible = false;

        double[][] corners = {
            {x - w, y, z - w}, {x + w, y, z - w},
            {x + w, y, z + w}, {x - w, y, z + w},
            {x - w, y + h, z - w}, {x + w, y + h, z - w},
            {x + w, y + h, z + w}, {x - w, y + h, z + w}
        };

        for (double[] c : corners) {
            Vec3d projected = Render3DEngine.worldToScreen(c[0], c[1], c[2]);
            if (projected != null) {
                visible = true;
                minX = Math.min(minX, (float) projected.x);
                minY = Math.min(minY, (float) projected.y);
                maxX = Math.max(maxX, (float) projected.x);
                maxY = Math.max(maxY, (float) projected.y);
            }
        }
        return visible ? new float[] {minX, minY, maxX, maxY} : null;
    }

    /** 框外发光（多层外扩描边，不填充内部） */
    private void drawBoxGlow(DrawContext ctx, float minX, float minY, float maxX, float maxY, float radius, int color) {
        int baseAlpha = 90;
        for (int i = 4; i >= 1; i--) {
            float factor = (float) i / 4.0F;
            int alpha = (int) (baseAlpha * (1.0F - factor) * (1.0F - factor));
            if (alpha <= 0) continue;
            Render2DEngine.drawRoundOutline(ctx,
                minX - i, minY - i,
                (maxX - minX) + i * 2, (maxY - minY) + i * 2,
                radius + i, 1.0F,
                (alpha << 24) | (color & 0x00FFFFFF));
        }
    }

    private void drawNameAndHealth(Render2DEvent event, Entity entity, float minX, float minY, float maxX, float maxY) {
        DrawContext ctx = event.getContext();

        if (showName.getValue() && entity instanceof LivingEntity) {
            String name = entity.getName().getString();
            int nameWidth = mc.textRenderer.getWidth(name);
            int nameX = (int) ((minX + maxX) / 2 - nameWidth / 2);
            int nameY = (int) (minY - 10);
            ctx.drawText(mc.textRenderer, name, nameX, nameY, 0xFFFFFFFF, true);
        }

        if (showHealth.getValue() && entity instanceof LivingEntity living) {
            float healthPercent = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0, 1);

            int barX = (int) (minX - 5);
            int barY = (int) minY;
            int barHeight = (int) (maxY - minY);

            // 背景 + 血量（圆角细条）
            Render2DEngine.fillRoundRect(ctx, barX, barY, 3, barHeight, 1.5F, 0x90000000);
            int healthHeight = (int) (barHeight * healthPercent);
            if (healthHeight > 0) {
                int healthColor = getHealthColor(healthPercent).getRGB();
                Render2DEngine.fillRoundRect(ctx, barX, (int) maxY - healthHeight, 3, healthHeight, 1.5F, healthColor);
            }
        }
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player && !self.getValue()) return false;
        if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) return false;
        if (entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity p && AntiBot.isBotStatic(p)) return false;
        return entity instanceof LivingEntity;
    }

    private Color getColor(Entity entity) {
        if (colorMode.is("Team") && entity instanceof PlayerEntity player && mc.player != null) {
            return player.isTeammate(mc.player) ? Color.GREEN : Color.RED;
        }
        return switch (colorMode.getValue()) {
            case "White" -> Color.WHITE;
            case "Red" -> Color.RED;
            case "Green" -> Color.GREEN;
            default -> Colors.currentColor();
        };
    }

    private Color getHealthColor(float healthPercent) {
        int r = (int) ((1 - healthPercent) * 255);
        int g = (int) (healthPercent * 255);
        return new Color(r, g, 0, 255);
    }

    public boolean isOutlineMode() {
        return getEnabled() && mode.is("Outline");
    }

    public boolean is3DMode() {
        return getEnabled() && mode.is("3D");
    }

    public Color getOutlineColor(Entity entity) {
        return getColor(entity);
    }

    @Override
    public void onRender3D(cn.advicenext.event.impl.Render3DEvent event) {
        if (!is3DMode()) return;
        if (mc.world == null || mc.player == null) return;

        VertexConsumer vertexConsumer = event.getVertexConsumers().getBuffer(RenderLayers.lines());

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            int color = getColor(entity).getRGB();
            int lineColor = (220 << 24) | (color & 0x00FFFFFF);
            Box box = entity.getBoundingBox();

            Render3DEngine.drawBox3D(event.getMatrices(), vertexConsumer, event.getCameraRenderState(), box, lineColor, 1.0F);
        }
    }
}