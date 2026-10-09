package me.rerere.matrix.commands;

import java.util.List;
import java.util.UUID;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Resets a player's violation level through the command API. */
public final class ResetViolationCommand extends AbstractCommandDescriptor {
    private List<String> aliases;

    public ResetViolationCommand() {
        super("reset", "<Player>", "Reset the violation level of the player", "matrix.reset");
    }

    @Override
    public List<String> getTabCompletions() {
        return aliases;
    }

    private static String tooFewParameters(ChatColor color) {
        return new StringBuilder().append((Object) color).append("Too few parameters").toString();
    }

    private static String resetConfirmation(ChatColor color) {
        return new StringBuilder().append((Object) color)
                .append("The violation level of the player has been reseted!").toString();
    }

    private static String notInternal(ChatColor color) {
        return new StringBuilder().append((Object) color)
                .append("The player is not in the internal!").toString();
    }

    private static String notOnline(ChatColor color) {
        return new StringBuilder().append((Object) color).append("The player is not online!").toString();
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            sender.sendMessage(tooFewParameters(ChatColor.RED));
            return;
        }

        Player player = Bukkit.getPlayer(arguments[1]);
        if (player == null) {
            sender.sendMessage(notOnline(ChatColor.RED));
            return;
        }

        Matrix matrix = Matrix.Companion.matrixInstance();
        PlayerSessionRegistry sessionRegistry = matrix.getPlayerDataManager();
        UUID playerId = player.getUniqueId();
        PlayerCheckContext playerState = sessionRegistry.find(playerId);
        if (playerState == null) {
            sender.sendMessage(notInternal(ChatColor.RED));
            return;
        }
        ViolationTotals violationState = playerState.playerStorage();
        violationState.updateEveryEntry();
        sender.sendMessage(resetConfirmation(ChatColor.GREEN));
    }
}
