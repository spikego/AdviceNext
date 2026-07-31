package cn.advicenext.utility.client.chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class IRCChat {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readThread;
    private boolean running;

    private final CopyOnWriteArrayList<Consumer<String>> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> systemListeners = new CopyOnWriteArrayList<>();

    private String server;
    private int port;
    private String nickname;
    private String channel = "#advicenext";

    public IRCChat() {}

    public void connect(String server, int port, String nickname) {
        this.server = server;
        this.port = port;
        this.nickname = nickname;

        running = true;
        readThread = new Thread(this::readLoop, "IRC-Reader");
        readThread.setDaemon(true);
        readThread.start();
    }

    private void readLoop() {
        try {
            socket = new Socket(server, port);
            socket.setSoTimeout(30000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            sendRaw("NICK " + nickname);
            sendRaw("USER " + nickname + " 0 * :AdviceNext Client");

            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (Exception e) {
            if (running) {
                notifySystem("Connection error: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleLine(String line) {
        if (line.startsWith("PING")) {
            sendRaw("PONG " + line.substring(5));
            return;
        }

        if (line.contains("376") || line.contains("422")) {
            sendRaw("JOIN " + channel);
            notifySystem("Connected to " + server + " as " + nickname);
            return;
        }

        if (line.contains("433")) {
            nickname = nickname + "_";
            sendRaw("NICK " + nickname);
            notifySystem("Nickname taken, changed to " + nickname);
            return;
        }

        if (line.contains("PRIVMSG")) {
            int senderEnd = line.indexOf('!');
            int msgStart = line.indexOf("PRIVMSG") + 8;
            int msgIdx = line.indexOf(':', msgStart);

            if (senderEnd > 1 && msgIdx > 0) {
                String sender = line.substring(1, senderEnd);
                String target = line.substring(msgStart, msgIdx).trim();
                String message = line.substring(msgIdx + 1);

                if (target.equalsIgnoreCase(nickname)) {
                    notifyMessage("[PM] " + sender + ": " + message);
                } else {
                    notifyMessage(sender + ": " + message);
                }
            }
        }
    }

    public void sendMessage(String message) {
        if (writer == null) return;
        try {
            sendRaw("PRIVMSG " + channel + " :" + message);
            notifyMessage("[" + nickname + "] " + message);
        } catch (Exception e) {
            notifySystem("Send error: " + e.getMessage());
        }
    }

    public void sendPrivateMessage(String target, String message) {
        if (writer == null) return;
        try {
            sendRaw("PRIVMSG " + target + " :" + message);
        } catch (Exception e) {
            notifySystem("PM error: " + e.getMessage());
        }
    }

    private void sendRaw(String line) {
        if (writer == null) return;
        try {
            writer.write(line + "\r\n");
            writer.flush();
        } catch (IOException ignored) {}
    }

    public void setChannel(String channel) {
        if (this.channel != null && writer != null) {
            sendRaw("PART " + this.channel);
        }
        this.channel = channel;
        if (writer != null) {
            sendRaw("JOIN " + channel);
        }
    }

    public void setNickname(String nickname) {
        if (writer != null) {
            sendRaw("NICK " + nickname);
        }
        this.nickname = nickname;
    }

    public void onMessage(Consumer<String> listener) {
        messageListeners.add(listener);
    }

    public void onSystem(Consumer<String> listener) {
        systemListeners.add(listener);
    }

    private void notifyMessage(String msg) {
        for (Consumer<String> l : messageListeners) {
            l.accept(msg);
        }
    }

    private void notifySystem(String msg) {
        for (Consumer<String> l : systemListeners) {
            l.accept(msg);
        }
    }

    public void disconnect() {
        running = false;
        if (writer != null) {
            try {
                sendRaw("QUIT :AdviceNext Client");
            } catch (Exception ignored) {}
        }
        cleanup();
    }

    private void cleanup() {
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        try { if (writer != null) writer.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        reader = null;
        writer = null;
        socket = null;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getServer() { return server; }
    public String getNickname() { return nickname; }
    public String getChannel() { return channel; }
}