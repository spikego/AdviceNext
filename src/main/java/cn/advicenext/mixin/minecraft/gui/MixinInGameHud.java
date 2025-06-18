package cn.advicenext.mixin.minecraft.gui;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.Render2DEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender2D(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Render2DEvent event = new Render2DEvent(context, tickCounter);
        EventBus.post(event);
    }

}
