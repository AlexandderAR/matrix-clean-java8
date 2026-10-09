package me.rerere.matrix.commands;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** The /matrix verbose command, including global and per-player state paths. */
public final class VerboseOutputCommand extends me.rerere.matrix.commands.AbstractCommandDescriptor {
    private final List<?> commandOptions;
    public VerboseOutputCommand() {
        super("verbose", "", commandDescription(), "matrix.verbose");
        this.commandOptions = null;
    }

    private static String commandDescription() {
        String description = me.rerere.matrix.i18n.LocaleMessages.getLocaleValue("commands.verbose");
        Intrinsics.checkNotNull(description);
        return description;
    }

    @Override
    public List<?> getTabCompletions() {
        return commandOptions;
    }

    private static String formatGlobalEnabled(String prefix, ChatColor color) {
        return new StringBuilder(prefix).append((Object) color)
                .append("You have enabled verbose message output").toString();
    }

    private static String formatPlayerEnabled(String prefix, ChatColor color, String target) {
        return new StringBuilder(prefix).append((Object) color).append("§aYou enabled ")
                .append(target).append("'s verbose message output").toString();
    }

    private static String formatPlayerOffline(String prefix, ChatColor color) {
        return new StringBuilder(prefix).append((Object) color)
                .append("The player is not online!").toString();
    }

    private static String formatDisabled(String prefix, ChatColor color) {
        return new StringBuilder(prefix).append((Object) color)
                .append("You have disabled verbose message output").toString();
    }

    private static String formatUsage(String prefix, ChatColor color) {
        return new StringBuilder(prefix).append((Object) color)
                .append("Usage: /matrix verbose [target]").toString();
    }

    private static String formatConsole(String prefix, ChatColor color, String state) {
        return new StringBuilder(prefix).append((Object) color).append("You have ")
                .append(state).append(" verbose message output").toString();
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        boolean planRestrictionApplies;
        try {
            planRestrictionApplies = me.rerere.matrix.player.PlanState.INSTANCE
                    .hasTrialOrNoPlans();
        } catch (me.rerere.matrix.license.LicenseResponseException refusal) {
            throw refusal;
        }
        if (planRestrictionApplies) {
            // Keep the original feature-state query and its ordering before the refusal message.
            me.rerere.matrix.config.MatrixConfigBridge config = me.rerere.matrix.Matrix.Companion;
            config.isRunning();
            sender.sendMessage("§cYour plan does not support this feature or Your Network is not connected to the internet.");
            return;
        }

        if (!(sender instanceof Player)) {
            me.rerere.matrix.config.MatrixConfigBridge config =
                    me.rerere.matrix.Matrix.Companion;
            boolean wasEnabled = config.isRunning();
            config.setEnabled(!wasEnabled);
            String state = config.isRunning() ? "enabled" : "disabled";
            ChatColor color = config.isRunning() ? ChatColor.GREEN : ChatColor.RED;
            sender.sendMessage(formatConsole(
                    me.rerere.matrix.i18n.LocaleMessages.margin, color, state));
            return;
        }

        Player player = (Player) sender;
        me.rerere.matrix.Matrix matrix = me.rerere.matrix.Matrix.Companion.matrixInstance();
        me.rerere.matrix.player.PlayerCheckContext checkContext =
                matrix.getPlayerDataManager().find(player.getUniqueId());
        Intrinsics.checkNotNull(checkContext);
        boolean wasEnabled = checkContext.verboseEnabled();
        checkContext.setVerboseEnabled(!wasEnabled);
        if (!checkContext.verboseEnabled()) {
            sender.sendMessage(formatDisabled(
                    me.rerere.matrix.i18n.LocaleMessages.margin, ChatColor.RED));
            checkContext.setVerboseRecipient(null);
            return;
        }

        if (arguments.length == 1) {
            sender.sendMessage(formatGlobalEnabled(
                    me.rerere.matrix.i18n.LocaleMessages.margin, ChatColor.GREEN));
            return;
        }
        if (arguments.length != 2) {
            sender.sendMessage(formatUsage(
                    me.rerere.matrix.i18n.LocaleMessages.margin, ChatColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(arguments[1]);
        if (target == null) {
            sender.sendMessage(formatPlayerOffline(
                    me.rerere.matrix.i18n.LocaleMessages.margin, ChatColor.RED));
            return;
        }
        checkContext.setVerboseRecipient(target.getUniqueId());
        sender.sendMessage(formatPlayerEnabled(
                me.rerere.matrix.i18n.LocaleMessages.margin, ChatColor.GREEN, target.getName()));
    }
}
