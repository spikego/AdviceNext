package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public class Animation extends Module {

    public static Animation INSTANCE;

    private final ModeSetting swingMode = new ModeSetting("Swing", "Swing animation style", "1.7",
            List.of("1.7", "PushDown", "None"));

    private final ModeSetting hitMode = new ModeSetting("Hit", "Hit animation style", "1.7",
            List.of("1.7", "PushDown", "None"));

    private final BooleanSetting slowSwing = new BooleanSetting("SlowSwing", "Slow down swing animation", false);
    private final DoubleSetting swingSpeed = new DoubleSetting("SwingSpeed", "Swing speed multiplier", 1.0, 3.0, 0.1, 0.1,
            () -> slowSwing.getValue());

    private final BooleanSetting oldBlock = new BooleanSetting("OldBlock", "1.7 style block animation", false);
    private final BooleanSetting oldEat = new BooleanSetting("OldEat", "1.7 style eat animation", false);

    private final DoubleSetting scale = new DoubleSetting("Scale", "Item scale", 1.0, 2.0, 0.1, 0.05);
    private final DoubleSetting itemX = new DoubleSetting("ItemX", "Item X offset", 0.0, 2.0, -1.0, 0.05);
    private final DoubleSetting itemY = new DoubleSetting("ItemY", "Item Y offset", 0.0, 2.0, -1.0, 0.05);
    private final DoubleSetting itemZ = new DoubleSetting("ItemZ", "Item Z offset", 0.0, 2.0, -1.0, 0.05);

    private final DoubleSetting blockX = new DoubleSetting("BlockX", "Blocking X offset", 0.0, 1.0, -1.0, 0.05);
    private final DoubleSetting blockY = new DoubleSetting("BlockY", "Blocking Y offset", 0.0, 1.0, -1.0, 0.05);
    private final DoubleSetting blockZ = new DoubleSetting("BlockZ", "Blocking Z offset", 0.0, 1.0, -1.0, 0.05);

    public Animation() {
        super("Animation", "Custom item animations", Category.RENDER);
        INSTANCE = this;
        this.settings.add(swingMode);
        this.settings.add(hitMode);
        this.settings.add(slowSwing);
        this.settings.add(swingSpeed);
        this.settings.add(oldBlock);
        this.settings.add(oldEat);
        this.settings.add(scale);
        this.settings.add(itemX);
        this.settings.add(itemY);
        this.settings.add(itemZ);
        this.settings.add(blockX);
        this.settings.add(blockY);
        this.settings.add(blockZ);
    }

    public void applySwing(float swingProgress, MatrixStack matrices, int i, Arm arm, CallbackInfo ci) {
        String mode = swingMode.getValue();

        if (mode.equals("None")) {
            ci.cancel();
            return;
        }

        ci.cancel();
        float f = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

        if (mode.equals("1.7")) {
            matrices.translate(0.0F, -0.1F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * f * 45.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * f * -30.0F));
        } else if (mode.equals("PushDown")) {
            matrices.translate(0.0F, -0.4F * swingProgress, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * f * 30.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * f * -20.0F));
        }
    }

    public void applyHit(float equipProgress, MatrixStack matrices, int i, Arm arm, CallbackInfo ci) {
        String mode = hitMode.getValue();

        if (mode.equals("None")) {
            ci.cancel();
            return;
        }

        ci.cancel();
        float f = MathHelper.sin(MathHelper.sqrt(equipProgress) * (float) Math.PI);

        if (mode.equals("1.7")) {
            matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        } else if (mode.equals("PushDown")) {
            matrices.translate(0.0F, equipProgress * -0.4F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * f * -15.0F));
        }
    }

    public String getSwingMode() {
        return swingMode.getValue();
    }

    public String getHitMode() {
        return hitMode.getValue();
    }

    public boolean isSlowSwing() {
        return slowSwing.getValue();
    }

    public double getSwingSpeed() {
        return swingSpeed.getValue();
    }

    public boolean isOldBlock() {
        return oldBlock.getValue();
    }

    public boolean isOldEat() {
        return oldEat.getValue();
    }

    public double getScale() {
        return scale.getValue();
    }

    public double getItemX() {
        return itemX.getValue();
    }

    public double getItemY() {
        return itemY.getValue();
    }

    public double getItemZ() {
        return itemZ.getValue();
    }

    public double getBlockX() {
        return blockX.getValue();
    }

    public double getBlockY() {
        return blockY.getValue();
    }

    public double getBlockZ() {
        return blockZ.getValue();
    }
}