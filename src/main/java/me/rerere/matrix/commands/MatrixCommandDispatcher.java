package me.rerere.matrix.commands;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.commands.LanguageCommand;
import me.rerere.matrix.gui.ChecksMenu;
import me.rerere.matrix.license.EnvironmentFingerprint;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.network.PacketOrderBuffer;
import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.player.PlanState;
import me.rerere.matrix.util.NumberUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.rerere.matrix.commands.StaffTool;

/** Matrix command routing and paginated, clickable help recovered directly from ASM. */
public class MatrixCommandDispatcher implements CommandExecutor, TabCompleter {
    public static String licenseAccountLabel;
    public static String hardwareId = "";
    public static String licenseCustomerLabel;
    private final List<AbstractCommandDescriptor> commands = new ArrayList<>();

    public MatrixCommandDispatcher() {
        registerCommands();
    }

    public void registerCommands() {
        commands.clear();
        commands.add(new PlayerStatusCommand());
        commands.add(new ChecksMenu());
        commands.add(new NotifyCommand());
        commands.add(new VerboseOutputCommand());
        commands.add(new SpectateCommand());
        commands.add(new ToggleNotifyCommand());
        commands.add(new KickCommand());
        commands.add(new MessageCommand());
        commands.add(new DelayCommand());
        commands.add(new NpcSpawnCommand());
        commands.add(new ResetViolationCommand());
        commands.add(new TempBypassCommand());
        commands.add(new UpdateCommand());
        commands.add(new DumpCommand());
        commands.add(new ReloadCommand());
        commands.add(new LanguageCommand());
        commands.add(new AddViolationCommand());
        commands.add(new DeveloperCommand());
        commands.add(new StaffTool());
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        boolean playerSender = sender instanceof Player;
        if (playerSender
                && !PermissionCacheManager.hasPermission((Player) sender, "matrix.command")) {
            sender.sendMessage("");
            sender.sendMessage("    §9§lMatrix §f§lAntiCheat");
            sender.sendMessage("    §fMade by §cRE §f& §cMIYU");
            sender.sendMessage("    §fVersion: §c" + Matrix.getInstance().getDescription().getVersion());
            String customer = licenseCustomerLabel;
            String account = licenseAccountLabel;
            sender.sendMessage("    §fCreated for: §c" + customer + " §f(§c" + account + "§f)");
            sender.sendMessage("");
            return true;
        }
        if (arguments.length == 0) {
            showHelp(sender, arguments);
            return true;
        }
        dispatch(sender, arguments);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length == 0) return commandNames();
        if (arguments.length == 1) {
            for (AbstractCommandDescriptor candidate : commands) {
                if (candidate.name().startsWith(arguments[0]) && !candidate.name().equalsIgnoreCase(arguments[0])) {
                    return Collections.singletonList(candidate.name());
                }
            }
            return commandNames();
        }
        if (arguments.length == 2) {
            for (AbstractCommandDescriptor candidate : commands) {
                if (candidate.name().equalsIgnoreCase(arguments[0])) {
                    return (List<String>) candidate.getTabCompletions();
                }
            }
        }
        return null;
    }

    private List<String> commandNames() {
        return commands.stream().map(AbstractCommandDescriptor::name).collect(Collectors.toList());
    }

    private List<AbstractCommandDescriptor> permittedCommands(CommandSender sender) {
        return commands.stream().filter(candidate -> {
            return !candidate.isEnabled();
        }).filter(candidate -> sender.hasPermission(candidate.permission()))
                .collect(Collectors.toList());
    }

    private int helpPageCount(CommandSender sender) {
        return (int) Math.ceil(permittedCommands(sender).size() / 5.0);
    }

    private List<AbstractCommandDescriptor> commandsOnPage(CommandSender sender, int page) {
        boolean console = sender instanceof ConsoleCommandSender;
        if (console) return permittedCommands(sender);
        if (helpPageCount(sender) == 0) return Collections.emptyList();
        if (page <= 0 || page > helpPageCount(sender)) return commandsOnPage(sender, 1);
        List<AbstractCommandDescriptor> result = new ArrayList<>();
        int start = (page - 1) * 5;
        List<AbstractCommandDescriptor> permitted = permittedCommands(sender);
        for (int offset = 0; offset < 5 && start + offset < permitted.size(); offset++) {
            result.add(permitted.get(start + offset));
        }
        return result;
    }

    private void dispatch(CommandSender sender, String[] arguments) {
        if (arguments[0].equalsIgnoreCase("help")) showHelp(sender, arguments);
        for (AbstractCommandDescriptor candidate : commands) {
            if (!candidate.name().equalsIgnoreCase(arguments[0])) continue;
            if (sender instanceof Player && !PermissionCacheManager.hasPermission(
                    (Player) sender, candidate.permission())) {
                sender.sendMessage("§cYou don't have permission to execute this command: ("
                        + candidate.permission() + ")");
                break;
            }
            candidate.execute(sender, arguments);
            break;
        }
        if (arguments[0].equalsIgnoreCase("hwid") && sender instanceof ConsoleCommandSender) {
            sender.sendMessage(EnvironmentFingerprint.fingerprint());
            sender.sendMessage(hardwareId);
        }
        if (arguments[0].equalsIgnoreCase("showpluginmd5")) {
            sender.sendMessage(PacketOrderBuffer.pluginFileMd5());
        }
        if (arguments[0].equalsIgnoreCase("gettps")) {
            sender.sendMessage("TPS: " + RollingStatistics.metric());
        }
        if (arguments[0].equalsIgnoreCase("license") && (sender.hasPermission("matrix.command.license") || sender.isOp())) {
            sendLicenseSummary(sender);
        }
    }

    private void sendLicenseSummary(CommandSender sender) {
        sender.sendMessage("§aMatrix License: §b" + PlanState.INSTANCE.display());
    }

    private void showHelpHeader(CommandSender sender) {
        if (sender instanceof Player) {
            for (int blankLine = 0; blankLine < 10; blankLine++) sender.sendMessage("");
        }
        String prefix = LocaleMessages.margin;
        String version = Matrix.getInstance().getDescription().getVersion();
        sender.sendMessage(new StringBuilder(prefix).append("§fVersion: §b").append(version)
                .append(" §fAuthor: §bRE §f& §bMIYU").toString());
    }

    private void showHelp(CommandSender sender, String[] arguments) {
        int page = 1;
        if (arguments.length > 0 && arguments[0].equalsIgnoreCase("help") && arguments.length > 1) {
            page = NumberUtils.parseInt(arguments[1]).orElse(1);
        }
        if (sender instanceof ConsoleCommandSender) page = 1;
        showHelpHeader(sender);
        int pageCount = helpPageCount(sender);
        if (pageCount == 0) return;
        if (sender instanceof Player) {
            sender.sendMessage("§7Help Pages: " + page);
            sender.sendMessage("");
        }
        for (AbstractCommandDescriptor candidate : commandsOnPage(sender, page)) {
            showCommandHelp(sender, candidate);
        }
        int previousPage = Math.max(1, page - 1);
        int nextPage = Math.min(page + 1, helpPageCount(sender));
        TextComponent navigation = new TextComponent("  ");
        TextComponent previous = new TextComponent(" §c[<-- Prev]    ");
        previous.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/matrix help " + previousPage));
        previous.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to view the previous page").create()));
        TextComponent next = new TextComponent(" §a[Next -->]    ");
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/matrix help " + nextPage));
        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to view the next page").create()));
        navigation.addExtra(previous);
        navigation.addExtra(next);
        if (sender instanceof Player) {
            sender.sendMessage("");
            ((Player) sender).spigot().sendMessage(navigation);
        }
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?> returnType,
                                             int parameterCount, Class<?>[] parameterTypes) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != returnType) continue;
            Class<?>[] actualTypes = method.getParameterTypes();
            if (actualTypes.length != parameterCount) continue;
            boolean matches = true;
            for (int index = 0; index < parameterCount; index++) {
                if (actualTypes[index] != parameterTypes[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return method;
        }
        return null;
    }

    private static Method findMethodInInterfaces(Class<?> owner, String name, Class<?> returnType,
                                                 int parameterCount, Class<?>[] parameterTypes) {
        Method method = findDeclaredMethod(owner, name, returnType, parameterCount, parameterTypes);
        if (method != null) return method;
        for (Class<?> implementedInterface : owner.getInterfaces()) {
            method = findMethodInInterfaces(implementedInterface, name, returnType,
                    parameterCount, parameterTypes);
            if (method != null) return method;
        }
        return null;
    }

    private void showCommandHelp(CommandSender sender, AbstractCommandDescriptor candidate) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String name = candidate.name();
            String description = candidate.description().isEmpty() ? ""
                    : " §7" + candidate.description();
            String localized = candidate.localizedDescription();
            TextComponent line = new TextComponent("  §7- §b" + name + description + " §7- §f" + localized);
            ComponentBuilder hover = new ComponentBuilder("§7Click to run this command: ");
            String hoverName = candidate.name();
            String hoverDescription = candidate.description();
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    hover.append("§7/matrix " + hoverName + " " + hoverDescription).create());
            line.setHoverEvent(hoverEvent);
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/matrix " + candidate.name()));
            player.spigot().sendMessage(line);
            return;
        }
        String name = candidate.name();
        String description = candidate.description();
        description = description.isEmpty() ? "" : " §7" + candidate.description();
        String localized = candidate.localizedDescription();
        sender.sendMessage("§f/matrix §5" + name + description + " §7- §f" + localized);
    }
}
