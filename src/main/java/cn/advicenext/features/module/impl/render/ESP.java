package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.Render2DEngine;
import cn.advicenext.utility.client.render.Render3DEngine;
import net.minecraft.client.gui.DrawContext;
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
 *   <li><b>Box</b> — 2D 圆角框 + 可选 Glow 发光。</li>
 *   <li><b>3D</b> — 真 3D 线框/填充盒（Render3DEngine）。</li>
 *   <li><b>Outline</b> — 模型轮廓（MixinLivingEntityRenderer）。</li>
 * </ul>
 */
public class ESP extends Module {
    public static ESP INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "ESP mode", "Box", List.of("Box", "3D", "Outline"));
    private final ModeSetting colorMode = new ModeSetting("Color", "ESP color mode", "Client", List.of("Client", "Team", "White", "Red", "Green"));
    private final BooleanSetting glow = new BooleanSetting("Glow", "Glow effect around boxes", true, () -> mode.is("Box"));
    private final DoubleSetting radius = new DoubleSetting("Corner Radius", "Box corner radius", 4.0, 10.0, 0.0, 0.5, () -> mode.is("Box"));
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
            render3DProjected(event);
            return;
        }
        if (!mode.is("Box")) return;

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

            Vec3d[] corners = new Vec3d[] {
                new Vec3d(x - w, y, z - w), new Vec3d(x + w, y, z - w),
                new Vec3d(x + w, y, z + w), new Vec3d(x - w, y, z + w),
                new Vec3d(x - w, y + h, z - w), new Vec3d(x + w, y + h, z - w),
                new Vec3d(x + w, y + h, z + w), new Vec3d(x - w, y + h, z + w)
            };

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean visible = false;

            for (Vec3d corner : corners) {
                Vec3d projected = mc.gameRenderer.project(corner);
                if (projected.z >= 0.0 && projected.z <= 1.0) {
                    visible = true;
                    minX = Math.min(minX, (float) projected.x);
                    minY = Math.min(minY, (float) projected.y);
                    maxX = Math.max(maxX, (float) projected.x);
                    maxY = Math.max(maxY, (float) projected.y);
                }
            }
            if (!visible) continue;

            minX = Math.max(0, minX);
            minY = Math.max(0, minY);
            maxX = Math.min(screenWidth, maxX);
            maxY = Math.min(screenHeight, maxY);

            int color = getColor(entity).getRGB();
            float boxRadius = radius.getValue().floatValue();

            if (glow.getValue()) {
                // Glow 只画扩散层，不填充主体（保持框内透明）
                drawBoxGlow(ctx, minX, minY, maxX, maxY, boxRadius, color);
            }
            Render2DEngine.drawRoundOutline(ctx, minX, minY, maxX - minX, maxY - minY, boxRadius, 1.0F, Render2DEngine.withAlpha(color, 230));

            drawNameAndHealth(event, entity, minX, minY, maxX, maxY);
        }
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

    /** 真 3D 盒（投影路径） */
    private void render3DProjected(Render2DEvent event) {
        DrawContext ctx = event.getContext();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float w = entity.getWidth() / 2.0F;
            Box box = new Box(x - w, y, z - w, x + w, y + entity.getHeight(), z + w);

            int color = getColor(entity).getRGB();
            int lineColor = Render2DEngine.withAlpha(color, 220);
            int fillColor = filledBox.getValue() ? Render2DEngine.withAlpha(color, 45) : 0;
            Render3DEngine.drawBox(ctx, box, fillColor, lineColor, 1.0F);

            // 名称与血量基于投影包围盒
            drawNameAndHealth(event, entity, x, y, z);
        }
    }

    private void drawNameAndHealth(Render2DEvent event, Entity entity, double x, double y, double z) {
        float w = entity.getWidth() / 2.0f;
        float h = entity.getHeight();

        Vec3d[] corners = new Vec3d[] {
            new Vec3d(x - w, y, z - w), new Vec3d(x + w, y, z - w),
            new Vec3d(x + w, y, z + w), new Vec3d(x - w, y, z + w),
            new Vec3d(x - w, y + h, z - w), new Vec3d(x + w, y + h, z - w),
            new Vec3d(x + w, y + h, z + w), new Vec3d(x - w, y + h, z + w)
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Vec3d corner : corners) {
            Vec3d projected = mc.gameRenderer.project(corner);
            if (projected.z >= 0.0 && projected.z <= 1.0) {
                minX = Math.min(minX, (float) projected.x);
                minY = Math.min(minY, (float) projected.y);
                maxX = Math.max(maxX, (float) projected.x);
                maxY = Math.max(maxY, (float) projected.y);
            }
        }
        if (minX > maxX) return;

        drawNameAndHealth(event, entity, minX, minY, maxX, maxY);
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

    public Color getOutlineColor(Entity entity) {
        return getColor(entity);
    }
}
