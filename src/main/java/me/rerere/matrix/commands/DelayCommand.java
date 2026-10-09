package me.rerere.matrix.commands;

import java.util.Arrays;
import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.compat.FoliaSupport;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Executes a console command after a caller-specified delay. */
public final class DelayCommand extends AbstractCommandDescriptor {
    private static final long TICKS_PER_SECOND = 20L;

    public DelayCommand() {
        super("delay", "<seconds> <command>", "delayed execution of a command", "matrix.delay");
    }

    private static String immediateMessage(String margin, ChatColor first, ChatColor second, String command) {
        return new StringBuilder(margin).append(first)
                .append("Execute the command immediately(because of Folia limit): ")
                .append(second).append(command).toString();
    }

    private static String tooFewArgumentsMessage() {
        return ChatColor.RED + "Parameter length is too short!";
    }

    public void dispatchCommand(CommandSender sender, String command) {
        Bukkit.dispatchCommand(sender, command);
    }

    private static String invalidDelayMessage(String margin, ChatColor color, String value) {
        return new StringBuilder(margin).append(color).append(value)
                .append(" is not a integer!").toString();
    }

    private static String delayedMessage(String margin, ChatColor first, int seconds,
            ChatColor second, String command) {
        return new StringBuilder(margin).append(first).append("Delayed ").append(seconds)
                .append(" seconds to execute the command: ").append(second).append(command).toString();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(tooFewArgumentsMessage());
            return;
        }
        Integer seconds = parseInteger(args[1]);
        if (seconds == null) {
            sender.sendMessage(invalidDelayMessage(
                    LocaleMessages.margin,
                    ChatColor.RED, args[1]));
            return;
        }
        String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        Runnable action = () -> dispatchCommand(sender, command);
        if (FoliaSupport.INSTANCE.isUsingFolia()) {
            action.run();
            sender.sendMessage(immediateMessage(
                    LocaleMessages.margin,
                    ChatColor.GREEN, ChatColor.WHITE, command));
        } else {
            Bukkit.getScheduler().runTaskLater(Matrix.Companion.matrixInstance(),
                    action, seconds.longValue() * TICKS_PER_SECOND);
            sender.sendMessage(delayedMessage(
                    LocaleMessages.margin,
                    ChatColor.GREEN, seconds, ChatColor.WHITE, command));
        }
    }

    private static Integer parseInteger(String text) {
        return me.rerere.matrix.thirdparty.kotlin.text.StringsKt.toIntOrNull(text);
    }

    @Override public List<String> getTabCompletions() { return null; }
    public static void setSharedMode(int value) { sharedMode = value; }
    public static int getSharedMode() { return sharedMode; }
    public static int getFallbackMode() { return sharedMode == 0 ? 7 : 0; }
    private static int sharedMode = 2;
}
