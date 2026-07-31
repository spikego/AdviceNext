package cn.advicenext.utility.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 真 3D 渲染引擎。
 * <ul>
 *   <li>真 3D 方法：使用 OpenGL 在世界空间中直接绘制（需在 WorldRenderer mixin 上下文中调用）</li>
 *   <li>投影工具：世界坐标 → 屏幕像素坐标（供 2D HUD / ESP 使用）</li>
 * </ul>
 */
public final class Render3DEngine {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Render3DEngine() {
    }

    // ==================== 投影工具（2D HUD / ESP 使用） ====================

    /**
     * 世界坐标 → 屏幕像素坐标。
     * 返回 null 表示该点在相机后方（z &lt; 0.05）。
     */
    public static Vec3d worldToScreen(double wx, double wy, double wz) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        double dx = wx - camPos.x;
        double dy = wy - camPos.y;
        double dz = wz - camPos.z;

        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());

        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);

        double x1 = -dx * cosY - dz * sinY;
        double z1 = -dx * sinY + dz * cosY;

        double y1 = dy * cosP + z1 * sinP;
        double z2 = -dy * sinP + z1 * cosP;

        if (z2 < 0.05)
            return null;

        double fov = getDynamicFov();
        double hw = mc.getWindow().getScaledWidth() / 2.0;
        double hh = mc.getWindow().getScaledHeight() / 2.0;

        double scale = hh / (z2 * Math.tan(Math.toRadians(fov / 2.0)));

        return new Vec3d(hw + x1 * scale, hh - y1 * scale, z2);
    }

    private static double getDynamicFov() {
        double fov = mc.options.getFov().getValue().intValue();
        if (mc.player != null) {
            float fovEffectScale = mc.options.getFovEffectScale().getValue().floatValue();
            fov *= mc.player.getFovMultiplier(mc.options.getPerspective().isFirstPerson(), fovEffectScale);
        }
        return fov;
    }

    public static Vec3d worldToScreen(Vec3d worldPos) {
        return worldToScreen(worldPos.x, worldPos.y, worldPos.z);
    }

    public static boolean isOnScreen(Vec3d projected) {
        if (projected == null) return false;
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        return projected.x >= 0 && projected.x <= sw
            && projected.y >= 0 && projected.y <= sh;
    }

    /**
     * 投影 AABB 8 角点到屏幕，返回 [minX, minY, maxX, maxY] 或 null。
     */
    public static float[] projectBoxCorners(Box box) {
        Vec3d[] corners = getCorners(box);

        float screenMinX = Float.MAX_VALUE, screenMinY = Float.MAX_VALUE;
        float screenMaxX = -Float.MAX_VALUE, screenMaxY = -Float.MAX_VALUE;
        boolean any = false;

        for (Vec3d corner : corners) {
            Vec3d s = worldToScreen(corner);
            if (s != null) {
                any = true;
                screenMinX = Math.min(screenMinX, (float) s.x);
                screenMinY = Math.min(screenMinY, (float) s.y);
                screenMaxX = Math.max(screenMaxX, (float) s.x);
                screenMaxY = Math.max(screenMaxY, (float) s.y);
            }
        }

        return any ? new float[]{screenMinX, screenMinY, screenMaxX, screenMaxY} : null;
    }

    // ==================== 真 3D 渲染（OpenGL 世界空间） ====================

    private static final int[][] BOX_EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private static final int[][] BOX_FACES = {
        {0, 1, 2, 3}, {4, 5, 6, 7},
        {2, 3, 7, 6}, {0, 1, 5, 4},
        {0, 3, 7, 4}, {1, 2, 6, 5}
    };

    private static Vec3d[] getCorners(Box box) {
        return new Vec3d[] {
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ),
        };
    }

    private static float[] getCornerFloats(Box box) {
        return new float[] {
            (float) box.minX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.minY, (float) box.maxZ,
            (float) box.minX, (float) box.minY, (float) box.maxZ,
            (float) box.minX, (float) box.maxY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.maxZ,
            (float) box.minX, (float) box.maxY, (float) box.maxZ,
        };
    }

    /**
     * 真 3D 线框盒 — 使用 OpenGL 在世界空间中绘制 12 条边。
     */
    public static void drawBox3D(MatrixStack matrices, VertexConsumer vertexConsumer,
                                  CameraRenderState camera, Box box, int color, float lineWidth) {
        MatrixStack.Entry entry = matrices.peek();
        float[] c = getCornerFloats(box);
        double camX = camera.pos.getX();
        double camY = camera.pos.getY();
        double camZ = camera.pos.getZ();

        for (int[] edge : BOX_EDGES) {
            int i0 = edge[0] * 3, i1 = edge[1] * 3;
            float x0 = c[i0] - (float) camX;
            float y0 = c[i0 + 1] - (float) camY;
            float z0 = c[i0 + 2] - (float) camZ;
            float x1 = c[i1] - (float) camX;
            float y1 = c[i1 + 1] - (float) camY;
            float z1 = c[i1 + 2] - (float) camZ;

            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;

            vertexConsumer.vertex(entry, x0, y0, z0)
                .normal(entry, dx, dy, dz)
                .color(color)
                .lineWidth(lineWidth);
            vertexConsumer.vertex(entry, x1, y1, z1)
                .normal(entry, dx, dy, dz)
                .color(color)
                .lineWidth(lineWidth);
        }
    }

    /**
     * 真 3D 线段 — 在世界空间中绘制单条线段。
     */
    public static void drawLine3D(MatrixStack matrices, VertexConsumer vertexConsumer,
                                   CameraRenderState camera, Vec3d a, Vec3d b, int color, float lineWidth) {
        MatrixStack.Entry entry = matrices.peek();
        double camX = camera.pos.getX();
        double camY = camera.pos.getY();
        double camZ = camera.pos.getZ();

        float x0 = (float) (a.x - camX);
        float y0 = (float) (a.y - camY);
        float z0 = (float) (a.z - camZ);
        float x1 = (float) (b.x - camX);
        float y1 = (float) (b.y - camY);
        float z1 = (float) (b.z - camZ);

        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;

        vertexConsumer.vertex(entry, x0, y0, z0)
            .normal(entry, dx, dy, dz)
            .color(color)
            .lineWidth(lineWidth);
        vertexConsumer.vertex(entry, x1, y1, z1)
            .normal(entry, dx, dy, dz)
            .color(color)
            .lineWidth(lineWidth);
    }

    /**
     * 真 3D 圆环 — 在世界空间水平面上绘制。
     */
    public static void drawCircle3D(MatrixStack matrices, VertexConsumer vertexConsumer,
                                     CameraRenderState camera, Vec3d center, float radius,
                                     int color, int segments, float lineWidth) {
        MatrixStack.Entry entry = matrices.peek();
        double camX = camera.pos.getX();
        double camY = camera.pos.getY();
        double camZ = camera.pos.getZ();

        float cx = (float) (center.x - camX);
        float cy = (float) (center.y - camY);
        float cz = (float) (center.z - camZ);

        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2.0 * i / segments;
            double a2 = Math.PI * 2.0 * (i + 1) / segments;

            float x0 = cx + (float) Math.cos(a1) * radius;
            float z0 = cz + (float) Math.sin(a1) * radius;
            float x1 = cx + (float) Math.cos(a2) * radius;
            float z1 = cz + (float) Math.sin(a2) * radius;

            float dx = x1 - x0, dz = z1 - z0;

            vertexConsumer.vertex(entry, x0, cy, z0)
                .normal(entry, dx, 0, dz)
                .color(color)
                .lineWidth(lineWidth);
            vertexConsumer.vertex(entry, x1, cy, z1)
                .normal(entry, dx, 0, dz)
                .color(color)
                .lineWidth(lineWidth);
        }
    }
}