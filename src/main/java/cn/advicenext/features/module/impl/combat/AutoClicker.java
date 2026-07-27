package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.RangeSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.item.consume.UseAction;

import java.util.List;
import java.util.Random;

/**
 * 自动连点。按住攻击键时按所选模式自动点击。
 *
 * 点击模式：
 * <ul>
 *   <li><b>Legit</b> — 拟人：CPS 围绕中心值正态分布波动，中心值缓慢漂移。</li>
 *   <li><b>Constant</b> — 恒定：固定 CPS（取范围中值）。</li>
 *   <li><b>Butterfly</b> — 蝴蝶点：两次快速连击 + 一次正常间隔，循环。</li>
 *   <li><b>Jitter</b> — 抖动：间隔在 CPS 范围内完全随机。</li>
 * </ul>
 */
public class AutoClicker extends Module {

    private final ModeSetting clickMode = new ModeSetting("Click Mode", "Click pattern", "Legit",
        List.of("Legit", "Constant", "Butterfly", "Jitter"));
    private final RangeSetting cps = new RangeSetting("CPS", "Clicks per second range",
        10.0, 14.0, 1.0, 30.0, 0.5);

    private final Random random = new Random();

    private long lastClick = 0;
    private long nextDelay = 0;

    // Butterfly 相位：0/1 = 快速连击，2 = 正常间隔
    private int butterflyPhase = 0;

    // Legit 漂移中心
    private double legitCenter = 12.0;
    private long lastDriftTime = 0;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks for you", Category.COMBAT);
        this.settings.add(clickMode);
        this.settings.add(cps);
    }

    @Override
    public void onTick(TickEvent event) {
        if (!this.enabled) return;
        if (mc.player == null || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) return;

        // 格挡时不点击
        if (mc.player.isUsingItem() && mc.player.getActiveItem().getUseAction() == UseAction.BLOCK) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClick < nextDelay) return;

        doClick();
        lastClick = now;
        nextDelay = computeNextDelay();
    }

    private void doClick() {
        mc.player.swingHand(Hand.MAIN_HAND);
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            if (mc.interactionManager != null) {
                mc.interactionManager.attackEntity(mc.player, entityHit.getEntity());
            }
        }
    }

    private long computeNextDelay() {
        return switch (clickMode.getValue()) {
            case "Constant" -> constantDelay();
            case "Butterfly" -> butterflyDelay();
            case "Jitter" -> jitterDelay();
            default -> legitDelay();
        };
    }

    /** 恒定：固定 CPS（范围中值） */
    private long constantDelay() {
        return (long) (1000.0 / cps.getCenter());
    }

    /** 抖动：CPS 范围内均匀随机 */
    private long jitterDelay() {
        return (long) (1000.0 / cps.getRandom());
    }

    /** 蝴蝶点：两个短间隔 + 一个正常间隔循环 */
    private long butterflyDelay() {
        long delay;
        if (butterflyPhase < 2) {
            delay = 40 + random.nextInt(50); // 40~90ms 快速连击
        } else {
            delay = (long) (1000.0 / cps.getRandom());
        }
        butterflyPhase = (butterflyPhase + 1) % 3;
        return delay;
    }

    /** 拟人：正态分布 CPS + 中心值每 2~4 秒缓慢漂移 */
    private long legitDelay() {
        long now = System.currentTimeMillis();
        if (now - lastDriftTime > 2000 + random.nextInt(2000)) {
            // 中心值在整个范围内漂移
            double span = (cps.getMaxValue() - cps.getMinValue()) * 0.25;
            legitCenter = cps.getCenter() + (random.nextGaussian() * span);
            legitCenter = Math.max(cps.getMinValue(), Math.min(cps.getMaxValue(), legitCenter));
            lastDriftTime = now;
        }

        double stddev = Math.max(0.5, (cps.getMaxValue() - cps.getMinValue()) / 4.0);
        double sample = legitCenter + random.nextGaussian() * stddev;
        sample = Math.max(cps.getMinValue(), Math.min(cps.getMaxValue(), sample));

        // 额外毫秒级抖动，避免间隔过于规整
        long delay = (long) (1000.0 / sample);
        return delay + random.nextInt(15) - 7;
    }

    @Override
    public void onEnable() {
        lastClick = 0;
        nextDelay = 0;
        butterflyPhase = 0;
        legitCenter = cps.getCenter();
        lastDriftTime = 0;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getDisplayValue() {
        return clickMode.getValue();
    }
}
