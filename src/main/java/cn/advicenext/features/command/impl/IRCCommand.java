package cn.advicenext.features.command.impl;

import cn.advicenext.features.command.Command;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.misc.IRC;
import cn.advicenext.features.notification.NotificationManager;

public class IRCCommand extends Command {

    public IRCCommand() {
        super("irc", "IRC chat commands: .irc <msg> | .irc connect | .irc disconnect",
                new String[] { "irc <message>", "irc connect", "irc disconnect", "irc server <name>" });
    }

    @Override
    public void run(String[] args) {
        IRC irc = (IRC) ModuleManager.getModules().stream()
                .filter(m -> m instanceof IRC)
                .findFirst()
                .orElse(null);

        if (irc == null) {
            NotificationManager.getInstance().addNotification(
                "IRC", "IRC module not found!",
                NotificationManager.NotificationType.ERROR, 3000);
            return;
        }

        if (args.length == 0) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Usage: .irc <msg> | .irc connect | .irc disconnect",
                NotificationManager.NotificationType.INFO, 3000);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "connect" -> {
                if (!irc.getEnabled()) {
                    irc.toggle();
                }
                irc.connect();
            }
            case "disconnect" -> irc.disconnect();
            case "server" -> {
                if (args.length < 2) {
                    NotificationManager.getInstance().addNotification(
                        "IRC", "Usage: .irc server <Advice|LiquidBounce>",
                        NotificationManager.NotificationType.INFO, 3000);
                    return;
                }
                String server = args[1];
                if (server.equalsIgnoreCase("Advice") || server.equalsIgnoreCase("LiquidBounce")) {
                    irc.server.setValue(server);
                    irc.disconnect();
                    if (irc.getEnabled()) {
                        irc.connect();
                    }
                    NotificationManager.getInstance().addNotification(
                        "IRC", "Server set to " + server,
                        NotificationManager.NotificationType.SUCCESS, 2000);
                } else {
                    NotificationManager.getInstance().addNotification(
                        "IRC", "Unknown server: " + server + ". Use Advice or LiquidBounce",
                        NotificationManager.NotificationType.ERROR, 3000);
                }
            }
            default -> {
                String message = String.join(" ", args);
                irc.sendMessage(message);
            }
        }
    }
}