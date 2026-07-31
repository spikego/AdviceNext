package cn.advicenext.features.module.impl.combat;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.slider.DoubleSetting;

public class Reach extends Module {

    public static Reach INSTANCE;

    private final DoubleSetting range = new DoubleSetting("Range", "Reach distance", 3.5, 6.0, 3.0, 0.1);

    public Reach() {
        super("Reach", "Extends player reach distance", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(range);
    }

    public double getReachDistance() {
        return range.getValue();
    }

    @Override
    public String getDisplayValue() {
        return String.format("%.1f", range.getValue());
    }
}