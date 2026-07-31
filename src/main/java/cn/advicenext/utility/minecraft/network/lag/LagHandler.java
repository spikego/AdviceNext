package cn.advicenext.utility.minecraft.network.lag;

import cn.advicenext.event.impl.PacketEvent;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class LagHandler {
    private long lastKeepAliveId = 0;
    private long lastKeepAliveSendTime = 0;
    private int currentPing = 0;
    private final List<Integer> pingHistory = new ArrayList<>();
    private static final int MAX_PING_HISTORY = 30;

    public int getPing() {
        return currentPing;
    }

    public int getAveragePing() {
        if (pingHistory.isEmpty()) return 0;
        int sum = 0;
        for (int ping : pingHistory) {
            sum += ping;
        }
        return sum / pingHistory.size();
    }

    public List<Integer> getPingHistory() {
        return pingHistory;
    }

    public void handlePacket(PacketEvent event) {
        if (event.getOrigin() == PacketEvent.TransferOrigin.RECEIVE
                && event.getPacket() instanceof KeepAliveS2CPacket packet) {
            lastKeepAliveId = packet.getId();
            lastKeepAliveSendTime = System.currentTimeMillis();
        } else if (event.getOrigin() == PacketEvent.TransferOrigin.SEND
                && event.getPacket() instanceof KeepAliveC2SPacket packet) {
            if (packet.getId() == lastKeepAliveId && lastKeepAliveSendTime > 0) {
                int ping = (int) (System.currentTimeMillis() - lastKeepAliveSendTime);
                currentPing = ping;
                pingHistory.add(ping);
                if (pingHistory.size() > MAX_PING_HISTORY) {
                    pingHistory.remove(0);
                }
            }
        }
    }

    public void clear() {
        pingHistory.clear();
        currentPing = 0;
        lastKeepAliveId = 0;
        lastKeepAliveSendTime = 0;
    }
}