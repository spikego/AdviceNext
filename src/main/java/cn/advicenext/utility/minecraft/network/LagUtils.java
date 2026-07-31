package cn.advicenext.utility.minecraft.network;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.utility.Utility;
import cn.advicenext.utility.minecraft.network.lag.*;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class LagUtils extends Utility {

    public static final LagUtils INSTANCE = new LagUtils();

    private final BidirectionalQueue queue = new BidirectionalQueue();
    private final Set<Packet<?>> packetFastTrack = Collections.synchronizedSet(new HashSet<>());
    private volatile Vec3d serverPosition;

    private final LagHandler lagHandler = new LagHandler();

    private boolean simulatingLag = false;
    private int simulatedLag = 0;

    private LagUtils() {
        EventBus.register(this);
    }

    public static void requestLag(LagRequest request) {
        INSTANCE.queue.requestLag(request);
    }

    public static Vec3d getServerPosition() {
        return INSTANCE.serverPosition;
    }

    public static int getPing() {
        return INSTANCE.lagHandler.getPing();
    }

    public static int getAveragePing() {
        return INSTANCE.lagHandler.getAveragePing();
    }

    public static void setSimulatedLag(int lag) {
        INSTANCE.simulatedLag = Math.max(0, lag);
        INSTANCE.simulatingLag = INSTANCE.simulatedLag > 0;
    }

    public static int getSimulatedLag() {
        return INSTANCE.simulatedLag;
    }

    public static boolean isSimulatingLag() {
        return INSTANCE.simulatingLag;
    }

    public static void clearHistory() {
        INSTANCE.lagHandler.clear();
        INSTANCE.queue.clear();
        INSTANCE.serverPosition = null;
    }

    @Listener(p = 100)
    private void onPacket(PacketEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            queue.clear();
            serverPosition = null;
            return;
        }

        Packet<?> packet = event.getPacket();

        lagHandler.handlePacket(event);

        boolean fastTracked = packetFastTrack.remove(packet);

        if (event.getOrigin() == PacketEvent.TransferOrigin.SEND) {
            if (fastTracked) {
                updateServerPosition(packet);
                return;
            }

            if (queue.tick(packet, LagDirection.OUTBOUND)) {
                event.cancelled = true;
                return;
            }

            updateServerPosition(packet);
        } else {
            if (fastTracked) {
                return;
            }

            if (queue.tick(packet, LagDirection.INBOUND)) {
                event.cancelled = true;
            }
        }
    }

    @Listener(p = 100)
    private void onTick(TickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            queue.clear();
            serverPosition = null;
            return;
        }

        queue.tick(null, null);
    }

    private void updateServerPosition(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket.Full full) {
            serverPosition = new Vec3d(full.getX(0), full.getY(0), full.getZ(0));
        } else if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround pos) {
            serverPosition = new Vec3d(pos.getX(0), pos.getY(0), pos.getZ(0));
        }
    }

    private class BidirectionalQueue {
        private final TrackState inboundTrack = new TrackState();
        private final TrackState outboundTrack = new TrackState();

        boolean tick(Packet<?> packet, LagDirection direction) {
            if (packet == null) {
                inboundTrack.tick(null, null);
                outboundTrack.tick(null, null);
                return false;
            }

            if (direction == LagDirection.INBOUND) {
                return inboundTrack.tick(packet, direction);
            } else {
                return outboundTrack.tick(packet, direction);
            }
        }

        void requestLag(LagRequest request) {
            for (LagDirection direction : request.getDirections()) {
                if (direction == LagDirection.OUTBOUND) {
                    outboundTrack.addRequest(request);
                } else {
                    inboundTrack.addRequest(request);
                }
            }
        }

        void clear() {
            inboundTrack.clear();
            outboundTrack.clear();
        }
    }

    private class TrackState {
        private final List<LagNode> track = new ArrayList<>();
        private LagRequest currentlyAwaiting = null;

        synchronized void addRequest(LagRequest request) {
            track.add(new RequestLagNode(request));
        }

        synchronized boolean tick(Packet<?> packet, LagDirection direction) {
            if (track.isEmpty() && (currentlyAwaiting == null || currentlyAwaiting.getTimeout().isTimedOut())) {
                currentlyAwaiting = null;
                return false;
            }

            if (packet != null) {
                track.add(new PacketLagNode(packet, direction));
            }

            LagRequest awaiting = currentlyAwaiting;

            try {
                while (awaiting == null || awaiting.getTimeout().isTimedOut()) {
                    LagNode popped = !track.isEmpty() ? track.remove(0) : null;

                    if (popped == null) {
                        awaiting = null;
                        break;
                    }

                    if (popped instanceof PacketLagNode) {
                        ((PacketLagNode) popped).goThrough(packetFastTrack);
                    } else if (popped instanceof RequestLagNode) {
                        awaiting = ((RequestLagNode) popped).getRequest();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            currentlyAwaiting = awaiting;

            return true;
        }

        synchronized void clear() {
            track.clear();
            currentlyAwaiting = null;
        }
    }
}