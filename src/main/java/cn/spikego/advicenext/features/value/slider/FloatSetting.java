package cn.spikego.advicenext.features.value.slider;

public class FloatSetting extends NumberSetting<Float> {
    public FloatSetting(String name, String description, Float value, Float max, Float min, Float step) {
        super(name, description, value, max, min, step);
    }
}