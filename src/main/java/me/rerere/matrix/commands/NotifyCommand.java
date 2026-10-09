package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.model.Parameter;
import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.text.ChatColorFormatter;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Broadcasts a formatted notification to eligible online players. */
public final class NotifyCommand extends AbstractCommandDescriptor {
    private static String errorPrefix;

    public NotifyCommand() {
        super("notify", "<Message>",
                LocaleMessages.getLocaleValue("commands.notify"),
                "matrix.notify");
    }

    @Override
    public List<?> getTabCompletions() {
        return null;
    }

    private static boolean isHoverOption(Parameter option) {
        return option.component1().equalsIgnoreCase("h");
    }

    private static boolean isClickOption(Parameter option) {
        return option.component1().equalsIgnoreCase("c");
    }

    private static String formatMessage(String prefix, String message) {
        return new StringBuilder(prefix).append(message).toString();
    }

    private static String decorateHover(String text) {
        return "§7" + text;
    }

    private static String argumentError(String prefix) {
        return new StringBuilder(prefix).append("§cError: Please check your args").toString();
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        final boolean extendedFormat = true;
        if (arguments.length < 2) {
            sender.sendMessage(argumentError(LocaleMessages.margin));
            return;
        }

        final String message;
        if (!extendedFormat) {
            message = arguments[1];
        } else {
                List<String> formattedParts = new ArrayList<String>();
            for (int index = 1; index < arguments.length; index++) {
                String part = arguments[index];
                if (part.startsWith("-")) break;
                formattedParts.add(ChatColorFormatter.translateWithDefaultColorCode(part));
            }
            message = String.join(" ", formattedParts);
        }

        Set<?> options = PrefixedTokenGroupParser.parseGroups(arguments);
        ServerVersion currentVersion = Matrix.getServerVersion();
        BaseComponent[] components;
        if (extendedFormat && currentVersion.isNewerThanOrEquals(ServerVersion.V_1_16)) {
            components = TextComponent.fromLegacyText(formatMessage(
                    LocaleMessages.margin, message));
        } else {
            components = new BaseComponent[] {
                    new TextComponent(LocaleMessages.margin),
                    new TextComponent(message)
            };
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (extendedFormat && !PermissionCacheManager.hasPermission(player,
                    permission())) continue;

            UUID playerId = player.getUniqueId();
            PlayerCheckContext playerState = Matrix.getInstance()
                    .getPlayerDataManager().find(playerId);
            if (extendedFormat && playerState == null) continue;
            boolean playerContextFlag = playerState.checksActive();
            if (extendedFormat && !playerContextFlag) continue;

            if (extendedFormat && options.stream().anyMatch(option -> isHoverOption((Parameter) option))) {
                Optional<String> hoverValue = options.stream()
                        .filter(option -> isHoverOption((Parameter) option))
                        .map(option -> ((Parameter) option).getValue())
                        .findFirst();
                String hoverText = hoverValue.orElse("null").replace("%break%", "\n");
                String formattedHover = ChatColorFormatter.translateWithDefaultColorCode(hoverText);
                HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[] {new TextComponent(
                                decorateHover(formattedHover))});
                for (BaseComponent component : components) component.setHoverEvent(hover);
            }

            if ((extendedFormat && options.stream().anyMatch(option -> isClickOption((Parameter) option)))
                    || (!extendedFormat && playerContextFlag)) {
                String command = options.stream()
                        .filter(option -> isClickOption((Parameter) option))
                        .map(option -> ((Parameter) option).getValue())
                        .findFirst().orElse("");
                ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
                for (BaseComponent component : components) component.setClickEvent(click);
            }

            player.spigot().sendMessage(components);
        }

        Bukkit.getConsoleSender().sendMessage(formatMessage(
                LocaleMessages.margin, message));
    }

    public static void setErrorPrefix(String prefix) {
        errorPrefix = prefix;
    }

    public static String getErrorPrefix() {
        return errorPrefix;
    }
}
