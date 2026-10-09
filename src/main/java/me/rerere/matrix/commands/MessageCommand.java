package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;

public class MessageCommand extends AbstractCommandDescriptor {
    public MessageCommand() {
        super("msg", "<Player> <Message>",
                LocaleMessages.getLocaleValue("commands.msg"), "matrix.message");
    }
    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length < 3) {
            sender.sendMessage(new StringBuilder(LocaleMessages.margin)
                    .append("\u00a7cError: Please check your args").toString());
            return;
        }
        Player target = Bukkit.getPlayer(arguments[1]);
        if (target == null) {
            sender.sendMessage("\u00a7cThe player is not online.");
            return;
        }
        List<String> messageParts = new ArrayList<>();
        for (int index = 2; index < arguments.length; index++) {
            messageParts.add(ChatColor.translateAlternateColorCodes('&', arguments[index]));
        }
        String prefix = LocaleMessages.margin;
        String message = String.join(" ", messageParts);
        target.sendMessage(new StringBuilder(prefix)
                .append(message).toString());
    }
    @Override
    public List<?> getTabCompletions() { return null; }
}
