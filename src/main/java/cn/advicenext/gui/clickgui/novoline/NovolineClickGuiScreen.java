package cn.advicenext.gui.clickgui.novoline;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.ClickGui;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ColorSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.FloatSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.features.value.slider.NumberSetting;
import cn.advicenext.features.value.slider.RangeSetting;
import cn.advicenext.gui.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NovolineClickGuiScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ClickGui clickGuiModule;

    // GUI dimensions
    private int startX = 50;
    private int startY = 50;
    private final int categoryWidth = 150;
    private final int categorySpacing = 10;
    private final int categoryHeaderHeight = 20;
    private final int moduleHeight = 16;
    private final int settingHeight = 16;

    // Colors - Novoline inspired
    private final Color backgroundColor = new Color(0, 0, 0, 255); // Changed from 180 to 255 to remove blur
    private final Color categoryHeaderColor = new Color(25, 25, 25, 255);
    private final Color moduleColor = new Color(40, 40, 40, 255); // Changed from 200 to 255 to remove blur
    private final Color moduleHoverColor = new Color(45, 45, 45, 255); // Changed from 200 to 255 to remove blur
    private final Color moduleActiveColor = new Color(50, 50, 50, 255); // Changed from 200 to 255 to remove blur
    private final Color accentColor = Colors.currentColor();
    private final Color textColor = Color.WHITE;
    private final Color subTextColor = new Color(170, 170, 170, 255);

    // State
    private Map<Category, Integer> categoryScrollOffsets = new HashMap<>();
    private Map<Module, Boolean> expandedModules = new HashMap<>();
    private Module bindingModule = null;

    // Dragging
    private boolean dragging = false;
    private int dragX, dragY;
    private Category draggingCategory = null;
    private Map<Category, Point> categoryPositions = new HashMap<>();

    // Range slider dragging
    private RangeSetting draggingRange = null;
    private boolean draggingRangeMin = false;

    public NovolineClickGuiScreen(ClickGui clickGuiModule) {
        super(Text.literal("ClickGui"));
        this.clickGuiModule = clickGuiModule;

        // Initialize scroll offsets and positions
        int x = startX;
        for (Category category : Category.values()) {
            categoryScrollOffsets.put(category, 0);
            categoryPositions.put(category, new Point(x, startY));
            x += categoryWidth + categorySpacing;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw categories
        for (Category category : Category.values()) {
            Point pos = categoryPositions.get(category);
            drawCategory(context, category, pos.x, pos.y, mouseX, mouseY);
        }

        // Draw binding text if binding a key
        if (bindingModule != null) {
            String text = "Press a key... (ESC to unbind)";
            int textWidth = textRenderer.getWidth(text);
            context.fill(width / 2 - textWidth / 2 - 5, height / 2 - 10, width / 2 + textWidth / 2 + 5, height / 2 + 10, 0xAA000000);
            context.drawCenteredTextWithShadow(textRenderer, text, width / 2, height / 2 - 4, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCategory(DrawContext context, Category category, int x, int y, int mouseX, int mouseY) {
        // Get scroll offset for this category
        int scrollOffset = categoryScrollOffsets.getOrDefault(category, 0);

        // Calculate total height needed
        int totalContentHeight = calculateTotalContentHeight(category);
        int visibleHeight = height - y - 30; // Leave some space at bottom

        // Draw category header
        context.fill(x, y, x + categoryWidth, y + categoryHeaderHeight, categoryHeaderColor.getRGB());
        context.drawCenteredTextWithShadow(textRenderer, category.getName(), x + categoryWidth / 2, y + (categoryHeaderHeight - 8) / 2, accentColor.getRGB());

        // Draw category background
        context.fill(x, y + categoryHeaderHeight, x + categoryWidth, y + categoryHeaderHeight + Math.min(totalContentHeight, visibleHeight), backgroundColor.getRGB());

        // Draw modules
        int moduleY = y + categoryHeaderHeight - scrollOffset;

        for (Module module : ModuleManager.getModules()) {
            if (module.getCategory() != category) continue;

            // Skip if not visible
            if (moduleY + moduleHeight < y + categoryHeaderHeight || moduleY > y + categoryHeaderHeight + visibleHeight) {
                moduleY += moduleHeight;
                // Add height for expanded settings
                if (expandedModules.getOrDefault(module, false)) {
                    moduleY += module.settings.size() * settingHeight;
                }
                continue;
            }

            boolean isHovered = mouseX >= x && mouseX <= x + categoryWidth && mouseY >= moduleY && mouseY <= moduleY + moduleHeight;
            boolean isExpanded = expandedModules.getOrDefault(module, false);

            // Draw module background
            context.fill(x, moduleY, x + categoryWidth, moduleY + moduleHeight,
                    module.getEnabled() ? moduleActiveColor.getRGB() : (isHovered ? moduleHoverColor.getRGB() : moduleColor.getRGB()));

            // Draw module name
            context.drawTextWithShadow(textRenderer, module.getName(), x + 5, moduleY + (moduleHeight - 8) / 2,
                    module.getEnabled() ? accentColor.getRGB() : textColor.getRGB());

            // Draw expand indicator if module has settings
            if (!module.settings.isEmpty()) {
                String indicator = isExpanded ? "-" : "+";
                context.drawTextWithShadow(textRenderer, indicator, x + categoryWidth - 10, moduleY + (moduleHeight - 8) / 2, subTextColor.getRGB());
            }

            // Draw settings if expanded
            if (isExpanded) {
                int settingY = moduleY + moduleHeight;

                // Track settings we've already displayed to avoid duplicates
                Set<String> displayedSettings = new HashSet<>();

                for (AbstractSetting<?> setting : module.settings) {
                    // Skip duplicate settings
                    if (displayedSettings.contains(setting.getName())) {
                        continue;
                    }
                    displayedSettings.add(setting.getName());

                    // Skip settings hidden by visibility condition
                    if (!setting.getVisible().get()) {
                        continue;
                    }

                    // Skip if outside the scrollable area
                    if (settingY + settingHeight < y + categoryHeaderHeight || settingY > y + categoryHeaderHeight + visibleHeight) {
                        settingY += settingHeight;
                        continue;
                    }

                    boolean settingHovered = mouseX >= x && mouseX <= x + categoryWidth && mouseY >= settingY && mouseY <= settingY + settingHeight;

                    // Draw setting background
                    context.fill(x, settingY, x + categoryWidth, settingY + settingHeight,
                            settingHovered ? moduleColor.brighter().getRGB() : moduleColor.getRGB());

                    // Draw setting based on type
                    if (setting instanceof BooleanSetting) {
                        drawBooleanSetting(context, (BooleanSetting) setting, x, settingY);
                    } else if (setting instanceof ModeSetting) {
                        drawModeSetting(context, (ModeSetting) setting, x, settingY);
                    } else if (setting instanceof RangeSetting) {
                        drawRangeSetting(context, (RangeSetting) setting, x, settingY);
                    } else if (setting instanceof NumberSetting<?>) {
                        drawNumberSetting(context, (NumberSetting<?>) setting, x, settingY, mouseX);
                    } else if (setting instanceof StringSetting) {
                        drawStringSetting(context, (StringSetting) setting, x, settingY);
                    } else if (setting instanceof ColorSetting) {
                        drawColorSetting(context, (ColorSetting) setting, x, settingY);
                    }

                    settingY += settingHeight;
                }
            }

            moduleY += moduleHeight;
            if (isExpanded) {
                // Count unique settings to calculate height correctly
                Set<String> uniqueSettings = new HashSet<>();
                for (AbstractSetting<?> setting : module.settings) {
                    if (!setting.getVisible().get()) continue;
                    uniqueSettings.add(setting.getName());
                }
                moduleY += uniqueSettings.size() * settingHeight;
            }
        }
    }

    private int calculateTotalContentHeight(Category category) {
        int height = 0;

        for (Module module : ModuleManager.getModules()) {
            if (module.getCategory() == category) {
                height += moduleHeight;

                if (expandedModules.getOrDefault(module, false)) {
                    // Count unique settings to calculate height correctly
                    Set<String> uniqueSettings = new HashSet<>();
                    for (AbstractSetting<?> setting : module.settings) {
                        if (!setting.getVisible().get()) continue;
                        uniqueSettings.add(setting.getName());
                    }
                    height += uniqueSettings.size() * settingHeight;
                }
            }
        }

        return height;
    }

    private void drawBooleanSetting(DrawContext context, BooleanSetting setting, int x, int y) {
        // Draw setting name
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());

        // Draw toggle indicator
        int toggleX = x + categoryWidth - 15;
        int toggleY = y + (settingHeight - 8) / 2;
        int toggleWidth = 10;
        int toggleHeight = 8;

        context.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight,
                setting.getValue() ? accentColor.getRGB() : new Color(80, 80, 80).getRGB());
    }

    private void drawModeSetting(DrawContext context, ModeSetting setting, int x, int y) {
        // Draw setting name
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());

        // Draw current value
        String value = setting.getValue();
        int valueWidth = textRenderer.getWidth(value);
        context.drawTextWithShadow(textRenderer, value, x + categoryWidth - valueWidth - 5, y + (settingHeight - 8) / 2, accentColor.getRGB());
    }

    private void drawNumberSetting(DrawContext context, NumberSetting<?> setting, int x, int y, int mouseX) {
        // Draw setting name
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());

        // Draw slider
        int sliderX = x + 5;
        int sliderY = y + settingHeight - 4;
        int sliderWidth = categoryWidth - 10;
        int sliderHeight = 2;

        // Draw slider background
        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, new Color(80, 80, 80).getRGB());

        // Calculate slider progress
        double min = ((Number)setting.getMin()).doubleValue();
        double max = ((Number)setting.getMax()).doubleValue();
        double value = ((Number)setting.getValue()).doubleValue();
        double percentage = (value - min) / (max - min);
        int progressWidth = (int)(sliderWidth * percentage);

        // Draw slider progress
        context.fill(sliderX, sliderY, sliderX + progressWidth, sliderY + sliderHeight, accentColor.getRGB());

        // Draw value
        String valueText = setting.getValue().toString();
        int valueWidth = textRenderer.getWidth(valueText);
        context.drawTextWithShadow(textRenderer, valueText, x + categoryWidth - valueWidth - 5, y + (settingHeight - 8) / 2, accentColor.getRGB());
    }

    private void drawStringSetting(DrawContext context, StringSetting setting, int x, int y) {
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());
        String value = setting.getValue();
        int valueWidth = textRenderer.getWidth(value);
        context.drawTextWithShadow(textRenderer, value, x + categoryWidth - valueWidth - 5, y + (settingHeight - 8) / 2, accentColor.getRGB());
    }

    private void drawColorSetting(DrawContext context, ColorSetting setting, int x, int y) {
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());
        int cbSize = 10;
        int cbx = x + categoryWidth - cbSize - 8;
        int cby = y + (settingHeight - cbSize) / 2;
        context.fill(cbx, cby, cbx + cbSize, cby + cbSize, setting.getValue());
    }

    private void drawRangeSetting(DrawContext context, RangeSetting setting, int x, int y) {
        // Draw setting name
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 5, y + (settingHeight - 8) / 2, textColor.getRGB());

        // Draw current range value
        String valueText = formatRangeValue(setting.getMinValue()) + "-" + formatRangeValue(setting.getMaxValue());
        int valueWidth = textRenderer.getWidth(valueText);
        context.drawTextWithShadow(textRenderer, valueText, x + categoryWidth - valueWidth - 5, y + (settingHeight - 8) / 2, accentColor.getRGB());

        // Draw dual-handle slider
        int sliderX = x + 5;
        int sliderY = y + settingHeight - 4;
        int sliderWidth = categoryWidth - 10;
        int sliderHeight = 2;

        double boundMin = setting.getBoundMin();
        double boundMax = setting.getBoundMax();
        int minHandleX = sliderX + (int)(sliderWidth * (setting.getMinValue() - boundMin) / (boundMax - boundMin));
        int maxHandleX = sliderX + (int)(sliderWidth * (setting.getMaxValue() - boundMin) / (boundMax - boundMin));

        // Background bar
        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, new Color(80, 80, 80).getRGB());
        // Selected range highlight
        context.fill(minHandleX, sliderY, maxHandleX, sliderY + sliderHeight, accentColor.getRGB());
        // Handles
        context.fill(minHandleX - 1, sliderY - 1, minHandleX + 2, sliderY + sliderHeight + 1, accentColor.brighter().getRGB());
        context.fill(maxHandleX - 1, sliderY - 1, maxHandleX + 2, sliderY + sliderHeight + 1, accentColor.brighter().getRGB());
    }

    /** 范围值紧凑显示：整数值不带小数点 */
    private String formatRangeValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        // Check if clicked on a category header (for dragging)
        for (Category category : Category.values()) {
            Point pos = categoryPositions.get(category);
            if (mouseX >= pos.x && mouseX <= pos.x + categoryWidth &&
                    mouseY >= pos.y && mouseY <= pos.y + categoryHeaderHeight) {
                if (button == 0) { // Left click for dragging
                    dragging = true;
                    draggingCategory = category;
                    dragX = (int) (mouseX - pos.x);
                    dragY = (int) (mouseY - pos.y);
                    return true;
                }
            }
        }

        // Check if clicked on a module or setting
        for (Category category : Category.values()) {
            Point pos = categoryPositions.get(category);
            int scrollOffset = categoryScrollOffsets.getOrDefault(category, 0);
            int moduleY = pos.y + categoryHeaderHeight - scrollOffset;

            for (Module module : ModuleManager.getModules()) {
                if (module.getCategory() != category) continue;

                boolean isExpanded = expandedModules.getOrDefault(module, false);

                // Check if clicked on module
                if (mouseX >= pos.x && mouseX <= pos.x + categoryWidth &&
                        mouseY >= moduleY && mouseY <= moduleY + moduleHeight) {
                    if (button == 0) { // Left click toggles module
                        module.toggle();
                        if (clickGuiModule.sound.getValue()) {
                            mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(
                                    net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                        }
                    } else if (button == 1 && !module.settings.isEmpty()) { // Right click expands settings
                        expandedModules.put(module, !isExpanded);
                    }
                    return true;
                }

                // Check if clicked on settings
                if (isExpanded) {
                    int settingY = moduleY + moduleHeight;

                    // Track settings we've already processed to avoid duplicates
                    Set<String> processedSettings = new HashSet<>();

                    for (AbstractSetting<?> setting : module.settings) {
                        // Skip duplicate settings
                        if (processedSettings.contains(setting.getName())) {
                            continue;
                        }
                        processedSettings.add(setting.getName());

                        // Skip settings hidden by visibility condition
                        if (!setting.getVisible().get()) {
                            continue;
                        }

                        if (mouseX >= pos.x && mouseX <= pos.x + categoryWidth &&
                                mouseY >= settingY && mouseY <= settingY + settingHeight) {
                            handleSettingClick(setting, (int)mouseX, pos.x);
                            return true;
                        }
                        settingY += settingHeight;
                    }
                }

                moduleY += moduleHeight;
                if (isExpanded) {
                    // Count unique settings to calculate height correctly
                    Set<String> uniqueSettings = new HashSet<>();
                    for (AbstractSetting<?> setting : module.settings) {
                        if (!setting.getVisible().get()) continue;
                        uniqueSettings.add(setting.getName());
                    }
                    moduleY += uniqueSettings.size() * settingHeight;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void handleSettingClick(AbstractSetting<?> setting, int mouseX, int baseX) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolSetting.setValue(!boolSetting.getValue());
        } else if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting) setting;
            modeSetting.cycle();
        } else if (setting instanceof NumberSetting<?>) {
            NumberSetting<?> numberSetting = (NumberSetting<?>) setting;
            updateNumberSetting(numberSetting, mouseX, baseX + 5, categoryWidth - 10);
        } else if (setting instanceof RangeSetting) {
            RangeSetting rangeSetting = (RangeSetting) setting;
            int sliderX = baseX + 5;
            int sliderWidth = categoryWidth - 10;
            draggingRange = rangeSetting;
            draggingRangeMin = isCloserToMinHandle(rangeSetting, mouseX, sliderX, sliderWidth);
            updateRangeSetting(rangeSetting, mouseX, sliderX, sliderWidth, draggingRangeMin);
        }

        if (clickGuiModule.sound.getValue()) {
            mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(
                    net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    /** 判断鼠标离范围滑块的 min 把手还是 max 把手更近 */
    private boolean isCloserToMinHandle(RangeSetting setting, int mouseX, int sliderX, int sliderWidth) {
        double boundMin = setting.getBoundMin();
        double boundMax = setting.getBoundMax();
        double minHandleX = sliderX + sliderWidth * (setting.getMinValue() - boundMin) / (boundMax - boundMin);
        double maxHandleX = sliderX + sliderWidth * (setting.getMaxValue() - boundMin) / (boundMax - boundMin);
        return Math.abs(mouseX - minHandleX) <= Math.abs(mouseX - maxHandleX);
    }

    private void updateRangeSetting(RangeSetting setting, int mouseX, int sliderX, int sliderWidth, boolean minHandle) {
        double percentage = Math.max(0, Math.min(1, (double)(mouseX - sliderX) / sliderWidth));
        double newValue = setting.getBoundMin() + (setting.getBoundMax() - setting.getBoundMin()) * percentage;
        if (minHandle) {
            setting.setMinValue(newValue);
        } else {
            setting.setMaxValue(newValue);
        }
    }

    private void updateNumberSetting(NumberSetting<?> setting, int mouseX, int sliderX, int sliderWidth) {
        double percentage = Math.max(0, Math.min(1, (double)(mouseX - sliderX) / sliderWidth));
        double min = ((Number)setting.getMin()).doubleValue();
        double max = ((Number)setting.getMax()).doubleValue();
        double newValue = min + (max - min) * percentage;

        if (setting instanceof IntSetting) {
            IntSetting intSetting = (IntSetting) setting;
            intSetting.setValue((int) Math.round(newValue));
        } else if (setting instanceof FloatSetting) {
            FloatSetting floatSetting = (FloatSetting) setting;
            floatSetting.setValue((float) newValue);
        } else if (setting instanceof DoubleSetting) {
            DoubleSetting doubleSetting = (DoubleSetting) setting;
            doubleSetting.setValue(newValue);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        // Range slider dragging
        if (draggingRange != null) {
            // 范围滑块只关心 X，找到该滑块所属的 category 以确定 sliderX
            for (Category category : Category.values()) {
                Point pos = categoryPositions.get(category);
                for (Module module : ModuleManager.getModules()) {
                    if (module.getCategory() == category && module.settings.contains(draggingRange)) {
                        updateRangeSetting(draggingRange, (int)mouseX, pos.x + 5, categoryWidth - 10, draggingRangeMin);
                        return true;
                    }
                }
            }
            draggingRange = null;
            return true;
        }

        if (dragging && draggingCategory != null) {
            Point pos = categoryPositions.get(draggingCategory);
            pos.x = (int) (mouseX - dragX);
            pos.y = (int) (mouseY - dragY);
            return true;
        }

        // Handle slider dragging
        for (Category category : Category.values()) {
            Point pos = categoryPositions.get(category);
            int scrollOffset = categoryScrollOffsets.getOrDefault(category, 0);
            int moduleY = pos.y + categoryHeaderHeight - scrollOffset;

            for (Module module : ModuleManager.getModules()) {
                if (module.getCategory() != category) continue;

                boolean isExpanded = expandedModules.getOrDefault(module, false);

                if (isExpanded) {
                    int settingY = moduleY + moduleHeight;

                    // Track settings we've already processed to avoid duplicates
                    Set<String> processedSettings = new HashSet<>();

                    for (AbstractSetting<?> setting : module.settings) {
                        // Skip duplicate settings
                        if (processedSettings.contains(setting.getName())) {
                            continue;
                        }
                        processedSettings.add(setting.getName());

                        // Skip settings hidden by visibility condition
                        if (!setting.getVisible().get()) {
                            continue;
                        }

                        if (mouseY >= settingY && mouseY <= settingY + settingHeight && setting instanceof NumberSetting<?>) {
                            updateNumberSetting((NumberSetting<?>) setting, (int)mouseX, pos.x + 5, categoryWidth - 10);
                            return true;
                        }
                        settingY += settingHeight;
                    }
                }

                moduleY += moduleHeight;
                if (isExpanded) {
                    // Count unique settings to calculate height correctly
                    Set<String> uniqueSettings = new HashSet<>();
                    for (AbstractSetting<?> setting : module.settings) {
                        if (!setting.getVisible().get()) continue;
                        uniqueSettings.add(setting.getName());
                    }
                    moduleY += uniqueSettings.size() * settingHeight;
                }
            }
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingRange != null) {
            draggingRange = null;
            return true;
        }
        if (dragging) {
            dragging = false;
            draggingCategory = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Handle scrolling for categories
        for (Category category : Category.values()) {
            Point pos = categoryPositions.get(category);

            if (mouseX >= pos.x && mouseX <= pos.x + categoryWidth &&
                    mouseY >= pos.y + categoryHeaderHeight && mouseY <= pos.y + height - 30) {
                int currentOffset = categoryScrollOffsets.getOrDefault(category, 0);
                int newOffset = (int) Math.max(0, currentOffset - verticalAmount * 10);

                // Calculate max scroll offset
                int totalHeight = calculateTotalContentHeight(category);
                int visibleHeight = height - pos.y - categoryHeaderHeight - 30;
                int maxOffset = Math.max(0, totalHeight - visibleHeight);

                newOffset = Math.min(newOffset, maxOffset);
                categoryScrollOffsets.put(category, newOffset);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        // Handle key binding
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.bindKey(-1); // Unbind
            } else {
                bindingModule.bindKey(keyCode);
            }
            bindingModule = null;
            return true;
        }

        // Close on escape
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void close() {
        super.close();
        clickGuiModule.disable();
    }
}