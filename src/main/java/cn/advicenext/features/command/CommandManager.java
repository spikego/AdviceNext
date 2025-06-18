package cn.advicenext.features.command;

import cn.advicenext.features.command.impl.ConfigCommand;
import cn.advicenext.features.command.impl.DebugCommand;
import cn.advicenext.features.command.impl.HelpCommand;
import cn.advicenext.features.command.impl.ScriptCommand;
import cn.advicenext.features.command.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandManager {
    public static final String commandPrefix = ".";
    public static final List<Command> commands = new CopyOnWriteArrayList<>();
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void initialize() {
        commands.add(new HelpCommand());
        commands.add(new ConfigCommand());
        commands.add(new ScriptCommand());
        commands.add(new DebugCommand());
    }

    public static void processCommand(String input) {
        if (!input.startsWith(commandPrefix)) return;

        String[] args = input.substring(commandPrefix.length()).split(" ");
        if (args.length == 0) return;

        String commandName = args[0].toLowerCase(Locale.getDefault());
        Command command = commands.stream()
                .filter(cmd -> cmd.getCommand().toLowerCase(Locale.getDefault()).equals(commandName))
                .findFirst()
                .orElse(null);

        if (command == null) {
            addMessage("§cUnknown command. Try " + commandPrefix + "help for a list of commands");
            return;
        }

        try {
            String[] commandArgs = new String[args.length - 1];
            System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
            command.run(commandArgs);
        } catch (Exception e) {
            addMessage("§cIncorrect usage. Correct usage: " + commandPrefix + command.getCommand() + " " + String.join(" ", command.getUsage()));
        }
    }

    public static void addMessage(String message) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        mc.inGameHud.getChatHud().addMessage(Text.of("§c[AdviceNext]§f"+message));
    }

    public static String getCommandPrefix() {
        return commandPrefix;
    }
}