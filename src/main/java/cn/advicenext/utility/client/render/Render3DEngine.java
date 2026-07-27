package cn.advicenext.utility.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 3D 渲染引擎：世界坐标几何 → 屏幕投影 → 2D 绘制。
 * 在 Render2DEvent 中调用（DrawContext 可用）。
 * 投影走 GameRenderer.project，与 ESP 名称标签同一条已验证路径，
 * 不触碰世界渲染 pipeline，稳定可靠。
 */
public final class Render3DEngine {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Render3DEngine() {
    }

    // ==================== 投影 ====================

    /** 世界坐标 → 屏幕坐标。返回的 z ∈ [0,1] 表示可见。 */
    public static Vec3d project(Vec3d worldPos) {
        return mc.gameRenderer.project(worldPos);
    }

    public static boolean isVisible(Vec3d projected) {
        return projected != null && projected.z >= 0.0 && projected.z <= 1.0;
    }

    // ==================== 3D 线段 ====================

    public static void drawLine3D(DrawContext ctx, Vec3d a, Vec3d b, int color, float thickness) {
        Vec3d pa = project(a);
        Vec3d pb = project(b);
        if (!isVisible(pa) || !isVisible(pb)) return;
        Render2DEngine.drawLine(ctx, (float) pa.x, (float) pa.y, (float) pb.x, (float) pb.y, thickness, color);
    }

    // ==================== 盒体 ====================

    private static Vec3d[] boxCorners(Box box) {
        return new Vec3d[] {
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ)
        };
    }

    private static final int[][] BOX_EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private static final int[][] BOX_FACES = {
        {0, 1, 2, 3}, // 底
        {4, 7, 6, 5}, // 顶
        {0, 4, 5, 1}, // 北
        {2, 6, 7, 3}, // 南
        {1, 5, 6, 2}, // 东
        {0, 3, 7, 4}  // 西
    };

    public static void drawBoxOutline(DrawContext ctx, Box box, int color, float thickness) {
        Vec3d[] corners = boxCorners(box);
        Vec3d[] projected = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            projected[i] = project(corners[i]);
        }

        for (int[] edge : BOX_EDGES) {
            Vec3d pa = projected[edge[0]];
            Vec3d pb = projected[edge[1]];
            if (!isVisible(pa) || !isVisible(pb)) continue;
            Render2DEngine.drawLine(ctx, (float) pa.x, (float) pa.y, (float) pb.x, (float) pb.y, thickness, color);
        }
    }

    public static void drawBoxFilled(DrawContext ctx, Box box, int color) {
        Vec3d[] corners = boxCorners(box);
        Vec3d[] projected = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            projected[i] = project(corners[i]);
        }

        for (int[] face : BOX_FACES) {
            Vec3d p0 = projected[face[0]];
            Vec3d p1 = projected[face[1]];
            Vec3d p2 = projected[face[2]];
            Vec3d p3 = projected[face[3]];
            if (!isVisible(p0) || !isVisible(p1) || !isVisible(p2) || !isVisible(p3)) continue;
            Render2DEngine.fillTriangle(ctx,
                (float) p0.x, (float) p0.y, (float) p1.x, (float) p1.y, (float) p2.x, (float) p2.y, color);
            Render2DEngine.fillTriangle(ctx,
                (float) p0.x, (float) p0.y, (float) p2.x, (float) p2.y, (float) p3.x, (float) p3.y, color);
        }
    }

    public static void drawBox(DrawContext ctx, Box box, int fillColor, int lineColor, float thickness) {
        if (((fillColor >>> 24) & 0xFF) > 0) {
            drawBoxFilled(ctx, box, fillColor);
        }
        if (((lineColor >>> 24) & 0xFF) > 0) {
            drawBoxOutline(ctx, box, lineColor, thickness);
        }
    }

    // ==================== 圆环 / 圆盘 ====================

    public static void drawCircleOutline(DrawContext ctx, Vec3d center, float radius, int color, int segments, float thickness) {
        Vec3d prev = null;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            Vec3d point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (prev != null) {
                drawLine3D(ctx, prev, point, color, thickness);
            }
            prev = point;
        }
    }

    /** 圆盘填充：圆心 + 相邻两点构成三角扇 */
    public static void drawCircleFilled(DrawContext ctx, Vec3d center, float radius, int color, int segments) {
        Vec3d projectedCenter = project(center);
        if (!isVisible(projectedCenter)) return;

        Vec3d prev = null;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            Vec3d point = project(center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            if (prev != null && isVisible(prev) && isVisible(point)) {
                Render2DEngine.fillTriangle(ctx,
                    (float) projectedCenter.x, (float) projectedCenter.y,
                    (float) prev.x, (float) prev.y,
                    (float) point.x, (float) point.y,
                    color);
            }
            prev = point;
        }
    }
}
