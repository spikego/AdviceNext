package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.KeyboardEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    @Inject(method = "onKey", at = @At("HEAD"))
    public void key(long window, int action, KeyInput input, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && MinecraftClient.getInstance().currentScreen == null) {
            EventBus.post(new KeyboardEvent(input.key()));
        }
    }
}