package cn.advicenext.features.module.impl.movement;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class Elytra extends Module {

    public static Elytra INSTANCE;

    private final ModeSetting mode = new ModeSetting("Mode", "Flight mode", "Boost",
            List.of("Boost", "Glide", "Control"));

    private final DoubleSetting speed = new DoubleSetting("Speed", "Flight speed", 1.5, 3.0, 0.1, 0.05);
    private final DoubleSetting upSpeed = new DoubleSetting("UpSpeed", "Upward boost speed", 0.5, 2.0, 0.0, 0.05);
    private final DoubleSetting downSpeed = new DoubleSetting("DownSpeed", "Downward speed", 0.5, 2.0, 0.0, 0.05);

    private final DoubleSetting minPitch = new DoubleSetting("MinPitch", "Minimum pitch for boost", 0.0, 90.0, -90.0, 1.0);
    private final DoubleSetting pitchControl = new DoubleSetting("PitchControl", "Pitch influence on speed", 1.0, 2.0, 0.0, 0.1);

    private final BooleanSetting autoEquip = new BooleanSetting("AutoEquip", "Auto equip elytra", true);
    private final BooleanSetting takeOff = new BooleanSetting("TakeOff", "Auto take-off", true);
    private final BooleanSetting firework = new BooleanSetting("Firework", "Use firework boost", false);
    private final DoubleSetting fireworkDelay = new DoubleSetting("FireworkDelay", "Firework delay ms", 500.0, 3000.0, 0.0, 100.0,
            () -> firework.getValue());

    private final BooleanSetting instantFly = new BooleanSetting("InstantFly", "Take off instantly", false);
    private final BooleanSetting noFall = new BooleanSetting("NoFall", "No fall damage", true);

    private long lastFirework = 0;

    public Elytra() {
        super("Elytra", "Elytra flight control", Category.MOVEMENT);
        INSTANCE = this;
        this.settings.add(mode);
        this.settings.add(speed);
        this.settings.add(upSpeed);
        this.settings.add(downSpeed);
        this.settings.add(minPitch);
        this.settings.add(pitchControl);
        this.settings.add(autoEquip);
        this.settings.add(takeOff);
        this.settings.add(firework);
        this.settings.add(fireworkDelay);
        this.settings.add(instantFly);
        this.settings.add(noFall);
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (!mc.player.isGliding()) {
            if (autoEquip.getValue()) {
                equipElytra();
            }
            if (takeOff.getValue() && canTakeOff()) {
                takeOff();
            }
            return;
        }

        String flightMode = mode.getValue();
        Vec3d velocity = mc.player.getVelocity();
        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());

        if (flightMode.equals("Boost")) {
            handleBoost(velocity, yaw, pitch);
        } else if (flightMode.equals("Glide")) {
            handleGlide(velocity, yaw, pitch);
        } else if (flightMode.equals("Control")) {
            handleControl(velocity, yaw, pitch);
        }

        if (firework.getValue()) {
            handleFirework();
        }
    }

    private void handleBoost(Vec3d velocity, double yaw, double pitch) {
        double pitchDeg = mc.player.getPitch();

        if (pitchDeg > minPitch.getValue()) {
            double boostSpeed = speed.getValue();
            double pitchFactor = Math.max(0, (pitchDeg - minPitch.getValue()) / (90.0 - minPitch.getValue()));
            boostSpeed *= 1.0 + pitchFactor * pitchControl.getValue();

            double motionX = -Math.sin(yaw) * boostSpeed * 0.05;
            double motionZ = Math.cos(yaw) * boostSpeed * 0.05;

            mc.player.addVelocity(motionX, 0, motionZ);
        } else if (pitchDeg < -minPitch.getValue()) {
            double boostSpeed = upSpeed.getValue();
            mc.player.addVelocity(0, -boostSpeed * 0.05, 0);
        } else {
            mc.player.setVelocity(velocity.x * 0.99, velocity.y * 0.98, velocity.z * 0.99);
        }
    }

    private void handleGlide(Vec3d velocity, double yaw, double pitch) {
        double pitchDeg = mc.player.getPitch();

        double motionX = -Math.sin(yaw) * speed.getValue() * 0.03;

        double motionZ = Math.cos(yaw) * speed.getValue() * 0.03;

        double motionY = 0;
        if (pitchDeg > 0) {
            motionY = -pitchDeg * downSpeed.getValue() * 0.0005;
        } else {
            motionY = -pitchDeg * upSpeed.getValue() * 0.0005;
        }

        mc.player.addVelocity(motionX, motionY, motionZ);
    }

    private void handleControl(Vec3d velocity, double yaw, double pitch) {
        double pitchDeg = mc.player.getPitch();
        double targetSpeed = speed.getValue() * 0.04;

        double motionX = -Math.sin(yaw) * targetSpeed;
        double motionZ = Math.cos(yaw) * targetSpeed;

        double motionY = 0;
        if (mc.options.jumpKey.isPressed()) {
            motionY = upSpeed.getValue() * 0.01;
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -downSpeed.getValue() * 0.01;
        }

        double lerpFactor = 0.2;
        mc.player.setVelocity(
                velocity.x + (motionX - velocity.x) * lerpFactor,
                velocity.y + (motionY - velocity.y) * lerpFactor,
                velocity.z + (motionZ - velocity.z) * lerpFactor
        );
    }

    private void handleFirework() {
        if (mc.player == null || mc.interactionManager == null) return;
        long now = System.currentTimeMillis();
        if (now - lastFirework < fireworkDelay.getValue().longValue()) return;

        if (mc.player.isGliding() && mc.options.jumpKey.isPressed()) {
            int fireworkSlot = findFireworkSlot();
            if (fireworkSlot == -1) return;

            int prevSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(fireworkSlot);
            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
            mc.player.getInventory().setSelectedSlot(prevSlot);
            lastFirework = now;
        }
    }

    private void equipElytra() {
        if (mc.player == null) return;
        int elytraSlot = findElytraSlot();
        if (elytraSlot == -1) return;

        int chestSlot = 38;
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                chestSlot,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                mc.player
        );
    }

    private void takeOff() {
        if (mc.player == null) return;
        if (instantFly.getValue()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
        } else {
            mc.player.jump();
        }
    }

    private boolean canTakeOff() {
        if (mc.player == null) return false;
        if (mc.player.isOnGround()) return false;
        return mc.player.fallDistance > 0.5;
    }

    private int findElytraSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    private int findFireworkSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }
}