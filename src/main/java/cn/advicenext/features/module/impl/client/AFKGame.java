package cn.advicenext.features.module.impl.client;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.gui.games.GameMenuScreen;

public class AFKGame extends Module{
    public AFKGame() {
        super("AFKGame", "AFKGame", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new GameMenuScreen());
        disable();
    }
}
