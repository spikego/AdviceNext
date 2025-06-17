package cn.spikego.advicenext.features.value;

import java.util.List;

public class ModeSetting extends AbstractSetting<String> {
    private List<String> modes;

    public ModeSetting(String name, String description, String value, List<String> modes) {
        super(name, description, value);
        this.modes = modes;
    }

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> modes) {
        this.modes = modes;
    }
}