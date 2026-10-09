package me.rerere.matrix.commands;

import java.util.List;
import org.bukkit.command.CommandSender;

/** Shared command metadata and enabled state for concrete command handlers. */
public abstract class AbstractCommandDescriptor {
    private boolean enabled;
    private final String description;
    private final String localizedDescription;
    private final String permission;
    private final String name;

    public abstract List<?> getTabCompletions();

    public final void setEnabled(boolean enabled) { this.enabled = enabled; }

    public final String description() { return description; }

    public final String permission() { return permission; }

    public abstract void execute(CommandSender sender, String[] arguments);

    public final boolean isEnabled() { return enabled; }

    public AbstractCommandDescriptor(String name, String description,
            String localizedDescription, String permission) {
        this.name = name;
        this.description = description;
        this.localizedDescription = localizedDescription;
        this.permission = permission;
    }

    public final String localizedDescription() { return localizedDescription; }

    public final String name() {
        return name;
    }

}
