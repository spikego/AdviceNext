package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.Render3DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.scoreboard.Team;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ESP extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "ESP", "2DESP", List.of("3DESP", "2DESP"));
    private final ModeSetting colorMode = new ModeSetting("Color", "Color", "Client", List.of("Client", "Team"));
    private final BooleanSetting nameList = new BooleanSetting("NameList", "Show player names", true);
    private final BooleanSetting smoothBox = new BooleanSetting("SmoothBox", "Smooth box corners", true);
    private final Map<PlayerEntity, float[]> entityPosMap = new HashMap<>();
    private final Map<PlayerEntity, Long> lastUpdateMap = new HashMap<>();
    private final long UPDATE_INTERVAL = 50; // 50ms更新一次位置

    public ESP() {
        super("ESP", "Allows you to see other players through walls.", Category.RENDER);
        this.enabled = false;
        this.settings.add(mode);
        this.settings.add(colorMode);
        this.settings.add(nameList);
        this.settings.add(smoothBox);
    }

    @Override
    public void onDisable() {
        entityPosMap.clear();
        lastUpdateMap.clear();
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!this.getEnabled() || !mode.getValue().equals("2DESP")) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        DrawContext context = event.getContext();

        // 实时更新玩家位置
        updatePlayerPositions(event.getTickCounter().getDynamicDeltaTicks());

        for (PlayerEntity player : entityPosMap.keySet()) {
            if (player == mc.player) continue;

            final float[] positions = entityPosMap.get(player);
            final float x = positions[0];
            final float y = positions[1];
            final float x2 = positions[2];
            final float y2 = positions[3];

            // 确保位置有效
            if (x < x2 && y < y2) {
                // 获取颜色
                int color = getPlayerColor(player);

                if (smoothBox.getValue()) {
                    // 绘制平滑方框
                    drawSmoothBox(context, (int)x, (int)y, (int)x2, (int)y2, color);
                } else {
                    // 绘制2D方框
                    // 上边框
                    context.fill((int) x, (int) y, (int) x2, (int) y + 1, color);
                    // 下边框
                    context.fill((int) x, (int) y2 - 1, (int) x2, (int) y2, color);
                    // 左边框
                    context.fill((int) x, (int) y, (int) x + 1, (int) y2, color);
                    // 右边框
                    context.fill((int) x2 - 1, (int) y, (int) x2, (int) y2, color);

                    // 四角加粗
                    int w = 2, l = 8;
                    context.fill((int) x, (int) y, (int) x + l, (int) y + w, color);
                    context.fill((int) x2 - l, (int) y, (int) x2, (int) y + w, color);
                    context.fill((int) x, (int) y2 - w, (int) x + l, (int) y2, color);
                    context.fill((int) x2 - l, (int) y2 - w, (int) x2, (int) y2, color);
                }

                // 显示玩家名称
                if (nameList.getValue()) {
                    String name = player.getName().getString();
                    float textWidth = mc.textRenderer.getWidth(name);
                    float textX = x + (x2 - x) / 2 - textWidth / 2;
                    float textY = y - 12; // 在方框上方显示名称

                    // 绘制半透明背景
                    context.fill((int)(textX - 2), (int)(textY - 2),
                               (int)(textX + textWidth + 2), (int)(textY + 10),
                               0x80000000);

                    // 绘制文本
                    context.drawText(mc.textRenderer, name, (int)textX, (int)textY, color, true);
                }
            }
        }
    }

    /**
     * 实时更新玩家位置
     */
    private void updatePlayerPositions(float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!isValid(player)) continue;

            // 检查是否需要更新位置
            Long lastUpdate = lastUpdateMap.get(player);
            if (lastUpdate == null || currentTime - lastUpdate >= UPDATE_INTERVAL) {
                // 计算玩家插值位置
                final double playerX = interpolate(player.lastRenderX, player.getX(), partialTicks);
                final double playerY = interpolate(player.lastRenderY, player.getY(), partialTicks);
                final double playerZ = interpolate(player.lastRenderZ, player.getZ(), partialTicks);

                // 计算相对于相机的位置
                final double posX = playerX - mc.gameRenderer.getCamera().getPos().x;
                final double posY = playerY - mc.gameRenderer.getCamera().getPos().y;
                final double posZ = playerZ - mc.gameRenderer.getCamera().getPos().z;

                // 创建包围盒
                final double halfWidth = player.getWidth() / 2.0D;
                final Box bb = new Box(posX - halfWidth, posY, posZ - halfWidth,
                        posX + halfWidth, posY + player.getHeight() + (player.isSneaking() ? -0.2D : 0.1D), posZ + halfWidth).expand(0.1, 0.1, 0.1);

                // 计算包围盒的8个顶点
                final double[][] vectors = {
                        {bb.minX, bb.minY, bb.minZ},
                        {bb.minX, bb.maxY, bb.minZ},
                        {bb.minX, bb.maxY, bb.maxZ},
                        {bb.minX, bb.minY, bb.maxZ},
                        {bb.maxX, bb.minY, bb.minZ},
                        {bb.maxX, bb.maxY, bb.minZ},
                        {bb.maxX, bb.maxY, bb.maxZ},
                        {bb.maxX, bb.minY, bb.maxZ}
                };

                // 投影到2D屏幕
                float[] projection;
                final float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};

                for (final double[] vec : vectors) {
                    projection = project2D((float) vec[0], (float) vec[1], (float) vec[2]);
                    if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                        final float pX = projection[0];
                        final float pY = projection[1];
                        position[0] = Math.min(position[0], pX);
                        position[1] = Math.min(position[1], pY);
                        position[2] = Math.max(position[2], pX);
                        position[3] = Math.max(position[3], pY);
                    }
                }

                // 保存投影结果
                entityPosMap.put(player, position);
                lastUpdateMap.put(player, currentTime);
            }
        }

        // 清理不再存在的玩家
        entityPosMap.keySet().removeIf(player -> !mc.world.getPlayers().contains(player));
        lastUpdateMap.keySet().removeIf(player -> !mc.world.getPlayers().contains(player));
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!this.getEnabled()) return;

        // 在3D渲染时更新位置
        updatePlayerPositions(event.getTickDelta());
    }

    private float[] project2D(float x, float y, float z) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // 获取相机信息
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();

        // 转换为弧度
        double yaw = Math.toRadians(cameraYaw);
        double pitch = Math.toRadians(cameraPitch);
        double roll = 0.0; // 通常为0

        // 计算旋转矩阵
        double cosYaw = Math.cos(-yaw);
        double sinYaw = Math.sin(-yaw);
        double cosPitch = Math.cos(-pitch);
        double sinPitch = Math.sin(-pitch);

        // 应用旋转 - 先Yaw后Pitch
        double xr = x * cosYaw - z * sinYaw;
        double zr = x * sinYaw + z * cosYaw;
        double yr = y;

        double xr2 = xr;
        double yr2 = yr * cosPitch - zr * sinPitch;
        double zr2 = yr * sinPitch + zr * cosPitch;

        // 如果在相机后面，返回null
        if (zr2 < 0.1) {
            return null;
        }

        // 获取屏幕尺寸
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // 计算FOV
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double fovScale = 1.0 / Math.tan(fov / 2.0);

        // 计算屏幕坐标 - 水平镜像
        // 考虑距离因素，近快远慢
        double distanceFactor = 1.0 / zr2; // 距离因子，距离越远越小
        double screenX = screenWidth / 2.0 - xr2 * (screenWidth / 2.0) * fovScale * distanceFactor;
        double screenY = screenHeight / 2.0 - yr2 * (screenHeight / 2.0) * fovScale * distanceFactor;

        // 返回结果
        float[] result = new float[3];
        result[0] = (float) screenX;
        result[1] = (float) screenY;
        result[2] = (float) (1.0 - (1.0 / zr2)); // 深度值

        return result;
    }

    private double interpolate(double prev, double current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private boolean isValid(PlayerEntity player) {
        if (!player.isAlive()) return false;
        if (player == MinecraftClient.getInstance().player) return false;
        return isInViewFrustum(player.getBoundingBox());
    }
    
    private boolean isInViewFrustum(Box box) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer.getCamera() == null) return false;
        
        // 简化的视锥体检查
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        if (box.contains(camPos)) return true;
        
        // 检查是否在视野范围内
        return true; // 简化处理，始终显示所有玩家
    }
    
    private int getPlayerColor(PlayerEntity player) {
        if (colorMode.getValue().equals("Team")) {
            Team team = player.getScoreboardTeam();
            if (team != null) {
                // 获取队伍颜色
                return team.getColor().getColorValue() | 0xFF000000;
            }
            // 如果没有队伍，使用默认颜色
            return 0xFFFF0000; // 红色
        } else {
            // 使用客户端颜色
            return Colors.currentColor().getRGB();
        }
    }
    
    private void drawSmoothBox(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int width = x2 - x1;
        int height = y2 - y1;
        int lineWidth = 1;
        int cornerRadius = 3;
        
        // 绘制四个圆角
        drawRoundedCorner(context, x1, y1, cornerRadius, 0, color); // 左上
        drawRoundedCorner(context, x2 - cornerRadius, y1, cornerRadius, 1, color); // 右上
        drawRoundedCorner(context, x1, y2 - cornerRadius, cornerRadius, 2, color); // 左下
        drawRoundedCorner(context, x2 - cornerRadius, y2 - cornerRadius, cornerRadius, 3, color); // 右下
        
        // 绘制四条边
        context.fill(x1 + cornerRadius, y1, x2 - cornerRadius, y1 + lineWidth, color); // 上
        context.fill(x1 + cornerRadius, y2 - lineWidth, x2 - cornerRadius, y2, color); // 下
        context.fill(x1, y1 + cornerRadius, x1 + lineWidth, y2 - cornerRadius, color); // 左
        context.fill(x2 - lineWidth, y1 + cornerRadius, x2, y2 - cornerRadius, color); // 右
    }
    
    private void drawRoundedCorner(DrawContext context, int x, int y, int radius, int corner, int color) {
        // corner: 0=左上, 1=右上, 2=左下, 3=右下
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                double distance = Math.sqrt(i * i + j * j);
                if (distance <= radius) {
                    int drawX = x;
                    int drawY = y;
                    
                    switch (corner) {
                        case 0: // 左上
                            drawX = x + radius - i - 1;
                            drawY = y + radius - j - 1;
                            break;
                        case 1: // 右上
                            drawX = x + i;
                            drawY = y + radius - j - 1;
                            break;
                        case 2: // 左下
                            drawX = x + radius - i - 1;
                            drawY = y + j;
                            break;
                        case 3: // 右下
                            drawX = x + i;
                            drawY = y + j;
                            break;
                    }
                    
                    if (distance >= radius - 1) {
                        context.fill(drawX, drawY, drawX + 1, drawY + 1, color);
                    }
                }
            }
        }
    }
}