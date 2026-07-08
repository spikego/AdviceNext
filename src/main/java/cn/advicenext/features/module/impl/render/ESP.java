package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

public class ESP extends Module {
    private final ModeSetting colorMode = new ModeSetting("Color", "ESP颜色模式", "Client", List.of("Client", "Team"));
    private final BooleanSetting showName = new BooleanSetting("Names", "显示玩家名称", true);
    private final BooleanSetting showHealth = new BooleanSetting("Health", "显示生命值", true);

    public ESP() {
        super("ESP", "透视玩家轮廓", Category.RENDER);
        this.settings.add(colorMode);
        this.settings.add(showName);
        this.settings.add(showHealth);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!enabled || mc.world == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Matrix4f projectionMatrix = getProjectionMatrix();
        Matrix4f modelViewMatrix = getModelViewMatrix(camera);

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target == mc.player || target.isRemoved()) continue;

            Box box = target.getBoundingBox();
            Vector4f[] corners = projectBox(box, camera.getPos(), modelViewMatrix, projectionMatrix);
            if (corners == null) continue;

            int color = getESPColor(target);
            renderESP(event.getContext(), corners, target, color);
        }
    }

    private Matrix4f getProjectionMatrix() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float fov = (float) Math.toRadians(mc.options.getFov().getValue().intValue());
        float aspectRatio = (float) mc.getWindow().getFramebufferWidth() / mc.getWindow().getFramebufferHeight();

        float farPlane = 1024.0f;

        return new Matrix4f().perspective(fov, aspectRatio, 0.05f, farPlane);
    }

    private Matrix4f getModelViewMatrix(Camera camera) {
        Matrix4f matrix = new Matrix4f();
        matrix.rotate((float) Math.toRadians(camera.getPitch()), 1.0f, 0.0f, 0.0f);
        matrix.rotate((float) Math.toRadians(camera.getYaw() + 180.0f), 0.0f, 1.0f, 0.0f);
        Vec3d pos = camera.getPos();
        matrix.translate(-(float)pos.x, -(float)pos.y, -(float)pos.z);
        return matrix;
    }

    private Vector4f[] projectBox(Box box, Vec3d cameraPos, Matrix4f modelView, Matrix4f projection) {
        Vector4f[] points = new Vector4f[8];
        Vec3d[] boxCorners = {
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ)
        };

        boolean allBehind = true;
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector4f(
                    (float)(boxCorners[i].x - cameraPos.x),
                    (float)(boxCorners[i].y - cameraPos.y),
                    (float)(boxCorners[i].z - cameraPos.z),
                    1.0f
            );
            points[i].mul(modelView).mul(projection);

            if (points[i].w() > 0) {
                allBehind = false;
                points[i].div(points[i].w());
            }
        }

        return allBehind ? null : points;
    }

    private void renderESP(DrawContext context, Vector4f[] corners, PlayerEntity player, int color) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (Vector4f v : corners) {
            if (v.w() <= 0) continue;
            float screenX = (v.x() * 0.5f + 0.5f) * width;
            float screenY = (1.0f - (v.y() * 0.5f + 0.5f)) * height;

            minX = Math.min(minX, screenX);
            minY = Math.min(minY, screenY);
            maxX = Math.max(maxX, screenX);
            maxY = Math.max(maxY, screenY);
        }

        if (minX < Float.MAX_VALUE) {
            drawBox(context, (int)minX, (int)minY, (int)maxX, (int)maxY, color);

            if (showName.getValue()) {
                String name = player.getName().getString();
                float textX = (minX + maxX) / 2 - mc.textRenderer.getWidth(name) / 2f;
                context.drawText(mc.textRenderer, name, (int)textX, (int)minY - 12, color, true);
            }

            if (showHealth.getValue()) {
                float health = player.getHealth();
                String healthText = String.format("%.1f HP", health);
                float textX = (minX + maxX) / 2 - mc.textRenderer.getWidth(healthText) / 2f;
                int healthColor = getHealthColor(health);
                context.drawText(mc.textRenderer, healthText, (int)textX, (int)maxY + 2, healthColor, true);
            }
        }
    }

    private void drawBox(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int thickness = 1;
        // 四个边
        context.fill(x1, y1, x2, y1 + thickness, color);
        context.fill(x1, y2 - thickness, x2, y2, color);
        context.fill(x1, y1, x1 + thickness, y2, color);
        context.fill(x2 - thickness, y1, x2, y2, color);

        // 边角加粗
        int cornerSize = 4;
        context.fill(x1, y1, x1 + cornerSize, y1 + thickness * 2, color);
        context.fill(x1, y1, x1 + thickness * 2, y1 + cornerSize, color);

        context.fill(x2 - cornerSize, y1, x2, y1 + thickness * 2, color);
        context.fill(x2 - thickness * 2, y1, x2, y1 + cornerSize, color);

        context.fill(x1, y2 - cornerSize, x1 + thickness * 2, y2, color);
        context.fill(x1, y2 - thickness * 2, x1 + cornerSize, y2, color);

        context.fill(x2 - cornerSize, y2 - thickness * 2, x2, y2, color);
        context.fill(x2 - thickness * 2, y2 - cornerSize, x2, y2, color);
    }

    private int getESPColor(PlayerEntity player) {
        if (colorMode.getValue().equals("Team") && player.getScoreboardTeam() != null) {
            var teamColor = player.getScoreboardTeam().getColor();
            if (teamColor != null && teamColor.getColorValue() != null) {
                return teamColor.getColorValue() | 0xFF000000;
            }
        }
        return Colors.currentColor().getRGB();
    }

    private int getHealthColor(float health) {
        if (health > 15) return 0xFF00FF00;
        else if (health > 10) return 0xFFFFFF00;
        else if (health > 5) return 0xFFFF8000;
        return 0xFFFF0000;
    }
}