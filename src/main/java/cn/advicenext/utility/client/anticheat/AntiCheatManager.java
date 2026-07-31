package cn.advicenext.utility.client.anticheat;

import cn.advicenext.features.module.impl.combat.AntiBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AntiCheatManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final AntiCheatManager INSTANCE = new AntiCheatManager();

    private final Map<String, AntiCheatPlayerData> playerData = new HashMap<>();
    private final List<AntiCheatCheck> checks = new ArrayList<>();
    private final Set<AntiCheatType> disabledChecks = new HashSet<>();
    private final List<AntiCheatFlag> pendingFlags = new ArrayList<>();
    private Consumer<AntiCheatFlag> alertCallback;
    private boolean enabled = true;
    private double strictness = 1.0;

    private AntiCheatManager() {
        registerChecks();
    }

    public static AntiCheatManager getInstance() { return INSTANCE; }

    private void registerChecks() {
        checks.add(new FlyCheckA());
        checks.add(new FlyCheckB());
        checks.add(new SpeedCheckA());
        checks.add(new SpeedCheckB());
        checks.add(new OmniSprintCheck());
        checks.add(new NoFallCheck());
        checks.add(new InvalidCheck());
        checks.add(new BlinkCheck());
        checks.add(new StepCheck());
        checks.add(new HighJumpCheck());
        checks.add(new JesusCheck());
        checks.add(new ScaffoldCheck());
        checks.add(new ReachCheck());
        checks.add(new KillAuraCheck());
        checks.add(new AutoClickerCheck());
    }

    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;
        pendingFlags.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isSpectator()) continue;
            if (AntiBot.isBotStatic(player)) continue;

            AntiCheatPlayerData data = playerData.computeIfAbsent(
                    player.getUuid().toString(), k -> new AntiCheatPlayerData(player));

            updateStatusEffects(player, data);
            updateEnvironment(player, data);

            for (AntiCheatCheck check : checks) {
                if (!disabledChecks.contains(check.type)) {
                    check.check(player, data);
                }
            }

            for (AntiCheatFlag flag : data.flags) {
                if (!flag.isExpired(30000)) {
                    pendingFlags.add(flag);
                }
            }
            data.flags.clear();

            data.prevPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            data.prevMotion = player.getVelocity();
            data.prevUpdateTime = System.currentTimeMillis();
            data.lastTickDelta = System.currentTimeMillis() - data.lastUpdateTime;
            data.lastUpdateTime = System.currentTimeMillis();
        }

        playerData.entrySet().removeIf(e -> e.getValue().player.isRemoved());

        for (AntiCheatFlag flag : pendingFlags) {
            if (alertCallback != null) {
                alertCallback.accept(flag);
            }
        }
    }

    private void updateStatusEffects(PlayerEntity player, AntiCheatPlayerData data) {
        data.hasLevitation = player.hasStatusEffect(StatusEffects.LEVITATION);
        data.hasSlowFalling = player.hasStatusEffect(StatusEffects.SLOW_FALLING);
        data.hasSpeed = player.hasStatusEffect(StatusEffects.SPEED);
        data.hasJumpBoost = player.hasStatusEffect(StatusEffects.JUMP_BOOST);
    }

    private void updateEnvironment(PlayerEntity player, AntiCheatPlayerData data) {
        data.isInLiquid = player.isTouchingWater() || player.isInLava();
        data.isOnClimbable = player.isClimbing();
        data.isInWeb = mc.world.getBlockState(player.getBlockPos()).getBlock()
                instanceof net.minecraft.block.CobwebBlock;
        data.isFlying = player.getAbilities().flying;
    }

    public void onAttack(PlayerEntity attacker, PlayerEntity target) {
        AntiCheatPlayerData data = playerData.get(attacker.getUuid().toString());
        if (data == null) return;
        data.lastHitTime = System.currentTimeMillis();
        data.lastHitPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        data.hitCount++;
    }

    public void onSwing(PlayerEntity player) {
        AntiCheatPlayerData data = playerData.get(player.getUuid().toString());
        if (data == null) return;
        data.lastSwingTime = System.currentTimeMillis();
        data.swingCount++;
        data.swingInWindow++;
    }

    public void addPendingFlag(AntiCheatFlag flag) {
        pendingFlags.add(flag);
    }

    public void setAlertCallback(Consumer<AntiCheatFlag> callback) {
        this.alertCallback = callback;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    public AntiCheatPlayerData getPlayerData(String uuid) {
        return playerData.get(uuid);
    }

    public List<AntiCheatFlag> getRecentFlags(int maxAgeMs) {
        return playerData.values().stream()
                .flatMap(d -> d.flags.stream())
                .filter(f -> !f.isExpired(maxAgeMs))
                .collect(Collectors.toList());
    }

    public List<AntiCheatPlayerData> getAllPlayerData() {
        return new ArrayList<>(playerData.values());
    }

    public void setCheckEnabled(AntiCheatType type, boolean enabled) {
        if (enabled) {
            disabledChecks.remove(type);
        } else {
            disabledChecks.add(type);
        }
    }

    public boolean isCheckEnabled(AntiCheatType type) {
        return !disabledChecks.contains(type);
    }

    public void setStrictness(double strictness) {
        this.strictness = Math.max(0.1, Math.min(3.0, strictness));
    }

    public double getStrictness() {
        return strictness;
    }
}