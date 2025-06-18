package cn.spikego.advicenext.features.module.impl.render;

import cn.spikego.advicenext.features.module.Category;
import cn.spikego.advicenext.features.module.Module;
import cn.spikego.advicenext.features.value.BooleanSetting;
import cn.spikego.advicenext.gui.clickgui.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Module {
    private ClickGuiScreen guiScreen;
    
    // Settings
    public final BooleanSetting animations = new BooleanSetting("Animations", "Enable GUI animations", true);
    public final BooleanSetting sound = new BooleanSetting("Sound", "Enable GUI sounds", true);

    public ClickGui() {
        super("ClickGui", "A GUI to toggle and configure modules", Category.RENDER);
        this.bindKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        
        // Add settings
        this.settings.add(animations);
        this.settings.add(sound);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            guiScreen = new ClickGuiScreen(this);
            mc.setScreen(guiScreen);
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGuiScreen) {
            mc.setScreen(null);
        }
    }
}