package me.rerere.matrix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.api.HackType;
import me.rerere.matrix.api.MatrixAPI;
import me.rerere.matrix.api.MatrixAPIProvider;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import java.util.List;

/** Implements the temporary check bypass command. */
public final class TempBypassCommand extends AbstractCommandDescriptor {

    public TempBypassCommand() {
        super("tempbypass", "<Player> <CheckType> <Bypass Time>",
                "Makes the player temporarily bypass a check", "matrix.tempbypass");
    }

    @Override
    public List<?> getTabCompletions() { return null; }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length < 4) {
            sender.sendMessage(formatError(LocaleMessages.margin));
            return;
        }
        Player player = Bukkit.getPlayer(arguments[1]);
        if (player == null) {
            sender.sendMessage("§cThe player is not online.");
            return;
        }
        CheckCategory check = CheckCategory.fromSettingName(arguments[2]);
        if (check == null) {
            sender.sendMessage(formatUnknownType(arguments[2]));
            return;
        }
        final long duration;
        try {
            duration = Long.parseLong(arguments[3]);
        } catch (Exception parseFailure) {
            sender.sendMessage(formatWrongTime(arguments[2]));
            return;
        }
        MatrixAPI api = MatrixAPIProvider.getAPI();
        HackType hackType = check.toApiHackType();
        api.tempBypass(player, hackType, Long.valueOf(duration));
        sender.sendMessage(formatSuccess(check.getDisplayName(), duration));
    }

    private static String formatError(String usage) {
        return new StringBuilder(usage).append("\u00a7cError: Please check your args").toString();
    }

    private static String formatUnknownType(String checkType) {
        return "§cThere is no such check type: " + checkType;
    }

    private static String formatSuccess(String checkName, long duration) {
        return "§ePlayers can now bypass " + checkName + " check for " + duration + " milliseconds~";
    }

    private static String formatWrongTime(String checkType) {
        return "§cWrong time: " + checkType;
    }

}








