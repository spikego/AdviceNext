package cn.spikego.advicenext.script;

import cn.spikego.advicenext.event.EventBus;
import cn.spikego.advicenext.event.impl.TickEvent;
import cn.spikego.advicenext.features.command.CommandManager;
import cn.spikego.advicenext.features.module.ModuleManager;
import cn.spikego.advicenext.features.notification.NotificationManager;
import cn.spikego.advicenext.features.notification.NotificationManager.NotificationType;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptManager {
    private static final ScriptManager INSTANCE = new ScriptManager();
    private final Path scriptsDir = Paths.get(System.getProperty("user.home"), ".advicenext", "scripts");
    private final List<Script> loadedScripts = new ArrayList<>();
    private final Map<String, LuaValue> eventCallbacks = new HashMap<>();
    
    private ScriptManager() {
        // 创建脚本目录
        try {
            if (!Files.exists(scriptsDir)) {
                Files.createDirectories(scriptsDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 注册事件监听器
        EventBus.register(this);
    }
    
    public static ScriptManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 加载所有脚本
     */
    public void loadScripts() {
        // 清除已加载的脚本
        unloadScripts();
        
        // 获取所有 .lua 文件
        File[] luaFiles = scriptsDir.toFile().listFiles((dir, name) -> name.endsWith(".lua"));
        if (luaFiles == null) return;
        
        for (File luaFile : luaFiles) {
            try {
                loadScript(luaFile);
            } catch (Exception e) {
                e.printStackTrace();
                NotificationManager.getInstance().addNotification(
                    "Script Error", 
                    "Failed to load " + luaFile.getName() + ": " + e.getMessage(), 
                    NotificationType.ERROR, 
                    5000
                );
            }
        }
    }
    
    /**
     * 加载单个脚本
     */
    private void loadScript(File luaFile) throws IOException {
        // 创建 Lua 环境
        Globals globals = JsePlatform.standardGlobals();
        
        // 注入 API
        injectAPI(globals);
        
        // 加载脚本
        String scriptContent = new String(Files.readAllBytes(luaFile.toPath()));
        LuaValue chunk = globals.load(scriptContent, luaFile.getName());
        
        try {
            // 执行脚本
            chunk.call();
            
            // 获取脚本信息
            LuaValue scriptInfo = globals.get("scriptInfo");
            if (!scriptInfo.istable()) {
                throw new LuaError("Script must define scriptInfo table");
            }
            
            LuaTable infoTable = scriptInfo.checktable();
            String name = infoTable.get("name").checkjstring();
            String author = infoTable.get("author").checkjstring();
            String version = infoTable.get("version").checkjstring();
            String description = infoTable.get("description").checkjstring();
            
            // 创建脚本对象
            Script script = new Script(name, author, version, description, luaFile, globals);
            loadedScripts.add(script);
            
            // 调用 onEnable 函数
            LuaValue onEnable = globals.get("onEnable");
            if (onEnable.isfunction()) {
                onEnable.call();
            }
            
            // 注册事件回调
            registerEventCallbacks(script);
            
            NotificationManager.getInstance().addNotification(
                "Script Loaded", 
                name + " v" + version + " by " + author, 
                NotificationType.SUCCESS, 
                3000
            );
        } catch (LuaError e) {
            throw new IOException("Lua error: " + e.getMessage(), e);
        }
    }
    
    /**
     * 注册脚本的事件回调
     */
    private void registerEventCallbacks(Script script) {
        LuaValue onTick = script.getGlobals().get("onTick");
        if (onTick.isfunction()) {
            eventCallbacks.put("onTick_" + script.getName(), onTick);
        }
        
        // 可以添加更多事件类型...
    }
    
    /**
     * 卸载所有脚本
     */
    public void unloadScripts() {
        for (Script script : loadedScripts) {
            try {
                // 调用 onDisable 函数
                LuaValue onDisable = script.getGlobals().get("onDisable");
                if (onDisable.isfunction()) {
                    onDisable.call();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        loadedScripts.clear();
        eventCallbacks.clear();
    }
    
    /**
     * 获取所有已加载的脚本
     */
    public List<Script> getLoadedScripts() {
        return new ArrayList<>(loadedScripts);
    }
    
    /**
     * 向 Lua 环境注入 API
     */
    private void injectAPI(Globals globals) {
        // 创建 API 表
        LuaTable api = new LuaTable();
        
        // 注入 Minecraft 客户端
        api.set("mc", CoerceJavaToLua.coerce(net.minecraft.client.MinecraftClient.getInstance()));
        
        // 注入通知 API
        LuaTable notifications = new LuaTable();
        notifications.set("info", new NotifyFunction(NotificationType.INFO));
        notifications.set("success", new NotifyFunction(NotificationType.SUCCESS));
        notifications.set("warning", new NotifyFunction(NotificationType.WARNING));
        notifications.set("error", new NotifyFunction(NotificationType.ERROR));
        api.set("notifications", notifications);
        
        // 注入命令 API
        LuaTable commands = new LuaTable();
        commands.set("sendMessage", new SendMessageFunction());
        api.set("commands", commands);
        
        // 注入模块 API
        LuaTable modules = new LuaTable();
        modules.set("getModule", new GetModuleFunction());
        modules.set("getAllModules", new GetAllModulesFunction());
        api.set("modules", modules);
        
        // 设置全局 API 变量
        globals.set("advicenext", api);
    }
    
    /**
     * 处理 Tick 事件
     */
    @cn.spikego.advicenext.event.Listener
    public void onTick(TickEvent event) {
        for (Map.Entry<String, LuaValue> entry : eventCallbacks.entrySet()) {
            if (entry.getKey().startsWith("onTick_")) {
                try {
                    entry.getValue().call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 通知函数
     */
    private static class NotifyFunction extends org.luaj.vm2.lib.OneArgFunction {
        private final NotificationType type;
        
        public NotifyFunction(NotificationType type) {
            this.type = type;
        }
        
        @Override
        public LuaValue call(LuaValue arg) {
            if (arg.istable()) {
                LuaTable table = arg.checktable();
                String title = table.get("title").optjstring("Script");
                String message = table.get("message").checkjstring();
                int duration = table.get("duration").optint(3000);
                
                NotificationManager.getInstance().addNotification(title, message, type, duration);
            }
            return LuaValue.NIL;
        }
    }
    
    /**
     * 发送消息函数
     */
    private static class SendMessageFunction extends org.luaj.vm2.lib.OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            String message = arg.checkjstring();
            CommandManager.addMessage(message);
            return LuaValue.NIL;
        }
    }
    
    /**
     * 获取模块函数
     */
    /**
     * 获取模块函数
     */
    private static class GetModuleFunction extends org.luaj.vm2.lib.OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            String name = arg.checkjstring();

            // 遍历所有模块查找匹配的名称
            for (cn.spikego.advicenext.features.module.Module module : ModuleManager.getModules()) {
                if (module.getName().equalsIgnoreCase(name)) {
                    return CoerceJavaToLua.coerce(module);
                }
            }

            // 如果没有找到，返回 nil
            return LuaValue.NIL;
        }
    }


    /**
     * 获取所有模块函数
     */
    private static class GetAllModulesFunction extends org.luaj.vm2.lib.ZeroArgFunction {
        @Override
        public LuaValue call() {
            return CoerceJavaToLua.coerce(ModuleManager.getModules());
        }
    }
}
