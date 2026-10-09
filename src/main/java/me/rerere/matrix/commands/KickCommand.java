package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.network.KickReportService;
import me.rerere.matrix.util.text.ChatColorFormatter;
import me.rerere.matrix.thirdparty.kotlin.Unit;

/** Handles the privileged player kick command. */
public final class KickCommand extends AbstractCommandDescriptor {
    public KickCommand() {
        super("kick", "<Player> <Reason>",
                LocaleMessages.getLocaleValue("commands.kick"),
                "matrix.kick");
    }

    public List<String> getTabCompletions() {
        return null;
    }

    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length < 3) {
            sender.sendMessage(buildArgumentError(LocaleMessages.margin));
            return;
        }

        Player target = Bukkit.getPlayer(arguments[1]);
        if (target == null) {
            sender.sendMessage("§cThe player is not online.");
            return;
        }

        List<String> reasonParts = new ArrayList<>();
        for (int i = 2; i < arguments.length; i++) {
            reasonParts.add(ChatColorFormatter.translateWithDefaultColorCode(arguments[i]));
        }

        Matrix matrix = Matrix.getInstance();
        matrix.getRelogManager().recordLogin(target.getUniqueId());
        FoliaSupport.INSTANCE.runSyncForPlayer(target, () -> runKickNow(target, reasonParts));
    }

    private static Unit runKickNow(Player target, List<String> reasonParts) {
        KickReportService actions = Matrix.getInstance().getKickReportService();
        actions.reportKick(target,
                String.join(" ", reasonParts).replace("%break%", " "));

        if (MatrixConfiguration.kickStrikeLightning) {
            target.getWorld().strikeLightningEffect(target.getLocation().clone());
        }

        String prefix = MatrixConfiguration.showPrefixInKick
                ? LocaleMessages.margin
                : "";
        String kickReason = String.join(" ", reasonParts).replace("%break%", "\n");
        target.kickPlayer(formatMessage(prefix, kickReason));

        boolean publicKickMessages = LocaleMessages.broadcastKick;
        String personalizedMessage = formatMessage(LocaleMessages.margin,
                LocaleMessages.kickFormat.replaceAll("%player%", target.getName()));
        if (publicKickMessages) {
            Bukkit.broadcastMessage(personalizedMessage);
        } else {
            Bukkit.broadcast(personalizedMessage, "matrix.kick");
        }
        return Unit.INSTANCE;
    }

    private static String buildArgumentError(String prefix) {
        return new StringBuilder(prefix).append("§cError: Please check your args").toString();
    }

    private static String formatMessage(String prefix, String message) {
        return new StringBuilder(prefix).append(message).toString();
    }

}
