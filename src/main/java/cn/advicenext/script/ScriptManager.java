package cn.advicenext.script;

import cn.advicenext.event.EventBus;
import cn.advicenext.event.Listener;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.features.notification.NotificationManager;
import cn.advicenext.features.notification.NotificationManager.NotificationType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager {
    private static final ScriptManager INSTANCE = new ScriptManager();
    private final Path scriptsDir = Paths.get(System.getProperty("user.home"), ".advicenext", "scripts");
    private final List<Script> loadedScripts = new ArrayList<>();
    
    private ScriptManager() {
        try {
            if (!Files.exists(scriptsDir)) {
                Files.createDirectories(scriptsDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventBus.register(this);
    }
    
    public static ScriptManager getInstance() {
        return INSTANCE;
    }
    
    public void loadScripts() {
        unloadScripts();
        
        File[] kotlinFiles = scriptsDir.toFile().listFiles((dir, name) -> name.endsWith(".kts"));
        if (kotlinFiles == null) return;
        
        for (File kotlinFile : kotlinFiles) {
            try {
                loadScript(kotlinFile);
            } catch (Exception e) {
                e.printStackTrace();
                NotificationManager.getInstance().addNotification(
                    "Script Error", 
                    "Failed to load " + kotlinFile.getName() + ": " + e.getMessage(), 
                    NotificationType.ERROR, 
                    5000
                );
            }
        }
    }
    
    private void loadScript(File kotlinFile) throws Exception {
        String scriptContent = new String(Files.readAllBytes(kotlinFile.toPath()));
        Script script = new Script(kotlinFile.getName(), scriptContent);
        script.load();
        loadedScripts.add(script);
        
        NotificationManager.getInstance().addNotification(
            "Script Loaded", 
            kotlinFile.getName(), 
            NotificationType.SUCCESS, 
            3000
        );
    }
    
    public void unloadScripts() {
        for (Script script : loadedScripts) {
            try {
                script.unload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loadedScripts.clear();
    }
    
    public List<Script> getLoadedScripts() {
        return new ArrayList<>(loadedScripts);
    }
    
    @Listener
    public void onTick(TickEvent event) {
        for (Script script : loadedScripts) {
            try {
                script.onTick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}