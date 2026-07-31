package cn.advicenext.features.command.impl;

import cn.advicenext.features.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import static cn.advicenext.features.command.CommandManager.addMessage;

public class TeleportCommand extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public TeleportCommand() {
        super("tp", "Teleport to a player or coordinates",
                new String[] { "tp <player>", "tp <x> <y> <z>", "tp <player> <x> <y> <z>" });
    }

    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            addMessage("§cUsage: .tp <player> | .tp <x> <y> <z> | .tp <player> <x> <y> <z>");
            return;
        }

        if (mc.player == null || mc.world == null) {
            addMessage("§cYou are not in a world!");
            return;
        }

        if (args.length == 1) {
            teleportToPlayer(args[0]);
        } else if (args.length == 3) {
            teleportToCoords(args[0], args[1], args[2]);
        } else if (args.length == 4) {
            teleportPlayerToCoords(args[0], args[1], args[2], args[3]);
        } else {
            addMessage("§cUsage: .tp <player> | .tp <x> <y> <z> | .tp <player> <x> <y> <z>");
        }
    }

    private void teleportToPlayer(String playerName) {
        PlayerEntity target = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(playerName)) {
                target = player;
                break;
            }
        }

        if (target == null) {
            addMessage("§cPlayer not found: " + playerName);
            return;
        }

        mc.player.setPosition(target.getX(), target.getY(), target.getZ());
        addMessage("§aTeleported to " + playerName);
    }

    private void teleportToCoords(String xStr, String yStr, String zStr) {
        try {
            double x = Double.parseDouble(xStr);
            double y = Double.parseDouble(yStr);
            double z = Double.parseDouble(zStr);

            mc.player.setPosition(x, y, z);
            addMessage("§aTeleported to " + (int) x + ", " + (int) y + ", " + (int) z);
        } catch (NumberFormatException e) {
            addMessage("§cInvalid coordinates! Use numbers like: .tp 100 64 200");
        }
    }

    private void teleportPlayerToCoords(String playerName, String xStr, String yStr, String zStr) {
        try {
            double x = Double.parseDouble(xStr);
            double y = Double.parseDouble(yStr);
            double z = Double.parseDouble(zStr);

            PlayerEntity target = null;
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    target = player;
                    break;
                }
            }

            if (target == null) {
                addMessage("§cPlayer not found: " + playerName);
                return;
            }

            if (target == mc.player) {
                mc.player.setPosition(x, y, z);
                addMessage("§aTeleported to " + (int) x + ", " + (int) y + ", " + (int) z);
            } else {
                mc.player.networkHandler.sendChatCommand(
                        "tp " + playerName + " " + x + " " + y + " " + z);
                addMessage("§aSent teleport command for " + playerName + " to " + (int) x + ", " + (int) y + ", " + (int) z);
            }
        } catch (NumberFormatException e) {
            addMessage("§cInvalid coordinates! Use numbers like: .tp Player 100 64 200");
        }
    }
}