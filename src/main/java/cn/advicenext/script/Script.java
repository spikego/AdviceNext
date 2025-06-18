package cn.advicenext.script;

import org.luaj.vm2.Globals;

import java.io.File;

/**
 * 表示一个已加载的 Lua 脚本
 */
public class Script {
    private final String name;
    private final String author;
    private final String version;
    private final String description;
    private final File file;
    private final Globals globals;
    
    public Script(String name, String author, String version, String description, File file, Globals globals) {
        this.name = name;
        this.author = author;
        this.version = version;
        this.description = description;
        this.file = file;
        this.globals = globals;
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
    
    public File getFile() {
        return file;
    }
    
    public Globals getGlobals() {
        return globals;
    }
}
