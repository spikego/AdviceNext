package cn.spikego.advicenext.mixin.minecraft.client;

import cn.spikego.advicenext.event.EventBus;
import cn.spikego.advicenext.event.impl.TickEvent;
import cn.spikego.advicenext.gui.mainmenu.MainMenuScreen;
import cn.spikego.advicenext.features.module.impl.world.FastPlace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow
    @Final
    private Window window;

    @Shadow private Screen currentScreen;

    @Shadow
    private int itemUseCooldown;

    @Inject(at = @At("HEAD"), method = "tick")
    private void init(CallbackInfo Info) {
        EventBus.post(new TickEvent());
    }

    @Redirect(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    public void setTitle(Window instance, String title) {
        String pageTitle = currentScreen != null ? currentScreen.getTitle().getString() : "Game";
        this.window.setTitle("AdviceNext # " + pageTitle);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FastPlace.isEnabled()) {  // 使用新的方法名
            this.itemUseCooldown = FastPlace.getDelay();
        }
    }


    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Replace TitleScreen with our custom main menu
        if (screen instanceof TitleScreen) {
            MinecraftClient.getInstance().setScreen(new MainMenuScreen());
            ci.cancel();
        }
    }
}