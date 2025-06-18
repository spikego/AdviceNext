package cn.spikego.advicenext.features.command.impl;

import cn.spikego.advicenext.features.command.Command;
import cn.spikego.advicenext.script.Script;
import cn.spikego.advicenext.script.ScriptManager;

import static cn.spikego.advicenext.features.command.CommandManager.addMessage;

public class ScriptCommand extends Command {
    
    public ScriptCommand() {
        super("script", "Manage Lua scripts", new String[]{"script <list|reload>"});
    }
    
    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            addMessage("Usage: .script <list|reload>");
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                listScripts();
                break;
                
            case "reload":
                reloadScripts();
                break;
                
            default:
                addMessage("Unknown action: " + args[0]);
                addMessage("Usage: .script <list|reload>");
                break;
        }
    }
    
    private void listScripts() {
        addMessage("§b--- Loaded Scripts ---");
        
        for (Script script : ScriptManager.getInstance().getLoadedScripts()) {
            addMessage("§a" + script.getName() + " §7v" + script.getVersion() + " §8by §7" + script.getAuthor());
            addMessage("§8" + script.getDescription());
        }
        
        if (ScriptManager.getInstance().getLoadedScripts().isEmpty()) {
            addMessage("§cNo scripts loaded.");
        }
    }
    
    private void reloadScripts() {
        addMessage("§bReloading scripts...");
        ScriptManager.getInstance().loadScripts();
        addMessage("§aScripts reloaded successfully.");
    }
}
