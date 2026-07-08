package cn.advicenext.features.value;

import java.util.function.Supplier;

public class BooleanSetting extends AbstractSetting<Boolean> {
    public BooleanSetting(String name, String description, Boolean value) {
        super(name, description, value);
    }

    public BooleanSetting(String name, String description, Boolean value, java.util.function.Supplier<Boolean> visible) {
        super(name, description, value, visible);
    }
}