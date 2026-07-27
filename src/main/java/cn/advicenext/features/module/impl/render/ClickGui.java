package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.gui.clickgui.ClickGuiScreen;
import cn.advicenext.gui.clickgui.novoline.NovolineClickGuiScreen;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Module {
    private ClickGuiScreen guiScreen;
    private NovolineClickGuiScreen novolineGuiScreen;

    public final ModeSetting guiMode = new ModeSetting("Mode", "GUI style", "Default",
            ObjectArrayList.of("Default", "Novoline"));
    public final BooleanSetting animations = new BooleanSetting("Animations", "Enable GUI animations", true,
            () -> guiMode.is("Default"));
    public final BooleanSetting sound = new BooleanSetting("Sound", "Enable GUI sounds", true,
            () -> guiMode.is("Default") || guiMode.is("Novoline"));

    public ClickGui() {
        super("ClickGui", "A GUI to toggle and configure modules", Category.RENDER);
        this.bindKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

        this.settings.add(guiMode);
        this.settings.add(animations);
        this.settings.add(sound);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            if (guiMode.getValue().equals("Novoline")) {
                novolineGuiScreen = new NovolineClickGuiScreen(this);
                mc.setScreen(novolineGuiScreen);
            } else {
                guiScreen = new ClickGuiScreen(this);
                mc.setScreen(guiScreen);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof NovolineClickGuiScreen) {
            mc.setScreen(null);
        }
    }
}