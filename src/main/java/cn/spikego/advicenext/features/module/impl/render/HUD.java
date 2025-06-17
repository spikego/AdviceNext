package cn.spikego.advicenext.features.module.impl.render;

import cn.spikego.advicenext.event.impl.Render2DEvent;
import cn.spikego.advicenext.features.module.Category;
import cn.spikego.advicenext.features.module.Module;
import cn.spikego.advicenext.features.value.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class HUD extends Module{

    private final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);

    public HUD() {
        super("HUD", "Render HUD", Category.RENDER);
        this.enabled = true;
        this.settings.add(WaterMark);
        this.key = GLFW.GLFW_KEY_H;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (WaterMark.getValue()) {
            event.getContext().drawText(mc.textRenderer, "AdviceNext", 10, 10, 0xFFFFFF,true);
        }
    }
}
