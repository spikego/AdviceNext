package cn.spikego.advicenext.features.value.slider;

import cn.spikego.advicenext.features.value.AbstractSetting;

public class NumberSetting<N extends Number> extends AbstractSetting<N> {
    protected N max;
    protected N min;
    protected N step;

    public NumberSetting(String name, String description, N value, N max, N min, N step) {
        super(name, description, value);
        this.max = max;
        this.min = min;
        this.step = step;
    }

    public N getMax() {
        return max;
    }

    public void setMax(N max) {
        this.max = max;
    }

    public N getMin() {
        return min;
    }

    public void setMin(N min) {
        this.min = min;
    }

    public N getStep() {
        return step;
    }

    public void setStep(N step) {
        this.step = step;
    }
}