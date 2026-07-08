package cn.advicenext.config;

import cn.advicenext.features.notification.NotificationManager;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AltConfig {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".advicenext", "alts");
    private final List<Alt> alts = new ArrayList<>();

    public static class Alt {
        private String username;
        private String password;
        private String type; // "microsoft", "mojang", "cracked"
        
        public Alt(String username, String password, String type) {
            this.username = username;
            this.password = password;
            this.type = type;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getType() { return type; }
        
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setType(String type) { this.type = type; }
    }

    public AltConfig() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addAlt(String username, String password, String type) {
        alts.add(new Alt(username, password, type));
    }
    
    public void removeAlt(int index) {
        if (index >= 0 && index < alts.size()) {
            alts.remove(index);
        }
    }
    
    public List<Alt> getAlts() {
        return new ArrayList<>(alts);
    }

    public void saveConfig(String configName) {
        try {
            Path configPath = configDir.resolve(configName + ".json");
            JsonObject root = new JsonObject();
            JsonArray altsArray = new JsonArray();

            for (Alt alt : alts) {
                JsonObject altObj = new JsonObject();
                altObj.addProperty("username", alt.getUsername());
                altObj.addProperty("password", alt.getPassword());
                altObj.addProperty("type", alt.getType());
                altsArray.add(altObj);
            }

            root.add("alts", altsArray);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configPath.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }

            NotificationManager.getInstance().addNotification(
                "Alt Config",
                "Alt configuration '" + configName + "' saved successfully",
                NotificationManager.NotificationType.SUCCESS,
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Alt Config",
                "Failed to save alt configuration '" + configName + "': " + e.getMessage(),
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

            alts.clear();

            if (root.has("alts") && root.get("alts").isJsonArray()) {
                JsonArray altsArray = root.getAsJsonArray("alts");
                
                for (JsonElement element : altsArray) {
                    if (element.isJsonObject()) {
                        JsonObject altObj = element.getAsJsonObject();
                        
                        String username = altObj.has("username") ? altObj.get("username").getAsString() : "";
                        String password = altObj.has("password") ? altObj.get("password").getAsString() : "";
                        String type = altObj.has("type") ? altObj.get("type").getAsString() : "cracked";
                        
                        alts.add(new Alt(username, password, type));
                    }
                }
            }

            NotificationManager.getInstance().addNotification(
                "Alt Config",
                "Alt configuration '" + configName + "' loaded successfully",
                NotificationManager.NotificationType.SUCCESS,
                3000
            );
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().addNotification(
                "Alt Config",
                "Failed to load alt configuration '" + configName + "': " + e.getMessage(),
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