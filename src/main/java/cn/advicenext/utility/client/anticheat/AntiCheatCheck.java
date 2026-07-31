package cn.advicenext.utility.client.anticheat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public abstract class AntiCheatCheck {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    public final AntiCheatType type;

    protected AntiCheatCheck(AntiCheatType type) {
        this.type = type;
    }

    public abstract void check(PlayerEntity player, AntiCheatPlayerData data);

    protected double getStrictness() {
        return AntiCheatManager.getInstance().getStrictness();
    }

    protected double adjustThreshold(double base, double lowerMultiplier, double upperMultiplier) {
        double s = getStrictness();
        if (s <= 1.0) {
            return base * (1.0 + (1.0 - s) * lowerMultiplier);
        } else {
            return base / (1.0 + (s - 1.0) * upperMultiplier);
        }
    }
}