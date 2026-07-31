package cn.advicenext.utility.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * 2D GUI 渲染工具——矩形、渐变、圆角矩形、圆形、边框。
 * 全部通过 {@link SimpleGuiElementRenderState} 提交到 GUI 延迟渲染管线。
 * 调用前需 {@link #setRenderState(GuiRenderState, Matrix3x2fc)} 设置当前帧状态。
 */
public final class RenderUtils {

    private RenderUtils() {}

    // ==================== 静态渲染上下文 ====================

    private static GuiRenderState currentState = null;
    private static Matrix3x2fc currentPose = new Matrix3x2f();

    /**
     * 每帧渲染前调用。
     * 调用方：HUD/Render2DEvent 监听器
     * {@code RenderUtils.setRenderState(event.getContext().state, event.getContext().getMatrices())}
     */
    public static void setRenderState(GuiRenderState state, Matrix3x2fc pose) {
        currentState = state;
        currentPose = pose;
    }

    // ==================== 颜色工具 ====================

    public static int packARGB(float a, float r, float g, float b) {
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public static int packARGB(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================== 基础矩形 ====================

    /** 纯色矩形 */
    public static void drawRect(float x, float y, float w, float h, int color) {
        drawRectInternal(x, y, x + w, y + h, color, color, color, color);
    }

    /** 4 角渐变矩形（左上、右上、左下、右下） */
    public static void drawGradientRect(float x, float y, float w, float h, int tl, int tr, int bl, int br) {
        drawRectInternal(x, y, x + w, y + h, tl, tr, bl, br);
    }

    /** 垂直渐变矩形（上 → 下） */
    public static void drawGradientVertical(float x, float y, float w, float h, int top, int bottom) {
        drawRectInternal(x, y, x + w, y + h, top, top, bottom, bottom);
    }

    /** 水平渐变矩形（左 → 右） */
    public static void drawGradientHorizontal(float x, float y, float w, float h, int left, int right) {
        drawRectInternal(x, y, x + w, y + h, left, right, left, right);
    }

    // ==================== 圆角矩形（三角形逼近） ====================

    /**
     * 圆角矩形。用三角形逼近圆弧边缘。
     * @param segments 每个角的三角形段数（越大越圆滑，4-16 之间）
     */
    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color, int segments) {
        if (radius <= 0) { drawRect(x, y, w, h, color); return; }
        radius = Math.min(radius, Math.min(w, h) / 2);
        segments = Math.max(4, Math.min(24, segments));

        // 5 个不重叠的矩形部分
        drawRect(x + radius, y, w - 2 * radius, radius, color);              // 上边
        drawRect(x + radius, y + h - radius, w - 2 * radius, radius, color);  // 下边
        drawRect(x, y + radius, radius, h - 2 * radius, color);              // 左边
        drawRect(x + w - radius, y + radius, radius, h - 2 * radius, color);  // 右边
        drawRect(x + radius, y + radius, w - 2 * radius, h - 2 * radius, color); // 中心

        // 4 个角（三角形扇逼近）
        drawCornerArc(x + radius, y + radius, radius, 180, 270, color, segments);
        drawCornerArc(x + w - radius, y + radius, radius, 270, 360, color, segments);
        drawCornerArc(x + radius, y + h - radius, radius, 90, 180, color, segments);
        drawCornerArc(x + w - radius, y + h - radius, radius, 0, 90, color, segments);
    }

    /** 圆角矩形（默认 16 段） */
    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        drawRoundedRect(x, y, w, h, radius, color, 16);
    }

    // ==================== 边框 ====================

    /** 矩形边框 */
    public static void drawBorder(float x, float y, float w, float h, float thickness, int color) {
        drawRect(x, y, w, thickness, color);
        drawRect(x, y + h - thickness, w, thickness, color);
        drawRect(x, y + thickness, thickness, h - 2 * thickness, color);
        drawRect(x + w - thickness, y + thickness, thickness, h - 2 * thickness, color);
    }

    /** 圆角矩形边框（外圆角 - 内圆角） */
    public static void drawRoundedBorder(float x, float y, float w, float h, float radius, float thickness, int color, int segments) {
        // 外圆角
        drawRoundedRect(x, y, w, h, radius, color, segments);
        // 用背景色覆盖内部（调用方需要自己处理背景色，这里用透明色表示只画边框）
        // 更好的方式：用 SDF 或两次绘制。简化版：直接画外边框线
        drawBorder(x, y, w, h, thickness, color);
    }

    // ==================== 圆形 ====================

    /**
     * 填充圆（三角形扇逼近）
     */
    public static void drawCircle(float cx, float cy, float radius, int color, int segments) {
        segments = Math.max(8, Math.min(32, segments));
        drawFilledArc(cx, cy, radius, 0, 360, color, segments);
    }

    /**
     * 圆环
     */
    public static void drawCircleOutline(float cx, float cy, float radius, float thickness, int color, int segments) {
        segments = Math.max(8, Math.min(32, segments));
        float inner = radius - thickness;
        if (inner <= 0) { drawCircle(cx, cy, radius, color, segments); return; }

        // 外圆 - 内圆 = 圆环（用多个梯形逼近）
        int finalSegments = segments;
        submitElements(segments * 2, vertices -> {
            for (int i = 0; i < finalSegments; i++) {
                float a1 = (float) (2 * Math.PI * i / finalSegments);
                float a2 = (float) (2 * Math.PI * (i + 1) / finalSegments);

                float ox1 = cx + (float) Math.cos(a1) * radius;
                float oy1 = cy + (float) Math.sin(a1) * radius;
                float ox2 = cx + (float) Math.cos(a2) * radius;
                float oy2 = cy + (float) Math.sin(a2) * radius;
                float ix1 = cx + (float) Math.cos(a1) * inner;
                float iy1 = cy + (float) Math.sin(a1) * inner;
                float ix2 = cx + (float) Math.cos(a2) * inner;
                float iy2 = cy + (float) Math.sin(a2) * inner;

                // 外三角形
                vertices.vertex(currentPose, ox1, oy1).color(color);
                vertices.vertex(currentPose, ox2, oy2).color(color);
                vertices.vertex(currentPose, ix2, iy2).color(color);
                // 内三角形
                vertices.vertex(currentPose, ox1, oy1).color(color);
                vertices.vertex(currentPose, ix2, iy2).color(color);
                vertices.vertex(currentPose, ix1, iy1).color(color);
            }
        });
    }

    // ==================== 内部实现 ====================

    /** 4 角渐变矩形核心实现 */
    private static void drawRectInternal(float x1, float y1, float x2, float y2,
                                          int tl, int tr, int bl, int br) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        int bx = (int) Math.floor(Math.min(x1, x2));
        int by = (int) Math.floor(Math.min(y1, y2));
        int bw = (int) Math.ceil(Math.abs(x2 - x1));
        int bh = (int) Math.ceil(Math.abs(y2 - y1));
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(bw, 1), Math.max(bh, 1));

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                vertices.vertex(currentPose, x1, y1).color(tl);
                vertices.vertex(currentPose, x1, y2).color(bl);
                vertices.vertex(currentPose, x2, y2).color(br);
                vertices.vertex(currentPose, x2, y1).color(tr);
            }

            @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
            @Override public TextureSetup textureSetup() { return TextureSetup.empty(); }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    /** 圆弧三角形扇（用于圆角矩形和圆形） */
    private static void drawCornerArc(float cx, float cy, float radius,
                                        float startDeg, float endDeg, int color, int segments) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        int bx = (int) Math.floor(cx - radius);
        int by = (int) Math.floor(cy - radius);
        int bs = (int) Math.ceil(radius * 2);
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(bs, 1), Math.max(bs, 1));

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                float startRad = (float) Math.toRadians(startDeg);
                float endRad = (float) Math.toRadians(endDeg);

                for (int i = 0; i < segments; i++) {
                    float a1 = startRad + (endRad - startRad) * i / segments;
                    float a2 = startRad + (endRad - startRad) * (i + 1) / segments;

                    float x1 = cx + (float) Math.cos(a1) * radius;
                    float y1 = cy + (float) Math.sin(a1) * radius;
                    float x2 = cx + (float) Math.cos(a2) * radius;
                    float y2 = cy + (float) Math.sin(a2) * radius;

                    vertices.vertex(currentPose, cx, cy).color(color);
                    vertices.vertex(currentPose, x1, y1).color(color);
                    vertices.vertex(currentPose, x2, y2).color(color);
                }
            }

            @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
            @Override public TextureSetup textureSetup() { return TextureSetup.empty(); }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    /** 填充圆弧（用于圆形） */
    private static void drawFilledArc(float cx, float cy, float radius,
                                       float startDeg, float endDeg, int color, int segments) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        int bx = (int) Math.floor(cx - radius);
        int by = (int) Math.floor(cy - radius);
        int bs = (int) Math.ceil(radius * 2);
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(bs, 1), Math.max(bs, 1));

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                float startRad = (float) Math.toRadians(startDeg);
                float endRad = (float) Math.toRadians(endDeg);

                for (int i = 0; i < segments; i++) {
                    float a1 = startRad + (endRad - startRad) * i / segments;
                    float a2 = startRad + (endRad - startRad) * (i + 1) / segments;

                    float x1 = cx + (float) Math.cos(a1) * radius;
                    float y1 = cy + (float) Math.sin(a1) * radius;
                    float x2 = cx + (float) Math.cos(a2) * radius;
                    float y2 = cy + (float) Math.sin(a2) * radius;

                    vertices.vertex(currentPose, cx, cy).color(color);
                    vertices.vertex(currentPose, x1, y1).color(color);
                    vertices.vertex(currentPose, x2, y2).color(color);
                }
            }

            @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
            @Override public TextureSetup textureSetup() { return TextureSetup.empty(); }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    /** 通用元素提交（用于自定义顶点生成） */
    private static void submitElements(int estimatedQuads, VertexWriter writer) {
        if (currentState == null) return;
        RenderSystem.assertOnRenderThread();

        ScreenRect bounds = new ScreenRect(0, 0, mc_window_width(), mc_window_height());

        currentState.addPreparedTextElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                writer.write(vertices);
            }

            @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
            @Override public TextureSetup textureSetup() { return TextureSetup.empty(); }
            @Override public ScreenRect scissorArea() { return null; }
            @Override public ScreenRect bounds() { return bounds; }
        });
    }

    @FunctionalInterface
    private interface VertexWriter {
        void write(VertexConsumer vertices);
    }

    private static int mc_window_width() {
        return net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    private static int mc_window_height() {
        return net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight();
    }
}
