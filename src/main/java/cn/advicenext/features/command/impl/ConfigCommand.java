package cn.advicenext.features.command.impl;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.command.Command;

import static cn.advicenext.features.command.CommandManager.addMessage;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manage client configurations", new String[]{"config <save|load> [name]", "config list"});
    }

    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            addMessage("Usage: .config <save|load|list> [name]");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "save":
                if (args.length < 2) {
                    ConfigManager.getInstance().saveConfig("default");
                    addMessage("Default configuration saved.");
                } else {
                    ConfigManager.getInstance().saveConfig(args[1]);
                    addMessage("Configuration '" + args[1] + "' saved.");
                }
                break;

            case "load":
                if (args.length < 2) {
                    ConfigManager.getInstance().loadConfig("default");
                    addMessage("Default configuration loaded.");
                } else {
                    ConfigManager.getInstance().loadConfig(args[1]);
                    addMessage("Configuration '" + args[1] + "' loaded.");
                }
                break;

            case "list":
                addMessage("Available configurations:");
                for (String config : ConfigManager.getInstance().getAvailableConfigs()) {
                    addMessage("- " + config);
                }
                break;

            default:
                addMessage("Unknown action: " + args[0]);
                addMessage("Usage: .config <save|load|list> [name]");
                break;
        }
    }
}