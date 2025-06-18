package cn.spikego.advicenext.mixin.minecraft.client;

import cn.spikego.advicenext.event.impl.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cn.spikego.advicenext.event.EventBus;


@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Final
    @Shadow
    private Window window;

    @Unique
    public Screen currentScreen;

    @Redirect(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    public void setTitle(Window instance, String title) {
        String pageTitle = currentScreen != null ? currentScreen.getTitle().getString() : "Game";
        this.window.setTitle("AdviceNext # " + pageTitle);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void init(CallbackInfo Info) {
        EventBus.post(new TickEvent());
    }

}
