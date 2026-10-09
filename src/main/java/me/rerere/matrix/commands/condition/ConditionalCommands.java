package me.rerere.matrix.commands.condition;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.condition.placeholders.Placeholder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitScheduler;

/** Evaluates a player-specific condition before dispatching a command. */
public class ConditionalCommands implements CommandExecutor {
    private static final Pattern DELAY_MARKER = Pattern.compile("/([0-9]*)/");

    public ConditionalCommands() {}

    private static String color(ChatColor color) { return String.valueOf(color); }
    private static String playerColor(CommandSender sender, ChatColor color) {
        return sender instanceof Player ? color(color) : "";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(color(ChatColor.RED) + "This command is only available in console.");
            return true;
        }

        if (args.length == 0 || (args.length > 1 && args.length < 5)) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] > Incorrect number of arguments.");
            sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] >" + playerColor(sender, ChatColor.GREEN) + "   /cc help");
            return false;
        }

        if (args.length == 1) {
            if (!args[0].equals("help")) {
                sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] > Incorrect subcommand.");
                sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] >"
                        + playerColor(sender, ChatColor.GREEN) + "   /cc help");
                return false;
            }
            sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "--------=ConditionalCommands=--------");
            sender.sendMessage(playerColor(sender, ChatColor.GREEN) + "  /cc help");
            sender.sendMessage(playerColor(sender, ChatColor.GREEN) + "  /cc <player> unless \""
                    + playerColor(sender, ChatColor.LIGHT_PURPLE) + "condition"
                    + playerColor(sender, ChatColor.GREEN) + "\" do \""
                    + playerColor(sender, ChatColor.LIGHT_PURPLE) + "command"
                    + playerColor(sender, ChatColor.GREEN) + "\"");
            sender.sendMessage(playerColor(sender, ChatColor.GREEN) + "  /cc <player> if \""
                    + playerColor(sender, ChatColor.LIGHT_PURPLE) + "condition"
                    + playerColor(sender, ChatColor.GREEN) + "\" do \""
                    + playerColor(sender, ChatColor.LIGHT_PURPLE) + "command"
                    + playerColor(sender, ChatColor.GREEN) + "\"");
            sender.sendMessage(playerColor(sender, ChatColor.GRAY) + "e.g.");
            sender.sendMessage(playerColor(sender, ChatColor.GREEN) + "  /cc konsolas unless -ping->100|-tps-<10.0 do kick konsolas");
            sender.sendMessage(playerColor(sender, ChatColor.GRAY) + "Please note that conditions cannot include any spaces.");
            sender.sendMessage(playerColor(sender, ChatColor.GRAY) + "For debug information, enable dev mode in the configuration file. ");
            sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
            return false;
        }

        String mode = args[1];
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                    + "[ConditionalCommands] > Not dispatching command because " + args[0] + " is not online...");
            return true;
        }

        String resolvedExpression = args[2];
        for (Placeholders placeholderType : Placeholders.values()) {
            Placeholder placeholder = placeholderType.getPlaceholder();
            if (placeholder.shouldApply(resolvedExpression)) {
                try {
                    resolvedExpression = placeholderType.getPlaceholder().doSubstitution(resolvedExpression, target);
                } catch (Exception failure) {
                    sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                            + "[ConditionalCommands] > Couldn't process placeholder.");
                    return true;
                }
            }
        }

        StringBuilder commandLine = new StringBuilder();
        for (int i = 4; i < args.length; i++) commandLine.append(args[i]).append(' ');
        if (!args[3].equals("do")) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] > Command has no action.");
            return true;
        }

        final Expression condition;
        try {
            condition = new Expression(resolvedExpression);
        } catch (Expression.ParseException parseFailure) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                    + "[ConditionalCommands] > Couldn't process command (syntax error).");
            return false;
        }

        if (mode.equals("unless")) {
            if (!condition.evaluate()) {
                dispatchCommand(sender, commandLine.toString());
            } else {
                sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                        + "[ConditionalCommands] > Not dispatching command because \"" + args[2]
                        + "\" evaluated to true");
            }
            return true;
        }
        if (mode.equals("if")) {
            if (condition.evaluate()) {
                dispatchCommand(sender, commandLine.toString());
            } else {
                sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                        + "[ConditionalCommands] > Not dispatching command because \"" + args[2]
                        + "\" evaluated to false");
            }
            return true;
        }
        sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] > Incorrect subcommand.");
        sender.sendMessage(playerColor(sender, ChatColor.GOLD) + "[ConditionalCommands] >"
                + playerColor(sender, ChatColor.GREEN) + "   /cc help");
        return false;
    }

    private void dispatchProtected(CommandSender sender, String commandLine) {
        try {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                    + "[ConditionalCommands] > Dispatching command \"" + commandLine + "\"");
            Bukkit.getServer().dispatchCommand(sender, commandLine);
        } catch (CommandException failure) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                    + "[ConditionalCommands] > There was a problem trying to run the command.");
        }
    }

    private void dispatchCommand(CommandSender sender, String commandLine) {
        Matcher matcher = DELAY_MARKER.matcher(commandLine);
        if (!matcher.find()) {
            dispatchProtected(sender, commandLine);
            return;
        }
        try {
            while (true) {
                int delay = Integer.parseInt(matcher.group(1));
                int commandStart = matcher.end();
                int commandEnd = matcher.find() ? matcher.start() : commandLine.length();
                String delayedCommand = commandLine.substring(commandStart, commandEnd).trim();
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(
                        Matrix.getInstance(), () -> dispatchProtected(sender, delayedCommand), (long) delay);
                if (commandEnd == commandLine.length()) break;
            }
        } catch (NumberFormatException failure) {
            sender.sendMessage(playerColor(sender, ChatColor.GOLD)
                    + "[ConditionalCommands] > Couldn't process delayed command.");
        }
    }

    public void enable() {
        for (Placeholders placeholder : Placeholders.values()) {
            placeholder.getPlaceholder().init(Matrix.getInstance());
        }
        PluginCommand command = (PluginCommand) java.util.Objects.requireNonNull(
                Bukkit.getPluginCommand("coc"), "Command `coc` didn't registered in plugin.yml!");
        command.setExecutor(this);
    }
}
