package cn.advicenext.config;

import cn.advicenext.features.value.slider.DoubleSetting;
import cn.advicenext.features.value.slider.FloatSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.value.AbstractSetting;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.ModeSetting;
import cn.advicenext.features.value.StringSetting;
import cn.advicenext.features.value.slider.NumberSetting;
import cn.advicenext.features.notification.NotificationManager;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".advicenext");
    private final Path modulesConfig = configDir.resolve("modules.json");
    
    private ConfigManager() {
        // 创建配置目录
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 保存所有模块配置
     */
    public void saveConfig() {
        try {
            // 创建一个JSON对象来存储所有模块配置
            JsonObject root = new JsonObject();
            
            // 遍历所有模块
            for (Module module : ModuleManager.getModules()) {
                JsonObject moduleObj = new JsonObject();
                
                // 保存模块启用状态
                moduleObj.addProperty("enabled", module.getEnabled());
                
                // 保存模块按键绑定
                moduleObj.addProperty("key", module.getKey());
                
                // 保存模块设置
                if (!module.settings.isEmpty()) {
                    JsonObject settingsObj = new JsonObject();
                    
                    for (AbstractSetting<?> setting : module.settings) {
                        if (setting instanceof BooleanSetting) {
                            settingsObj.addProperty(setting.getName(), ((BooleanSetting) setting).getValue());
                        } else if (setting instanceof NumberSetting) {
                            settingsObj.addProperty(setting.getName(), ((NumberSetting<?>) setting).getValue().toString());
                        } else if (setting instanceof ModeSetting) {
                            settingsObj.addProperty(setting.getName(), ((ModeSetting) setting).getValue());
                        } else if (setting instanceof StringSetting) {
                            settingsObj.addProperty(setting.getName(), ((StringSetting) setting).getValue());
                        }
                    }
                    
                    moduleObj.add("settings", settingsObj);
                }
                
                // 将模块添加到根对象
                root.add(module.getName(), moduleObj);
            }
            
            // 写入文件
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(modulesConfig.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
            
            NotificationManager.getInstance().addNotification(
                "Config", 
                "Configuration saved successfully", 
                NotificationManager.NotificationType.SUCCESS, 
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Config", 
                "Failed to save configuration: " + e.getMessage(), 
                NotificationManager.NotificationType.ERROR, 
                5000
            );
        }
    }
    
    /**
     * 加载所有模块配置
     */
    public void loadConfig() {
        try {
            // 如果配置文件不存在，创建默认配置
            if (!Files.exists(modulesConfig)) {
                saveConfig();
                return;
            }
            
            // 读取配置文件
            String content = new String(Files.readAllBytes(modulesConfig), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            // 遍历所有模块
            for (Module module : ModuleManager.getModules()) {
                JsonElement moduleElement = root.get(module.getName());
                
                if (moduleElement != null && moduleElement.isJsonObject()) {
                    JsonObject moduleObj = moduleElement.getAsJsonObject();
                    
                    // 加载模块启用状态
                    if (moduleObj.has("enabled")) {
                        boolean enabled = moduleObj.get("enabled").getAsBoolean();
                        if (enabled != module.getEnabled()) {
                            module.toggle(); // 只有当状态不同时才切换
                        }
                    }
                    
                    // 加载模块按键绑定
                    if (moduleObj.has("key")) {
                        module.bindKey(moduleObj.get("key").getAsInt());
                    }
                    
                    // 加载模块设置
                    if (moduleObj.has("settings") && moduleObj.get("settings").isJsonObject()) {
                        JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                        
                        for (AbstractSetting<?> setting : module.settings) {
                            if (settingsObj.has(setting.getName())) {
                                JsonElement settingElement = settingsObj.get(setting.getName());
                                
                                if (setting instanceof BooleanSetting) {
                                    ((BooleanSetting) setting).setValue(settingElement.getAsBoolean());
                                } else if (setting instanceof NumberSetting) {
                                    try {
                                        if (setting instanceof IntSetting) {
                                            ((IntSetting) setting).setValue(settingElement.getAsInt());
                                        } else if (setting instanceof FloatSetting) {
                                            ((FloatSetting) setting).setValue(settingElement.getAsFloat());
                                        } else if (setting instanceof DoubleSetting) {
                                            ((DoubleSetting) setting).setValue(settingElement.getAsDouble());
                                        }
                                    } catch (Exception e) {
                                        // 忽略转换错误
                                    }
                                } else if (setting instanceof ModeSetting) {
                                    ((ModeSetting) setting).setValue(settingElement.getAsString());
                                } else if (setting instanceof StringSetting) {
                                    ((StringSetting) setting).setValue(settingElement.getAsString());
                                }
                            }
                        }
                    }
                }
            }
            
            NotificationManager.getInstance().addNotification(
                "Config", 
                "Configuration loaded successfully", 
                NotificationManager.NotificationType.SUCCESS, 
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Config", 
                "Failed to load configuration: " + e.getMessage(), 
                NotificationManager.NotificationType.ERROR, 
                5000
            );
        }
    }
}
