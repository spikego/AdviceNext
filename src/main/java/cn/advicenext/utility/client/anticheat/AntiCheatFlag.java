package cn.advicenext.utility.client.anticheat;

public class AntiCheatFlag {
    public final AntiCheatType type;
    public final String playerName;
    public final int level;
    public final long timestamp;
    public final String detail;

    public AntiCheatFlag(AntiCheatType type, String playerName, int level, String detail) {
        this.type = type;
        this.playerName = playerName;
        this.level = level;
        this.timestamp = System.currentTimeMillis();
        this.detail = detail;
    }

    public boolean isExpired(long maxAgeMs) {
        return System.currentTimeMillis() - timestamp > maxAgeMs;
    }
}