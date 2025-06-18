package cn.advicenext.mixin.minecraft.network;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.ChatEvent;
import cn.advicenext.features.command.CommandManager;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendMessage(String content, CallbackInfo ci) {
        if (content.startsWith(CommandManager.getCommandPrefix())) {
            CommandManager.processCommand(content);
            ci.cancel();
            return;
        }

        ChatEvent chatEvent = new ChatEvent(content);
        EventBus.post(chatEvent);
        if(chatEvent.getCancelled()) ci.cancel();
    }
}
