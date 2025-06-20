package cn.advicenext.features.module.impl.client;

import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;

public class ClientTheme extends Module{
    public static ClientTheme INSTANCE;
    private final IntSetting themeRed = new IntSetting("ThemeRed1", "Red component of the theme color", 193, 255, 0, 1);
    private final IntSetting themeGreen = new IntSetting("ThemeGreen1", "Green component of the theme color", 0, 255, 0, 1);
    private final IntSetting themeBlue = new IntSetting("ThemeBlue1", "Blue component of the theme color", 0, 255, 0, 1);
    private final IntSetting themeRed2 = new IntSetting("ThemeRed2", "Red component of the theme color", 255, 255, 0, 1);
    private final IntSetting themeGreen2 = new IntSetting("ThemeGreen2", "Green component of the theme color", 255, 255, 0, 1);
    private final IntSetting themeBlue2 = new IntSetting("ThemeBlue2", "Blue component of the theme color", 255, 255, 0, 1);

    public ClientTheme() {
        super("ClientTheme", "Change the theme color of the client", Category.CLIENT);
        this.settings.add(themeRed);
        this.settings.add(themeGreen);
        this.settings.add(themeBlue);
        this.settings.add(themeRed2);
        this.settings.add(themeGreen2);
        this.settings.add(themeBlue2);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        enabled = false;
    }

    public int getThemeRed1() {
        return themeRed.getValue();
    }

    public int getThemeGreen1() {
        return themeGreen.getValue();
    }

    public int getThemeBlue1() {
        return themeBlue.getValue();
    }

    public int getThemeRed2() {
        return themeRed2.getValue();
    }
    public int getThemeGreen2() {
        return themeGreen2.getValue();
    }

    public int getThemeBlue2() {
        return themeBlue2.getValue();
    }

}
