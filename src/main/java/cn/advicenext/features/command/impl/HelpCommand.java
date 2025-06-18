package cn.advicenext.features.command.impl;

import cn.advicenext.features.command.Command;
import cn.advicenext.features.command.CommandManager;

public class HelpCommand extends Command {
    public static final HelpCommand INSTANCE = new HelpCommand();

    public HelpCommand() {
        super("help", "Displays a list of all available commands", new String[]{"help"});
    }

    @Override
    public void run(String[] args) {
        sendMessage("§aAvailable Commands:");
        for (Command command : CommandManager.commands) {
            sendMessage("§b." + command.getCommand() + " §7- " + command.getDescription());
        }
    }

    private void sendMessage(String message) {
        CommandManager.addMessage(message);
    }
}