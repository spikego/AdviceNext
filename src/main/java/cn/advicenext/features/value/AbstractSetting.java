package cn.advicenext.features.value;

import java.util.function.Supplier;

public abstract class AbstractSetting<T> {
    protected String name;
    protected String description;
    protected T defaultValue;
    protected T value;
    protected Supplier<Boolean> visible;

    public AbstractSetting(String name, String description, T defaultValue, Supplier<Boolean> visible) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visible = visible != null ? visible : () -> true;
    }

    public AbstractSetting(String name, String description, T defaultValue) {
        this(name, description, defaultValue, () -> true);
    }

    public AbstractSetting(String name, T defaultValue) {
        this(name, "none", defaultValue, () -> true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Supplier<Boolean> getVisible() {
        return visible;
    }

    public void setVisible(Supplier<Boolean> visible) {
        this.visible = visible != null ? visible : () -> true;
    }
}