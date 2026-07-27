package cn.advicenext.features.module.impl.misc;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.utility.minecraft.movement.MovementCorrection;
import cn.advicenext.utility.minecraft.player.RotationUtils;

import java.util.List;

public class AntiAim extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Anti-aim mode", "Spin",
        List.of("Spin", "Jitter", "Random", "Backwards", "Down"));
    private final DoubleSetting speed = new DoubleSetting("Speed", "Rotation speed", 10.0, 50.0, 1.0, 1.0);
    private final ModeSetting movementCorrection = new ModeSetting("MovementCorrection", "How to correct movement input", "Silent",
        List.of("Off", "Strict", "Silent"));

    private float currentYaw;
    private boolean jitterDirection = true;

    public AntiAim() {
        super("AntiAim", "Silent anti-aim rotation", Category.MISC);
        this.settings.add(mode);
        this.settings.add(speed);
        this.settings.add(movementCorrection);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        RotationUtils.Rotation rotation = null;

        switch (mode.getValue()) {
            case "Spin" -> {
                currentYaw += speed.getValue().floatValue();
                if (currentYaw > 360) currentYaw -= 360;
                rotation = new RotationUtils.Rotation(currentYaw, mc.player.getPitch());
            }
            case "Jitter" -> {
                float yaw = mc.player.getYaw();
                yaw += jitterDirection ? speed.getValue().floatValue() : -speed.getValue().floatValue();
                jitterDirection = !jitterDirection;
                rotation = new RotationUtils.Rotation(yaw, mc.player.getPitch());
            }
            case "Random" -> {
                float randomYaw = (float) (Math.random() * 360);
                float randomPitch = (float) (Math.random() * 180 - 90);
                rotation = new RotationUtils.Rotation(randomYaw, Math.max(-90, Math.min(90, randomPitch)));
            }
            case "Backwards" -> {
                rotation = new RotationUtils.Rotation(mc.player.getYaw() + 180, mc.player.getPitch());
            }
            case "Down" -> {
                rotation = new RotationUtils.Rotation(mc.player.getYaw(), 90);
            }
        }

        if (rotation != null) {
            MovementCorrection.Mode correction = switch (movementCorrection.getValue()) {
                case "Silent" -> MovementCorrection.Mode.SILENT;
                case "Strict" -> MovementCorrection.Mode.STRICT;
                default -> MovementCorrection.Mode.OFF;
            };
            RotationUtils.setSilentRotation(rotation, correction);
        }
    }

    @Override
    public void onDisable() {
        RotationUtils.resetSilentRotation();
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}