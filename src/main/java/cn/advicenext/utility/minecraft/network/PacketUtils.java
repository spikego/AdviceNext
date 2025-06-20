package cn.advicenext.utility.minecraft.network;

import cn.advicenext.utility.Utility;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;

public class PacketUtils extends Utility {
    static ClientPlayNetworkHandler network = mc.player.networkHandler;
    public static void sendPacket(Object packet) {
        if (mc.player != null && network != null) {
            mc.player.networkHandler.sendPacket((Packet<?>) packet);
        }
    }

    public static void blink(boolean enable) {

    }

}
