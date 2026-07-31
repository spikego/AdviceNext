package cn.advicenext.features.module.impl.misc;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.gui.musicplayer.MusicPlayerScreen;
import cn.advicenext.gui.musicplayer.MusicTheme;
import net.minecraft.client.MinecraftClient;
import java.util.List;

public class MusicPlayer extends Module {
    public final ModeSetting theme = new ModeSetting("Theme", "UI theme style", "Chrome Dark",
        List.of("Chrome Light", "Chrome Dark", "Ocean", "Sunset", "Midnight", "Forest", "Sakura", "Monochrome"));
    public final BooleanSetting visualizer = new BooleanSetting("Visualizer", "Show audio visualizer on HUD", true);

    public MusicPlayer() {
        super("MusicPlayer", "Music player with NetEase Cloud Music API", Category.MISC);
        this.settings.add(theme);
        this.settings.add(visualizer);
    }

    @Override
    public void onEnable() {
        MinecraftClient.getInstance().setScreen(new MusicPlayerScreen());
        this.enabled = false;
    }

    public static MusicTheme getCurrentTheme() {
        return MusicTheme.valueOf(MusicTheme.class, "CHROME_DARK");
    }
}