package cn.advicenext.debug;

import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.FloatSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.features.value.slider.NumberSetting;
import cn.advicenext.script.Script;
import cn.advicenext.script.ScriptManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

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
    private boolean isRunning = false;
    private LogAppender logAppender;

    private DebugServer() {}

    public static DebugServer getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (isRunning) return;

        try {
            // 将 "127.0.0.1" 改为 "0.0.0.0" 以允许局域网访问
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/api/modules", new ModulesApiHandler());
            server.createContext("/api/settings", new SettingsApiHandler());
            server.createContext("/api/logs", new LogsApiHandler());
            server.createContext("/api/config", new ConfigHandler());
            server.createContext("/api/scripts", new ScriptsHandler());
            server.createContext("/api/shutdown", new ShutdownHandler());
            server.setExecutor(null);
            server.start();

            // 设置日志捕获
            setupLogCapture();

            // 获取本机IP地址，方便用户访问
            String localIp = getLocalIpAddress();
            isRunning = true;
            log("Debug server started on http://" + localIp + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取本机IP地址的辅助方法
    private String getLocalIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "0.0.0.0"; // 如果无法获取IP，返回通配地址
        }
    }


    public void stop() {
        if (!isRunning) return;

        // 移除日志捕获
        removeLogCapture();

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

    private void setupLogCapture() {
        logAppender = new LogAppender();
        ((Logger) LogManager.getRootLogger()).addAppender(logAppender);
    }

    private void removeLogCapture() {
        if (logAppender != null) {
            ((Logger) LogManager.getRootLogger()).removeAppender(logAppender);
        }
    }

    private class LogAppender extends AbstractAppender {
        public LogAppender() {
            super("DebugAppender", null, null);
            start();
        }

        @Override
        public void append(LogEvent event) {
            log("[" + event.getLevel() + "] " + event.getMessage().getFormattedMessage());
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getHtmlTemplate();
            sendResponse(exchange, html);
        }
    }

    private class ModulesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 处理 OPTIONS 预检请求
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                sendJsonResponse(exchange, "");
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, getModulesJson());
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                // 支持多种格式的请求体
                String moduleName = "";
                boolean enabled = false;

                // 尝试解析 JSON 格式
                if (body.startsWith("{")) {
                    try {
                        // 简单的 JSON 解析
                        String namePattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
                        String enabledPattern = "\"enabled\"\\s*:\\s*(true|false)";

                        java.util.regex.Pattern nameRegex = java.util.regex.Pattern.compile(namePattern);
                        java.util.regex.Pattern enabledRegex = java.util.regex.Pattern.compile(enabledPattern);

                        java.util.regex.Matcher nameMatcher = nameRegex.matcher(body);
                        java.util.regex.Matcher enabledMatcher = enabledRegex.matcher(body);

                        if (nameMatcher.find()) {
                            moduleName = nameMatcher.group(1);
                        }

                        if (enabledMatcher.find()) {
                            enabled = Boolean.parseBoolean(enabledMatcher.group(1));
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
                // 尝试解析表单格式
                else if (body.contains("=")) {
                    String[] parts = body.split("=");
                    if (parts.length == 2) {
                        moduleName = parts[0];
                        enabled = Boolean.parseBoolean(parts[1]);
                    }
                }

                if (!moduleName.isEmpty()) {
                    toggleModule(moduleName, enabled);
                    sendJsonResponse(exchange, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Invalid request format\"}");
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
                }
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String[] parts = body.split("&");
                if (parts.length >= 3) {
                    String moduleName = parts[0].split("=")[1];
                    String settingName = parts[1].split("=")[1];
                    String value = parts[2].split("=")[1];
                    updateSetting(moduleName, settingName, value);
                    sendJsonResponse(exchange, "{\"success\":true}");
                }
            }
        }
    }

    private class LogsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, getLogsJson());
        }
    }

    private class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if ("action=save".equals(query)) {
                ConfigManager.getInstance().saveConfig();
                sendJsonResponse(exchange, "{\"success\":true,\"message\":\"Config saved\"}");
            } else if ("action=load".equals(query)) {
                ConfigManager.getInstance().loadConfig();
                sendJsonResponse(exchange, "{\"success\":true,\"message\":\"Config loaded\"}");
            }
        }
    }

    private class ScriptsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, getScriptsJson());
            } else {
                String query = exchange.getRequestURI().getQuery();
                if ("action=reload".equals(query)) {
                    ScriptManager.getInstance().loadScripts();
                    sendJsonResponse(exchange, "{\"success\":true,\"message\":\"Scripts reloaded\"}");
                }
            }
        }
    }

    private class ShutdownHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, "{\"success\":true,\"message\":\"Shutting down...\"}");
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    MinecraftClient.getInstance().stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        // 添加 CORS 头，允许跨域请求
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        // 处理 OPTIONS 预检请求
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        // 添加 CORS 头，允许跨域请求
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        // 处理 OPTIONS 预检请求
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes());
        }
    }


    private String getModulesJson() {
        StringBuilder json = new StringBuilder("{\"modules\":[");
        boolean first = true;

        for (Module module : ModuleManager.getModules()) {
            if (!first) json.append(",");
            first = false;

            json.append("{\"name\":\"").append(module.getName()).append("\",");
            json.append("\"enabled\":").append(module.getEnabled()).append("}");
        }

        return json.append("]}").toString();
    }

    private String getSettingsJson(String moduleName) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getName().equals(moduleName)) {
                StringBuilder json = new StringBuilder("{\"settings\":[");
                boolean first = true;

                for (AbstractSetting<?> setting : module.settings) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{\"name\":\"").append(setting.getName()).append("\",");

                    if (setting instanceof BooleanSetting) {
                        json.append("\"type\":\"boolean\",");
                        json.append("\"value\":").append(((BooleanSetting) setting).getValue());
                    } else if (setting instanceof NumberSetting) {
                        json.append("\"type\":\"number\",");
                        json.append("\"value\":").append(((NumberSetting<?>) setting).getValue());
                        json.append(",\"min\":").append(((NumberSetting<?>) setting).getMin());
                        json.append(",\"max\":").append(((NumberSetting<?>) setting).getMax());
                    } else if (setting instanceof ModeSetting) {
                        json.append("\"type\":\"mode\",");
                        json.append("\"value\":\"").append(((ModeSetting) setting).getValue()).append("\",");
                        json.append("\"modes\":[");

                        String[] modes = ((ModeSetting) setting).getModes().toArray(new String[0]);
                        for (int i = 0; i < modes.length; i++) {
                            if (i > 0) json.append(",");
                            json.append("\"").append(modes[i]).append("\"");
                        }

                        json.append("]");
                    } else if (setting instanceof StringSetting) {
                        json.append("\"type\":\"string\",");
                        json.append("\"value\":\"").append(((StringSetting) setting).getValue()).append("\"");
                    }

                    json.append("}");
                }

                return json.append("]}").toString();
            }
        }

        return "{\"settings\":[]}";
    }

    private String getLogsJson() {
        StringBuilder json = new StringBuilder("{\"logs\":[");
        boolean first = true;

        for (String log : logMessages) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(log.replace("\"", "\\\"")).append("\"");
        }

        return json.append("]}").toString();
    }

    private String getScriptsJson() {
        StringBuilder json = new StringBuilder("{\"scripts\":[");
        boolean first = true;

        try {
            // 直接从 ScriptManager 获取脚本列表
            List<Script> scripts = ScriptManager.getInstance().getLoadedScripts();

            for (Script script : scripts) {
                if (!first) json.append(",");
                first = false;

                json.append("{");
                json.append("\"name\":\"").append(script.getName()).append("\",");
                json.append("\"author\":\"").append(script.getAuthor()).append("\",");
                json.append("\"version\":\"").append(script.getVersion()).append("\",");
                json.append("\"description\":\"").append(script.getDescription()).append("\"");
                json.append("}");
            }
        } catch (Exception e) {
            // 如果出现异常，返回空列表
            log("Error getting scripts: " + e.getMessage());
        }

        return json.append("]}").toString();
    }


    private void toggleModule(String name, boolean enabled) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getName().equals(name)) {
                if (module.getEnabled() != enabled) {
                    module.toggle();
                }
                break;
            }
        }
    }

    private void updateSetting(String moduleName, String settingName, String value) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getName().equals(moduleName)) {
                for (AbstractSetting<?> setting : module.settings) {
                    if (setting.getName().equals(settingName)) {
                        if (setting instanceof BooleanSetting) {
                            ((BooleanSetting) setting).setValue(Boolean.parseBoolean(value));
                        } else if (setting instanceof NumberSetting) {
                            try {
                                if (setting instanceof IntSetting) {
                                    ((IntSetting) setting).setValue(Integer.parseInt(value));
                                } else if (setting instanceof FloatSetting) {
                                    ((FloatSetting) setting).setValue(Float.parseFloat(value));
                                } else if (setting instanceof DoubleSetting) {
                                    ((DoubleSetting) setting).setValue(Double.parseDouble(value));
                                }
                            } catch (NumberFormatException e) {
                                // 忽略无效的数字
                            }
                        } else if (setting instanceof ModeSetting) {
                            ((ModeSetting) setting).setValue(value);
                        } else if (setting instanceof StringSetting) {
                            ((StringSetting) setting).setValue(value);
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private String getHtmlTemplate() {
        return "<!DOCTYPE html><html><head><title>AdviceNext Debug</title>" +
                "<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body{font-family:Arial,sans-serif;margin:0;padding:0;background:#1e1e1e;color:#fff}" +
                ".container{max-width:1200px;margin:0 auto;padding:20px}" +
                ".header{background:#222;padding:15px;border-bottom:2px solid #0078d7}" +
                ".title{margin:0;color:#0078d7}" +
                ".tabs{display:flex;margin:20px 0;border-bottom:1px solid #444}" +
                ".tab{padding:10px 20px;cursor:pointer;color:#aaa}" +
                ".tab.active{color:#0078d7;border-bottom:2px solid #0078d7}" +
                ".tab-content{display:none}" +
                ".tab-content.active{display:block}" +
                ".card{background:#252525;border-radius:5px;padding:15px;margin-bottom:20px}" +
                ".card-header{font-size:18px;font-weight:bold;margin-bottom:15px;color:#0078d7}" +
                ".module{background:#333;padding:10px;margin:5px 0;border-radius:3px;display:flex;justify-content:space-between}" +
                ".module-enabled{border-left:3px solid #0f0}" +
                ".module-disabled{border-left:3px solid #f00}" +
                ".switch{position:relative;display:inline-block;width:60px;height:30px}" +
                ".switch input{opacity:0;width:0;height:0}" +
                ".slider{position:absolute;cursor:pointer;top:0;left:0;right:0;bottom:0;background-color:#444;border-radius:34px}" +
                ".slider:before{position:absolute;content:\"\";height:22px;width:22px;left:4px;bottom:4px;background-color:white;border-radius:50%}" +
                "input:checked+.slider{background-color:#0078d7}" +
                "input:checked+.slider:before{transform:translateX(30px)}" +
                ".logs{height:400px;overflow-y:auto;background:#1a1a1a;padding:10px;font-family:monospace}" +
                ".log-entry{margin:5px 0;border-bottom:1px solid #333;padding-bottom:5px}" +
                ".setting{background:#333;padding:15px;margin:10px 0;border-radius:3px}" +
                ".setting-name{font-weight:bold;margin-bottom:10px}" +
                ".btn{background:#0078d7;color:#fff;border:none;padding:10px 15px;border-radius:3px;cursor:pointer}" +
                ".btn:hover{background:#0069c0}" +
                ".btn-danger{background:#d9534f}" +
                ".btn-danger:hover{background:#c9302c}" +
                "input[type=text],input[type=number],select{background:#333;border:1px solid #555;color:#fff;padding:8px;border-radius:3px;width:100%}" +
                "</style>" +
                "</head><body>" +
                "<div class=\"header\"><h1 class=\"title\">AdviceNext Debug Dashboard</h1></div>" +
                "<div class=\"container\">" +
                "<div class=\"tabs\">" +
                "<div class=\"tab active\" data-tab=\"modules\">Modules</div>" +
                "<div class=\"tab\" data-tab=\"settings\">Settings</div>" +
                "<div class=\"tab\" data-tab=\"logs\">Logs</div>" +
                "<div class=\"tab\" data-tab=\"scripts\">Scripts</div>" +
                "<div class=\"tab\" data-tab=\"actions\">Actions</div>" +
                "</div>" +
                "<div id=\"modules\" class=\"tab-content active\">" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Modules</div>" +
                "<input type=\"text\" id=\"moduleSearch\" placeholder=\"Search modules...\" style=\"margin-bottom:10px\">" +
                "<div id=\"modules-list\"></div>" +
                "</div>" +
                "</div>" +
                "<div id=\"settings\" class=\"tab-content\">" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Module Settings</div>" +
                "<select id=\"moduleSelect\" style=\"margin-bottom:15px\">" +
                "<option value=\"\">Select a module</option>" +
                "</select>" +
                "<div id=\"settings-list\"></div>" +
                "</div>" +
                "</div>" +
                "<div id=\"logs\" class=\"tab-content\">" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Console Logs</div>" +
                "<div class=\"logs\" id=\"log-container\"></div>" +
                "</div>" +
                "</div>" +
                "<div id=\"scripts\" class=\"tab-content\">" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Lua Scripts</div>" +
                "<button class=\"btn\" onclick=\"reloadScripts()\">Reload Scripts</button>" +
                "<div id=\"scripts-list\" style=\"margin-top:15px\"></div>" +
                "</div>" +
                "</div>" +
                "<div id=\"actions\" class=\"tab-content\">" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Configuration</div>" +
                "<button class=\"btn\" onclick=\"saveConfig()\">Save Config</button> " +
                "<button class=\"btn\" onclick=\"loadConfig()\">Load Config</button>" +
                "</div>" +
                "<div class=\"card\">" +
                "<div class=\"card-header\">Game Control</div>" +
                "<button class=\"btn btn-danger\" onclick=\"shutdownGame()\">Shutdown Game</button>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<script>" +
                "// Tab switching" +
                "document.querySelectorAll('.tab').forEach(tab => {" +
                "  tab.addEventListener('click', () => {" +
                "    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));" +
                "    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));" +
                "    tab.classList.add('active');" +
                "    document.getElementById(tab.dataset.tab).classList.add('active');" +
                "  });" +
                "});" +

                "// Load modules" +
                "function loadModules() {" +
                "  fetch('/api/modules').then(r => r.json()).then(data => {" +
                "    const list = document.getElementById('modules-list');" +
                "    list.innerHTML = '';" +
                "    const select = document.getElementById('moduleSelect');" +
                "    select.innerHTML = '<option value=\"\">Select a module</option>';" +
                "    data.modules.forEach(m => {" +
                "      const div = document.createElement('div');" +
                "      div.className = `module ${m.enabled ? 'module-enabled' : 'module-disabled'}`;" +
                "      div.innerHTML = `<div>${m.name}</div><label class=\"switch\"><input type=\"checkbox\" ${m.enabled ? 'checked' : ''} " +
                "        onchange=\"toggleModule('${m.name}', this.checked)\"><span class=\"slider\"></span></label>`;" +
                "      list.appendChild(div);" +
                "      const opt = document.createElement('option');" +
                "      opt.value = m.name;" +
                "      opt.textContent = m.name;" +
                "      select.appendChild(opt);" +
                "    });" +
                "  });" +
                "}" +

                "// Toggle module" +
                "function toggleModule(name, enabled) {" +
                "  fetch('/api/modules', {" +
                "    method: 'POST'," +
                "    body: `${name}=${enabled}`" +
                "  }).then(() => loadModules());" +
                "}" +

                "// Load settings" +
                "document.getElementById('moduleSelect').addEventListener('change', function() {" +
                "  if (this.value) {" +
                "    fetch(`/api/settings?module=${this.value}`).then(r => r.json()).then(data => {" +
                "      const list = document.getElementById('settings-list');" +
                "      list.innerHTML = '';" +
                "      data.settings.forEach(s => {" +
                "        const div = document.createElement('div');" +
                "        div.className = 'setting';" +
                "        div.innerHTML = `<div class=\"setting-name\">${s.name}</div>`;" +
                "        if (s.type === 'boolean') {" +
                "          div.innerHTML += `<label class=\"switch\"><input type=\"checkbox\" ${s.value ? 'checked' : ''} " +
                "            onchange=\"updateSetting('${this.value}', '${s.name}', this.checked)\"><span class=\"slider\"></span></label>`;" +
                "        } else if (s.type === 'number') {" +
                "          div.innerHTML += `<input type=\"number\" value=\"${s.value}\" min=\"${s.min}\" max=\"${s.max}\" " +
                "            onchange=\"updateSetting('${this.value}', '${s.name}', this.value)\">`;" +
                "        } else if (s.type === 'mode') {" +
                "          let select = `<select onchange=\"updateSetting('${this.value}', '${s.name}', this.value)\">`;" +
                "          s.modes.forEach(mode => {" +
                "            select += `<option value=\"${mode}\" ${mode === s.value ? 'selected' : ''}>${mode}</option>`;" +
                "          });" +
                "          select += '</select>';" +
                "          div.innerHTML += select;" +
                "        } else if (s.type === 'string') {" +
                "          div.innerHTML += `<input type=\"text\" value=\"${s.value}\" " +
                "            onchange=\"updateSetting('${this.value}', '${s.name}', this.value)\">`;" +
                "        }" +
                "        list.appendChild(div);" +
                "      });" +
                "    });" +
                "  }" +
                "});" +

                "// Update setting" +
                "function updateSetting(module, setting, value) {" +
                "  fetch('/api/settings', {" +
                "    method: 'POST'," +
                "    body: `module=${module}&setting=${setting}&value=${value}`" +
                "  });" +
                "}" +

                "// Load logs" +
                "function loadLogs() {" +
                "  fetch('/api/logs').then(r => r.json()).then(data => {" +
                "    const container = document.getElementById('log-container');" +
                "    container.innerHTML = '';" +
                "    data.logs.forEach(log => {" +
                "      const div = document.createElement('div');" +
                "      div.className = 'log-entry';" +
                "      div.textContent = log;" +
                "      container.appendChild(div);" +
                "    });" +
                "    container.scrollTop = container.scrollHeight;" +
                "  });" +
                "}" +

                "// Load scripts" +
                "function loadScripts() {" +
                "  fetch('/api/scripts').then(r => r.json()).then(data => {" +
                "    const list = document.getElementById('scripts-list');" +
                "    list.innerHTML = '';" +
                "    data.scripts.forEach(s => {" +
                "      const div = document.createElement('div');" +
                "      div.className = 'module';" +
                "      div.innerHTML = `<div>${s.name} v${s.version} by ${s.author}</div>`;" +
                "      list.appendChild(div);" +
                "    });" +
                "    if (data.scripts.length === 0) {" +
                "      list.innerHTML = '<p>No scripts loaded</p>';" +
                "    }" +
                "  });" +
                "}" +

                "// Reload scripts" +
                "function reloadScripts() {" +
                "  fetch('/api/scripts?action=reload', {method: 'POST'}).then(() => loadScripts());" +
                "}" +

                "// Save config" +
                "function saveConfig() {" +
                "  fetch('/api/config?action=save', {method: 'POST'});" +
                "}" +

                "// Load config" +
                "function loadConfig() {" +
                "  fetch('/api/config?action=load', {method: 'POST'}).then(() => {" +
                "    loadModules();" +
                "    document.getElementById('moduleSelect').value = '';" +
                "    document.getElementById('settings-list').innerHTML = '';" +
                "  });" +
                "}" +

                "// Shutdown game" +
                "function shutdownGame() {" +
                "  if (confirm('Are you sure you want to shutdown the game?')) {" +
                "    fetch('/api/shutdown');" +
                "  }" +
                "}" +

                "// Module search" +
                "document.getElementById('moduleSearch').addEventListener('input', function() {" +
                "  const term = this.value.toLowerCase();" +
                "  document.querySelectorAll('#modules-list .module').forEach(m => {" +
                "    const name = m.querySelector('div').textContent.toLowerCase();" +
                "    m.style.display = name.includes(term) ? '' : 'none';" +
                "  });" +
                "});" +

                "// Initial load" +
                "loadModules();" +
                "loadLogs();" +
                "loadScripts();" +

                "// Refresh data" +
                "setInterval(loadLogs, 2000);" +
                "setInterval(loadModules, 5000);" +
                "</script>" +
                "</body></html>";
    }
}
