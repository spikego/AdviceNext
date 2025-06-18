package cn.spikego.advicenext.features.command.impl;

import cn.spikego.advicenext.debug.DebugServer;
import cn.spikego.advicenext.features.command.Command;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import static cn.spikego.advicenext.features.command.CommandManager.addMessage;

public class DebugCommand extends Command {

    public DebugCommand() {
        super("debug", "Manage debug server", new String[]{"debug <start|stop|status>"});
    }

    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            addMessage("Usage: .debug <start|stop|status>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                startDebugServer();
                break;

            case "stop":
                stopDebugServer();
                break;

            case "status":
                showStatus();
                break;

            default:
                addMessage("Unknown action: " + args[0]);
                addMessage("Usage: .debug <start|stop|status>");
                break;
        }
    }

    private void startDebugServer() {
        if (DebugServer.getInstance().isRunning()) {
            showAccessInstructions();
            return;
        }

        DebugServer.getInstance().start();
        showAccessInstructions();
    }

    private void stopDebugServer() {
        if (!DebugServer.getInstance().isRunning()) {
            addMessage("Debug server is not running");
            return;
        }

        DebugServer.getInstance().stop();
        addMessage("Debug server stopped");
    }

    private void showStatus() {
        if (DebugServer.getInstance().isRunning()) {
            showAccessInstructions();
        } else {
            addMessage("Debug server is not running");
        }
    }

    private void showAccessInstructions() {
        addMessage("§a§lDebug server is running!");
        addMessage("§7Local access: §f§nhttp://127.0.0.1:8080");

        // 获取本地IP地址
        String localIp = getLocalIpAddress();
        if (localIp != null) {
            addMessage("§7Network access: §f§nhttp://" + localIp + ":8080");
            addMessage("§7Use the network address to access from other devices (phone, tablet, etc.)");
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 过滤掉回环接口、虚拟接口等
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    // 只返回IPv4地址
                    if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }

        // 如果无法获取IP，尝试使用简单方法
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }
}
