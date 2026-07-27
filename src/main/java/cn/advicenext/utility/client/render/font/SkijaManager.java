package cn.advicenext.utility.client.render.font;

import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;

/**
 * Skija 引擎管理：加载默认字体（优先中文支持的微软雅黑），
 * 提供初始化状态检测。初始化失败时 FontRenderer 回退到原版字体。
 */
public final class SkijaManager {

    private static boolean initialized = false;
    private static boolean attempted = false;
    private static Typeface typeface = null;

    private SkijaManager() {
    }

    public static boolean isInitialized() {
        if (!attempted) {
            init();
        }
        return initialized;
    }

    public static synchronized void init() {
        if (attempted) return;
        attempted = true;
        try {
            FontMgr mgr = FontMgr.getDefault();
            if (mgr == null) return;

            // 优先中文字体，保证界面中文清晰
            String[] candidates = {"Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", "SimHei", "Arial"};
            for (String family : candidates) {
                typeface = mgr.matchFamilyStyle(family, FontStyle.NORMAL);
                if (typeface != null) break;
            }

            initialized = typeface != null;
        } catch (Throwable t) {
            initialized = false;
        }
    }

    public static Typeface getTypeface() {
        return typeface;
    }
}
