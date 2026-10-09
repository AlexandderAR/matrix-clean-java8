package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;

/** Player command that starts, switches, and stops spectating. */
public final class SpectateCommand extends AbstractCommandDescriptor {
    public SpectateCommand() {
        super("spectate", "<player>", "Spectate a player", "matrix.spectate");
    }

    private static String successMessage(String playerName) {
        return "§aYou are now spectating " + playerName;
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        final boolean guard = false; // guard is proven false in the canonical Matrix call graph.
        final boolean rejectSender;
        try {
            rejectSender = !guard && !(sender instanceof Player);
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (rejectSender) {
            sender.sendMessage("§cYou must be a player to use this command");
            return;
        }

        Player player = (Player) sender;
        PlayerCheckContext playerData = Matrix.Companion.matrixInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            sender.sendMessage("§cPlayer data not found");
            return;
        }

        List<String> targetArguments = new ArrayList<>(Arrays.asList(arguments));
        if (!targetArguments.isEmpty()) targetArguments.remove(0);
        if (targetArguments.isEmpty()) {
            if (playerData.lastLocation() == null) {
                sender.sendMessage("§cYou are not spectating anyone");
                return;
            }
            GameMode savedMode = playerData.previousGameMode();
            player.setGameMode(savedMode == null ? GameMode.SURVIVAL : savedMode);
            Location savedLocation = playerData.lastLocation();
            Intrinsics.checkNotNull(savedLocation);
            PlayerUtilities.teleportViaPlatform(player, savedLocation);
            playerData.setLastLocation(null);
            playerData.setPreviousGameMode(null);
            playerData.setVerboseRecipient(null);
            playerData.setVerboseEnabled(false);
            sender.sendMessage("§aYou are no longer spectating");
            return;
        }

        Player target = Bukkit.getPlayerExact(targetArguments.get(0));
        if (target == null) {
            sender.sendMessage("§cPlayer not found");
            return;
        }
        if (!guard && Intrinsics.areEqual(target, sender)) {
            sender.sendMessage("§cYou can't spectate yourself");
            return;
        }

        if (playerData.lastLocation() != null) {
            player.setGameMode(GameMode.SPECTATOR);
            Location targetLocation = target.getLocation().clone();
            PlayerUtilities.teleportViaPlatform(player, targetLocation);
            playerData.setVerboseRecipient(target.getUniqueId());
            playerData.setVerboseEnabled(true);
            String targetName = target.getName();
            sender.sendMessage(successMessage(targetName));
            return;
        }

        playerData.setLastLocation(player.getLocation().clone());
        playerData.setPreviousGameMode(player.getGameMode());
        playerData.setVerboseRecipient(target.getUniqueId());
        playerData.setVerboseEnabled(true);
        player.setGameMode(GameMode.SPECTATOR);
        Location targetLocation = target.getLocation().clone();
        PlayerUtilities.teleportViaPlatform(player, targetLocation);
        String targetName = target.getName();
        sender.sendMessage(successMessage(targetName));
    }

    @Override
    public List<?> getTabCompletions() {
        return null;
    }
}
