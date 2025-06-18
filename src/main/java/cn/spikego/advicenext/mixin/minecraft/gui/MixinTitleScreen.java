package cn.spikego.advicenext.mixin.minecraft.gui;

import cn.spikego.advicenext.gui.mainmenu.MainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    private static boolean firstLoad = true;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        // Only show loading screen on first load
        if (firstLoad) {
            MinecraftClient.getInstance().setScreen(new MainMenuScreen());
            firstLoad = false;
            ci.cancel();
        }
    }
}