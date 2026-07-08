package cn.advicenext.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final ModuleConfig moduleConfig;
    private final AltConfig altConfig;
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".advicenext");

    private ConfigManager() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        moduleConfig = new ModuleConfig();
        altConfig = new AltConfig();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    public ModuleConfig getModuleConfig() {
        return moduleConfig;
    }
    
    public AltConfig getAltConfig() {
        return altConfig;
    }

    // 兼容性方法，委托给ModuleConfig
    public void saveConfig(String configName) {
        moduleConfig.saveConfig(configName);
    }
    
    public void loadConfig(String configName) {
        moduleConfig.loadConfig(configName);
    }
}