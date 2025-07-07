package cn.advicenext.script;

public class Script {
    private final String name;
    private final String author;
    private final String version;
    private final String description;
    private final String content;
    private boolean loaded = false;
    
    public Script(String name, String content) {
        this.name = name;
        this.author = "Unknown";
        this.version = "1.0";
        this.description = "No description";
        this.content = content;
    }
    
    public Script(String name, String author, String version, String description, String content) {
        this.name = name;
        this.author = author;
        this.version = version;
        this.description = description;
        this.content = content;
    }
    
    public void load() throws Exception {
        // 简化版本：直接执行基础功能
        loaded = true;
        onEnable();
    }
    
    public void unload() {
        if (loaded) {
            onDisable();
            loaded = false;
        }
    }
    
    public void onEnable() {
        // 脚本启用时调用
    }
    
    public void onDisable() {
        // 脚本禁用时调用
    }
    
    public void onTick() {
        // 每tick调用
    }
    
    public String getName() {
        return name;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isLoaded() {
        return loaded;
    }
}