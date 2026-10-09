package me.rerere.matrix.commands;

import java.util.List;
import java.util.Map;
import me.rerere.matrix.Matrix;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.commands.PlayerViolationStatusCommand;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.VerboseDetail;
import me.rerere.matrix.checks.combat.CombatClickRateCheck;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;

/** Command that reports a player's current Matrix status. */
public final class PlayerStatusCommand extends AbstractCommandDescriptor {
    public static final PlayerViolationStatusCommand statusCommand = new PlayerViolationStatusCommand(null);
    private final List<String> recentMessages = null;

    public PlayerStatusCommand() {
        super("status", "<Player>", "Show the player's status", "matrix.status");
    }

    @Override
    public List<String> getTabCompletions() {
        return recentMessages;
    }

    private static String averageAndCurrentCps(double average, int current) {
        return new StringBuilder("§aAverage CPS: §f").append(average)
                .append(" §aCurrent CPS: §f").append(current).toString();
    }

    private static String violationLine(String checkName, int count) {
        return new StringBuilder("§7  - §b").append(checkName).append(": §f")
                .append(count).toString();
    }

    private static String bedrockStatus(boolean bedrock) {
        return new StringBuilder("§aBedrock Player: §f").append(bedrock).toString();
    }

    private static String currentViolationCount(int count) {
        return new StringBuilder("§aCurrent Violations: §7(total: ").append(count)
                .append(")").toString();
    }

    private static String offlinePlayer(String name) {
        return new StringBuilder("§cThe player ").append(name)
                .append(" is offline!").toString();
    }

    private static String latency(String latency) {
        return new StringBuilder("§aLatency: §f").append(latency).toString();
    }

    private static String verboseLine(String checkName, String details) {
        return new StringBuilder("§7  - ").append(checkName).append(" (")
                .append(details).append(")").toString();
    }

    private static String unmanagedPlayer(String name) {
        return new StringBuilder("§cThe player ").append(name)
                .append(" is offline, or has not managed by Matrix!").toString();
    }

    private static String clientVersion(String version) {
        return new StringBuilder("§aClient Version: §f").append(version).toString();
    }

    private static String statusHeader(String playerName) {
        return new StringBuilder("§f-------- §bStatus of §2").append(playerName)
                .append(" §f-------").toString();
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            statusCommand.execute(sender, args);
            return;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(offlinePlayer(args[1]));
            return;
        }

        Matrix matrix = Matrix.Companion.matrixInstance();
        PlayerCheckContext playerState = matrix.getPlayerDataManager().find(player.getUniqueId());
        if (playerState == null) {
            sender.sendMessage(unmanagedPlayer(args[1]));
            return;
        }

        sender.sendMessage(statusHeader(player.getName()));
        sender.sendMessage(clientVersion(playerState.clientVersion().getReleaseName()));
        sender.sendMessage(latency(me.rerere.matrix.compat.KotlinTimeAccess.durationToString(
                        playerState.smoothedLatency())));
        sender.sendMessage(bedrockStatus(playerState.isBedrockClient()));

        CheckRegistry dataSource = playerState.checks();
        CombatClickRateCheck cpsData = (CombatClickRateCheck) dataSource.firstCheckOfType(
                CombatClickRateCheck.class);
        if (cpsData != null) {
            double average = NumberUtils.round3(cpsData.getAveragedClickRate());
            int current = cpsData.getLastClickRate();
            sender.sendMessage(averageAndCurrentCps(average, current));
            int currentViolations = playerState.playerStorage().sumTotals();
            sender.sendMessage(currentViolationCount(currentViolations));
        }

        ViolationTotals violations = playerState.playerStorage();

        boolean displayedViolations = false;
        for (CheckCategory hackType : CheckCategory.values()) {
            if (!hackType.isEnabled()) continue;
            int count = violations.totalFor(hackType);
            if (count <= 0) continue;
            sender.sendMessage(violationLine(hackType.getDisplayName(), count));
            displayedViolations = true;
        }
        if (!displayedViolations) sender.sendMessage("§7  - The player currently looks very legit");

        sender.sendMessage("§aRecent Verbose Messages:");
        VerboseDetail[] messageSamples = (VerboseDetail[]) playerState.packetHistory().getSamples();
        boolean wroteMessage = false;
        for (int index = messageSamples.length - 1; index >= 0; index--) {
            VerboseDetail message = messageSamples[index];
            if (message == null) continue;
            String checkName = message.message();
            Map<?, ?> values = message.tooltips();
            String details = DiagnosticFormat.joinMap(values);
            sender.sendMessage(verboseLine(checkName, details));
            wroteMessage = true;
        }
        if (!wroteMessage) sender.sendMessage("§7  - No historical verbose messages yet");
    }
}
