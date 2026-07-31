package cn.advicenext.gui.hud.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WidgetRegistry {

    private static final Map<String, Widget> widgets = new LinkedHashMap<>();

    public static void register(Widget widget) {
        widgets.put(widget.getId(), widget);
    }

    public static Widget get(String id) {
        return widgets.get(id);
    }

    public static List<Widget> getAll() {
        return new ArrayList<>(widgets.values());
    }

    public static int size() {
        return widgets.size();
    }

    public static void clear() {
        widgets.clear();
    }
}