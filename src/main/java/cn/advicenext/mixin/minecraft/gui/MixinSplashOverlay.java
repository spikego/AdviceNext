package cn.advicenext.mixin.minecraft.gui;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.IntSupplier;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
    @Unique
    private static final Identifier CUSTOM_LOGO = Identifier.of("advice", "icon/logo.png");

    private long reloadCompleteTime = -1L;

    @Unique
    private static final int BACKGROUND_COLOR = ColorHelper.getArgb(255, 255, 255, 255);

    @Unique
    private static final IntSupplier BRAND_ARGB
            = () -> BACKGROUND_COLOR;

    @Unique
    private static final int PROGRESS_START_COLOR = ColorHelper.getArgb(255, 50, 0, 0);

    @Unique
    private static final int PROGRESS_END_COLOR = ColorHelper.getArgb(255, 200, 0, 0);

    @Shadow
    private float progress;

    @Shadow @Final
    private net.minecraft.resource.ResourceReload reload;


    @Mutable
    @Shadow @Final private static int MOJANG_RED;

    @Inject(method = "init", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        MOJANG_RED = ColorHelper.getArgb(255, 24, 26, 27);
    }
}