package cn.advicenext.utility.client.anticheat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class AntiCheatPlayerData {
    public final PlayerEntity player;
    public final String name;
    public Vec3d prevPos = Vec3d.ZERO;
    public Vec3d prevMotion = Vec3d.ZERO;
    public int airTicks;
    public int groundTicks;
    public double lastFallDistance;
    public double highestFallDist;
    public boolean tookFallDamage;
    public int noFallVL;
    public int flyVL;
    public int speedVL;
    public int omniSprintVL;
    public int timerVL;
    public long lastSwingTime;
    public int swingCount;
    public long swingWindowStart;
    public int swingInWindow;
    public float lastYaw;
    public float lastPitch;
    public float yawDelta;
    public float pitchDelta;
    public int auraVL;
    public int reachVL;
    public int autoClickerVL;
    public int blinkVL;
    public int stepVL;
    public int highJumpVL;
    public int jesusVL;
    public int scaffoldVL;
    public int invalidVL;
    public long lastUpdateTime;
    public long prevUpdateTime;
    public double lastTickDelta;
    public double lastHorizontalSpeed;
    public boolean hasLevitation;
    public boolean hasSlowFalling;
    public boolean hasSpeed;
    public boolean hasJumpBoost;
    public boolean isInLiquid;
    public boolean isInWeb;
    public boolean isOnClimbable;
    public boolean isFlying;
    public boolean wasOnGround;
    public Vec3d lastGroundPos = Vec3d.ZERO;
    public int lastHurtTime;
    public int hitCount;
    public long lastHitTime;
    public Vec3d lastHitPos = Vec3d.ZERO;
    public List<AntiCheatFlag> flags = new ArrayList<>();

    public AntiCheatPlayerData(PlayerEntity player) {
        this.player = player;
        this.name = player.getName().getString();
        this.prevPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        this.lastGroundPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        this.lastUpdateTime = System.currentTimeMillis();
        this.prevUpdateTime = System.currentTimeMillis();
    }

    public void addFlag(AntiCheatFlag flag) {
        flags.add(flag);
        flags.removeIf(f -> f.isExpired(30000));
    }

    public int getVL(AntiCheatType type) {
        return switch (type) {
            case FLY_A, FLY_B -> flyVL;
            case SPEED_A, SPEED_B -> speedVL;
            case OMNI_SPRINT -> omniSprintVL;
            case NO_FALL -> noFallVL;
            case TIMER -> timerVL;
            case REACH -> reachVL;
            case KILL_AURA -> auraVL;
            case AUTO_CLICKER -> autoClickerVL;
            case BLINK -> blinkVL;
            case STEP -> stepVL;
            case HIGH_JUMP -> highJumpVL;
            case JESUS -> jesusVL;
            case SCAFFOLD -> scaffoldVL;
            case INVALID -> invalidVL;
        };
    }

    public void incrementVL(AntiCheatType type) {
        switch (type) {
            case FLY_A, FLY_B -> flyVL++;
            case SPEED_A, SPEED_B -> speedVL++;
            case OMNI_SPRINT -> omniSprintVL++;
            case NO_FALL -> noFallVL++;
            case TIMER -> timerVL++;
            case REACH -> reachVL++;
            case KILL_AURA -> auraVL++;
            case AUTO_CLICKER -> autoClickerVL++;
            case BLINK -> blinkVL++;
            case STEP -> stepVL++;
            case HIGH_JUMP -> highJumpVL++;
            case JESUS -> jesusVL++;
            case SCAFFOLD -> scaffoldVL++;
            case INVALID -> invalidVL++;
        }
    }
}