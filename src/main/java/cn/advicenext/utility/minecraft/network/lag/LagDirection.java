package cn.advicenext.utility.minecraft.network.lag;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public enum LagDirection {
    INBOUND(packet -> {
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Packet pkt = (Packet) packet;
            pkt.apply(MinecraftClient.getInstance().getNetworkHandler());
        } catch (Exception ignored) {
        }
    }),
    OUTBOUND(packet -> {
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
    });

    public static final Set<LagDirection> ONLY_INBOUND = EnumSet.of(INBOUND);
    public static final Set<LagDirection> ONLY_OUTBOUND = EnumSet.of(OUTBOUND);
    public static final Set<LagDirection> BIDIRECTIONAL = EnumSet.allOf(LagDirection.class);

    private final Consumer<Packet<?>> channel;

    LagDirection(Consumer<Packet<?>> channel) {
        this.channel = channel;
    }

    public void passThrough(Packet<?> packet) {
        channel.accept(packet);
    }
}