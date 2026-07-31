package cn.advicenext.gui.clickgui.newgui;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.ClickGui;
import cn.advicenext.features.value.*;
import cn.advicenext.features.value.slider.*;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.KawaseBloom;
import cn.advicenext.utility.client.render.KawaseBlur;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.List;

public class NewClickGuiScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ClickGui clickGuiModule;

    private enum Page { MODULES, CONFIGS, THEME }
    private Page currentPage = Page.MODULES;

    private enum ThemeMode { DARK, LIGHT }
    private ThemeMode theme = ThemeMode.DARK;

    private record ThemeColors(int bg, int panel, int card, int cardHover, int accent, int text, int subText, int divider) {}

    private ThemeColors colors() {
        if (theme == ThemeMode.LIGHT) {
            return new ThemeColors(
                0x18F0F0F0, 0x20FFFFFF, 0x18E8E8E8, 0x22DDDDDD,
                0xCC1A1A1A, 0xCC1A1A1A, 0x88666666, 0x10D0D0D0);
        }
        return new ThemeColors(
            0x180D0D0D, 0x201A1A1A, 0x18252525, 0x22303030,
            0xCCFFFFFF, 0xCCFFFFFF, 0x88AAAAAA, 0x102A2A2A);
    }

    private static final int PANEL_W = 530;
    private static final int PANEL_H = 370;
    private static final int TOPBAR_H = 32;
    private static final int SIDEBAR_W = 100;
    private static final int MODULE_W = 155;
    private static final float RADIUS = 10f;
    private static final float CARD_RADIUS = 5f;

    private final AnimatedValue scaleAnim = new AnimatedValue(0.85f, 0.15f, 0.78f);
    private final Map<Category, AnimatedValue> categoryYAnims = new LinkedHashMap<>();
    private final AnimatedValue catIndicatorY = new AnimatedValue(0, 0.2f, 0.7f);

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule = null;
    private Module bindingModule = null;
    private boolean bindingKey = false;
    private int scrollOffset = 0;
    private int configScrollOffset = 0;
    private int settingsScrollOffset = 0;

    private AbstractSetting<?> expandedMode = null;
    private AbstractSetting<?> expandedColor = null;

    private StringSetting editingString = null;
    private TextFieldWidget editField;

    private boolean dragging = false;
    private int dragX, dragY;
    private int panelX, panelY;
    private NumberSetting<?> draggingSlider = null;
    private RangeSetting draggingRange = null;
    private boolean draggingRangeMin = false;

    private final Set<String> displayedSettings = new HashSet<>();

    private boolean editingConfigName = false;

    private Module hoveredModule = null;
    private AbstractSetting<?> hoveredSetting = null;
    private long hoverStartTime = 0;
    private final AnimatedValue tooltipAlpha = new AnimatedValue(0, 0.12f, 0.75f);
    private static final int HOVER_DELAY = 350;

    private FontRenderer font12, font13, font14, font16, fontBold14;

    public NewClickGuiScreen(ClickGui clickGuiModule) {
        super(Text.literal("NewClickGui"));
        this.clickGuiModule = clickGuiModule;
        this.panelX = (mc.getWindow().getScaledWidth() - PANEL_W) / 2;
        this.panelY = (mc.getWindow().getScaledHeight() - PANEL_H) / 2;

        scaleAnim.setTarget(1f);

        int y = TOPBAR_H + 8;
        for (Category cat : Category.values()) {
            categoryYAnims.put(cat, new AnimatedValue(y));
            y += 22;
        }
    }

    @Override protected void init() {
        editField = new TextFieldWidget(textRenderer, 0, 0, 200, 16, Text.literal(""));
        editField.setVisible(false);
        addDrawableChild(editField);
    }

    private void initFonts() {
        if (font12 == null) {
            font12 = Fonts.roboto.get(12f);
            font13 = Fonts.roboto.get(13f);
            font14 = Fonts.robotoBold.get(14f);
            font16 = Fonts.robotoBold.get(16f);
            fontBold14 = Fonts.robotoBold.get(14f);
        }
    }

    private float textCenterY(float cardY, float cardH, FontRenderer font) {
        return cardY + (cardH - font.getHeight()) / 2.0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        initFonts();
        scaleAnim.update();
        catIndicatorY.update();
        categoryYAnims.values().forEach(AnimatedValue::update);
        tooltipAlpha.update();

        if (editField != null && editingString == null && !editingConfigName) {
            editField.setVisible(false);
        }

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        float scale = scaleAnim.get();
        int px = panelX, py = panelY;

        if (scale < 0.99f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(sw / 2f, sh / 2f);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-sw / 2f, -sh / 2f);
            renderPanel(context, mouseX, mouseY, px, py);
            context.getMatrices().popMatrix();
        } else {
            renderPanel(context, mouseX, mouseY, px, py);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void renderPanel(DrawContext context, int mx, int my, int px, int py) {
        ThemeColors c = colors();
        int accent = Colors.currentColor().getRGB();

        KawaseBlur.renderBlurRegion(32f, px, py, PANEL_W, PANEL_H);
        SkijaUIRenderer.drawRoundedRect("ng_panel", px, py, PANEL_W, PANEL_H, RADIUS, c.panel());

        String[] tabs = {"Modules", "Configs", "Theme"};
        int tabX = px + 12;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = currentPage.ordinal() == i;
            int color = active ? accent : c.subText();
            float tY = textCenterY(py, TOPBAR_H, font14);
            font14.drawString(tabs[i], tabX, tY, color);
            if (active) {
                context.fill(tabX, py + TOPBAR_H - 2, tabX + (int) font14.getStringWidth(tabs[i]), py + TOPBAR_H, accent);
            }
            tabX += (int) font14.getStringWidth(tabs[i]) + 20;
        }
        context.fill(px + 8, py + TOPBAR_H, px + PANEL_W - 8, py + TOPBAR_H + 1, c.divider());

        switch (currentPage) {
            case MODULES -> renderModulesPage(context, mx, my, px, py, c, accent);
            case CONFIGS -> renderConfigsPage(context, mx, my, px, py, c, accent);
            case THEME -> renderThemePage(context, mx, my, px, py, c, accent);
        }

        if (bindingModule != null) {
            String hint = "Press a key... (ESC to unbind)";
            int hw = (int) font14.getStringWidth(hint);
            int hx = px + PANEL_W / 2 - hw / 2;
            int hy = py + PANEL_H / 2 - 6;
            float bh = 22;
            SkijaUIRenderer.drawRoundedRect(hx - 12, hy - 3, hw + 24, bh, CARD_RADIUS, 0xCC000000);
            font14.drawString(hint, hx, textCenterY(hy - 3, bh, font14), 0xFFFFAA00);
        }

        if (mx < px || mx > px + PANEL_W || my < py + TOPBAR_H || my > py + PANEL_H) {
            updateHover(null, null);
        }

        renderTooltip(context, mx, my, c);
    }

    private void renderModulesPage(DrawContext context, int mx, int my, int px, int py, ThemeColors c, int accent) {
        int contentY = py + TOPBAR_H + 1;
        int contentBottom = py + PANEL_H - (int) RADIUS;

        int catY = contentY + 8;
        for (Category cat : Category.values()) {
            boolean sel = cat == selectedCategory;
            float cty = textCenterY(catY, 22, font14);
            font14.drawString(cat.getName(), px + 10, cty, sel ? accent : c.text());
            if (sel) catIndicatorY.setTarget(catY);
            categoryYAnims.get(cat).setTarget(catY);
            catY += 22;
        }
        context.fill(px + 2, (int) catIndicatorY.get(), px + 4, (int) catIndicatorY.get() + 14, accent);
        context.fill(px + SIDEBAR_W, contentY, px + SIDEBAR_W + 1, contentBottom, c.divider());

        int modX = px + SIDEBAR_W + 1;
        int modAreaW = MODULE_W;

        int modY = contentY + 8 - scrollOffset;
        int visibleBottom = py + PANEL_H - 4;

        Module newHovered = null;
        for (Module mod : ModuleManager.getModules()) {
            if (mod.getCategory() != selectedCategory) continue;
            int modH = 22;
            if (modY + modH < contentY || modY > visibleBottom) { modY += modH; continue; }

            boolean hovered = mx >= modX && mx <= modX + modAreaW && my >= modY && my <= modY + modH;
            if (hovered) newHovered = mod;
            boolean sel = mod == selectedModule;
            int bg = sel ? c.cardHover() : (hovered ? c.cardHover() : 0);
            if (bg != 0 || sel) {
                SkijaUIRenderer.drawRoundedRect(modX + 2, modY, modAreaW - 4, modH, CARD_RADIUS, bg != 0 ? bg : c.card());
            }

            int nameColor = mod.getEnabled() ? accent : c.text();
            float mty = textCenterY(modY, modH, font13);
            font13.drawString(mod.getName(), modX + 8, mty, nameColor);

            int key = mod.getKey();
            if (key > 0) {
                String kn = key < 256 ? GLFW.glfwGetKeyName(key, 0) : "M" + (key - 256);
                if (kn != null) {
                    int kw = (int) font12.getStringWidth(kn);
                    float kty = textCenterY(modY, modH, font12);
                    font12.drawString(kn, modX + modAreaW - kw - 6, kty, c.subText());
                }
            }
            modY += modH;
            context.fill(modX + 8, modY - 1, modX + modAreaW - 8, modY, c.divider());
        }

        updateHover(newHovered, null);

        context.fill(modX + modAreaW, contentY, modX + modAreaW + 1, contentBottom, c.divider());

        if (selectedModule != null) {
            int setX = modX + modAreaW + 1;
            int setW = PANEL_W - SIDEBAR_W - modAreaW - 2;
            renderSettings(context, mx, my, setX, contentY, setW, contentBottom, c, accent);
        }
    }

    private void renderSettings(DrawContext context, int mx, int my, int sx, int sy, int sw, int bottom, ThemeColors c, int accent) {
        if (selectedModule == null) return;
        int y = sy + 8 - settingsScrollOffset;

        fontBold14.drawString(selectedModule.getName(), sx + 8, y, accent);
        y += 18;

        String keyText;
        if (bindingKey && bindingModule == selectedModule) {
            keyText = "Key: ...";
        } else {
            int k = selectedModule.getKey();
            keyText = "Key: " + (k > 0 ? (k < 256 ? GLFW.glfwGetKeyName(k, 0) : "M" + (k - 256)) : "None");
        }
        int keyH = 18;
        boolean keyHover = mx >= sx && mx <= sx + sw && my >= y && my <= y + keyH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, keyH, CARD_RADIUS, keyHover ? c.cardHover() : c.card());
        font13.drawString(keyText, sx + 10, textCenterY(y, keyH, font13), c.text());
        y += keyH + 4;

        AbstractSetting<?> newHoveredSetting = null;
        boolean inSettingsArea = mx >= sx && mx <= sx + sw;
        displayedSettings.clear();
        for (AbstractSetting<?> setting : selectedModule.settings) {
            if (displayedSettings.contains(setting.getName())) continue;
            displayedSettings.add(setting.getName());
            if (!setting.getVisible().get()) continue;
            int cardH = (setting instanceof NumberSetting || setting instanceof RangeSetting) ? 30 : 18;
            if (y + cardH < sy || y > bottom) { y += (setting instanceof ColorSetting && expandedColor == setting) ? cardH + 2 + 4 * 20 + 20 : cardH + 2; continue; }
            if (inSettingsArea && my >= y && my <= y + cardH) {
                newHoveredSetting = setting;
            }
            y = renderSetting(context, mx, my, sx, y, sw, setting, c, accent);
        }

        if (inSettingsArea) {
            updateHover(null, newHoveredSetting);
        }
    }

    private int renderSetting(DrawContext context, int mx, int my, int sx, int y, int sw, AbstractSetting<?> s, ThemeColors c, int accent) {
        if (s instanceof BooleanSetting bs) return renderBoolean(context, mx, my, sx, y, sw, bs, c, accent);
        if (s instanceof ModeSetting ms) return renderMode(context, mx, my, sx, y, sw, ms, c, accent);
        if (s instanceof ColorSetting cs) return renderColor(context, mx, my, sx, y, sw, cs, c, accent);
        if (s instanceof RangeSetting rs) return renderRange(context, mx, my, sx, y, sw, rs, c, accent);
        if (s instanceof NumberSetting<?> ns) return renderNumber(context, mx, my, sx, y, sw, ns, c, accent);
        if (s instanceof StringSetting ss) return renderString(context, mx, my, sx, y, sw, ss, c, accent);
        return y + 22;
    }

    private int renderBoolean(DrawContext context, int mx, int my, int sx, int y, int sw, BooleanSetting bs, ThemeColors c, int accent) {
        int cardH = 18;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        font13.drawString(bs.getName(), sx + 10, textCenterY(y, cardH, font13), c.text());

        int tw = 28, th = 14, tx = sx + sw - tw - 10, ty = y + (cardH - th) / 2;
        boolean on = bs.getValue();
        SkijaUIRenderer.drawRoundedRect(tx, ty, tw, th, th / 2f, on ? accent : 0x44555555);
        float kd = th - 4;
        float kcx = on ? tx + tw - kd / 2f - 2 : tx + kd / 2f + 2;
        float kcy = ty + th / 2f;
        SkijaUIRenderer.drawCircle(kcx, kcy, kd, 0xFFFFFFFF);
        return y + cardH + 2;
    }

    private int renderMode(DrawContext context, int mx, int my, int sx, int y, int sw, ModeSetting ms, ThemeColors c, int accent) {
        int cardH = 18;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        font13.drawString(ms.getName() + ": " + ms.getValue(), sx + 10, textCenterY(y, cardH, font13), c.text());

        int extraH = 0;
        if (expandedMode == ms) {
            List<String> modes = ms.getModes();
            for (int i = 0; i < modes.size(); i++) {
                int oy = y + cardH + i * cardH;
                boolean oHov = mx >= sx + 4 && mx <= sx + sw - 4 && my >= oy && my <= oy + cardH;
                boolean act = modes.get(i).equals(ms.getValue());
                SkijaUIRenderer.drawRoundedRect(sx + 4, oy, sw - 8, cardH, CARD_RADIUS, oHov ? c.cardHover() : c.card());
                font13.drawString("  " + modes.get(i), sx + 10, textCenterY(oy, cardH, font13), act ? accent : c.subText());
            }
            extraH = modes.size() * cardH;
        }
        return y + cardH + 2 + extraH;
    }

    private int renderColor(DrawContext context, int mx, int my, int sx, int y, int sw, ColorSetting cs, ThemeColors c, int accent) {
        int cardH = 18;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        font13.drawString(cs.getName(), sx + 10, textCenterY(y, cardH, font13), c.text());

        int cbSize = 12;
        int cbx = sx + sw - cbSize - 12, cby = y + (cardH - cbSize) / 2;
        SkijaUIRenderer.drawRoundedRect(cbx, cby, cbSize, cbSize, CARD_RADIUS, cs.getValue());

        int extraH = 0;
        if (expandedColor == cs) {
            int slY = y + cardH + 2;
            slY = renderColorChannel(context, mx, my, sx, sw, slY, "R", cs.getRed(), 0xFFFF4444, c);
            slY = renderColorChannel(context, mx, my, sx, sw, slY, "G", cs.getGreen(), 0xFF44FF44, c);
            slY = renderColorChannel(context, mx, my, sx, sw, slY, "B", cs.getBlue(), 0xFF4444FF, c);
            slY = renderColorChannel(context, mx, my, sx, sw, slY, "A", cs.getAlpha(), 0xFFFFFFFF, c);
            SkijaUIRenderer.drawRoundedRect(sx + 10, slY, sw - 20, 16, CARD_RADIUS, cs.getValue());
            if (cs.getAlpha() < 128) {
                for (int ckx = sx + 10; ckx < sx + sw - 10; ckx += 4) {
                    for (int cky = slY; cky < slY + 16; cky += 4) {
                        boolean white = ((ckx - sx - 10) / 4 + (cky - slY) / 4) % 2 == 0;
                        context.fill(ckx, cky, Math.min(ckx + 4, sx + sw - 10), Math.min(cky + 4, slY + 16), white ? 0xFFFFFFFF : 0xFFCCCCCC);
                    }
                }
                SkijaUIRenderer.drawRoundedRect(sx + 10, slY, sw - 20, 16, CARD_RADIUS, cs.getValue());
            }
            extraH = 4 * 20 + 20;
        }
        return y + cardH + 2 + extraH;
    }

    private int renderColorChannel(DrawContext context, int mx, int my, int sx, int sw, int y, String label, int val, int labelColor, ThemeColors c) {
        font12.drawString(label, sx + 10, y + 2, labelColor);
        int sliderW = sw - 50;
        int sX = sx + 24;
        context.fill(sX, y + 8, sX + sliderW, y + 12, 0xFF444444);
        int fillW = (int) (val / 255.0 * sliderW);
        context.fill(sX, y + 8, sX + fillW, y + 12, 0xFFFFFFFF);
        font12.drawString(String.valueOf(val), sx + sw - 24, y + 2, c.text());
        return y + 18;
    }

    private int renderRange(DrawContext context, int mx, int my, int sx, int y, int sw, RangeSetting rs, ThemeColors c, int accent) {
        int cardH = 30;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        font13.drawString(rs.getName(), sx + 10, y + 2, c.text());

        int sly = y + 16, slx = sx + 10, slw = sw - 20;
        context.fill(slx, sly, slx + slw, sly + 4, 0xFF444444);
        double minV = rs.getMinValue(), maxV = rs.getMaxValue();
        double bMin = rs.getBoundMin(), bMax = rs.getBoundMax();
        double range = bMax - bMin;
        int minHx = slx + (int) ((minV - bMin) / range * slw);
        int maxHx = slx + (int) ((maxV - bMin) / range * slw);
        context.fill(minHx, sly, maxHx, sly + 4, accent);
        SkijaUIRenderer.drawRoundedRect(minHx - 3, sly - 2, 6, 8, 3, 0xFFFFFFFF);
        SkijaUIRenderer.drawRoundedRect(maxHx - 3, sly - 2, 6, 8, 3, 0xFFFFFFFF);
        font12.drawString(String.format("%.1f - %.1f", minV, maxV), sx + 10, y + 22, c.subText());
        return y + cardH + 2;
    }

    private int renderNumber(DrawContext context, int mx, int my, int sx, int y, int sw, NumberSetting<?> ns, ThemeColors c, int accent) {
        int cardH = 30;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        font13.drawString(ns.getName(), sx + 10, y + 2, c.text());

        int sly = y + 16, slx = sx + 10, slw = sw - 20;
        context.fill(slx, sly, slx + slw, sly + 4, 0xFF444444);
        double val = ns.getValue().doubleValue(), min = ns.getMin().doubleValue(), max = ns.getMax().doubleValue();
        int fillW = (int) ((val - min) / (max - min) * slw);
        context.fill(slx, sly, slx + fillW, sly + 4, accent);

        String vs = ns instanceof IntSetting ? String.valueOf((int) val) : String.format("%.1f", val);
        font12.drawString(vs, sx + sw - (int) font12.getStringWidth(vs) - 12, y + 2, c.subText());
        return y + cardH + 2;
    }

    private int renderString(DrawContext context, int mx, int my, int sx, int y, int sw, StringSetting ss, ThemeColors c, int accent) {
        int cardH = 18;
        boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + cardH;
        SkijaUIRenderer.drawRoundedRect(sx + 4, y, sw - 8, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
        if (editingString == ss) {
            String label = ss.getName() + ": ";
            float lw = font13.getStringWidth(label);
            font13.drawString(label, sx + 10, textCenterY(y, cardH, font13), c.text());
            editField.setPosition(sx + 10 + (int)lw, y);
            editField.setDimensions(sw - 18 - (int)lw, cardH);
            editField.setVisible(true);
            editField.renderWidget(context, mx, my, 0);
        } else {
            String display = ss.getName() + ": " + (ss.getValue().isEmpty() ? "(empty)" : ss.getValue());
            font13.drawString(display, sx + 10, textCenterY(y, cardH, font13), c.text());
        }
        return y + cardH + 2;
    }

    private void renderConfigsPage(DrawContext context, int mx, int my, int px, int py, ThemeColors c, int accent) {
        int contentY = py + TOPBAR_H + 1;
        int cy = contentY + 8;
        font14.drawString("Config Manager", px + 12, cy, c.text());
        cy += 22;

        List<String> configs = ConfigManager.getInstance().getModuleConfig().getAvailableConfigs();
        int listY = cy - configScrollOffset;
        int visibleBottom = py + PANEL_H - 4;
        int listW = PANEL_W - 24;

        for (String cfg : configs) {
            int cardH = 20;
            if (listY + cardH + 2 < contentY || listY > visibleBottom) { listY += cardH + 2; continue; }
            boolean hovered = mx >= px + 12 && mx <= px + 12 + listW && my >= listY && my <= listY + cardH;
            SkijaUIRenderer.drawRoundedRect(px + 12, listY, listW, cardH, CARD_RADIUS, hovered ? c.cardHover() : c.card());
            font13.drawString(cfg, px + 18, textCenterY(listY, cardH, font13), c.text());

            int btnW = 40, btnH = 16;
            int bx = px + PANEL_W - btnW - 20;
            SkijaUIRenderer.drawRoundedRect(bx, listY + (cardH - btnH) / 2, btnW, btnH, CARD_RADIUS, hovered ? accent : 0xFF555555);
            font13.drawString("Load", bx + 8, textCenterY(listY, cardH, font13), 0xFFFFFFFF);
            listY += cardH + 2;
            context.fill(px + 16, listY - 1, px + PANEL_W - 16, listY, c.divider());
        }

        int newY = py + PANEL_H - 28;
        int nch = 14;
        if (editingConfigName) {
            font13.drawString("Name: ", px + 12, textCenterY(newY, nch, font13), c.text());
            float lw = font13.getStringWidth("Name: ");
            editField.setPosition(px + 12 + (int)lw, newY);
            editField.setDimensions(150, nch);
            editField.setVisible(true);
            editField.renderWidget(context, mx, my, 0);
        } else {
            font13.drawString("New Config", px + 12, textCenterY(newY, nch, font13), c.text());
        }
        int saveW = 40;
        SkijaUIRenderer.drawRoundedRect(px + PANEL_W - saveW - 20, newY, saveW, nch, CARD_RADIUS, accent);
        font13.drawString("Save", px + PANEL_W - saveW - 14, textCenterY(newY, nch, font13), 0xFFFFFFFF);
    }

    private void renderThemePage(DrawContext context, int mx, int my, int px, int py, ThemeColors c, int accent) {
        int contentY = py + TOPBAR_H + 1;
        int ty = contentY + 8;
        font14.drawString("Theme", px + 12, ty, c.text());
        ty += 22;

        for (ThemeMode mode : ThemeMode.values()) {
            int cardH = 20;
            boolean active = theme == mode;
            boolean hovered = mx >= px + 12 && mx <= px + 200 && my >= ty && my <= ty + cardH;
            SkijaUIRenderer.drawRoundedRect(px + 12, ty, 188, cardH, CARD_RADIUS, active ? accent : (hovered ? c.cardHover() : c.card()));
            String name = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();
            font13.drawString(name, px + 20, textCenterY(ty, cardH, font13), active ? 0xFF000000 : c.text());
            ty += cardH + 4;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();

        if (my >= panelY && my <= panelY + TOPBAR_H) {
            String[] tabs = {"Modules", "Configs", "Theme"};
            int tabX = panelX + 12;
            for (int i = 0; i < tabs.length; i++) {
                int tw = (int) font14.getStringWidth(tabs[i]);
                if (mx >= tabX && mx <= tabX + tw) { currentPage = Page.values()[i]; return true; }
                tabX += tw + 20;
            }
        }

        if (my >= panelY && my <= panelY + TOPBAR_H && mx >= panelX && mx <= panelX + PANEL_W) {
            dragging = true; dragX = mx - panelX; dragY = my - panelY; return true;
        }

        if (my < panelY + TOPBAR_H || my > panelY + PANEL_H) return false;

        switch (currentPage) {
            case MODULES: return handleModulesClick(mx, my);
            case CONFIGS: return handleConfigsClick(mx, my);
            case THEME: return handleThemeClick(mx, my);
        }
        return false;
    }

    private boolean handleModulesClick(int mx, int my) {
        int contentY = panelY + TOPBAR_H;
        int catY = contentY + 8;
        for (Category cat : Category.values()) {
            if (mx >= panelX && mx <= panelX + SIDEBAR_W && my >= catY && my <= catY + 20) {
                selectedCategory = cat; selectedModule = null; scrollOffset = 0; return true;
            }
            catY += 22;
        }

        int modX = panelX + SIDEBAR_W;
        int modY = contentY + 8 - scrollOffset;
        for (Module mod : ModuleManager.getModules()) {
            if (mod.getCategory() != selectedCategory) continue;
            int modH = 22;
            if (mx >= modX && mx <= modX + MODULE_W && my >= modY && my <= modY + modH) {
                if (selectedModule == mod) { mod.toggle(); } else { selectedModule = mod; settingsScrollOffset = 0; }
                return true;
            }
            modY += modH;
        }

        if (selectedModule != null) return handleSettingsClick(mx, my);
        return false;
    }

    private boolean handleSettingsClick(int mx, int my) {
        int setX = panelX + SIDEBAR_W + MODULE_W;
        int setW = PANEL_W - SIDEBAR_W - MODULE_W;
        int contentY = panelY + TOPBAR_H;
        int y = contentY + 26 - settingsScrollOffset;

        int keyH = 18;
        if (mx >= setX && mx <= setX + setW && my >= y && my <= y + keyH) {
            bindingKey = true; bindingModule = selectedModule; return true;
        }
        y += keyH + 4;

        displayedSettings.clear();
        for (AbstractSetting<?> setting : selectedModule.settings) {
            if (displayedSettings.contains(setting.getName())) continue;
            displayedSettings.add(setting.getName());
            if (!setting.getVisible().get()) continue;

            int cardH = (setting instanceof NumberSetting || setting instanceof RangeSetting) ? 30 : 18;
            if (y + cardH > panelY + PANEL_H) break;

            if (setting instanceof ColorSetting cs && expandedColor == cs) {
                int csY = y + cardH + 2;
                for (int ch = 0; ch < 4; ch++) {
                    if (mx >= setX && mx <= setX + setW && my >= csY && my <= csY + 18) {
                        int sliderX = setX + 24, sliderW = setW - 50;
                        double ratio = (double)(mx - sliderX) / sliderW;
                        ratio = Math.max(0, Math.min(1, ratio));
                        int v = (int)(ratio * 255);
                        switch (ch) { case 0: cs.setRed(v); break; case 1: cs.setGreen(v); break; case 2: cs.setBlue(v); break; case 3: cs.setAlpha(v); break; }
                        return true;
                    }
                    csY += 18;
                }
                y += 4 * 20 + 20;
            }

            if (mx >= setX && mx <= setX + setW && my >= y && my <= y + cardH) {
                return handleSettingClick(setting, mx, my, setX, y, setW);
            }

            if (setting instanceof ModeSetting ms && expandedMode == ms) {
                int extra = ms.getModes().size() * 18;
                for (int i = 0; i < ms.getModes().size(); i++) {
                    int oy = y + cardH + i * 18;
                    if (mx >= setX + 4 && mx <= setX + setW - 4 && my >= oy && my <= oy + 18) {
                        ms.setValue(ms.getModes().get(i)); expandedMode = null; return true;
                    }
                }
                y += extra;
            }

            y += cardH + 2;
            if (setting instanceof ColorSetting && expandedColor == setting) y += 4 * 20 + 20 - (cardH + 2);
        }
        return false;
    }

    private boolean handleSettingClick(AbstractSetting<?> setting, int mx, int my, int sx, int y, int sw) {
        if (setting instanceof BooleanSetting bs) { bs.setValue(!bs.getValue()); return true; }
        if (setting instanceof ModeSetting ms) { expandedMode = (expandedMode == ms) ? null : ms; return true; }
        if (setting instanceof ColorSetting cs) { expandedColor = (expandedColor == cs) ? null : cs; return true; }
        if (setting instanceof StringSetting ss) {
            if (editingString == ss) { editingString = null; editField.setVisible(false); }
            else { editingString = ss; editField.setText(ss.getValue()); editField.setVisible(true); setFocused(editField); }
            return true;
        }
        if (setting instanceof RangeSetting rs) {
            int slx = sx + 10, slw = sw - 20;
            double minV = rs.getMinValue(), maxV = rs.getMaxValue();
            double bMin = rs.getBoundMin(), bMax = rs.getBoundMax();
            int minHx = slx + (int) ((minV - bMin) / (bMax - bMin) * slw);
            int maxHx = slx + (int) ((maxV - bMin) / (bMax - bMin) * slw);
            if (Math.abs(mx - minHx) < Math.abs(mx - maxHx)) { draggingRange = rs; draggingRangeMin = true; }
            else { draggingRange = rs; draggingRangeMin = false; }
            return true;
        }
        if (setting instanceof NumberSetting<?> ns) { draggingSlider = ns; return true; }
        return false;
    }

    private boolean handleConfigsClick(int mx, int my) {
        int contentY = panelY + TOPBAR_H;
        int cy = contentY + 30;
        List<String> configs = ConfigManager.getInstance().getModuleConfig().getAvailableConfigs();
        int listY = cy - configScrollOffset;

        for (String cfg : configs) {
            int cardH = 20;
            if (mx >= panelX + 12 && mx <= panelX + PANEL_W - 20 && my >= listY && my <= listY + cardH) {
                int btnW = 40, bx = panelX + PANEL_W - btnW - 20;
                if (mx >= bx && mx <= bx + btnW) {
                    ConfigManager.getInstance().getModuleConfig().loadConfig(cfg); return true;
                }
            }
            listY += cardH + 2;
        }

        int newY = panelY + PANEL_H - 28;
        int nch = 14;
        if (my >= newY && my <= newY + nch) {
            if (mx >= panelX + 12 && mx <= panelX + 200) { editingConfigName = true; editField.setText(""); editField.setVisible(true); setFocused(editField); return true; }
            int saveW = 40;
            if (mx >= panelX + PANEL_W - saveW - 20 && mx <= panelX + PANEL_W - 20) {
                String name = editingConfigName ? editField.getText() : "";
                if (!name.isEmpty()) {
                    ConfigManager.getInstance().getModuleConfig().saveConfig(name);
                    editField.setText(""); editingConfigName = false; editField.setVisible(false);
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleThemeClick(int mx, int my) {
        int contentY = panelY + TOPBAR_H;
        int ty = contentY + 30;
        int cardH = 20;
        for (ThemeMode mode : ThemeMode.values()) {
            if (mx >= panelX + 12 && mx <= panelX + 200 && my >= ty && my <= ty + cardH) { theme = mode; return true; }
            ty += cardH + 4;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false; draggingSlider = null; draggingRange = null; return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        int mx = (int) click.x(), my = (int) click.y();
        if (dragging) { panelX = mx - dragX; panelY = my - dragY; return true; }

        if (draggingSlider != null) {
            int setX = panelX + SIDEBAR_W + MODULE_W;
            int slw = PANEL_W - SIDEBAR_W - MODULE_W - 20;
            double ratio = (double)(mx - setX - 10) / slw;
            ratio = Math.max(0, Math.min(1, ratio));
            double r = draggingSlider.getMax().doubleValue() - draggingSlider.getMin().doubleValue();
            draggingSlider.setValueFromDouble(draggingSlider.getMin().doubleValue() + ratio * r);
            return true;
        }

        if (draggingRange != null) {
            int setX = panelX + SIDEBAR_W + MODULE_W;
            int slw = PANEL_W - SIDEBAR_W - MODULE_W - 20;
            double ratio = (double)(mx - setX - 10) / slw;
            ratio = Math.max(0, Math.min(1, ratio));
            double r = draggingRange.getBoundMax() - draggingRange.getBoundMin();
            double v = draggingRange.getBoundMin() + ratio * r;
            if (draggingRangeMin) draggingRange.setMinValue(v); else draggingRange.setMaxValue(v);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (bindingKey && bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) bindingModule.bindKey(-1);
            else bindingModule.bindKey(keyCode);
            bindingModule = null; bindingKey = false; return true;
        }

        if (editingString != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingString = null; editField.setVisible(false); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                editingString.setValue(editField.getText());
                editingString = null; editField.setVisible(false); return true;
            }
            return super.keyPressed(input);
        }

        if (editingConfigName) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingConfigName = false; editField.setText(""); editField.setVisible(false); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                String name = editField.getText();
                if (!name.isEmpty()) {
                    ConfigManager.getInstance().getModuleConfig().saveConfig(name);
                    editField.setText(""); editingConfigName = false; editField.setVisible(false);
                }
                return true;
            }
            return super.keyPressed(input);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int amt = (int) verticalAmount * 15;
        int mx = (int) mouseX, my = (int) mouseY;

        if (currentPage == Page.CONFIGS) {
            configScrollOffset = Math.max(0, configScrollOffset - amt);
            return true;
        }

        if (currentPage == Page.MODULES && selectedModule != null) {
            int setX = panelX + SIDEBAR_W + MODULE_W;
            int setW = PANEL_W - SIDEBAR_W - MODULE_W;
            if (mx >= setX && mx <= setX + setW && my >= panelY + TOPBAR_H && my <= panelY + PANEL_H) {
                int maxSettingsScroll = Math.max(0, getSettingsMaxScroll());
                settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset - amt, maxSettingsScroll));
                return true;
            }
        }

        scrollOffset = Math.max(0, scrollOffset - amt);
        return true;
    }

    private int getSettingsMaxScroll() {
        if (selectedModule == null) return 0;
        int totalH = 0;
        for (AbstractSetting<?> setting : selectedModule.settings) {
            if (!setting.getVisible().get()) continue;
            totalH += (setting instanceof NumberSetting || setting instanceof RangeSetting) ? 30 : 18;
            totalH += 2;
            if (setting instanceof ColorSetting && expandedColor == setting) totalH += 4 * 20 + 20;
        }
        int availableH = PANEL_H - TOPBAR_H - 30;
        return Math.max(0, totalH - availableH);
    }

    @Override
    public void close() {
        clickGuiModule.toggle();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void updateHover(Module mod, AbstractSetting<?> setting) {
        if (hoveredModule != mod || hoveredSetting != setting) {
            hoveredModule = mod;
            hoveredSetting = setting;
            hoverStartTime = System.currentTimeMillis();
            tooltipAlpha.setTarget(0);
        }
        if (mod != null || setting != null) {
            if (System.currentTimeMillis() - hoverStartTime >= HOVER_DELAY) {
                tooltipAlpha.setTarget(1);
            }
        } else {
            tooltipAlpha.setTarget(0);
        }
    }

    private void renderTooltip(DrawContext context, int mx, int my, ThemeColors c) {
        float alpha = tooltipAlpha.get();
        if (alpha <= 0.02f) return;

        String desc = null;
        if (hoveredModule != null) desc = hoveredModule.getDescription();
        else if (hoveredSetting != null) desc = hoveredSetting.getDescription();
        if (desc == null || desc.isEmpty() || desc.equals(hoveredModule != null ? hoveredModule.getName() : hoveredSetting.getName())) return;

        int pad = 6;
        int tw = (int) font12.getStringWidth(desc) + pad * 2;
        int th = (int) font12.getHeight() + pad * 2;
        int tx = mx + 10;
        int ty = my - th - 6;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        if (tx + tw > sw) tx = mx - tw - 10;
        if (ty < 0) ty = my + 10;
        if (tx < 0) tx = 4;
        if (ty + th > sh) ty = sh - th - 4;

        int a = (int)(alpha * 0xE0);
        int bg = (a << 24) | 0x001A1A1A;
        int fg = (a << 24) | 0x00FFFFFF;

        SkijaUIRenderer.drawRoundedRect(tx, ty, tw, th, 4, bg);
        font12.drawString(desc, tx + pad, ty + pad, fg);
    }
}