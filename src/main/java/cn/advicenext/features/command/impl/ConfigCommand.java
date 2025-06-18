package cn.advicenext.features.command.impl;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.command.Command;

import static cn.advicenext.features.command.CommandManager.addMessage;

public class ConfigCommand extends Command {
    
    public ConfigCommand() {
        super("config", "Manage client configurations", new String[]{"config <save|load>"});
    }
    
    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            addMessage("Usage: .config <save|load>");
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "save":
                ConfigManager.getInstance().saveConfig();
                addMessage("Configuration saved successfully.");
                break;
                
            case "load":
                ConfigManager.getInstance().loadConfig();
                addMessage("Configuration loaded successfully.");
                break;
                
            default:
                addMessage("Unknown action: " + args[0]);
                addMessage("Usage: .config <save|load>");
                break;
        }
    }
}
