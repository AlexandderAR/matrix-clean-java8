package me.rerere.matrix.commands;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Adds violation levels to a player's selected check. */
public final class AddViolationCommand extends me.rerere.matrix.commands.AbstractCommandDescriptor {
    private final java.util.List<String> completions = null;

    public AddViolationCommand() {
        super("addvl", "<Player> <CheckType> <Violations>", "Add player's violation", "matrix.addvl");
        this.setEnabled(true);
    }

    private static String offlineMessage(String player) {
        return "player " + player + " is offline";
    }

    @Override
    public java.util.List<String> getTabCompletions() {
        return this.completions;
    }

    private static String joinPrefix(String prefix, String message) {
        return new StringBuilder(prefix).append(message).toString();
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length != 4) return;

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(offlineMessage(args[1]));
            return;
        }

        me.rerere.matrix.checks.CheckCategory check = me.rerere.matrix.checks.CheckCategory.settingLookup
                .bySettingName(args[2]);
        if (check == null) return;
        int amount = Integer.parseInt(args[3]);

        me.rerere.matrix.player.PlayerSessionRegistry sessions =
                me.rerere.matrix.Matrix.Companion.matrixInstance()
                        .getPlayerDataManager();
        me.rerere.matrix.player.PlayerCheckContext playerData = sessions.find(target.getUniqueId());
        Intrinsics.checkNotNull(playerData);
        me.rerere.matrix.checks.player.ViolationTotals violations = playerData.playerStorage();

        final me.rerere.matrix.player.PlayerSessionRegistry taskSessions = sessions;
        final me.rerere.matrix.checks.CheckCategory selectedCheck = check;
        final me.rerere.matrix.checks.player.ViolationTotals violationState = violations;
        Runnable messageTask = () -> dispatchViolations(
                taskSessions, target, selectedCheck, amount, violationState);
        BukkitScheduler scheduler = Bukkit.getScheduler();
        Plugin plugin = (Plugin) me.rerere.matrix.Matrix.Companion.matrixInstance();
        scheduler.runTask(plugin, messageTask);
        violationState.updateViolationCommands(playerData,check,amount,"custom","command adding violation");
    }

    private static void dispatchViolations(me.rerere.matrix.player.PlayerSessionRegistry sessions, Player target,
            me.rerere.matrix.checks.CheckCategory check, int amount,
            me.rerere.matrix.checks.player.ViolationTotals violationState) {
        Iterator<?> iterator = sessions.sessions().values().iterator();
        while (iterator.hasNext()) {
            me.rerere.matrix.player.PlayerCheckContext data =
                    (me.rerere.matrix.player.PlayerCheckContext) iterator.next();
            if (!data.verboseEnabled()) continue;

            String playerName = target.getName();
            String checkName = check.getDisplayName();
            int ping = me.rerere.matrix.player.PlayerPingAccessor.getPing(target);
            double tps = me.rerere.matrix.util.NumberUtils.roundPowerOfTen(
                    me.rerere.matrix.metrics.RollingStatistics.metric(), 2);
            int resultingViolationLevel = amount + violationState.totalFor(check);
            String raw = String.format(
                    "\u00a77%s failed \u00a7c%s \u00a77check \u00a78| \u00a77%s \u00a7b(+%s) \u00a78| \u00a77vl:%s ping:%s tps:%s",
                    playerName, checkName, "command adding violation", Integer.valueOf(amount),
                    Integer.valueOf(resultingViolationLevel), Integer.valueOf(ping), Double.valueOf(tps));
            Intrinsics.checkNotNullExpressionValue(raw, "format(...)");
            Player recipient = data.getPlayer();
            String message = joinPrefix(me.rerere.matrix.i18n.LocaleMessages.margin, raw);
            recipient.sendMessage(message);

            if (me.rerere.matrix.config.MatrixConfiguration.logFile) {
                String notice = raw;
                Runnable noticeTask = () -> scheduleNotice(notice);
                me.rerere.matrix.Matrix.Companion.matrixInstance()
                        .getAsyncPool().enqueue(noticeTask);
            }
        }
    }

    private static void scheduleNotice(String message) {
        me.rerere.matrix.Matrix.Companion.matrixInstance()
                .getMatrixLogger().appendLog(message);
    }

}
