package cn.advicenext.features.module.impl.misc;

import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.utility.client.chat.IRCChat;

import java.util.List;

public class IRC extends Module {
    public static IRC INSTANCE;

    public final ModeSetting server = new ModeSetting("Server", "IRC server to connect to", "Advice",
            List.of("Advice", "LiquidBounce"));
    public final StringSetting nick = new StringSetting("Nick", "IRC nickname", "");
    public final BooleanSetting autoConnect = new BooleanSetting("AutoConnect", "Auto-connect on enable", true);

    private final IRCChat chat = new IRCChat();
    private boolean connected = false;
    private long lastConnectAttempt = 0;

    public IRC() {
        super("IRC", "IRC chat client", Category.MISC);
        INSTANCE = this;

        chat.onMessage(msg -> {
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("[IRC] " + msg), false);
            }
        });

        chat.onSystem(msg -> {
            NotificationManager.getInstance().addNotification("IRC", msg,
                    NotificationManager.NotificationType.INFO, 3000);
        });
    }

    @Override
    public void onEnable() {
        if (autoConnect.getValue()) {
            connect();
        }
    }

    @Override
    public void onDisable() {
        disconnect();
    }

    public void connect() {
        if (chat.isConnected()) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Already connected to " + chat.getServer(),
                NotificationManager.NotificationType.INFO, 2000);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastConnectAttempt < 2000) return;
        lastConnectAttempt = now;

        if (server.is("Advice")) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Advice server is currently unavailable - cannot connect",
                NotificationManager.NotificationType.ERROR, 4000);
            return;
        }

        if (server.is("LiquidBounce")) {
            String nickname = nick.getValue().isEmpty() ? mc.player.getName().getString() : nick.getValue();
            try {
                chat.connect("127.0.0.1", 6667, nickname);
                NotificationManager.getInstance().addNotification(
                    "IRC", "Connecting to LiquidBounce IRC...",
                    NotificationManager.NotificationType.INFO, 3000);
            } catch (Exception e) {
                NotificationManager.getInstance().addNotification(
                    "IRC", "Connection failed: " + e.getMessage(),
                    NotificationManager.NotificationType.ERROR, 4000);
            }
        }
    }

    public void disconnect() {
        if (chat.isConnected()) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Disconnected from IRC",
                NotificationManager.NotificationType.INFO, 2000);
        }
        chat.disconnect();
    }

    public boolean isConnected() {
        return chat.isConnected();
    }

    public void sendMessage(String message) {
        if (!getEnabled()) {
            NotificationManager.getInstance().addNotification(
                "IRC", "IRC is not enabled! Use .irc to enable it first",
                NotificationManager.NotificationType.ERROR, 3000);
            return;
        }

        if (!chat.isConnected()) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Not connected to any IRC server. Connect first!",
                NotificationManager.NotificationType.ERROR, 3000);
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            NotificationManager.getInstance().addNotification(
                "IRC", "Message cannot be empty",
                NotificationManager.NotificationType.ERROR, 2000);
            return;
        }

        chat.sendMessage(message);
    }

    @Override
    public String getDisplayValue() {
        return chat.isConnected() ? chat.getServer() : "disconnected";
    }
}