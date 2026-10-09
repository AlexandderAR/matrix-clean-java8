package me.rerere.matrix.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;

/** Command for toggling a player's notify-message output. */
public final class ToggleNotifyCommand extends AbstractCommandDescriptor {
    private static String formatEnabled(String messagePrefix) {
        return new StringBuilder(messagePrefix)
                .append("§aYou enabled the notify message output!")
                .toString();
    }

    @Override
    public java.util.List<?> getTabCompletions() {
        return null;
    }

    public ToggleNotifyCommand() {
        super("togglenotify", "", LocaleMessages.getLocaleValue("commands.toggle-notify"),
                "matrix.notify");
    }

    @Override
    public void execute(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You can't execute this command in console!");
            return;
        }

        Player player = (Player) sender;
        PlayerCheckContext playerData = Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        boolean currentlyEnabled = playerData.checksActive();
        boolean newEnabledState = !currentlyEnabled;
        playerData.setChecksActive(newEnabledState);

        if (playerData.checksActive()) {
            player.sendMessage(formatEnabled(LocaleMessages.margin));
        } else {
            player.sendMessage(formatDisabled(LocaleMessages.margin));
        }
    }

    private static String formatDisabled(String messagePrefix) {
        return new StringBuilder(messagePrefix)
                .append("§cYou disabled the notify message output!")
                .toString();
    }
}
