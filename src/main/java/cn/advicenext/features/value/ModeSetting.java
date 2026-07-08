package cn.advicenext.features.value;

import java.util.List;

public class ModeSetting extends AbstractSetting<String> {
    private List<String> modes;

    public ModeSetting(String name, String description, String value, List<String> modes) {
        super(name, description, value);
        this.modes = modes;
    }

    public ModeSetting(String name, String description, String value, java.util.List<String> modes, java.util.function.Supplier<Boolean> visible) {
        super(name, description, value, visible);
        this.modes = modes;
    }

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> modes) {
        this.modes = modes;
    }
    
    /**
     * Cycles to the next mode in the list
     */
    public void cycle() {
        int index = modes.indexOf(getValue());
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }

    public boolean is(String mode) {
        return getValue().equals(mode);
    }
}