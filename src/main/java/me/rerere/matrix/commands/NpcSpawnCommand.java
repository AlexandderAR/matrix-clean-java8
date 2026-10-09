package me.rerere.matrix.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.combat.CombatTargetTracker;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.player.PlanState;

/** Administrative command that starts the configured KillAura NPC check. */
public final class NpcSpawnCommand extends AbstractCommandDescriptor {
    public NpcSpawnCommand() {
        super("npc", "<player> <check ticks>",
                LocaleMessages.getLocaleValue("commands.npc"),
                "matrix.npc");
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        boolean licenseUnavailable = PlanState.INSTANCE.hasTrialOrNoPlans();
        if (licenseUnavailable) {
            sender.sendMessage("License is offline, please check your license token or contact the developer.");
            return;
        }

        if (!CheckCategory.KILLAURA.isEnabled()) {
            sender.sendMessage(disabledKillAuraMessage(ChatColor.RED.toString()));
            return;
        }
        if (!CombatTargetTracker.isEnabled()) {
            sender.sendMessage(disabledForceFieldMessage(ChatColor.RED.toString()));
            return;
        }
        if (arguments.length < 3) {
            sender.sendMessage(incorrectArgumentsMessage(ChatColor.RED.toString()));
            return;
        }

        Player target = Bukkit.getPlayer(arguments[1]);
        if (target == null) {
            sender.sendMessage(offlinePlayerMessage(ChatColor.RED.toString(), arguments[1]));
            return;
        }

        try {
            int checkDurationTicks = Integer.parseInt(arguments[2]);
            CombatTargetTracker.registerCommandTarget(target, checkDurationTicks);
            sender.sendMessage(spawnedMessage(ChatColor.GREEN.toString(), target.getName(), arguments[2]));
        } catch (Exception parseOrSpawnFailure) {
            sender.sendMessage(parseTicksMessage(ChatColor.RED.toString(), arguments[2]));
        }
    }

    private static String incorrectArgumentsMessage(String color) {
        return new StringBuilder(color).append("Incorrect arguments length!").toString();
    }

    private static String parseTicksMessage(String color, String badValue) {
        return new StringBuilder(color)
                .append("Can't parse the check ticks number: ")
                .append(badValue).toString();
    }

    private static String disabledKillAuraMessage(String color) {
        return new StringBuilder(color)
                .append("KillAura check is disabled, you can't run this command!").toString();
    }

    private static String spawnedMessage(String color, String playerName, String ticks) {
        return new StringBuilder(color).append("Spawned KillAura Npc for ")
                .append(playerName).append(" (").append(ticks).append(" ticks)").toString();
    }

    private static String disabledForceFieldMessage(String color) {
        return new StringBuilder(color)
                .append("ForceField check is disabled, you can't run this command!").toString();
    }

    @Override
    public java.util.List<String> getTabCompletions() {
        return null;
    }

    private static String offlinePlayerMessage(String color, String playerName) {
        return new StringBuilder(color).append("Player ").append(playerName)
                .append(" is not online!").toString();
    }
}
