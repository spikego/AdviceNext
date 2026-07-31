package cn.advicenext.utility.minecraft.network.lag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTimeout {
    private final Map<Integer, List<EntityPosition>> positionHistory = new HashMap<>();
    private static final int MAX_POSITION_HISTORY = 20;

    public void savePosition(int entityId, double x, double y, double z, float yaw, float pitch) {
        EntityPosition pos = new EntityPosition(x, y, z, yaw, pitch, System.currentTimeMillis());
        positionHistory.computeIfAbsent(entityId, k -> new ArrayList<>()).add(pos);
        List<EntityPosition> positions = positionHistory.get(entityId);
        if (positions.size() > MAX_POSITION_HISTORY) {
            positions.remove(0);
        }
    }

    public EntityPosition getPositionAtTime(int entityId, long time) {
        List<EntityPosition> positions = positionHistory.get(entityId);
        if (positions == null || positions.isEmpty()) return null;
        EntityPosition closest = null;
        long minDiff = Long.MAX_VALUE;
        for (EntityPosition pos : positions) {
            long diff = Math.abs(pos.time - time);
            if (diff < minDiff) {
                minDiff = diff;
                closest = pos;
            }
        }
        return closest;
    }

    public List<EntityPosition> getPositionHistory(int entityId) {
        return positionHistory.getOrDefault(entityId, new ArrayList<>());
    }

    public void clearHistory() {
        positionHistory.clear();
    }

    public abstract void onTimeout(int entityId);

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