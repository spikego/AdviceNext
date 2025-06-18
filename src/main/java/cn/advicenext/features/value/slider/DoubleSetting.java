package cn.advicenext.features.value.slider;

public class DoubleSetting extends NumberSetting<Double> {
    public DoubleSetting(String name, String description, Double value, Double max, Double min, Double step) {
        super(name, description, value, max, min, step);
    }
}