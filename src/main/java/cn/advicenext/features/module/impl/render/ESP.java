package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import cn.advicenext.event.impl.Render2DEvent;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class ESP extends Module{
    private final ModeSetting mode = new ModeSetting("Mode", "ESP", "3DESP", List.of("3DESP", "2DESP"));
    public ESP(){
        super("ESP", "Allows you to see other players through walls.", Category.RENDER);
        this.enabled = false;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!this.getEnabled() || !mode.getValue().equals("2DESP")) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext ctx = event.getContext();
        float tickDelta = event.getTickCounter().getDynamicDeltaTicks();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            double px = player.lastRenderX + (player.getX() - player.lastRenderX) * tickDelta;
            double py = player.lastRenderY + (player.getY() - player.lastRenderY) * tickDelta;
            double pz = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * tickDelta;

            var box = player.getBoundingBox().offset(-player.getX(), -player.getY(), -player.getZ());

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -1, maxY = -1;

            for (double x : new double[]{box.minX, box.maxX}) {
                for (double y : new double[]{box.minY, box.maxY}) {
                    for (double z : new double[]{box.minZ, box.maxZ}) {
                        Vec3d worldPos = new Vec3d(px + x, py + y, pz + z);
                        Vec2d screen = projectTo2D(worldPos, mc);
                        if (screen != null) {
                            minX = Math.min(minX, (float) screen.x);
                            minY = Math.min(minY, (float) screen.y);
                            maxX = Math.max(maxX, (float) screen.x);
                            maxY = Math.max(maxY, (float) screen.y);
                        }
                    }
                }
            }

            if (minX < maxX && minY < maxY) {
                int color = 0xFFFF0000;
                ctx.fill((int) minX, (int) minY, (int) maxX, (int) minY + 2, color);
                ctx.fill((int) minX, (int) maxY - 2, (int) maxX, (int) maxY, color);
                ctx.fill((int) minX, (int) minY, (int) minX + 2, (int) maxY, color);
                ctx.fill((int) maxX - 2, (int) minY, (int) maxX, (int) maxY, color);
            }
        }
    }
    // 数学投影逻辑
    private Vec2d projectTo2D(Vec3d pos, MinecraftClient mc) {
        // 获取摄像机参数
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double camYaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        double camPitch = Math.toRadians(mc.gameRenderer.getCamera().getPitch());

        // 世界坐标转相机局部坐标
        double x = pos.x - camPos.x;
        double y = pos.y - camPos.y;
        double z = pos.z - camPos.z;

        // 旋转：先绕Y轴（yaw），再绕X轴（pitch）
        double cosYaw = Math.cos(-camYaw), sinYaw = Math.sin(-camYaw);
        double cosPitch = Math.cos(-camPitch), sinPitch = Math.sin(-camPitch);

        double dx = x * cosYaw - z * sinYaw;
        double dz = x * sinYaw + z * cosYaw;
        double dy = y;

        double dy2 = dy * cosPitch - dz * sinPitch;
        double dz2 = dy * sinPitch + dz * cosPitch;

        // 透视投影
        if (dz2 <= 0.1) return null; // 背面或太近

        Window window = mc.getWindow();
        double fov = mc.options.getFov().getValue();
        double scale = window.getHeight() / (2.0 * Math.tan(Math.toRadians(fov / 2)));

        double screenX = window.getWidth() / 2.0 + dx * scale / dz2;
        double screenY = window.getHeight() / 2.0 - dy2 * scale / dz2;

        // 屏幕外不渲染
        if (screenX < 0 || screenX > window.getWidth() || screenY < 0 || screenY > window.getHeight())
            return null;

        return new Vec2d(screenX, screenY);
    }

    // 简单2D向量类
    private static class Vec2d {
        public final double x, y;
        public Vec2d(double x, double y) { this.x = x; this.y = y; }
    }


}
