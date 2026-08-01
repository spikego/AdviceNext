package cn.advicenext.mixin.minecraft.gui;

import cn.advicenext.gui.mainmenu.MainMenuScreen;
import cn.advicenext.utility.client.render.SkijaUIRenderer;
import cn.advicenext.utility.client.render.font.FontRenderer;
import cn.advicenext.utility.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {

    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Unique
    private static final int BG_COLOR = 0xFF181A1B;

    @Unique
    private long animStartTime = -1;

    @Unique
    private long reloadCompleteTime = -1;

    @Unique
    private float smoothProgress = 0f;

    @Unique
    private boolean menuSet = false;

    @Shadow
    private float progress;

    @Shadow
    @Final
    private ResourceReload reload;

    @Mutable
    @Shadow
    @Final
    private static int MOJANG_RED;

    @Inject(method = "init", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        MOJANG_RED = ColorHelper.getArgb(255, 24, 26, 27);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustom(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        if (animStartTime == -1) {
            animStartTime = now;
        }

        if (reloadCompleteTime == -1 && reload.isComplete()) {
            reloadCompleteTime = now;
        }

        this.progress = reload.getProgress();
        smoothProgress += (this.progress - smoothProgress) * 0.05f;

        SkijaUIRenderer.setRenderState(context.state, context.getMatrices());
        FontRenderer.setRenderState(context.state, context.getMatrices());

        SkijaUIRenderer.drawRect("splash_bg", 0, 0, width, height, BG_COLOR);

        FontRenderer font = Fonts.interBold.get(40);
        float centerX = width / 2f;
        float centerY = height / 2f - 50 + 7;

        String advice = "Advice";
        String next = "Next";
        float totalWidth = font.getStringWidth(advice) + font.getStringWidth(next);
        float x = centerX - totalWidth / 2f;

        float timeSec = now / 1000f;
        float cursorX = x;
        for (int i = 0; i < advice.length(); i++) {
            char c = advice.charAt(i);
            String ch = String.valueOf(c);
            float hue = (timeSec * 60f + i * 36f) % 360f;
            int rgb = Color.HSBtoRGB(hue / 360f, 0.9f, 1.0f);
            int argb = 0xFF000000 | (rgb & 0x00FFFFFF);
            font.drawString(ch, cursorX, centerY, argb);
            cursorX += font.getStringWidth(ch);
        }
        font.drawString(next, cursorX, centerY, 0xFFFFFFFF);

        float barWidth = 170;
        float barHeight = 5;
        float barX = centerX - barWidth / 2f;
        float barY = height / 2f + 15;

        SkijaUIRenderer.drawRoundedRect("splash_progress_bg", barX, barY, barWidth, barHeight, 2, 0xFFDDE4FF);

        float filledWidth = barWidth * smoothProgress;
        if (filledWidth > 0.5f) {
            float hueLeft = (timeSec * 120f) % 360f;
            float hueRight = (hueLeft + 180f) % 360f;
            int leftArgb = 0xFF000000 | (Color.HSBtoRGB(hueLeft / 360f, 0.85f, 1.0f) & 0x00FFFFFF);
            int rightArgb = 0xFF000000 | (Color.HSBtoRGB(hueRight / 360f, 0.85f, 1.0f) & 0x00FFFFFF);
            SkijaUIRenderer.drawRoundedRect4C(null, barX, barY, filledWidth, barHeight, 2,
                    leftArgb, rightArgb, leftArgb, rightArgb);
        }

        if (reloadCompleteTime != -1) {
            if (mc.world != null) {
                if (mc.getOverlay() instanceof SplashOverlay) {
                    mc.setOverlay(null);
                }
                return;
            }

            float fadeT = (now - reloadCompleteTime) / 500f;

            if (fadeT >= 1.0f) {
                if (!menuSet) {
                    mc.setScreen(new MainMenuScreen());
                    menuSet = true;
                }
                if (mc.getOverlay() instanceof SplashOverlay) {
                    mc.setOverlay(null);
                }
                return;
            }

            if (!menuSet && fadeT > 0.6f) {
                mc.setScreen(new MainMenuScreen());
                menuSet = true;
            }

            int alpha = (int) (Math.min(fadeT, 1.0f) * 255);
            SkijaUIRenderer.drawRect("splash_fade", 0, 0, width, height, (alpha << 24) | 0x000000);
        }
    }
}