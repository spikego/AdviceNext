package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.gui.clickgui.ClickGuiScreen;
import cn.advicenext.gui.clickgui.neverlose.NeverloseClickGuiScreen;
import cn.advicenext.gui.clickgui.newgui.NewClickGuiScreen;
import cn.advicenext.gui.clickgui.novoline.NovolineClickGuiScreen;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Module {
    private ClickGuiScreen guiScreen;
    private NovolineClickGuiScreen novolineGuiScreen;
    private NewClickGuiScreen newGuiScreen;
    private NeverloseClickGuiScreen neverloseGuiScreen;

    public final ModeSetting guiMode = new ModeSetting("Mode", "GUI style", "Default",
            ObjectArrayList.of("Default", "Novoline", "NewGUI", "Neverlose"));
    public final BooleanSetting animations = new BooleanSetting("Animations", "Enable GUI animations", true,
            () -> guiMode.is("Default"));
    public final BooleanSetting sound = new BooleanSetting("Sound", "Enable GUI sounds", true,
            () -> guiMode.is("Default") || guiMode.is("Novoline") || guiMode.is("NewGUI") || guiMode.is("Neverlose"));

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
            switch (guiMode.getValue()) {
                case "Novoline":
                    novolineGuiScreen = new NovolineClickGuiScreen(this);
                    mc.setScreen(novolineGuiScreen);
                    break;
                case "NewGUI":
                    newGuiScreen = new NewClickGuiScreen(this);
                    mc.setScreen(newGuiScreen);
                    break;
                case "Neverlose":
                    neverloseGuiScreen = new NeverloseClickGuiScreen(this);
                    mc.setScreen(neverloseGuiScreen);
                    break;
                default:
                    guiScreen = new ClickGuiScreen(this);
                    mc.setScreen(guiScreen);
                    break;
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGuiScreen
                || mc.currentScreen instanceof NovolineClickGuiScreen
                || mc.currentScreen instanceof NewClickGuiScreen
                || mc.currentScreen instanceof NeverloseClickGuiScreen) {
            mc.setScreen(null);
        }
    }
}