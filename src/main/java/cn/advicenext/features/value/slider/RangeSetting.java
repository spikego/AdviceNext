package cn.advicenext.features.value.slider;

import cn.advicenext.features.value.AbstractSetting;

import java.util.function.Supplier;

/**
 * 范围设置：一个控件同时调整 [min, max] 两个端点，
 * 代替过去 "MinXxx + MaxXxx" 两个独立滑块的写法。
 * ClickGUI 中渲染为双把手滑块，点击靠近哪端拖哪端。
 */
public class RangeSetting extends AbstractSetting<RangeSetting.Range> {

    public static class Range {
        private double min;
        private double max;

        public Range(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }
    }

    private final double boundMin;
    private final double boundMax;
    private final double step;

    public RangeSetting(String name, String description, double valueMin, double valueMax,
                        double boundMin, double boundMax, double step) {
        super(name, description, new Range(valueMin, valueMax));
        this.boundMin = boundMin;
        this.boundMax = boundMax;
        this.step = step;
    }

    public RangeSetting(String name, String description, double valueMin, double valueMax,
                        double boundMin, double boundMax, double step, Supplier<Boolean> visible) {
        super(name, description, new Range(valueMin, valueMax), visible);
        this.boundMin = boundMin;
        this.boundMax = boundMax;
        this.step = step;
    }

    public double getMinValue() {
        return value.getMin();
    }

    public double getMaxValue() {
        return value.getMax();
    }

    public void setMinValue(double v) {
        v = clamp(v);
        // 不允许越过另一端
        value.min = Math.min(v, value.max);
    }

    public void setMaxValue(double v) {
        v = clamp(v);
        value.max = Math.max(v, value.min);
    }

    public double getBoundMin() {
        return boundMin;
    }

    public double getBoundMax() {
        return boundMax;
    }

    public double getStep() {
        return step;
    }

    /** 范围内均匀随机值（Legit 随机化常用） */
    public double getRandom() {
        return getMinValue() + Math.random() * (getMaxValue() - getMinValue());
    }

    /** 范围中值 */
    public double getCenter() {
        return (getMinValue() + getMaxValue()) / 2.0;
    }

    private double clamp(double v) {
        v = Math.max(boundMin, Math.min(boundMax, v));
        if (step > 0) {
            v = boundMin + Math.round((v - boundMin) / step) * step;
        }
        return v;
    }
}
