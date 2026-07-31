package cn.advicenext.gui.clickgui.neverlose;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.impl.render.ClickGui;
import cn.advicenext.gui.clickgui.animation.AnimationUtil;
import cn.advicenext.utility.client.render.KawaseBlur;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import cn.advicenext.utility.minecraft.client.ColorUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NeverloseClickGuiScreen extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Panel> panels = new ArrayList<>();
    private boolean dragging = false;
    private int posX = 40;
    private int posY = 40;
    private int dragX;
    private int dragY;
    private static final int WIDTH = 520;
    private static final int HEIGHT = 420;

    private final ClickGui clickGuiModule;

    private float hoverAnim = 0f;

    public static Color bgColor, bgColor2, bgColor3, bgColor4,
            topColor, categoryBgColor, lineColor, lineColor2,
            outlineColor, outlineColor2, sliderBarColor,
            sliderCircleColor, boolCircleColor, boolBgColor,
            boolCircleColor2, boolBgColor2, sliderBgColor;
    public static int textRGB, iconRGB, outlineTextRGB, moduleTextRGB;

    public NeverloseClickGuiScreen(ClickGui clickGuiModule) {
        super(Text.literal("Neverlose ClickGui"));
        this.clickGuiModule = clickGuiModule;

        for (Category category : Category.values()) {
            panels.add(new Panel(category));
        }

        initColors();
    }

    private void initColors() {
        sliderBarColor = new Color(0x046190).darker();
        sliderCircleColor = new Color(0x2482ff);
        sliderBgColor = new Color(0x000f25).darker();

        boolBgColor = new Color(0x000314);
        boolBgColor2 = new Color(0x00173a);

        boolCircleColor = new Color(0x00BBFF);
        boolCircleColor2 = new Color(0x7a899a);

        topColor = new Color(0x111821).darker();
        bgColor = new Color(0x000C18);
        bgColor2 = new Color(0xDA081222, true);
        bgColor3 = new Color(0x3400BEFF, true).darker().darker().darker().darker();
        bgColor4 = new Color(0x001020);
        lineColor = new Color(0x131c29);
        lineColor2 = new Color(0x031124).brighter();
        outlineColor = new Color(0x051321).brighter();
        outlineColor2 = new Color(0x00193A).darker();
        textRGB = -1;
        iconRGB = new Color(0x00BBFF).getRGB();
        outlineTextRGB = new Color(0x00BBFF).getRGB();
        moduleTextRGB = new Color(0x2c313b).getRGB();
    }

    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }

    public Panel getSelected() {
        return panels.stream().filter(Panel::isSelected).findAny().orElse(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        initColors();

        if (getSelected() == null) {
            if (!panels.isEmpty()) {
                panels.get(0).setSelected(true);
            }
        }

        if (dragging) {
            posX = mouseX + dragX;
            posY = mouseY + dragY;
        }

        KawaseBlur.renderBlurRegion(8f, posX, posY, WIDTH, HEIGHT);

        SkijaUIRenderer.drawRoundedRect(posX, posY, WIDTH, HEIGHT, 6f, bgColor2.getRGB());
        SkijaUIRenderer.drawRoundedRect(posX + 136, posY, WIDTH - 136, HEIGHT, 6f, bgColor.getRGB());

        SkijaUIRenderer.drawRoundedRect(posX + 136, posY, WIDTH - 136, 46, 6f, topColor.getRGB());
        SkijaUIRenderer.drawRoundedRect(posX + 136, posY, 4, HEIGHT, 0, bgColor.getRGB());
        SkijaUIRenderer.drawRoundedRect(posX + 136, posY, 4, 46, 0, topColor.getRGB());
        SkijaUIRenderer.drawRoundedRect(posX + 136, posY + 44, WIDTH - 136, 4, 0, topColor.getRGB());

        SkijaUIRenderer.drawRect(posX + 135, posY, 1, HEIGHT, lineColor.getRGB());
        SkijaUIRenderer.drawRect(posX + 136, posY + 48, WIDTH - 136, 1, lineColor.getRGB());

        FontRenderer fontBold36 = Fonts.interBold.get(36f);
        fontBold36.drawCenteredString("AdviceNext", posX + 65, posY + 12, textRGB);

        FontRenderer fontSemiBold14 = Fonts.interSemiBold.get(14f);
        FontRenderer fontSemiBold16 = Fonts.interSemiBold.get(16f);
        FontRenderer fontSemiBold18 = Fonts.interSemiBold.get(18f);

        SkijaUIRenderer.drawRect(posX, posY + 384, 134, 1, lineColor.getRGB());

        if (mc.player != null) {
            fontSemiBold16.drawString(mc.player.getName().getString(), posX + 37, posY + 396, textRGB);
        }

        int rageY = posY + 42;
        int visualsY = posY + 103;
        int commonY = posY + 212;

        fontSemiBold14.drawString("Rage", posX + 14, rageY, Color.GRAY.getRGB());
        fontSemiBold14.drawString("Visuals", posX + 14, visualsY, Color.GRAY.getRGB());
        fontSemiBold14.drawString("Common", posX + 14, commonY, Color.GRAY.getRGB());

        for (Panel panel : panels) {
            categoryBgColor = ColorUtils.applyOpacity(new Color(0, 52, 84), (float) panel.getAnimation().getOutput());
            panel.drawScreen(mouseX, mouseY);
            if (panel.isSelected()) {
                int catIdx = panel.getCategory().ordinal();
                int cy;
                if (catIdx >= 7) cy = posY + 92 + catIdx * 24;
                else if (catIdx >= 6) cy = posY + 78 + catIdx * 24;
                else if (catIdx >= 2) cy = posY + 65 + catIdx * 24;
                else cy = posY + 52 + catIdx * 24;

                SkijaUIRenderer.drawRoundedRect(posX + 8, cy, 120, 19, 5, categoryBgColor.getRGB());
                fontSemiBold18.drawString(panel.getCategory().getName(), posX + 34,
                    catIdx >= 7 ? posY + 99 + catIdx * 24 :
                    catIdx >= 6 ? posY + 85 + catIdx * 24 :
                    catIdx >= 2 ? posY + 72 + catIdx * 24 : posY + 59 + catIdx * 24, textRGB);
            }
            renderIcon(panel);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderIcon(Panel panel) {
        Color icon = new Color(iconRGB);
        int catIdx = panel.getCategory().ordinal();
        float y = catIdx == 7 ? posY + 98 + catIdx * 24 :
                  catIdx == 6 ? posY + 85 + catIdx * 24 :
                  catIdx >= 2 ? posY + 72 + catIdx * 24 :
                  posY + 59 + catIdx * 24;

        FontRenderer iconFont = Fonts.interSemiBold.get(24f);
        switch (panel.getCategory()) {
            case COMBAT:
                iconFont.drawString("a", posX + 12, y - 1, icon.getRGB());
                break;
            case PLAYER:
                iconFont.drawString("b", posX + 12, y - 1, icon.getRGB());
                break;
            case WORLD:
                iconFont.drawCenteredString("v", posX + 17, y - 1, icon.getRGB());
                break;
            case MOVEMENT:
                iconFont.drawString("f", posX + 12, y - 1, icon.getRGB());
                break;
            case MISC:
                iconFont.drawString("l", posX + 12, y, icon.getRGB());
                break;
            case RENDER:
                iconFont.drawString("d", posX + 14, y + 1, icon.getRGB());
                break;
            default:
                break;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int button = click.button();

        if (button == 0) {
            for (Panel panel : panels) {
                if (handleCategoryPanel(panel, mx, my)) {
                    break;
                }
            }
            if (isHovered(mx, my, posX, posY, 136, 42)) {
                dragging = true;
                dragX = posX - mx;
                dragY = posY - my;
                return true;
            }
        }
        Panel selected = getSelected();
        if (selected != null) {
            if (!isHovered(mx, my, posX + 140, posY + 49, 380, 368)) return false;
            selected.mouseClicked(mx, my, button);
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean handleCategoryPanel(Panel panel, int mouseX, int mouseY) {
        int catIdx = panel.getCategory().ordinal();
        int cy;
        if (catIdx >= 7) cy = posY + 92 + catIdx * 24;
        else if (catIdx >= 6) cy = posY + 78 + catIdx * 24;
        else if (catIdx >= 2) cy = posY + 65 + catIdx * 24;
        else cy = posY + 52 + catIdx * 24;

        if (isHovered(mouseX, mouseY, posX + 8, cy, 120, 19)) {
            for (Panel p : panels) p.setSelected(false);
            panel.setSelected(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            dragging = false;
        }
        Panel selected = getSelected();
        if (selected != null) {
            selected.mouseReleased((int) click.x(), (int) click.y(), click.button());
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) {
            posX = (int) click.x() + dragX;
            posY = (int) click.y() + dragY;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Panel selected = getSelected();
        if (selected != null) {
            selected.addScroll((float) verticalAmount * 20);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        Panel selected = getSelected();
        if (selected != null) {
            selected.keyTyped((char) 0, keyCode);
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (clickGuiModule != null && clickGuiModule.getEnabled()) {
            clickGuiModule.toggle();
        }
        super.close();
    }

    private static boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}