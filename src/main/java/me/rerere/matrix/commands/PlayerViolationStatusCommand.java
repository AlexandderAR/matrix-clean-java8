package me.rerere.matrix.commands;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.player.ViolationTotals;

/** Lists online players whose stored violation count passes the status filter. */
public final class PlayerViolationStatusCommand {

    private PlayerViolationStatusCommand() {
    }

    public PlayerViolationStatusCommand(DefaultConstructorMarker ignored) {
        this();
    }

    private static Pair<Player, Object> playerAndData(Map.Entry<?, ?> entry) {
        Player player = Bukkit.getPlayer((UUID) entry.getKey());
        return new Pair<>(player, entry.getValue());
    }

    private static CharSequence playerName(Player player) {
        return player.getName();
    }

    private static boolean hasOnlinePlayer(Pair<?, ?> playerAndData) {
        return (Player) playerAndData.component1() != null;
    }

    private static Player playerFrom(Pair<?, ?> playerAndData) {
        return (Player) playerAndData.component1();
    }

    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length > 1) {
            return;
        }

        Map<?, ?> violations = Matrix.Companion.matrixInstance()
                .getPlayerDataManager().sessions();
        sender.sendMessage("§aPlayers with violations:");

        StringBuilder joinedNames = new StringBuilder();
        int matchingPlayers = 0;
        for (Map.Entry<?, ?> entry : violations.entrySet()) {
            Pair<Player, Object> candidate = playerAndData(entry);
            if (!hasOnlinePlayer(candidate) || !hasViolations(candidate)) {
                continue;
            }
            Player player = playerFrom(candidate);
            if (player == null) continue;
            if (matchingPlayers > 0) {
                joinedNames.append(' ');
            }
            joinedNames.append(playerName(player));
            matchingPlayers++;
        }

        String nameList = joinedNames.toString();
        boolean blank = me.rerere.matrix.thirdparty.kotlin.text.StringsKt.isBlank(nameList);
        String formatted = blank ? null : " - §6" + nameList;
        if (formatted == null) {
            sender.sendMessage(" - §6No players with violations!");
        } else {
            sender.sendMessage(formatted);
        }
        sender.sendMessage("§7Use /matrix status <player> to view violations");
    }

    private static boolean hasViolations(Pair<?, ?> playerAndData) {
        PlayerCheckContext playerData = (PlayerCheckContext) playerAndData.component2();
        ViolationTotals totals = playerData.playerStorage();
        int count = totals.sumTotals();
        return count > 0;
    }
}
