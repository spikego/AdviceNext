package cn.advicenext.utility.minecraft.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 静默旋转管理器（Silent Rotation Manager）
 *
 * 单例工具类，管理"当前应发给服务端的旋转"。
 * 支持多模块提交旋转请求，按数值优先级仲裁。
 * 同一模块（requester）新请求顶掉旧请求。
 *
 * 未来供 KillAura、Scaffold 等模块复用。
 */
public final class RotationManager {

    // ==================== 优先级常量 ====================
    public static final int PRIORITY_ANTIAIM  = -20;
    public static final int PRIORITY_NORMAL   = 0;
    public static final int PRIORITY_KILLAURA = 30;
    public static final int PRIORITY_SCAFFOLD = 40;
    public static final int PRIORITY_SAFETY   = 60;

    // ==================== 单例 ====================
    public static final RotationManager INSTANCE = new RotationManager();

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // ==================== 内部数据结构 ====================

    private static class RotationRequest {
        final float yaw;
        final float pitch;
        final int priority;
        final Object requester;
        final int ticksToLive;
        final long bornTick;

        RotationRequest(float yaw, float pitch, int priority, Object requester, int ticksToLive, long bornTick) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.requester = requester;
            this.ticksToLive = ticksToLive;
            this.bornTick = bornTick;
        }
    }

    private final Map<Object, RotationRequest> byRequester = new HashMap<>();
    private RotationRequest activeRequest;
    private long currentTick = 0;

    // ==================== 核心 API ====================

    public void submit(float yaw, float pitch, int priority, Object requester) {
        byRequester.put(requester, new RotationRequest(yaw, pitch, priority, requester, -1, currentTick));
        refreshActiveRequest();
    }

    public void submit(float yaw, float pitch, int priority, Object requester, int ticksToLive) {
        byRequester.put(requester, new RotationRequest(yaw, pitch, priority, requester, ticksToLive, currentTick));
        refreshActiveRequest();
    }

    public void revoke(Object requester) {
        byRequester.remove(requester);
        refreshActiveRequest();
    }

    public float getServerYaw() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return 0;
        if (activeRequest != null) return activeRequest.yaw;
        return player.getYaw();
    }

    public float getServerPitch() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return 0;
        if (activeRequest != null) return activeRequest.pitch;
        return player.getPitch();
    }

    public boolean hasActiveRequest() {
        return activeRequest != null;
    }

    // ==================== Tick 生命周期 ====================

    public void tick() {
        currentTick++;
        byRequester.values().removeIf(r ->
            r.ticksToLive != -1 && (currentTick - r.bornTick) >= r.ticksToLive
        );
        refreshActiveRequest();
    }

    // ==================== 内部方法 ====================

    private void refreshActiveRequest() {
        activeRequest = byRequester.values().stream()
            .max(Comparator.comparingInt(r -> r.priority))
            .orElse(null);
    }
}