package cn.advicenext.gui.clickgui.newgui;

/**
 * 基于弹簧物理的动画值，提供自然的 Q 弹缓动效果。
 * 自动追踪当前值、速度和目标值，每帧调用 {@link #update()} 即可。
 */
public class AnimatedValue {
    private float position;
    private float velocity;
    private float target;
    private final float stiffness;
    private final float damping;
    private boolean settled = true;

    private static final float TOLERANCE = 0.01f;
    private static final float VELOCITY_TOLERANCE = 0.1f;

    public AnimatedValue(float start) {
        this(start, 0.18f, 0.75f);
    }

    public AnimatedValue(float start, float stiffness, float damping) {
        this.position = start;
        this.target = start;
        this.velocity = 0;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void setTarget(float target) {
        if (Math.abs(this.target - target) > 0.001f) {
            this.target = target;
            settled = false;
        }
    }

    public void setImmediate(float value) {
        this.position = value;
        this.target = value;
        this.velocity = 0;
        settled = true;
    }

    public void update() {
        if (settled) return;

        float force = (target - position) * stiffness;
        velocity += force;
        velocity *= damping;

        position += velocity;

        if (Math.abs(target - position) < TOLERANCE && Math.abs(velocity) < VELOCITY_TOLERANCE) {
            position = target;
            velocity = 0;
            settled = true;
        }
    }

    public float get() {
        return position;
    }

    public float getTarget() {
        return target;
    }

    public boolean isSettled() {
        return settled;
    }

    /** 获取归一化进度 (0-1)，用于插值 */
    public float getProgress(float start, float end) {
        if (Math.abs(end - start) < 0.001f) return 1f;
        return (position - start) / (end - start);
    }
}