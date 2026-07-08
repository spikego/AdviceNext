package cn.advicenext.features.command.impl;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.command.Command;
import cn.advicenext.features.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cn.advicenext.features.command.CommandManager.addMessage;

public class ConfigCommand extends Command implements TabCompleter {

    public ConfigCommand() {
        super("config", "Manage client configurations", new String[]{"config <save|load|list> [name]", "config list"});
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
                for (String config : ConfigManager.getInstance().getModuleConfig().getAvailableConfigs()) {
                    addMessage("- " + config);
                }
                break;

            default:
                addMessage("Unknown action: " + args[0]);
                addMessage("Usage: .config <save|load|list> [name]");
                break;
        }
    }
    
    @Override
    public List<String> getCompletions(String[] args, String currentArg) {
        List<String> completions = new ArrayList<>();
        
        // 第一个参数的补全
        if (args.length == 0) {
            List<String> actions = Arrays.asList("save", "load", "list");
            for (String action : actions) {
                if (action.startsWith(currentArg.toLowerCase())) {
                    completions.add(action);
                }
            }
        }
        // 第二个参数的补全（配置名称）
        else if (args.length == 1 && (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("save"))) {
            List<String> configs = ConfigManager.getInstance().getModuleConfig().getAvailableConfigs();
            for (String config : configs) {
                if (config.startsWith(currentArg.toLowerCase())) {
                    completions.add(config);
                }
            }
            
            // 对于save命令，总是提供default选项
            if ("default".startsWith(currentArg.toLowerCase()) && !completions.contains("default")) {
                completions.add("default");
            }
        }
        
        return completions;
    }
}