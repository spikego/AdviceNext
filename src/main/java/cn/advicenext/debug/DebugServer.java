package cn.advicenext.debug;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.features.value.slider.NumberSetting;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DebugServer {
    private static final DebugServer INSTANCE = new DebugServer();
    private HttpServer server;
    private final int port = 8080;
    private final List<String> logMessages = new CopyOnWriteArrayList<>();
    private final List<String> chatMessages = new CopyOnWriteArrayList<>();
    private boolean isRunning = false;
    private boolean[] movementStates = new boolean[6];

    private DebugServer() {}

    public static DebugServer getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (isRunning) return;

        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/api/modules", new ModulesApiHandler());
            server.createContext("/api/settings", new SettingsApiHandler());
            server.createContext("/api/logs", new LogsApiHandler());
            server.createContext("/api/system", new SystemInfoHandler());
            server.createContext("/api/players", new PlayersApiHandler());
            server.createContext("/api/chat", new ChatApiHandler());
            server.createContext("/api/movement", new MovementApiHandler());
            server.createContext("/api/screenshot", new ScreenshotApiHandler());
            server.createContext("/api/disconnect", new DisconnectApiHandler());
            server.createContext("/api/exit", new ExitApiHandler());
            server.createContext("/api/command", new CommandApiHandler());
            server.createContext("/api/configs", new ConfigsApiHandler());
            server.createContext("/api/config/save", new SaveConfigApiHandler());
            server.createContext("/api/config/load", new LoadConfigApiHandler());
            server.setExecutor(null);
            server.start();

            String localIp = getLocalIpAddress();
            isRunning = true;
            log("Debug server started on http://" + localIp + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocalIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    public void stop() {
        if (!isRunning) return;
        server.stop(0);
        isRunning = false;
        log("Debug server stopped");
    }

    public void log(String message) {
        logMessages.add("[" + System.currentTimeMillis() + "] " + message);
        if (logMessages.size() > 1000) {
            logMessages.remove(0);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
    
    public void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 100) {
            chatMessages.remove(0);
        }
    }
    
    public void tickMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        
        if (movementStates[0]) mc.options.forwardKey.setPressed(true);
        if (movementStates[1]) mc.options.backKey.setPressed(true);
        if (movementStates[2]) mc.options.leftKey.setPressed(true);
        if (movementStates[3]) mc.options.rightKey.setPressed(true);
        if (movementStates[4]) mc.options.jumpKey.setPressed(true);
        if (movementStates[5]) mc.options.sneakKey.setPressed(true);
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/") || path.equals("/index.html")) {
                serveStaticFile(exchange, "index.html", "text/html");
            } else if (path.equals("/style.css")) {
                serveStaticFile(exchange, "style.css", "text/css");
            } else if (path.equals("/script.js")) {
                serveStaticFile(exchange, "script.js", "application/javascript");
            } else {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        }
    }

    private class ModulesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, getModulesJson());
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String moduleName = "";
                boolean enabled = false;

                if (body.startsWith("{")) {
                    try {
                        java.util.regex.Pattern nameRegex = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                        java.util.regex.Pattern enabledRegex = java.util.regex.Pattern.compile("\"enabled\"\\s*:\\s*(true|false)");

                        java.util.regex.Matcher nameMatcher = nameRegex.matcher(body);
                        java.util.regex.Matcher enabledMatcher = enabledRegex.matcher(body);

                        if (nameMatcher.find()) {
                            moduleName = nameMatcher.group(1);
                        }
                        if (enabledMatcher.find()) {
                            enabled = Boolean.parseBoolean(enabledMatcher.group(1));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }

                if (!moduleName.isEmpty()) {
                    toggleModule(moduleName, enabled);
                    sendJsonResponse(exchange, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, "{\"success\":false}");
                }
            }
        }
    }

    private class SettingsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("module=")) {
                    String moduleName = query.substring(7);
                    sendJsonResponse(exchange, getSettingsJson(moduleName));
                } else {
                    sendJsonResponse(exchange, "{\"error\":\"No module specified\"}");
                }
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                updateSetting(body);
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }

    private class LogsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, getLogsJson());
        }
    }

    private class SystemInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            MinecraftClient mc = MinecraftClient.getInstance();
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"fps\":").append(mc.getCurrentFps()).append(",");
            json.append("\"memory\":").append(Runtime.getRuntime().totalMemory() / 1024 / 1024).append(",");
            if (mc.player != null) {
                json.append("\"x\":").append((int)mc.player.getX()).append(",");
                json.append("\"y\":").append((int)mc.player.getY()).append(",");
                json.append("\"z\":").append((int)mc.player.getZ());
            } else {
                json.append("\"x\":0,\"y\":0,\"z\":0");
            }
            json.append("}");
            sendJsonResponse(exchange, json.toString());
        }
    }

    private void serveStaticFile(HttpExchange exchange, String filename, String contentType) throws IOException {
        try {
            String content = loadWebResource(filename);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            sendResponse(exchange, content);
        } catch (Exception e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
        }
    }
    
    private String loadWebResource(String filename) throws IOException {
        try (var stream = getClass().getResourceAsStream("/assets/advicenext/web/" + filename)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + filename);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendResponse(exchange, json);
    }
    
    private String getModulesJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"modules\":[");
        boolean first = true;
        for (Module module : ModuleManager.getModules()) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"name\":\"").append(module.getName()).append("\",");
            json.append("\"enabled\":").append(module.getEnabled()).append(",");
            json.append("\"hasSettings\":").append(!module.settings.isEmpty());
            json.append("}");
            first = false;
        }
        json.append("]}");
        return json.toString();
    }
    
    private String getLogsJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"logs\":[");
        boolean first = true;
        for (String log : logMessages) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"message\":\"").append(log.replace("\"", "\\\"")).append("\",");
            json.append("\"type\":\"info\"}");
            first = false;
        }
        json.append("]}");
        return json.toString();
    }
    
    private String getSettingsJson(String moduleName) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getName().equalsIgnoreCase(moduleName)) {
                StringBuilder json = new StringBuilder();
                json.append("{\"settings\":[");
                boolean first = true;
                for (AbstractSetting<?> setting : module.settings) {
                    if (!first) json.append(",");
                    json.append("{");
                    json.append("\"name\":\"").append(setting.getName()).append("\",");
                    
                    String type = "String";
                    String value = "";
                    
                    if (setting instanceof BooleanSetting) {
                        type = "Boolean";
                        value = String.valueOf(((BooleanSetting) setting).getValue());
                    } else if (setting instanceof ModeSetting) {
                        type = "Mode";
                        value = ((ModeSetting) setting).getValue();
                    } else if (setting instanceof NumberSetting) {
                        type = "Number";
                        value = ((NumberSetting<?>) setting).getValue().toString();
                    } else if (setting instanceof StringSetting) {
                        type = "String";
                        value = ((StringSetting) setting).getValue();
                    }
                    
                    json.append("\"type\":\"").append(type).append("\",");
                    json.append("\"value\":\"").append(value).append("\"");
                    json.append("}");
                    first = false;
                }
                json.append("]}");
                return json.toString();
            }
        }
        return "{\"settings\":[]}"; 
    }
    
    private void updateSetting(String body) {
        try {
            java.util.regex.Pattern moduleRegex = java.util.regex.Pattern.compile("\"module\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Pattern settingRegex = java.util.regex.Pattern.compile("\"setting\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Pattern valueRegex = java.util.regex.Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
            
            java.util.regex.Matcher moduleMatcher = moduleRegex.matcher(body);
            java.util.regex.Matcher settingMatcher = settingRegex.matcher(body);
            java.util.regex.Matcher valueMatcher = valueRegex.matcher(body);
            
            if (moduleMatcher.find() && settingMatcher.find() && valueMatcher.find()) {
                String moduleName = moduleMatcher.group(1);
                String settingName = settingMatcher.group(1);
                String value = valueMatcher.group(1);
                
                for (Module module : ModuleManager.getModules()) {
                    if (module.getName().equalsIgnoreCase(moduleName)) {
                        for (AbstractSetting<?> setting : module.settings) {
                            if (setting.getName().equalsIgnoreCase(settingName)) {
                                try {
                                    if (setting instanceof BooleanSetting) {
                                        ((BooleanSetting) setting).setValue(Boolean.parseBoolean(value));
                                    } else if (setting instanceof ModeSetting) {
                                        ((ModeSetting) setting).setValue(value);
                                    } else if (setting instanceof NumberSetting) {
                                        if (setting instanceof IntSetting) {
                                            ((IntSetting) setting).setValue(Integer.parseInt(value));
                                        } else if (setting instanceof DoubleSetting) {
                                            ((DoubleSetting) setting).setValue(Double.parseDouble(value));
                                        }
                                    } else if (setting instanceof StringSetting) {
                                        ((StringSetting) setting).setValue(value);
                                    }
                                    log("Updated setting: " + moduleName + "." + settingName + " = " + value);
                                } catch (Exception e) {
                                    log("Failed to update setting: " + e.getMessage());
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log("Failed to update setting: " + e.getMessage());
        }
    }
    
    private void toggleModule(String name, boolean enabled) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getName().equalsIgnoreCase(name)) {
                if (module.getEnabled() != enabled) {
                    module.toggle();
                }
                break;
            }
        }
    }
    
    private class PlayersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            MinecraftClient mc = MinecraftClient.getInstance();
            StringBuilder json = new StringBuilder();
            json.append("{\"players\":[");
            
            boolean first = true;
            if (mc.world != null && mc.player != null) {
                json.append("{");
                json.append("\"name\":\"You\",");
                json.append("\"health\":").append((int)mc.player.getHealth()).append(",");
                json.append("\"distance\":0,");
                json.append("\"x\":").append((int)mc.player.getX()).append(",");
                json.append("\"y\":").append((int)mc.player.getY()).append(",");
                json.append("\"z\":").append((int)mc.player.getZ());
                json.append("}");
                first = false;
                
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player != mc.player) {
                        if (!first) json.append(",");
                        json.append("{");
                        json.append("\"name\":\"").append(player.getName().getString()).append("\",");
                        json.append("\"health\":").append((int)player.getHealth()).append(",");
                        json.append("\"distance\":").append((int)mc.player.distanceTo(player)).append(",");
                        json.append("\"x\":").append((int)player.getX()).append(",");
                        json.append("\"y\":").append((int)player.getY()).append(",");
                        json.append("\"z\":").append((int)player.getZ());
                        json.append("}");
                        first = false;
                    }
                }
            }
            
            json.append("]}");
            sendJsonResponse(exchange, json.toString());
        }
    }
    
    private class ChatApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder();
                json.append("{\"messages\":[");
                boolean first = true;
                for (String msg : chatMessages) {
                    if (!first) json.append(",");
                    json.append("{");
                    json.append("\"text\":\"").append(msg.replace("\"", "\\\"")).append("\"");
                    json.append("}");
                    first = false;
                }
                json.append("]}");
                sendJsonResponse(exchange, json.toString());
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String message = extractJsonValue(body, "message");
                if (!message.isEmpty()) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        mc.player.networkHandler.sendChatMessage(message);
                    }
                }
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private class MovementApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String action = extractJsonValue(body, "action");
                String state = extractJsonValue(body, "state");
                
                boolean pressed = "start".equals(state);
                
                switch (action) {
                    case "forward": movementStates[0] = pressed; break;
                    case "back": movementStates[1] = pressed; break;
                    case "left": movementStates[2] = pressed; break;
                    case "right": movementStates[3] = pressed; break;
                    case "jump": movementStates[4] = pressed; break;
                    case "sneak": movementStates[5] = pressed; break;
                }
                
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private class ScreenshotApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                MinecraftClient mc = MinecraftClient.getInstance();
                try {
                    ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(),
                        (text) -> log("Screenshot: " + text.getString()));
                    sendJsonResponse(exchange, "{\"success\":true}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, "{\"success\":false}");
                }
            }
        }
    }
    
    private class DisconnectApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world != null) {
                    mc.world.disconnect();
                }
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private class ExitApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, "{\"success\":true}");
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        MinecraftClient.getInstance().scheduleStop();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    private class CommandApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String command = extractJsonValue(body, "command");
                if (!command.isEmpty()) {
                    log("Executing command: " + command);
                }
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private class ConfigsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{\"configs\":[");
            json.append("{\"name\":\"Default\"}");
            json.append("]}");
            sendJsonResponse(exchange, json.toString());
        }
    }
    
    private class SaveConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String name = extractJsonValue(body, "name");
                if (!name.isEmpty()) {
                    ConfigManager.getInstance().saveConfig(name);
                    log("Config saved: " + name);
                }
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private class LoadConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String name = extractJsonValue(body, "name");
                if (!name.isEmpty()) {
                    ConfigManager.getInstance().loadConfig(name);
                    log("Config loaded: " + name);
                }
                sendJsonResponse(exchange, "{\"success\":true}");
            }
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }
}