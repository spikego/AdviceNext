package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class ESP extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "ESP", "2DESP", List.of("3DESP", "2DESP"));

    public ESP() {
        super("ESP", "Allows you to see other players through walls.", Category.RENDER);
        this.enabled = false;
    }

    // 仅展示核心方法
    @Override
    public void onRender2D(Render2DEvent event) {
        if (!this.getEnabled() || !mode.getValue().equals("2DESP")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext ctx = event.getContext();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        // 本地玩家插值位置和朝向
        PlayerEntity self = mc.player;
        double camX = self.lastRenderX + (self.getX() - self.lastRenderX) * tickDelta;
        double camY = self.lastRenderY + (self.getY() - self.lastRenderY) * tickDelta;
        double camZ = self.lastRenderZ + (self.getZ() - self.lastRenderZ) * tickDelta;
        float camYaw = self.getYaw(tickDelta);
        float camPitch = self.getPitch(tickDelta);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == self) continue;

            // 目标玩家插值位置
            double px = player.lastRenderX + (player.getX() - player.lastRenderX) * tickDelta;
            double py = player.lastRenderY + (player.getY() - player.lastRenderY) * tickDelta;
            double pz = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * tickDelta;

            // 包围盒8个顶点
            var box = player.getBoundingBox().offset(-player.getX(), -player.getY(), -player.getZ());
            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -1, maxY = -1;
            for (double x : xs)
                for (double y : ys)
                    for (double z : zs) {
                        Vec3d worldPos = new Vec3d(px + x, py + y, pz + z);
                        Vec2d screen = projectTo2D(worldPos, mc, camX, camY, camZ, camYaw, camPitch);
                        if (screen != null) {
                            minX = Math.min(minX, (float) screen.x);
                            minY = Math.min(minY, (float) screen.y);
                            maxX = Math.max(maxX, (float) screen.x);
                            maxY = Math.max(maxY, (float) screen.y);
                        }
                    }

            if (minX < maxX && minY < maxY) {
                int color = getTeamColor(self, player);
                // 细框+四角加粗
                int w = 2, l = 8;
                ctx.fill((int) minX, (int) minY, (int) maxX, (int) minY + w, color); // 顶
                ctx.fill((int) minX, (int) maxY - w, (int) maxX, (int) maxY, color); // 底
                ctx.fill((int) minX, (int) minY, (int) minX + w, (int) maxY, color); // 左
                ctx.fill((int) maxX - w, (int) minY, (int) maxX, (int) maxY, color); // 右
                // 四角
                ctx.fill((int) minX, (int) minY, (int) minX + l, (int) minY + w, color);
                ctx.fill((int) maxX - l, (int) minY, (int) maxX, (int) minY + w, color);
                ctx.fill((int) minX, (int) maxY - w, (int) minX + l, (int) maxY, color);
                ctx.fill((int) maxX - l, (int) maxY - w, (int) maxX, (int) maxY, color);
            }
        }
    }

    // 视图投影，考虑Yaw/Pitch
    private Vec2d projectTo2D(Vec3d pos, MinecraftClient mc, double camX, double camY, double camZ, float yaw, float pitch) {
        double radYaw = Math.toRadians(-yaw);
        double radPitch = Math.toRadians(-pitch);

        double x = pos.x - camX;
        double y = pos.y - camY;
        double z = pos.z - camZ;

        // 先Yaw后Pitch
        double cosYaw = Math.cos(radYaw), sinYaw = Math.sin(radYaw);
        double cosPitch = Math.cos(radPitch), sinPitch = Math.sin(radPitch);

        double dx = x * cosYaw - z * sinYaw;
        double dz = x * sinYaw + z * cosYaw;
        double dy = y;

        double dy2 = dy * cosPitch - dz * sinPitch;
        double dz2 = dy * sinPitch + dz * cosPitch;

        if (dz2 <= 0.1) return null;

        Window window = mc.getWindow();
        double fov = mc.options.getFov().getValue();
        double scale = window.getHeight() / (2.0 * Math.tan(Math.toRadians(fov / 2)));

        double screenX = window.getWidth() / 2.0 + dx * scale / dz2;
        double screenY = window.getHeight() / 2.0 - dy2 * scale / dz2;

        double scaleFactor = window.getScaleFactor();
        screenX /= scaleFactor;
        screenY /= scaleFactor;

        if (screenX < 0 || screenX > window.getWidth() / scaleFactor || screenY < 0 || screenY > window.getHeight() / scaleFactor)
            return null;

        return new Vec2d(screenX, screenY);
    }

    // 队伍颜色
    private int getTeamColor(PlayerEntity self, PlayerEntity other) {
        if (self.getScoreboardTeam() != null && self.getScoreboardTeam().equals(other.getScoreboardTeam())) {
            return 0xFF00FF00; // 绿
        }
        return 0xFFFF0000; // 红
    }

    private record Vec2d(double x, double y) {
    }
}