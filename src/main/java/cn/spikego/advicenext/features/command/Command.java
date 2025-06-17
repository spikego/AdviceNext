package cn.spikego.advicenext.features.command;

public abstract class Command {
    protected String command;
    protected String description;
    protected String[] usage;

    public Command(String command, String description, String[] usage) {
        this.command = command;
        this.description = description;
        this.usage = usage;
    }

    public abstract void run(String[] args);

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public String[] getUsage() {
        return usage;
    }
}