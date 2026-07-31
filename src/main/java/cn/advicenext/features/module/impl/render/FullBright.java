package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.IntSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public class FullBright extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "FullBright mode", "Gamma",
            List.of("Gamma", "NightVision"));

    private final IntSetting brightness = new IntSetting("Brightness", "Gamma brightness level", 15, 15, 1, 1,
            () -> mode.is("Gamma"));

    private double originalGamma = 0.0;
    private double currentGamma = 0.0;

    public FullBright() {
        super("FullBright", "Brightens the world", Category.RENDER);
        this.settings.add(mode);
        this.settings.add(brightness);
    }

    @Override
    public void onEnable() {
        if (mode.is("Gamma")) {
            originalGamma = mc.options.getGamma().getValue();
            currentGamma = originalGamma;
        }
    }



    @Override
    public void onDisable() {
        if (mode.is("Gamma") && mc.options != null) {
            mc.options.getGamma().setValue(originalGamma);
        }
        if (mode.is("NightVision") && mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        currentGamma = 0.0;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (mode.is("Gamma")) {
            double target = brightness.getValue();
            if (currentGamma < target) {
                currentGamma = Math.min(currentGamma + 0.1, target);
            }
            mc.options.getGamma().setValue(currentGamma);
        } else if (mode.is("NightVision")) {
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 1337, 0, false, false, false));
        }
    }
}