package me.rerere.matrix.api;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerPingAccessor;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.violation.Violation;

/** Concrete public API adapter backed by Matrix's player and violation managers. */
public class MatrixAPIBack implements MatrixAPI {
    public MatrixAPIBack() {}

    public void markAsBedrockPlayer(Player player) {
        playerManager().find(player.getUniqueId()).markBedrockClient();
    }

    public void registerClientBlock(Block block) {
        Matrix.getInstance().getClientBlockManager()
                .queueAir(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    private static CheckCategory getCheckTypeByHackType(HackType hackType) {
        return CheckCategory.valueOf(hackType.name());
    }

    @Override
    public void flag(Player player, HackType hackType, String message, String component, int addedViolations) {
        if (player == null) throw new IllegalArgumentException("null player");
        if (message == null) throw new IllegalArgumentException("null message");
        if (component == null) throw new IllegalArgumentException("null component");
        if (addedViolations < 0) throw new IllegalArgumentException("Negative VL");
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("You cannot call this method asynchronously!");

        PlayerSessionRegistry sessions = playerManager();
        PlayerCheckContext targetData = sessions.find(player.getUniqueId());
        if (targetData == null) throw new IllegalArgumentException("No statistics found for this player");
        ViolationTotals violationTotals = targetData.playerStorage();
        String playerName = player.getName();
        CheckCategory check = getCheckTypeByHackType(hackType);
        String checkName = check.getDisplayName();
        int ping = PlayerPingAccessor.getPing(player);
        double tps = NumberUtils.roundPowerOfTen(RollingStatistics.metric(), 2);
        int violationLevel = violationTotals.totalFor(check) + addedViolations;
        String formatted = LocaleMessages.verboseFormat
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%message%", message)
                .replace("%add_vl%", String.valueOf(addedViolations))
                .replace("%vl%", String.valueOf(violationLevel))
                .replace("%ping%", String.valueOf(ping))
                .replace("%tps%", String.valueOf(tps))
                .replace("%component%", component)
                .replace("%version%", targetData.clientVersion().getReleaseName());

        if (Matrix.isRuntimeEnabled()) {
            Matrix.logMessage(formatted);
        }
        for (Object session : sessions.sessions().values()) {
            PlayerCheckContext recipient = (PlayerCheckContext) session;
            if (!recipient.verboseEnabled()) continue;
            if (recipient.verboseRecipient() == null
                    || recipient.verboseRecipient() != null
                    && player.getUniqueId().equals(recipient.verboseRecipient())) {
                recipient.getPlayer().sendMessage(concatenateFlagMessage(LocaleMessages.margin, formatted));
            }
        }
        if (MatrixConfiguration.logFile) {
            Matrix.getInstance().getAsyncPool().execute(() -> logFlagMessage(formatted));
        }
        violationTotals.updateViolationCommands(targetData, check, addedViolations, component, message);
    }

    private static String concatenateFlagMessage(String prefix, String value) {
        return new StringBuilder(prefix).append(value).toString();
    }

    private static void logFlagMessage(String value) {
        Matrix.getInstance().getMatrixLogger().appendLog(value);
    }

    @Override
    public boolean isBypass(Player player) {
        PlayerCheckContext data = playerManager().find(player.getUniqueId());
        return data != null && data.permissionBypass();
    }

    @Override
    public int getViolations(Player player, HackType hackType) {
        PlayerSessionRegistry sessions = playerManager();
        if (sessions == null) return 0;
        PlayerCheckContext data = sessions.find(player.getUniqueId());
        if (data == null) return 0;
        ViolationTotals violationTotals = data.playerStorage();
        if (violationTotals == null) return 0;
        return violationTotals.totalFor(getCheckTypeByHackType(hackType));
    }

    @Override
    public int getLatency(Player player) { return PlayerPingAccessor.getPing(player); }

    @Override
    public void setVerbose(Player target, boolean verbose, Player actor) {
        PlayerCheckContext targetContext = playerManager().find(target.getUniqueId());
        targetContext.setVerboseEnabled(verbose);
        if (verbose && actor != null) {
            playerManager().find(target.getUniqueId()).setVerboseRecipient(actor.getUniqueId());
        } else {
            playerManager().find(target.getUniqueId()).setVerboseRecipient(null);
        }
    }

    @Override
    public double getTPS() { return RollingStatistics.metric(); }

    @Override
    public void tempBypass(Player player, HackType hackType, Long expiry) {
        Objects.requireNonNull(player, "null player!");
        long durationMillis = expiry.longValue();
        if (durationMillis <= 0) return;
        PlayerCheckContext data = playerManager().find(player.getUniqueId());
        if (data == null) throw new IllegalArgumentException("No statistics found for this player");
        data.bypassUntil(getCheckTypeByHackType(hackType), MillisecondSampler.currentTimeMillis() + durationMillis);
    }

    @Override
    public boolean hasVerbose(Player player) {
        return playerManager().find(player.getUniqueId()).verboseEnabled();
    }

    @Override
    public boolean isEnable(HackType hackType) { return getCheckTypeByHackType(hackType).isEnabled(); }

    @Override
    public boolean isBypass(Player player, HackType hackType) {
        PlayerCheckContext data = playerManager().find(player.getUniqueId());
        return data != null && data.shouldBypass(getCheckTypeByHackType(hackType));
    }

    @Override
    public void setViolations(Player player, HackType hackType, int violations) {
        PlayerCheckContext data = playerManager().find(player.getUniqueId());
        ViolationTotals manager = data.playerStorage();
        Violation counter = manager.totals().get(getCheckTypeByHackType(hackType));
        counter.setLevel(violations);
    }

    @Override
    public void setVerbose(Player target, boolean verbose) {
        PlayerCheckContext data = playerManager().find(target.getUniqueId());
        data.setVerboseEnabled(verbose);
        if (!verbose) playerManager().find(target.getUniqueId()).setVerboseRecipient(null);
    }

    @Override
    public void setEnable(HackType hackType, boolean enabled) { getCheckTypeByHackType(hackType).setEnabled(enabled); }

    @Override
    public void reloadPermissionCache(Player player) {
        PermissionCacheManager manager = Matrix.getInstance().getPermissionManager();
        manager.refreshPlayer(player);
    }

    private static PlayerSessionRegistry playerManager() {
        return Matrix.getInstance().getPlayerDataManager();
    }
}
