package cn.advicenext.features.command;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.command.impl.ConfigCommand;
import cn.advicenext.features.command.impl.DebugCommand;
import cn.advicenext.features.command.impl.HelpCommand;
import cn.advicenext.features.command.impl.ScriptCommand;
import cn.advicenext.features.command.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class CommandManager {
    public static final String commandPrefix = ".";
    public static final List<Command> commands = new CopyOnWriteArrayList<>();
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void initialize() {
        commands.add(new HelpCommand());
        commands.add(new ConfigCommand());
        commands.add(new ScriptCommand());
        commands.add(new DebugCommand());
        commands.add(new IRCCommand());
        commands.add(new TeleportCommand());
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

    /**
     * 获取命令补全建议
     * @param input 当前输入的命令
     * @return 补全建议列表
     */
    public static List<String> getCompletions(String input) {
        List<String> completions = new ArrayList<>();

        // 如果不是命令前缀开头，不提供补全
        if (!input.startsWith(commandPrefix)) {
            return completions;
        }

        String withoutPrefix = input.substring(commandPrefix.length());
        String[] args = withoutPrefix.split(" ", -1);

        // 命令名称补全
        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            // 查找匹配的命令
            for (Command command : commands) {
                if (command.getCommand().toLowerCase().startsWith(partial)) {
                    completions.add(commandPrefix + command.getCommand());
                }
            }
        }
        // 命令参数补全
        else if (args.length > 1) {
            String commandName = args[0].toLowerCase();
            Command command = commands.stream()
                    .filter(cmd -> cmd.getCommand().toLowerCase().equals(commandName))
                    .findFirst()
                    .orElse(null);

            if (command != null && command instanceof TabCompleter) {
                // 获取当前正在输入的参数
                String currentArg = args[args.length - 1].toLowerCase();

                // 获取命令特定的补全
                String[] previousArgs = new String[args.length - 2];
                System.arraycopy(args, 1, previousArgs, 0, previousArgs.length);

                List<String> argCompletions = ((TabCompleter) command).getCompletions(previousArgs, currentArg);

                // 构建完整的命令字符串
                StringBuilder baseCommand = new StringBuilder(commandPrefix + commandName);
                for (int i = 1; i < args.length - 1; i++) {
                    baseCommand.append(" ").append(args[i]);
                }
                baseCommand.append(" ");

                // 添加到补全列表
                for (String completion : argCompletions) {
                    completions.add(baseCommand.toString() + completion);
                }
            }
        }

        return completions;
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