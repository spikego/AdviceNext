package cn.advicenext.config;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.features.module.impl.render.HUD;
import cn.advicenext.features.value.*;
import cn.advicenext.features.value.slider.*;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.gui.hud.widget.Widget;
import cn.advicenext.gui.hud.widget.WidgetRegistry;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModuleConfig {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".advicenext", "modules");

    public ModuleConfig() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig(String configName) {
        try {
            Path configPath = configDir.resolve(configName + ".json");
            JsonObject root = new JsonObject();

            for (Module module : ModuleManager.getModules()) {
                JsonObject moduleObj = new JsonObject();
                
                moduleObj.addProperty("enabled", module.getEnabled());
                moduleObj.addProperty("key", module.getKey());

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
                        } else if (setting instanceof ColorSetting) {
                            settingsObj.addProperty(setting.getName(), ((ColorSetting) setting).getValue());
                        }
                    }
                    
                    moduleObj.add("settings", settingsObj);
                }

                if (module instanceof HUD hud) {
                    JsonObject widgetObj = new JsonObject();
                    for (cn.advicenext.gui.hud.widget.Widget w : cn.advicenext.gui.hud.widget.WidgetRegistry.getAll()) {
                        JsonObject pos = new JsonObject();
                        pos.addProperty("x", w.getX());
                        pos.addProperty("y", w.getY());
                        widgetObj.add(w.getId(), pos);
                    }
                    moduleObj.add("widgets", widgetObj);
                }

                root.add(module.getName(), moduleObj);
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configPath.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }

            NotificationManager.getInstance().addNotification(
                "Module Config",
                "Configuration '" + configName + "' saved successfully",
                NotificationManager.NotificationType.SUCCESS,
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Module Config",
                "Failed to save configuration '" + configName + "': " + e.getMessage(),
                NotificationManager.NotificationType.ERROR,
                5000
            );
        }
    }

    public void loadConfig(String configName) {
        try {
            Path configPath = configDir.resolve(configName + ".json");

            if (!Files.exists(configPath)) {
                saveConfig(configName);
                return;
            }

            String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            for (Module module : ModuleManager.getModules()) {
                JsonElement moduleElement = root.get(module.getName());

                if (moduleElement != null && moduleElement.isJsonObject()) {
                    JsonObject moduleObj = moduleElement.getAsJsonObject();

                    if (moduleObj.has("enabled")) {
                        boolean enabled = moduleObj.get("enabled").getAsBoolean();
                        if (enabled != module.getEnabled()) {
                            module.toggle();
                        }
                    }

                    if (moduleObj.has("key")) {
                        module.bindKey(moduleObj.get("key").getAsInt());
                    }

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
                                } else if (setting instanceof ColorSetting) {
                                    ((ColorSetting) setting).setValue(settingElement.getAsInt());
                                }
                            }
                        }
                    }

                    if (moduleObj.has("widgets") && moduleObj.get("widgets").isJsonObject() && module instanceof HUD) {
                        JsonObject widgetObj = moduleObj.getAsJsonObject("widgets");
                        for (Widget w : WidgetRegistry.getAll()) {
                            if (widgetObj.has(w.getId())) {
                                JsonObject pos = widgetObj.getAsJsonObject(w.getId());
                                w.setPosition(pos.get("x").getAsFloat(), pos.get("y").getAsFloat());
                            }
                        }
                    } else if (moduleObj.has("hud") && moduleObj.get("hud").isJsonObject() && module instanceof HUD) {
                        JsonObject hudObj = moduleObj.getAsJsonObject("hud");
                        if (hudObj.has("watermarkX")) {
                            Widget w = WidgetRegistry.get("watermark");
                            if (w != null) w.setPosition(hudObj.get("watermarkX").getAsFloat(), hudObj.get("watermarkY").getAsFloat());
                        }
                        if (hudObj.has("arrayListX")) {
                            Widget w = WidgetRegistry.get("arraylist");
                            if (w != null) w.setPosition(hudObj.get("arrayListX").getAsFloat(), hudObj.get("arrayListY").getAsFloat());
                        }
                        if (hudObj.has("targetInfoX")) {
                            Widget w = WidgetRegistry.get("targetinfo");
                            if (w != null) w.setPosition(hudObj.get("targetInfoX").getAsFloat(), hudObj.get("targetInfoY").getAsFloat());
                        }
                    }
                }
            }

            NotificationManager.getInstance().addNotification(
                "Module Config",
                "Configuration '" + configName + "' loaded successfully",
                NotificationManager.NotificationType.SUCCESS,
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Module Config",
                "Failed to load configuration '" + configName + "': " + e.getMessage(),
                NotificationManager.NotificationType.ERROR,
                5000
            );
        }
    }

    public List<String> getAvailableConfigs() {
        List<String> configNames = new ArrayList<>();
        try {
            if (Files.exists(configDir)) {
                Files.list(configDir)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> configNames.add(path.getFileName().toString().replace(".json", "")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configNames;
    }
}