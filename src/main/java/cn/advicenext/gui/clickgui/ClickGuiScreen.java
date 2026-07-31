package cn.advicenext.gui.clickgui;

import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.FloatSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.ClickGui;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ColorSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.features.value.slider.NumberSetting;
import cn.advicenext.features.value.slider.RangeSetting;
import cn.advicenext.gui.clickgui.animation.AnimationUtil;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.utility.client.render.font.Fonts;
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

public class ClickGuiScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ClickGui clickGuiModule;

    // GUI dimensions
    private int guiX, guiY;
    private final int guiWidth = 500;
    private final int guiHeight = 300;
    private final int sidebarWidth = 120;

    // Colors - Neverlose inspired
    private final Color backgroundColor = new Color(17, 17, 17, 255);
    private final Color sidebarColor = new Color(22, 22, 22, 255);
    private final Color accentColor = Colors.currentColor();
    private final Color moduleColor = new Color(30, 30, 30, 255);
    private final Color moduleHoverColor = new Color(35, 35, 35, 255);
    private final Color moduleActiveColor = new Color(40, 40, 40, 255);
    private final Color textColor = Color.WHITE;
    private final Color subTextColor = new Color(170, 170, 170, 255);

    // State
    private Category selectedCategory = Category.COMBAT;
    private Category previousCategory = Category.COMBAT;
    private Module selectedModule = null;
    private Map<Module, Boolean> expandedModules = new HashMap<>();
    private boolean bindingKey = false;
    private Module bindingModule = null;
    private int scrollOffset = 0;

    // Animation
    private long openTime;
    private float openAnimation = 0f;
    private float categoryIndicatorY = 40f;
    private float targetIndicatorY = 40f;

    // Dragging
    private boolean dragging = false;
    private int dragX, dragY;

    // Settings
    private AbstractSetting<?> draggingSetting = null;
    private boolean draggingRangeMinHandle = false;

    public ClickGuiScreen(ClickGui clickGuiModule) {
        super(Text.literal("ClickGui"));
        this.clickGuiModule = clickGuiModule;
        this.openTime = System.currentTimeMillis();

        // Center the GUI
        this.guiX = (mc.getWindow().getScaledWidth() - guiWidth) / 2;
        this.guiY = (mc.getWindow().getScaledHeight() - guiHeight) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update open animation
        long timeSinceOpen = System.currentTimeMillis() - openTime;
        openAnimation = Math.min(1f, timeSinceOpen / 300f);

        // Update category indicator animation
        int categoryIndex = 0;
        for (Category category : Category.values()) {
            if (category == selectedCategory) {
                targetIndicatorY = guiY + 40 + categoryIndex * 25;
                break;
            }
            categoryIndex++;
        }

        // Apply smooth animation to the indicator
        categoryIndicatorY = AnimationUtil.animate(categoryIndicatorY, targetIndicatorY, 0.2f);

        // Draw background
        context.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), new Color(0, 0, 0, 120).getRGB());

        // Apply animation
        int animatedWidth = (int)(guiWidth * openAnimation);
        int animatedHeight = (int)(guiHeight * openAnimation);
        int animatedX = guiX + (guiWidth - animatedWidth) / 2;
        int animatedY = guiY + (guiHeight - animatedHeight) / 2;

        // Draw main background
        context.fill(animatedX, animatedY, animatedX + animatedWidth, animatedY + animatedHeight, backgroundColor.getRGB());

        // If animation not complete, don't render content
        if (openAnimation < 1f) {
            return;
        }

        // Draw sidebar
        context.fill(guiX, guiY, guiX + sidebarWidth, guiY + guiHeight, sidebarColor.getRGB());

        // Draw title
        //context.drawTextWithShadow(mc.textRenderer, "AdviceNext", guiX + 10, guiY + 10, textColor.getRGB());
        Fonts.inter.get(16).drawString("AdviceNext", guiX + 10, guiY + 10, textColor.getRGB());

        // Draw animated category indicator
        context.fill(guiX, (int)categoryIndicatorY, guiX + 3, (int)categoryIndicatorY + 20, accentColor.getRGB());

        // Draw categories
        int categoryY = guiY + 40;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;

            // Draw category text
            context.drawTextWithShadow(mc.textRenderer, category.getName(), guiX + 15, categoryY + 6,
                    isSelected ? accentColor.getRGB() : textColor.getRGB());

            categoryY += 25;
        }

        // Draw modules
        int moduleX = guiX + sidebarWidth + 10;
        int moduleY = guiY + 10;
        int moduleWidth = 150;
        int moduleHeight = 20;

        // Draw modules for selected category
        for (Module module : ModuleManager.getModules()) {
            if (module.getCategory() == selectedCategory) {
                boolean isHovered = mouseX >= moduleX && mouseX <= moduleX + moduleWidth &&
                        mouseY >= moduleY && mouseY <= moduleY + moduleHeight;
                boolean isSelected = module == selectedModule;

                // Draw module background
                Color bgColor;
                if (isSelected) {
                    bgColor = moduleActiveColor;
                } else if (isHovered) {
                    bgColor = moduleHoverColor;
                } else {
                    bgColor = moduleColor;
                }

                context.fill(moduleX, moduleY, moduleX + moduleWidth, moduleY + moduleHeight, bgColor.getRGB());

                // Draw module name
                context.drawTextWithShadow(mc.textRenderer, module.getName(), moduleX + 5, moduleY + 6,
                        module.getEnabled() ? accentColor.getRGB() : textColor.getRGB());

                // Draw toggle indicator
                String indicator = module.getEnabled() ? "ON" : "OFF";
                int indicatorWidth = mc.textRenderer.getWidth(indicator);
                context.drawTextWithShadow(mc.textRenderer, indicator, moduleX + moduleWidth - indicatorWidth - 5, moduleY + 6,
                        module.getEnabled() ? accentColor.getRGB() : subTextColor.getRGB());

                moduleY += moduleHeight + 5;
            }
        }

        // Draw settings panel if a module is selected
        if (selectedModule != null) {
            int settingsX = guiX + sidebarWidth + moduleWidth + 20;
            int settingsY = guiY + 10;
            int settingsWidth = guiWidth - sidebarWidth - moduleWidth - 30;

            // Draw settings title
            context.drawTextWithShadow(mc.textRenderer, selectedModule.getName() + " Settings", settingsX, settingsY, textColor.getRGB());
            settingsY += 20;

            // Draw key bind option
            context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());
            context.drawTextWithShadow(mc.textRenderer, "Key Bind", settingsX + 5, settingsY + 6, textColor.getRGB());

            String keyName;
            if (bindingKey && bindingModule == selectedModule) {
                keyName = "Press a key...";
            } else {
                int key = selectedModule.getKey();
                keyName = key == -1 ? "None" : GLFW.glfwGetKeyName(key, 0);
                if (keyName == null) keyName = "Key " + key;
            }

            int keyWidth = mc.textRenderer.getWidth(keyName);
            context.drawTextWithShadow(mc.textRenderer, keyName, settingsX + settingsWidth - keyWidth - 5, settingsY + 6, subTextColor.getRGB());
            settingsY += 25;

            // Track settings we've already displayed to avoid duplicates
            Set<String> displayedSettings = new HashSet<>();

            // Draw settings
            for (AbstractSetting<?> setting : selectedModule.settings) {
                // Skip duplicate settings
                if (displayedSettings.contains(setting.getName())) {
                    continue;
                }
                displayedSettings.add(setting.getName());

                // Skip settings hidden by visibility condition
                if (!setting.getVisible().get()) {
                    continue;
                }

                if (setting instanceof BooleanSetting) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;

                    // Draw setting background
                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());

                    // Draw setting name
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    // Draw toggle state
                    String value = boolSetting.getValue() ? "ON" : "OFF";
                    int valueWidth = mc.textRenderer.getWidth(value);
                    context.drawTextWithShadow(mc.textRenderer, value, settingsX + settingsWidth - valueWidth - 5, settingsY + 6,
                            boolSetting.getValue() ? accentColor.getRGB() : subTextColor.getRGB());

                } else if (setting instanceof ModeSetting) {
                    ModeSetting modeSetting = (ModeSetting) setting;

                    // Draw setting background
                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());

                    // Draw setting name
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    // Draw current mode
                    String value = modeSetting.getValue();
                    int valueWidth = mc.textRenderer.getWidth(value);
                    context.drawTextWithShadow(mc.textRenderer, value, settingsX + settingsWidth - valueWidth - 5, settingsY + 6, accentColor.getRGB());

                } else if (setting instanceof NumberSetting<?>) {
                    NumberSetting<?> numberSetting = (NumberSetting<?>) setting;

                    // Draw setting background
                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());

                    // Draw setting name
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    // Draw current value
                    String value = numberSetting.getValue().toString();
                    int valueWidth = mc.textRenderer.getWidth(value);
                    context.drawTextWithShadow(mc.textRenderer, value, settingsX + settingsWidth - valueWidth - 5, settingsY + 6, accentColor.getRGB());

                    // Draw slider
                    int sliderY = settingsY + 15;
                    int sliderWidth = settingsWidth - 10;
                    int sliderX = settingsX + 5;

                    // Draw slider background
                    context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 2, new Color(50, 50, 50, 255).getRGB());

                    // Draw slider progress
                    double min = ((Number)numberSetting.getMin()).doubleValue();
                    double max = ((Number)numberSetting.getMax()).doubleValue();
                    double currentValue = ((Number)numberSetting.getValue()).doubleValue();
                    double percentage = (currentValue - min) / (max - min);
                    int progressWidth = (int)(sliderWidth * percentage);
                    context.fill(sliderX, sliderY, sliderX + progressWidth, sliderY + 2, accentColor.getRGB());

                } else if (setting instanceof RangeSetting) {
                    RangeSetting rangeSetting = (RangeSetting) setting;

                    // Draw setting background
                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());

                    // Draw setting name
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    // Draw current range value
                    String value = formatRangeValue(rangeSetting.getMinValue()) + " - " + formatRangeValue(rangeSetting.getMaxValue());
                    int valueWidth = mc.textRenderer.getWidth(value);
                    context.drawTextWithShadow(mc.textRenderer, value, settingsX + settingsWidth - valueWidth - 5, settingsY + 6, accentColor.getRGB());

                    // Draw dual-handle slider
                    int sliderY = settingsY + 15;
                    int sliderWidth = settingsWidth - 10;
                    int sliderX = settingsX + 5;

                    double boundMin = rangeSetting.getBoundMin();
                    double boundMax = rangeSetting.getBoundMax();
                    int minHandleX = sliderX + (int)(sliderWidth * (rangeSetting.getMinValue() - boundMin) / (boundMax - boundMin));
                    int maxHandleX = sliderX + (int)(sliderWidth * (rangeSetting.getMaxValue() - boundMin) / (boundMax - boundMin));

                    // Background bar
                    context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 2, new Color(50, 50, 50, 255).getRGB());
                    // Selected range highlight
                    context.fill(minHandleX, sliderY, maxHandleX, sliderY + 2, accentColor.getRGB());
                    // Handles
                    context.fill(minHandleX - 1, sliderY - 2, minHandleX + 2, sliderY + 4, accentColor.brighter().getRGB());
                    context.fill(maxHandleX - 1, sliderY - 2, maxHandleX + 2, sliderY + 4, accentColor.brighter().getRGB());

                } else if (setting instanceof StringSetting) {
                    StringSetting stringSetting = (StringSetting) setting;

                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    String value = stringSetting.getValue();
                    int valueWidth = mc.textRenderer.getWidth(value);
                    context.drawTextWithShadow(mc.textRenderer, value, settingsX + settingsWidth - valueWidth - 5, settingsY + 6, accentColor.getRGB());

                } else if (setting instanceof ColorSetting) {
                    ColorSetting colorSetting = (ColorSetting) setting;

                    context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, moduleColor.getRGB());
                    context.drawTextWithShadow(mc.textRenderer, setting.getName(), settingsX + 5, settingsY + 6, textColor.getRGB());

                    int colorBoxSize = 12;
                    int cbx = settingsX + settingsWidth - colorBoxSize - 8;
                    int cby = settingsY + 4;
                    context.fill(cbx, cby, cbx + colorBoxSize, cby + colorBoxSize, colorSetting.getValue());
                }

                settingsY += 25;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (openAnimation < 1f) return false;

        // Check if clicked on the header (for dragging)
        if (mouseX >= guiX && mouseX <= guiX + guiWidth && mouseY >= guiY && mouseY <= guiY + 20) {
            dragging = true;
            dragX = (int) (mouseX - guiX);
            dragY = (int) (mouseY - guiY);
            return true;
        }

        // Check if clicked on a category
        int categoryY = guiY + 40;
        for (Category category : Category.values()) {
            if (mouseX >= guiX && mouseX <= guiX + sidebarWidth && mouseY >= categoryY && mouseY <= categoryY + 20) {
                previousCategory = selectedCategory;
                selectedCategory = category;
                selectedModule = null;
                return true;
            }
            categoryY += 25;
        }

        // Check if clicked on a module
        int moduleX = guiX + sidebarWidth + 10;
        int moduleY = guiY + 10;
        int moduleWidth = 150;
        int moduleHeight = 20;

        for (Module module : ModuleManager.getModules()) {
            if (module.getCategory() == selectedCategory) {
                if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth && mouseY >= moduleY && mouseY <= moduleY + moduleHeight) {
                    if (button == 0) {
                        module.toggle();
                    } else if (button == 1) {
                        selectedModule = module;
                    }
                    return true;
                }
                moduleY += moduleHeight + 5;
            }
        }

        // Check if clicked on settings
        if (selectedModule != null) {
            int settingsX = guiX + sidebarWidth + moduleWidth + 20;
            int settingsY = guiY + 10;
            int settingsWidth = guiWidth - sidebarWidth - moduleWidth - 30;

            // Skip title
            settingsY += 20;

            // Check key bind option
            if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth && mouseY >= settingsY && mouseY <= settingsY + 20) {
                bindingKey = true;
                bindingModule = selectedModule;
                return true;
            }
            settingsY += 25;

            // Track settings we've already processed to avoid duplicates
            Set<String> processedSettings = new HashSet<>();

            // Check settings
            for (AbstractSetting<?> setting : selectedModule.settings) {
                // Skip duplicate settings
                if (processedSettings.contains(setting.getName())) {
                    continue;
                }
                processedSettings.add(setting.getName());

                // Skip settings hidden by visibility condition
                if (!setting.getVisible().get()) {
                    continue;
                }

                if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth && mouseY >= settingsY && mouseY <= settingsY + 20) {
                    if (setting instanceof BooleanSetting) {
                        BooleanSetting boolSetting = (BooleanSetting) setting;
                        boolSetting.setValue(!boolSetting.getValue());
                    } else if (setting instanceof ModeSetting) {
                        ModeSetting modeSetting = (ModeSetting) setting;
                        modeSetting.cycle();
                    } else if (setting instanceof NumberSetting<?>) {
                        NumberSetting<?> numberSetting = (NumberSetting<?>) setting;
                        draggingSetting = setting;
                        updateNumberSetting(numberSetting, (int)mouseX, settingsX + 5, settingsWidth - 10);
                    } else if (setting instanceof RangeSetting) {
                        RangeSetting rangeSetting = (RangeSetting) setting;
                        draggingSetting = setting;
                        int sliderX = settingsX + 5;
                        int sliderWidth = settingsWidth - 10;
                        draggingRangeMinHandle = isCloserToMinHandle(rangeSetting, (int)mouseX, sliderX, sliderWidth);
                        updateRangeSetting(rangeSetting, (int)mouseX, sliderX, sliderWidth, draggingRangeMinHandle);
                    }
                    return true;
                }
                settingsY += 25;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        draggingSetting = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (dragging) {
            guiX = (int) (mouseX - dragX);
            guiY = (int) (mouseY - dragY);
            return true;
        }

        if (draggingSetting instanceof NumberSetting<?>) {
            NumberSetting<?> numberSetting = (NumberSetting<?>) draggingSetting;
            int settingsX = guiX + sidebarWidth + 150 + 20;
            int settingsWidth = guiWidth - sidebarWidth - 150 - 30;
            updateNumberSetting(numberSetting, (int)mouseX, settingsX + 5, settingsWidth - 10);
            return true;
        }

        if (draggingSetting instanceof RangeSetting) {
            RangeSetting rangeSetting = (RangeSetting) draggingSetting;
            int settingsX = guiX + sidebarWidth + 150 + 20;
            int settingsWidth = guiWidth - sidebarWidth - 150 - 30;
            updateRangeSetting(rangeSetting, (int)mouseX, settingsX + 5, settingsWidth - 10, draggingRangeMinHandle);
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateNumberSetting(NumberSetting<?> setting, int mouseX, int sliderX, int sliderWidth) {
        double percentage = Math.max(0, Math.min(1, (double)(mouseX - sliderX) / sliderWidth));
        double min = ((Number)setting.getMin()).doubleValue();
        double max = ((Number)setting.getMax()).doubleValue();
        double newValue = min + (max - min) * percentage;

        if (setting instanceof IntSetting) {
            ((IntSetting)setting).setValue((int)newValue);
        } else if (setting instanceof FloatSetting) {
            ((FloatSetting)setting).setValue((float)newValue);
        } else if (setting instanceof DoubleSetting) {
            ((DoubleSetting)setting).setValue(newValue);
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

    /** 范围值紧凑显示：整数值不带小数点 */
    private String formatRangeValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (bindingKey && bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.bindKey(-1);
            } else {
                bindingModule.bindKey(keyCode);
            }
            bindingKey = false;
            bindingModule = null;
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