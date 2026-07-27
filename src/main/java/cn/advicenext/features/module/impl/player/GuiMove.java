package cn.advicenext.features.module.impl.player;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;

import net.minecraft.world.tick.Tick;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class GuiMove extends Module {

    private ModeSetting mode = new ModeSetting("Mode", "Mode", "Basic", List.of("Basic", "Advanced"));
    private BooleanSetting allowSprint = new BooleanSetting("Allow Sprint", "Allow Sprint", true,() -> mode.is("Basic"));
    private BooleanSetting allowJump = new BooleanSetting("Allow Jump", "Allow Jump", true,() -> mode.is("Basic"));
    private BooleanSetting allowSneak = new BooleanSetting("Allow Sneak", "Allow Sneak", true,() -> mode.is("Basic"));
    public GuiMove() {
        super("GuiMove", "Allows you to move the GUI", Category.PLAYER);
    }

    @Override
    public void onTick(TickEvent event){
        if(mode.is("Basic")) {
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
                mc.options.forwardKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
                mc.options.backKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
                mc.options.leftKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
                mc.options.rightKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS && allowJump.getValue()) {
                mc.options.jumpKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS && allowSneak.getValue()) {
                mc.options.sneakKey.setPressed(true);
            }
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS && allowSprint.getValue()) {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    @Override
    public void onDisable() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    @Override
    public String getDisplayValue() {
        return mode.getValue();
    }

}