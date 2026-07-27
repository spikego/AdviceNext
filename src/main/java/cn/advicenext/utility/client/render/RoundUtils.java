package cn.advicenext.utility.client.render;

/**
 * 圆角几何工具：计算圆角矩形逐行扫描线的内缩量，
 * 供 2D 渲染引擎按行绘制平滑圆角。
 */
public final class RoundUtils {

    private RoundUtils() {
    }

    /** 将圆角半径限制在合法范围 [0, min(w,h)/2] */
    public static float clampRadius(float radius, float width, float height) {
        return Math.max(0.0F, Math.min(radius, Math.min(width, height) / 2.0F));
    }

    /**
     * 计算圆角区域某一行的水平内缩量。
     * 以左上角为例：圆心位于 (radius, radius)，对于距离矩形顶边 dy 的行
     * （dy < radius），该行左端需要内缩 radius - sqrt(radius² - (radius - dy)²)。
     *
     * @param radius 圆角半径
     * @param dy     当前行到矩形边缘的距离（0 ~ radius）
     * @return 水平内缩像素数
     */
    public static float insetForRow(float radius, float dy) {
        float y = radius - dy;
        return radius - (float) Math.sqrt(radius * radius - y * y);
    }

    /** 平滑插值 */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** easeOutBack 弹跳缓动（NewClickGui 动画用） */
    public static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3.0) + c1 * (float) Math.pow(t - 1.0F, 2.0);
    }

    /** easeOutCubic */
    public static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0);
    }

    /** easeInOutCubic */
    public static float easeInOutCubic(float t) {
        return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0F) / 2.0F;
    }

    /** easeOutElastic 弹性缓动 */
    public static float easeOutElastic(float t) {
        if (t <= 0.0F) return 0.0F;
        if (t >= 1.0F) return 1.0F;
        float c4 = (float) (2.0 * Math.PI / 3.0);
        return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
    }
}
