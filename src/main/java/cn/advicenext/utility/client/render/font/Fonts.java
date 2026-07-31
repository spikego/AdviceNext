package cn.advicenext.utility.client.render.font;

import it.unimi.dsi.fastutil.floats.Float2ObjectArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;

import java.io.InputStream;

/**
 * 字体规格枚举（MoonLight 风格）。
 * 每个枚举值对应一个 .ttf 字体文件，{@link #get(float)} 返回其 {@link FontRenderer} 实例。
 * 字体文件不存在时自动回退到系统 SansSerif，不崩溃。
 */
public enum Fonts {
    monoBold("jetbrains-mono/JetBrainsMono-Bold"),
    monoLight("jetbrains-mono/JetBrainsMono-Light"),
    mono("jetbrains-mono/JetBrainsMono-Medium"),
    interBold("inter/Inter_Bold"),
    interLight("inter/Inter_Light"),
    inter("inter/Inter_Regular"),
    interSemiBold("inter/Inter_SemiBold"),
    roboto("roboto/Roboto-Regular-14"),
    robotoBold("roboto/Roboto-Bold-3"),
    robotoLight("roboto/Roboto-Light-10"),
    robotoMedium("roboto/Roboto-Medium-12"),
    yahei("yahei/msyh"),
    yaheiBold("yahei/msyhbd"),
    yaheiLight("yahei/msyhlight");



    private final String file;
    private final Float2ObjectMap<FontRenderer> fontMap = new Float2ObjectArrayMap<>();

    Fonts(String file) {
        this.file = file;
    }

    public FontRenderer get(float size) {
        return get(size, true);
    }

    public FontRenderer get(float size, boolean antiAlias) {
        return fontMap.computeIfAbsent(size, key -> {
            FontRenderer fr = tryLoad(this.file, size, antiAlias);
            if (fr == null) fr = tryLoad("SansSerif", size, antiAlias);
            if (fr == null) fr = new FontRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (int) size), false);
            return fr;
        });
    }

    private static FontRenderer tryLoad(String file, float size, boolean antiAlias) {
        FontRenderer fr = tryLoadFile(file, ".ttf", size, antiAlias);
        if (fr != null) return fr;
        fr = tryLoadFile(file, ".ttc", size, antiAlias);
        if (fr != null) return fr;
        try {
            java.awt.Font sysFont = new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, (int) size);
            if (sysFont != null && sysFont.getFamily().contains("YaHei")) {
                return new FontRenderer(sysFont.deriveFont(size), antiAlias);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static FontRenderer tryLoadFile(String file, String ext, float size, boolean antiAlias) {
        String path = "/assets/advicenext/fonts/" + file + ext;
        try (InputStream in = Fonts.class.getResourceAsStream(path)) {
            if (in != null) {
                java.awt.Font awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in)
                    .deriveFont(java.awt.Font.PLAIN, size);
                return new FontRenderer(awtFont, antiAlias);
            }
        } catch (Exception ignored) { }
        return null;
    }

    public static boolean containsCJK(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
                return true;
            }
            if (Character.isSupplementaryCodePoint(cp)) i++;
        }
        return false;
    }

    public static FontRenderer forText(String text, float size) {
        return containsCJK(text) ? yaheiBold.get(size) : yahei.get(size);
    }
}