package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;

import java.util.List;

/**
 * 动感相机（第三人称相机平滑跟随）。
 *
 * 模式：
 * <ul>
 *   <li><b>Bounce</b> — 弹性：相机带惯性，转向时过冲回弹，有"弹簧"手感。</li>
 *   <li><b>Classic</b> — 经典：相机向目标位置匀速插值跟随，
 *       X/Y/Z 三个轴向的跟随速度可独立调整。</li>
 * </ul>
 */
public class MotionCamera extends Module {
    public static MotionCamera INSTANCE;

    public static final ModeSetting mode = new ModeSetting("Mode", "Camera follow mode", "Bounce",
        List.of("Bounce", "Classic"));

    // Bounce 模式参数
    public static final DoubleSetting stiffness = new DoubleSetting("Stiffness", "Spring stiffness (higher = snappier)",
        120.0, 300.0, 40.0, 5.0, () -> mode.is("Bounce"));
    public static final DoubleSetting damping = new DoubleSetting("Damping", "Spring damping (lower = more bounce)",
        10.0, 24.0, 3.0, 0.5, () -> mode.is("Bounce"));
    public static final DoubleSetting maxDistance = new DoubleSetting("Max Distance", "Maximum camera lag distance",
        4.0, 8.0, 1.0, 0.5, () -> mode.is("Bounce"));

    // Classic 模式参数（每轴跟随速度，60fps 基准的每帧插值比例）
    public static final DoubleSetting speedX = new DoubleSetting("Speed X", "Horizontal (X) follow speed",
        0.2, 1.0, 0.02, 0.01, () -> mode.is("Classic"));
    public static final DoubleSetting speedY = new DoubleSetting("Speed Y", "Vertical (Y) follow speed",
        0.35, 1.0, 0.02, 0.01, () -> mode.is("Classic"));
    public static final DoubleSetting speedZ = new DoubleSetting("Speed Z", "Horizontal (Z) follow speed",
        0.2, 1.0, 0.02, 0.01, () -> mode.is("Classic"));

    public static final BooleanSetting onlyThirdPerson = new BooleanSetting("Only Third Person", "Only work in third person", true);

    public MotionCamera() {
        super("MotionCamera", "Smooth camera movement in third person view.", Category.RENDER);
        this.settings.add(mode);
        this.settings.add(stiffness);
        this.settings.add(damping);
        this.settings.add(maxDistance);
        this.settings.add(speedX);
        this.settings.add(speedY);
        this.settings.add(speedZ);
        this.settings.add(onlyThirdPerson);
        INSTANCE = this;
        this.enabled = false;
    }

    public static boolean isEnabled() {
        return INSTANCE != null && INSTANCE.enabled;
    }
}
