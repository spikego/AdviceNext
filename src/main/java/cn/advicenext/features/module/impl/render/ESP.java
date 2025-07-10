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

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!this.getEnabled() || !mode.getValue().equals("2DESP")) return;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.gameRenderer.getCamera() == null) return;
        
        DrawContext context = event.getContext();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            Vec3d playerPos = player.getBoundingBox().getCenter();
            Vec3d screenPos = worldToScreen(playerPos, mc);
            
            if (screenPos == null) continue;
            
            int x = (int) screenPos.x;
            int y = (int) screenPos.y;
            
            // 计算方框大小
            double distance = mc.player.distanceTo(player);
            int boxWidth = (int) Math.max(20, 60 / Math.max(distance / 10, 1));
            int boxHeight = (int) (boxWidth * 1.8);
            
            // 绘制2D方框
            int x1 = x - boxWidth / 2;
            int y1 = y - boxHeight / 2;
            int x2 = x1 + boxWidth;
            int y2 = y1 + boxHeight;
            
            // 方框边框
            context.fill(x1, y1, x2, y1 + 1, 0xFFFFFFFF); // 上
            context.fill(x1, y2 - 1, x2, y2, 0xFFFFFFFF); // 下
            context.fill(x1, y1, x1 + 1, y2, 0xFFFFFFFF); // 左
            context.fill(x2 - 1, y1, x2, y2, 0xFFFFFFFF); // 右
        }
    }
    
    @Override
    public void onRender3D(cn.advicenext.event.impl.Render3DEvent event) {
        if (!this.getEnabled() || !mode.getValue().equals("3DESP")) return;
        
        // 3D ESP渲染逻辑
        // 这里可以添加3D渲染代码
    }

    private Vec3d worldToScreen(Vec3d worldPos, MinecraftClient mc) {
        try {
            Vec3d camera = mc.gameRenderer.getCamera().getPos();
            Vec3d relative = worldPos.subtract(camera);
            
            double yaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
            double pitch = Math.toRadians(mc.gameRenderer.getCamera().getPitch());
            
            double x = relative.x * Math.cos(yaw) - relative.z * Math.sin(yaw);
            double z = relative.x * Math.sin(yaw) + relative.z * Math.cos(yaw);
            double y = relative.y;
            
            if (z <= 0) return null;
            
            double fov = mc.options.getFov().getValue();
            double screenX = mc.getWindow().getScaledWidth() / 2.0 + (x / z) * (mc.getWindow().getScaledWidth() / 2.0) / Math.tan(Math.toRadians(fov / 2.0));
            double screenY = mc.getWindow().getScaledHeight() / 2.0 - (y / z) * (mc.getWindow().getScaledHeight() / 2.0) / Math.tan(Math.toRadians(fov / 2.0));
            
            return new Vec3d(screenX, screenY, z);
        } catch (Exception e) {
            return null;
        }
    }
}