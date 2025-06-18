package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.gui.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module{

    private final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);
    private final BooleanSetting ArrayList = new BooleanSetting("ArrayList", "Shows enabled modules", true);
    private final BooleanSetting Notification = new BooleanSetting("Notifications", "Shows notifications", true);

    public HUD() {
        super("HUD", "Render HUD", Category.RENDER);
        this.enabled = true;
        this.settings.add(WaterMark);
        this.settings.add(ArrayList);
        this.key = GLFW.GLFW_KEY_H;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (WaterMark.getValue()) {
            event.getContext().drawText(mc.textRenderer, "AdviceNext", 10, 10, Colors.currentColor().getRGB(), true);
        }

        if (ArrayList.getValue()) {
            renderArrayList(event);
        }

        if(Notification.getValue()) {
            NotificationManager.getInstance().render(event);
        }
    }


    private void renderArrayList(Render2DEvent event) {
        // 使用类名调用静态方法，而不是通过实例
        List<Module> enabledModules = ModuleManager.getModules().stream()
                .filter(Module::getEnabled)
                .sorted(Comparator.comparing(m -> -mc.textRenderer.getWidth(m.getName())))
                .collect(Collectors.toList());

        int y = 10;
        int screenWidth = mc.getWindow().getScaledWidth();

        for (Module module : enabledModules) {
            String name = module.getName();
            int width = mc.textRenderer.getWidth(name);
            int x = screenWidth - width - 5;

            // Draw module name with current color
            event.getContext().drawText(mc.textRenderer, name, x, y, Colors.currentColor().getRGB(), true);

            y += 10; // Move down for next module
        }
    }
}