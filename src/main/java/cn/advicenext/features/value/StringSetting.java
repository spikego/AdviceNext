package cn.advicenext.features.value;

import java.util.function.Supplier;

public class StringSetting extends AbstractSetting<String> {
    public StringSetting(String name, String description, String value) {
        super(name, description, value);
    }

    public StringSetting(String name, String description, String value, java.util.function.Supplier<Boolean> visible) {
        super(name, description, value, visible);
    }
}