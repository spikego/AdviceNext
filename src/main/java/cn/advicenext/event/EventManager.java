package cn.advicenext.event;

import cn.advicenext.event.impl.ChatEvent;
import cn.advicenext.event.impl.KeyboardEvent;
import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.event.impl.TickEvent;
import cn.advicenext.event.impl.*;
import cn.advicenext.features.command.CommandManager;
import cn.advicenext.features.command.Command;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.ModuleManager;

import static cn.advicenext.features.module.ModuleManager.modules;

public class EventManager {
    public static void initialize() {
        EventBus.register(new EventManager());
    }

    @Listener
    public void onTick(TickEvent event) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getEnabled()) {
                module.onTick(event);
            }
        }
    }

    @Listener
    public void onRender2D(Render2DEvent event) {
        for (Module module : ModuleManager.getModules()) {
            if (module.getEnabled()) {
                module.onRender2D(event);
            }
        }
    }

    @Listener
    public void onChat(ChatEvent event) {
        String content = event.getContent();
        if (content.startsWith(CommandManager.commandPrefix)) {
            event.cancelled = true;
            if (content.length() > 1) {
                String[] args = content.substring(CommandManager.commandPrefix.length()).split(" ");
                for (Command command : CommandManager.commands) {
                    if (command.getCommand().equals(args[0])) {
                        String[] commandArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
                        command.run(commandArgs);
                    }
                }
            }
        }
    }

    @Listener
    public void onKey(KeyboardEvent event) {
        for (Module module : modules) {
            if (module.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }
}