package cn.advicenext.utility.minecraft.network.lag;

import net.minecraft.network.packet.Packet;

import java.util.Set;

public class PacketLagNode extends LagNode {
    private final Packet<?> packet;
    private final LagDirection direction;
    private final long queuedAtMs;

    public PacketLagNode(Packet<?> packet, LagDirection direction) {
        this.packet = packet;
        this.direction = direction;
        this.queuedAtMs = System.currentTimeMillis();
    }

    public long getQueuedAtMs() {
        return queuedAtMs;
    }

    public void goThrough(Set<Packet<?>> fastTrack) {
        if (direction == LagDirection.OUTBOUND) {
            fastTrack.add(packet);
        }
        direction.passThrough(packet);
    }
}