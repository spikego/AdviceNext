package cn.advicenext.features.value.slider;

public class IntSetting extends NumberSetting<Integer> {
    public IntSetting(String name, String description, Integer value, Integer max, Integer min, Integer step) {
        super(name, description, value, max, min, step);
    }

    public IntSetting(String name, String description, Integer value, Integer max, Integer min, Integer step, java.util.function.Supplier<Boolean> visible) {
        super(name, description, value, max, min, step, visible);
    }
}