package cn.advicenext.features.module.impl.combat;

import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiBot extends Module {

    public static AntiBot INSTANCE;

    private final BooleanSetting tab = new BooleanSetting("Tab", "Check if player is in tab list", true);
    private final BooleanSetting ground = new BooleanSetting("Ground", "Check ground state", true);
    private final BooleanSetting ping = new BooleanSetting("Ping", "Check unrealistic ping", false);
    private final BooleanSetting name = new BooleanSetting("Name", "Check invalid name pattern", true);
    private final BooleanSetting health = new BooleanSetting("Health", "Check always full health", false);
    private final BooleanSetting gameMode = new BooleanSetting("GameMode", "Check creative/spectator", false);
    private final BooleanSetting invisible = new BooleanSetting("Invisible", "Check invisible players", false);
    private final BooleanSetting duplicate = new BooleanSetting("Duplicate", "Check duplicate UUID", false);

    private final Set<PlayerEntity> bots = ConcurrentHashMap.newKeySet();
    private final Set<UUID> seenUUIDs = new HashSet<>();

    private static final String VALID_NAME_REGEX = "^[a-zA-Z0-9_]{1,16}$";

    public AntiBot() {
        super("AntiBot", "Detects and ignores bots", Category.COMBAT);
        INSTANCE = this;
        this.settings.add(tab);
        this.settings.add(ground);
        this.settings.add(ping);
        this.settings.add(name);
        this.settings.add(health);
        this.settings.add(gameMode);
        this.settings.add(invisible);
        this.settings.add(duplicate);
    }

    @Override
    public void onEnable() {
        bots.clear();
        seenUUIDs.clear();
    }

    @Override
    public void onDisable() {
        bots.clear();
        seenUUIDs.clear();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        seenUUIDs.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (duplicate.getValue()) {
                if (!seenUUIDs.add(player.getUuid())) {
                    bots.add(player);
                    continue;
                }
            }

            if (isBot(player)) {
                bots.add(player);
            }
        }
    }

    public boolean isBot(PlayerEntity player) {
        if (!enabled) return false;
        if (player == null || player == mc.player) return false;

        if (tab.getValue() && !isInTab(player)) {
            return true;
        }

        if (ground.getValue() && !player.isOnGround() && player.age > 100) {
            if (player.getVelocity().y == 0.0 && !player.isClimbing() && !player.isTouchingWater()) {
                return true;
            }
        }

        if (ping.getValue() && hasUnrealisticPing(player)) {
            return true;
        }

        if (name.getValue() && !isValidName(player)) {
            return true;
        }

        if (health.getValue() && hasAlwaysFullHealth(player)) {
            return true;
        }

        if (gameMode.getValue() && hasInvalidGameMode(player)) {
            return true;
        }

        if (invisible.getValue() && player.isInvisible()) {
            return true;
        }

        return false;
    }

    public boolean isBotCheck(PlayerEntity player) {
        return bots.contains(player);
    }

    public static boolean isBotStatic(PlayerEntity player) {
        if (INSTANCE == null || !INSTANCE.enabled) return false;
        return INSTANCE.bots.contains(player) || INSTANCE.isBot(player);
    }

    private boolean isInTab(PlayerEntity player) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return false;
        for (PlayerListEntry entry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().id().equals(player.getUuid())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnrealisticPing(PlayerEntity player) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return false;
        PlayerListEntry entry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) return false;
        int delay = entry.getLatency();
        return delay < 0 || delay > 1000;
    }

    private boolean isValidName(PlayerEntity player) {
        String name = player.getName().getString();
        if (name.isEmpty()) return false;
        if (name.startsWith("[NPC]")) return false;
        if (name.contains("\u00a7") && name.length() <= 4) return false;
        return name.matches(VALID_NAME_REGEX);
    }

    private boolean hasAlwaysFullHealth(PlayerEntity player) {
        return player.getHealth() == player.getMaxHealth()
                && player.age > 100
                && player.hurtTime == 0;
    }

    private boolean hasInvalidGameMode(PlayerEntity player) {
        if (player instanceof net.minecraft.client.network.OtherClientPlayerEntity) {
            GameMode gm = ((net.minecraft.client.network.OtherClientPlayerEntity) player).getGameMode();
            return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
        }
        return false;
    }

    @Override
    public String getDisplayValue() {
        int count = bots.size();
        return count > 0 ? count + " bots" : "0";
    }
}