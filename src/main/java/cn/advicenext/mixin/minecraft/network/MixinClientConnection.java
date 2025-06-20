package cn.advicenext.mixin.minecraft.network;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.PacketEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Shadow
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void hookSendingPacket(Packet<?> packet, final CallbackInfo callbackInfo) {
        PacketEvent packetEvent = new PacketEvent(PacketEvent.TransferOrigin.SEND,packet,true);
        EventBus.post(packetEvent);

        if(packetEvent.getCancelled()) callbackInfo.cancel();
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true, require = 1)
    private static void hookReceivingPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            ci.cancel();

            for (Packet<?> packetInBundle : bundleS2CPacket.getPackets()) {
                try {
                    // This will call this method again, but with a single packet instead of a bundle
                    handlePacket(packetInBundle, listener);
                } catch (OffThreadException ignored) {
                }
                // usually we also handle RejectedExecutionException and
                // ClassCastException, but both of them will disconnect the player
                // and therefore are handled by the upper layer
            }
            return;
        }

        PacketEvent packetEvent = new PacketEvent(PacketEvent.TransferOrigin.RECEIVE, packet, true);
        EventBus.post(packetEvent);
        if (packetEvent.getCancelled()) {
            ci.cancel();
        }
    }
}
