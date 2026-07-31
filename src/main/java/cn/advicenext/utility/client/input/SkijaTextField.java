package cn.advicenext.utility.client.input;

import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

public class SkijaTextField {
    private float x, y, width, height;
    private String text = "";
    private String placeholder = "";
    private boolean focused;
    private int cursorPos;
    private int selectionStart = -1;
    private long tickCounter;
    private FontRenderer font;
    private FontRenderer placeholderFont;
    private int bgColor = 0x30FFFFFF;
    private int borderColor = 0x60FFFFFF;
    private int focusBorderColor = 0xFF4A90D9;
    private int textColor = 0xFFFFFFFF;
    private int placeholderColor = 0x80FFFFFF;
    private int cursorColor = 0xFFFFFFFF;
    private float radius = 6f;
    private float padding = 8f;
    private int maxLength = 256;
    private boolean shiftHeld;

    public SkijaTextField(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = Fonts.inter.get(13);
        this.placeholderFont = Fonts.inter.get(13);
    }

    public SkijaTextField setFont(FontRenderer font) { this.font = font; return this; }
    public SkijaTextField setPlaceholderFont(FontRenderer font) { this.placeholderFont = font; return this; }
    public SkijaTextField setPlaceholder(String text) { this.placeholder = text; return this; }
    public SkijaTextField setMaxLength(int max) { this.maxLength = max; return this; }
    public SkijaTextField setBgColor(int color) { this.bgColor = color; return this; }
    public SkijaTextField setBorderColor(int color) { this.borderColor = color; return this; }
    public SkijaTextField setFocusBorderColor(int color) { this.focusBorderColor = color; return this; }
    public SkijaTextField setTextColor(int color) { this.textColor = color; return this; }
    public SkijaTextField setPlaceholderColor(int color) { this.placeholderColor = color; return this; }
    public SkijaTextField setCursorColor(int color) { this.cursorColor = color; return this; }
    public SkijaTextField setRadius(float r) { this.radius = r; return this; }
    public SkijaTextField setPadding(float p) { this.padding = p; return this; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text != null ? text : ""; cursorPos = this.text.length(); selectionStart = -1; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; if (focused) cursorPos = text.length(); }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setSize(float w, float h) { this.width = w; this.height = h; }

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            focused = true;
            cursorPos = getCursorFromMouse(mx);
            selectionStart = -1;
            return true;
        }
        focused = false;
        selectionStart = -1;
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        if (!focused) return false;
        int key = input.key();
        shiftHeld = hasShiftModifier(input.modifiers());

        if (key == GLFW.GLFW_KEY_LEFT) {
            if (shiftHeld) { if (selectionStart < 0) selectionStart = cursorPos; }
            else selectionStart = -1;
            if (cursorPos > 0) cursorPos--;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            if (shiftHeld) { if (selectionStart < 0) selectionStart = cursorPos; }
            else selectionStart = -1;
            if (cursorPos < text.length()) cursorPos++;
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            if (shiftHeld) { if (selectionStart < 0) selectionStart = cursorPos; }
            else selectionStart = -1;
            cursorPos = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_END) {
            if (shiftHeld) { if (selectionStart < 0) selectionStart = cursorPos; }
            else selectionStart = -1;
            cursorPos = text.length();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectionStart >= 0 && selectionStart != cursorPos) deleteSelection();
            else if (cursorPos > 0) { text = text.substring(0, cursorPos - 1) + text.substring(cursorPos); cursorPos--; }
            selectionStart = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (selectionStart >= 0 && selectionStart != cursorPos) deleteSelection();
            else if (cursorPos < text.length()) text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
            selectionStart = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_A && hasCtrlModifier(input.modifiers())) {
            selectionStart = 0; cursorPos = text.length();
            return true;
        }
        if (key == GLFW.GLFW_KEY_C && hasCtrlModifier(input.modifiers())) {
            if (selectionStart >= 0) {
                int start = Math.min(selectionStart, cursorPos);
                int end = Math.max(selectionStart, cursorPos);
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(text.substring(start, end));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_V && hasCtrlModifier(input.modifiers())) {
            String clipboard = net.minecraft.client.MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                if (selectionStart >= 0 && selectionStart != cursorPos) deleteSelection();
                insertText(clipboard);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_X && hasCtrlModifier(input.modifiers())) {
            if (selectionStart >= 0) {
                int start = Math.min(selectionStart, cursorPos);
                int end = Math.max(selectionStart, cursorPos);
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(text.substring(start, end));
                deleteSelection();
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127 && text.length() < maxLength) {
            if (selectionStart >= 0 && selectionStart != cursorPos) deleteSelection();
            insertChar(chr);
            return true;
        }
        return false;
    }

    public void render() {
        tickCounter++;
        int border = focused ? focusBorderColor : borderColor;
        if (bgColor != 0) SkijaUIRenderer.drawRoundedRect(x, y, width, height, radius, bgColor);
        SkijaUIRenderer.drawRoundedRect(x, y, width, height, radius, border);

        float textY = y + (height - font.getHeight()) / 2;
        float textX = x + padding;
        float maxTextWidth = width - padding * 2;

        if (text.isEmpty() && !focused) {
            if (!placeholder.isEmpty()) {
                placeholderFont.drawString(placeholder, textX, textY, placeholderColor);
            }
            return;
        }

        int selStart = selectionStart >= 0 ? Math.min(selectionStart, cursorPos) : 0;
        int selEnd = selectionStart >= 0 ? Math.max(selectionStart, cursorPos) : cursorPos;

        float scrollOff = getScrollOffset(maxTextWidth);
        float drawX = textX - scrollOff;

        if (selStart > 0) {
            String pre = text.substring(0, selStart);
            font.drawString(pre, drawX, textY, textColor);
            drawX += font.getStringWidth(pre);
        }

        if (selectionStart >= 0 && selStart != selEnd) {
            String sel = text.substring(selStart, selEnd);
            float selW = font.getStringWidth(sel);
            SkijaUIRenderer.drawRect(drawX, y + 2, selW, height - 4, 0x604A90D9);
            font.drawString(sel, drawX, textY, 0xFFFFFFFF);
            drawX += selW;
        }

        if (selEnd < text.length()) {
            font.drawString(text.substring(selEnd), drawX, textY, textColor);
        }

        if (focused && (tickCounter / 30) % 2 == 0 && selectionStart < 0) {
            float cursorX = textX + font.getStringWidth(text.substring(0, cursorPos)) - scrollOff;
            cursorX = Math.min(cursorX, x + width - 4);
            SkijaUIRenderer.drawRect(cursorX, y + 4, 1, height - 8, cursorColor);
        }
    }

    private float getScrollOffset(float maxWidth) {
        float textWidth = font.getStringWidth(text.substring(0, cursorPos));
        if (textWidth <= maxWidth) return 0;
        return textWidth - maxWidth + 4;
    }

    private int getCursorFromMouse(double mx) {
        float relX = (float) mx - (x + padding);
        if (relX <= 0) return 0;
        for (int i = 1; i <= text.length(); i++) {
            if (font.getStringWidth(text.substring(0, i)) > relX) return i - 1;
        }
        return text.length();
    }

    private void insertChar(char c) {
        text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
        cursorPos++;
    }

    private void insertText(String s) {
        String filtered = s.replaceAll("[\\r\\n]", "");
        if (text.length() + filtered.length() > maxLength)
            filtered = filtered.substring(0, maxLength - text.length());
        text = text.substring(0, cursorPos) + filtered + text.substring(cursorPos);
        cursorPos += filtered.length();
    }

    private void deleteSelection() {
        if (selectionStart < 0) return;
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        text = text.substring(0, start) + text.substring(end);
        cursorPos = start;
        selectionStart = -1;
    }

    private static boolean hasShiftModifier(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    private static boolean hasCtrlModifier(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }
}