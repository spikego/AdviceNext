package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.gui.hud.HUDEditScreen;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.gui.hud.widget.WidgetRegistry;
import cn.advicenext.gui.hud.widgets.ArrayListWidget;
import cn.advicenext.gui.hud.widgets.BlockCounterWidget;
import cn.advicenext.gui.hud.widgets.MusicPlayerInfo;
import cn.advicenext.gui.hud.widgets.TargetInfoWidget;
import cn.advicenext.gui.hud.widgets.WatermarkWidget;

public class HUD extends Module {

    public final BooleanSetting WaterMark = new BooleanSetting("WaterMark", "WaterMark", true);
    public StringSetting WaterMarkText = new StringSetting("WaterMarkText", "WaterMarkText", "AdviceNext");
    public final ModeSetting WatermarkMode = new ModeSetting("WM Mode", "Watermark display mode", "Text",
            java.util.List.of("Text", "Bar"), () -> WaterMark.getValue());
    public final BooleanSetting ArrayList = new BooleanSetting("ArrayList", "Shows enabled modules", true);
    public final ModeSetting ALSortMode = new ModeSetting("AL Sort", "ArrayList sort order", "Length",
            java.util.List.of("Length", "A-Z", "Z-A"), () -> ArrayList.getValue());
    public final ModeSetting ALDisplayValue = new ModeSetting("AL Value", "Display value format", "[]",
            java.util.List.of("[]", "Space", "-"), () -> ArrayList.getValue());
    public final ModeSetting ALStyle = new ModeSetting("AL Style", "ArrayList background style", "NoBackground",
            java.util.List.of("NoBackground", "OneBlur", "Outline", "Sidebar"), () -> ArrayList.getValue());
    public final ModeSetting ALAnimation = new ModeSetting("AL Anim", "ArrayList entry animation", "Bounce",
            java.util.List.of("Bounce", "Motion", "Fall"), () -> ArrayList.getValue());
    public final BooleanSetting Notification = new BooleanSetting("Notifications", "Shows notifications", true);
    public final BooleanSetting TargetInfo = new BooleanSetting("TargetInfo", "Shows target player info", true);
    public final ModeSetting TargetInfoMode = new ModeSetting("TI Mode", "TargetInfo display mode", "Default",
            java.util.List.of("Default", "New"), () -> TargetInfo.getValue());
    public final BooleanSetting TIFollowTarget = new BooleanSetting("TI Follow", "Follow target position", true,
            () -> TargetInfo.getValue() && TargetInfoMode.is("New"));
    public final BooleanSetting TIBlur = new BooleanSetting("TI Blur", "Gaussian blur behind TargetInfo", true,
            () -> TargetInfo.getValue());
    public final BooleanSetting MPInfo = new BooleanSetting("MPInfo", "Shows music player info", true);
    public final BooleanSetting BlockCounter = new BooleanSetting("BlockCounter", "Shows block counter", true);
    public final BooleanSetting HudEdit = new BooleanSetting("HudEdit", "Opens the HUD editor", false);

    private static HUD hudInstance;

    public static HUD getHudInstance() {
        return hudInstance;
    }

    public HUD() {
        super("HUD", "Render HUD", Category.RENDER);
        this.settings.add(HudEdit);
        hudInstance = this;
        initWidgets();
    }

    private void initWidgets() {
        WidgetRegistry.register(new WatermarkWidget());
        WidgetRegistry.register(new ArrayListWidget());
        WidgetRegistry.register(new TargetInfoWidget());
        WidgetRegistry.register(new MusicPlayerInfo());
        WidgetRegistry.register(new BlockCounterWidget());
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (WaterMark.getValue()) {
            Widget wm = WidgetRegistry.get("watermark");
            if (wm instanceof WatermarkWidget w) {
                w.setText(WaterMarkText.getValue());
                w.render(event.getContext(), 0, 0, 0);
            }
        }

        if (ArrayList.getValue()) {
            Widget al = WidgetRegistry.get("arraylist");
            if (al != null) {
                al.render(event.getContext(), 0, 0, 0);
            }
        }

        if (Notification.getValue()) {
            NotificationManager.getInstance().render(event);
        }

        if (TargetInfo.getValue()) {
            Widget ti = WidgetRegistry.get("targetinfo");
            if (ti != null) {
                ti.render(event.getContext(), 0, 0, 0);
            }
        }

        if (MPInfo.getValue()) {
            Widget mp = WidgetRegistry.get("musicplayerinfo");
            if (mp != null) {
                mp.render(event.getContext(), 0, 0, 0);
            }
        }

        if (BlockCounter.getValue()) {
            Widget bc = WidgetRegistry.get("blockcounter");
            if (bc != null) {
                bc.render(event.getContext(), 0, 0, 0);
            }
        }

        if (HudEdit.getValue()) {
            mc.setScreen(new HUDEditScreen());
            HudEdit.setValue(false);
        }
    }
}