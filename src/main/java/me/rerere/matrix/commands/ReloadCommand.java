package me.rerere.matrix.commands;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;

public final class ReloadCommand extends AbstractCommandDescriptor {
    private static String localizedName() {
        return LocaleMessages.getLocaleValue("commands.reload");
    }

    public ReloadCommand() { super("reload", "", localizedName(), "matrix.reload"); }

    private static String formatReloadMessage(String prefix) {
        return new StringBuilder(prefix).append("\u00a7fReloaded!").toString();
    }

    @Override
    public java.util.List<?> getTabCompletions() { return null; }

    @Override
    public void execute(org.bukkit.command.CommandSender sender, String[] commandArguments) {
        Matrix.getInstance().reloadConfiguration();
        String prefix = LocaleMessages.margin;
        sender.sendMessage(formatReloadMessage(prefix));
    }
}

