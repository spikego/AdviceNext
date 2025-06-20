package cn.advicenext.event.impl;

import cn.advicenext.event.Event;
import net.minecraft.network.packet.Packet;

public class PacketEvent extends Event {
    private final TransferOrigin origin;
    private final Packet<?> packet;
    private final boolean original;

    public PacketEvent(TransferOrigin origin, Packet<?> packet, boolean original) {
        this.origin = origin;
        this.packet = packet;
        this.original = original;
    }

    public PacketEvent(TransferOrigin origin, Packet<?> packet) {
        this(origin, packet, true);
    }

    public TransferOrigin getOrigin() {
        return origin;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public boolean isOriginal() {
        return original;
    }

    // 内部枚举类
    public enum TransferOrigin {
        SEND,
        RECEIVE
    }
}