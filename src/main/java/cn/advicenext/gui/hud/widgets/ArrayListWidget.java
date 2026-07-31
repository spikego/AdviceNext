package cn.advicenext.gui.hud.widgets;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.gui.colors.Colors;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.utility.client.render.RenderUtils;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArrayListWidget extends Widget {

    private final Map<Module, Float> animProgress = new HashMap<>();
    private final Map<Module, Float> animVelocity = new HashMap<>();
    private final Set<Module> wasEnabled = new HashSet<>();
    private static final float ANIM_SPEED = 0.18f;
    private static final float GRAVITY = 0.6f;
    private static final float BOUNCE_DAMP = 0.6f;
    private static final float MOTION_ACCEL = 0.08f;
    private static final float FALL_GRAVITY = 0.04f;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+\\.?\\d*");

    public ArrayListWidget() {
        super("arraylist", -5, 10, 120, 50);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        HUD hud = HUD.getHudInstance();
        FontRenderer font = Fonts.interSemiBold.get(8);
        int screenWidth = mc.getWindow().getScaledWidth();

        String sortMode = hud != null ? hud.ALSortMode.getValue() : "Length";
        String displayMode = hud != null ? hud.ALDisplayValue.getValue() : "[]";
        String style = hud != null ? hud.ALStyle.getValue() : "NoBackground";
        String animMode = hud != null ? hud.ALAnimation.getValue() : "Bounce";

        List<Module> enabledModules = new ArrayList<>(ModuleManager.getModules().stream()
                .filter(Module::getEnabled)
                .toList());

        if (sortMode.equals("A-Z")) {
            enabledModules.sort(Comparator.comparing(Module::getName));
        } else if (sortMode.equals("Z-A")) {
            enabledModules.sort(Comparator.comparing(Module::getName).reversed());
        } else {
            enabledModules.sort((m1, m2) ->
                    Integer.compare(getDisplayWidth(m2, font, displayMode), getDisplayWidth(m1, font, displayMode)));
        }

        float lineH = 10;
        float renderY = y;
        boolean rightAligned = x > screenWidth / 2f;

        float maxWidth = 0;
        for (Module mod : enabledModules) {
            float w = getDisplayWidth(mod, font, displayMode);
            if (w > maxWidth) maxWidth = w;
        }
        this.width = (int) Math.ceil(maxWidth);

        boolean isOneBlur = style.equals("OneBlur");
        boolean isOutline = style.equals("Outline");
        boolean isSidebar = style.equals("Sidebar");

        if (isOneBlur && !enabledModules.isEmpty()) {
            float blurX = rightAligned ? x + width - maxWidth - 6 : x - 6;
            float blurY = y - 4;
            float blurW = maxWidth + 12;
            float blurH = enabledModules.size() * lineH + 8;
            drawOneBlurBackground(blurX, blurY, blurW, blurH, 6);
        }

        if (isSidebar && !enabledModules.isEmpty()) {
            float sidebarX = rightAligned ? x + width - maxWidth - 4 : x - 4;
            float sidebarY = y;
            float sidebarW = 2;
            float sidebarH = enabledModules.size() * lineH;
            context.fill((int) sidebarX, (int) sidebarY, (int) (sidebarX + sidebarW), (int) (sidebarY + sidebarH),
                    Colors.currentColor().getRGB());
        }

        Set<Module> currentEnabled = new HashSet<>(enabledModules);
        for (Module module : enabledModules) {
            if (!wasEnabled.contains(module)) {
                animProgress.put(module, 0f);
                animVelocity.put(module, 0f);
            }
        }
        for (Module module : enabledModules) {
            animProgress.putIfAbsent(module, 0f);
            animVelocity.putIfAbsent(module, 0f);
        }
        wasEnabled.clear();
        wasEnabled.addAll(currentEnabled);

        int i = 0;
        for (Module module : enabledModules) {
            float progress = animProgress.getOrDefault(module, 0f);
            float velocity = animVelocity.getOrDefault(module, 0f);

            if (animMode.equals("Bounce")) {
                if (progress < 1f) {
                    velocity += GRAVITY;
                    progress += velocity;
                    if (progress >= 1f) {
                        progress = 1f;
                        velocity = -velocity * BOUNCE_DAMP;
                        if (Math.abs(velocity) < 0.02f) {
                            progress = 1f;
                            velocity = 0f;
                        }
                    }
                }
            } else if (animMode.equals("Motion")) {
                if (progress < 1f) {
                    velocity += MOTION_ACCEL;
                    progress = Math.min(1f, progress + velocity);
                }
            } else if (animMode.equals("Fall")) {
                if (progress < 1f) {
                    velocity += FALL_GRAVITY;
                    progress = Math.min(1f, progress + velocity);
                }
            } else {
                progress = lerp(progress, 1f, ANIM_SPEED);
                if (Math.abs(progress - 1f) < 0.005f) progress = 1f;
            }

            animProgress.put(module, progress);
            animVelocity.put(module, velocity);

            float animOffset = (1f - progress) * 20f;
            String name = module.getName();
            String value = module.getDisplayValue();
            int totalWidth = getDisplayWidth(module, font, displayMode);
            int color = Colors.gradientColor(i, enabledModules.size()).getRGB();

            float actualY = renderY + animOffset;
            int alpha = (int) (progress * 255);
            if (alpha < 0) alpha = 0;
            int alphaColor = (alpha << 24) | (color & 0x00FFFFFF);

            float renderX;
            if (rightAligned) {
                renderX = x + width - totalWidth;
            } else {
                renderX = x;
            }

            if (isOutline) {
                float outlineX = renderX - 4;
                float outlineW = totalWidth + 8;
                float outlineH = lineH + 2;
                SkijaUIRenderer.drawRoundedRect("al_outline_" + module.getName(), outlineX, actualY - 2, outlineW, outlineH, 4,
                        0x00000000);
                int outlineAlpha = (int)(alpha * 0.5f);
                if (outlineAlpha < 0) outlineAlpha = 0;
                RenderUtils.drawBorder(outlineX, actualY - 2, outlineW, outlineH, 1, (outlineAlpha << 24) | (color & 0x00FFFFFF));
            }

            font.drawString(name, renderX, actualY, alphaColor);

            if (value != null && !value.isEmpty()) {
                float nameWidth = font.getStringWidth(name);
                String formatted = formatValue(value, displayMode);
                font.drawString(formatted, renderX + nameWidth, actualY, 0x808080 | (alpha << 24));
            }

            renderY += lineH;
            i++;
        }

        animProgress.keySet().removeIf(m -> !enabledModules.contains(m));
        animVelocity.keySet().removeIf(m -> !enabledModules.contains(m));
    }

    private String formatNumber(String raw) {
        try {
            double d = Double.parseDouble(raw);
            return String.format(Locale.ROOT, "%.2f", d);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private String formatValue(String value, String mode) {
        Matcher m = NUMBER_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, formatNumber(m.group()));
        }
        m.appendTail(sb);
        String formatted = sb.toString();

        if (mode.equals("Space")) {
            return " " + formatted;
        } else if (mode.equals("-")) {
            return " - " + formatted;
        }
        return " [" + formatted + "]";
    }

    private int getDisplayWidth(Module module, FontRenderer font, String displayMode) {
        int width = (int) font.getStringWidth(module.getName());
        String value = module.getDisplayValue();
        if (value != null && !value.isEmpty()) {
            width += (int) font.getStringWidth(formatValue(value, displayMode));
        }
        return width;
    }

    private void drawOneBlurBackground(float x, float y, float w, float h, float radius) {
        float pad = 4;
        int[] alphas = {15, 20, 25, 30};
        float[] expansions = {pad, pad * 0.65f, pad * 0.35f, pad * 0.1f};

        for (int i = 0; i < alphas.length; i++) {
            float expand = expansions[i];
            SkijaUIRenderer.drawRoundedRect(
                "al_blur_" + i,
                x - expand, y - expand,
                w + expand * 2, h + expand * 2,
                radius + expand,
                (alphas[i] << 24) | 0x000000
            );
        }
    }

    private float lerp(float start, float end, float factor) {
        return start + factor * (end - start);
    }
}