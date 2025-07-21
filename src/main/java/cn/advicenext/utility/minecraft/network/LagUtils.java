package cn.advicenext.utility.minecraft.network;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.impl.PacketEvent;
import cn.advicenext.utility.Utility;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LagUtils extends Utility {
    // 延迟相关
    private static long lastKeepAliveId = 0;
    private static long lastKeepAliveSendTime = 0;
    private static int currentPing = 0;
    private static final List<Integer> pingHistory = new ArrayList<>();
    private static final int MAX_PING_HISTORY = 30; // 保存最近30次ping
    
    // 模拟延迟相关
    private static boolean simulatingLag = false;
    private static int simulatedLag = 0;
    private static final ConcurrentLinkedQueue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Backtrack相关
    private static final Map<Integer, List<EntityPosition>> entityPositionHistory = new HashMap<>();
    private static final int MAX_POSITION_HISTORY = 20; // 保存最近20个位置
    
    static {
        // 注册事件监听器
        EventBus.register(LagUtils.class);
    }
    
    /**
     * 获取当前延迟
     * @return 当前延迟(ms)
     */
    public static int getPing() {
        return currentPing;
    }
    
    /**
     * 获取平均延迟
     * @return 平均延迟(ms)
     */
    public static int getAveragePing() {
        if (pingHistory.isEmpty()) return 0;
        
        int sum = 0;
        for (int ping : pingHistory) {
            sum += ping;
        }
        return sum / pingHistory.size();
    }
    
    /**
     * 设置模拟延迟
     * @param lag 延迟时间(ms)
     */
    public static void setSimulatedLag(int lag) {
        simulatedLag = Math.max(0, lag);
        simulatingLag = simulatedLag > 0;
    }
    
    /**
     * 获取模拟延迟
     * @return 模拟延迟时间(ms)
     */
    public static int getSimulatedLag() {
        return simulatedLag;
    }
    
    /**
     * 是否正在模拟延迟
     * @return 是否正在模拟延迟
     */
    public static boolean isSimulatingLag() {
        return simulatingLag;
    }
    
    /**
     * 处理KeepAlive数据包，用于计算延迟
     * @param event 数据包事件
     */
    public static void onPacket(PacketEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        
        // 处理KeepAlive数据包，计算延迟
        if (event.getOrigin() == PacketEvent.TransferOrigin.RECEIVE && event.getPacket() instanceof KeepAliveS2CPacket packet) {
            lastKeepAliveId = packet.getId();
            lastKeepAliveSendTime = System.currentTimeMillis();
        } else if (event.getOrigin() == PacketEvent.TransferOrigin.SEND && event.getPacket() instanceof KeepAliveC2SPacket packet) {
            if (packet.getId() == lastKeepAliveId && lastKeepAliveSendTime > 0) {
                // 计算延迟
                int ping = (int) (System.currentTimeMillis() - lastKeepAliveSendTime);
                currentPing = ping;
                
                // 更新历史记录
                pingHistory.add(ping);
                if (pingHistory.size() > MAX_PING_HISTORY) {
                    pingHistory.remove(0);
                }
            }
        }
        
        // 处理模拟延迟
        if (simulatingLag && event.getOrigin() == PacketEvent.TransferOrigin.SEND) {
            // 模拟发送延迟
            if (simulatedLag > 0 && !(event.getPacket() instanceof KeepAliveC2SPacket)) {
                event.cancelled = true;
                
                DelayedPacket delayedPacket = new DelayedPacket(event.getPacket(), System.currentTimeMillis() + simulatedLag);
                delayedPackets.add(delayedPacket);
                
                // 安排延迟发送
                scheduler.schedule(() -> {
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(delayedPacket.packet);
                    }
                    delayedPackets.remove(delayedPacket);
                }, simulatedLag, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * 保存实体位置历史，用于Backtrack
     * @param entityId 实体ID
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yaw 偏航角
     * @param pitch 俯仰角
     */
    public static void saveEntityPosition(int entityId, double x, double y, double z, float yaw, float pitch) {
        EntityPosition position = new EntityPosition(x, y, z, yaw, pitch, System.currentTimeMillis());
        
        entityPositionHistory.computeIfAbsent(entityId, k -> new ArrayList<>()).add(position);
        
        // 限制历史记录大小
        List<EntityPosition> positions = entityPositionHistory.get(entityId);
        if (positions.size() > MAX_POSITION_HISTORY) {
            positions.remove(0);
        }
    }
    
    /**
     * 获取指定时间的实体位置，用于Backtrack
     * @param entityId 实体ID
     * @param time 时间(ms)
     * @return 实体位置，如果没有找到则返回null
     */
    public static EntityPosition getEntityPositionAtTime(int entityId, long time) {
        List<EntityPosition> positions = entityPositionHistory.get(entityId);
        if (positions == null || positions.isEmpty()) return null;
        
        // 找到最接近指定时间的位置
        EntityPosition closest = null;
        long minDiff = Long.MAX_VALUE;
        
        for (EntityPosition position : positions) {
            long diff = Math.abs(position.time - time);
            if (diff < minDiff) {
                minDiff = diff;
                closest = position;
            }
        }
        
        return closest;
    }
    
    /**
     * 清除所有历史记录
     */
    public static void clearHistory() {
        pingHistory.clear();
        entityPositionHistory.clear();
        delayedPackets.clear();
    }
    
    /**
     * 关闭LagUtils，释放资源
     */
    public static void shutdown() {
        scheduler.shutdown();
        clearHistory();
    }
    
    /**
     * 延迟数据包类
     */
    private static class DelayedPacket {
        final Packet<?> packet;
        final long sendTime;
        
        public DelayedPacket(Packet<?> packet, long sendTime) {
            this.packet = packet;
            this.sendTime = sendTime;
        }
    }
    
    /**
     * 实体位置类，用于Backtrack
     */
    public static class EntityPosition {
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final long time;
        
        public EntityPosition(double x, double y, double z, float yaw, float pitch, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.time = time;
        }
    }
}