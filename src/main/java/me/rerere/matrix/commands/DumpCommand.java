package me.rerere.matrix.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt___CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.io.FilesKt;
import me.rerere.matrix.thirdparty.kotlin.io.FilesKt__FileReadWriteKt;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.compat.SystemPropertyAccess;

/** Console command that writes Matrix's Markdown diagnostic dump. */
public final class DumpCommand extends AbstractCommandDescriptor {
    public DumpCommand() {
        super("dump", "", "Dump server information", "matrix.dump");
        super.setEnabled(true);
    }

    @Override
    public List<?> getTabCompletions() {
        return null;
    }

    private static void writeText(File file, String text) {
        FilesKt__FileReadWriteKt.writeText(file, text, StandardCharsets.UTF_8);
    }

    private static void appendText(File file, String text) {
        FilesKt__FileReadWriteKt.appendText(file, text, StandardCharsets.UTF_8);
    }

    private static String worldRow(String world, String environment) {
        return "- " + world + " [" + environment + "]\n";
    }

    private static String bukkitVersion(String version) {
        return "- Bukkit Version: " + version + "\n";
    }

    private static String onlinePlayerCount(int count) {
        return "- Online Players: " + count + "\n";
    }

    private static String maximumMemory(long megabytes) {
        return "- Max Memory: " + megabytes + "MB\n";
    }

    private static String totalMemory(long megabytes) {
        return "- Total Memory: " + megabytes + "MB\n";
    }

    private static String versionCode(String version) {
        return "- Version Code: " + version + "\n";
    }

    private static String javaVersion(String version) {
        return "- Java Version: " + version + "\n";
    }

    private static String matrixVersion(String version) {
        return "- Matrix Version: " + version + "\n";
    }

    private static String operatingSystem(String name, String version, String architecture) {
        return "- OS: " + name + " " + version + " " + architecture + "\n";
    }

    private static String freeMemory(long megabytes) {
        return "- Free Memory: " + megabytes + "MB\n";
    }

    private static String onlineMode(boolean enabled) {
        return "- Online Mode: " + enabled + "\n";
    }

    private static String processorCount(int count) {
        return "- Available Processors: " + count + "\n";
    }

    private static String disabledWorlds(String worlds) {
        return " [Disable Worlds: " + worlds + "]";
    }

    private static String playerRow(String name, java.util.UUID id, String flags) {
        return "- " + name + " [" + id + "][" + flags + "]\n";
    }

    private static String playerFlags(Boolean permissionBypass, Integer protocolVersion,
                                      String clientVersion, Boolean bedrockClient) {
        return new StringBuilder().append(permissionBypass).append(',').append(protocolVersion).append('-')
                .append(clientVersion).append(',').append(bedrockClient).toString();
    }

    private static String dumpPath(String path) {
        return "\u00a7aDump file generated: " + path;
    }

    private static String serverVersion(String version) {
        return "- Server Version: " + version + "\n";
    }

    private static String pluginRow(String name, String description, boolean enabled) {
        return "- " + name + "[" + description + "]: " + enabled + "\n";
    }

    private static String booleanStatus(String label, boolean enabled) {
        return "- " + label + ": " + enabled;
    }

    @Override
    public void execute(CommandSender sender, String[] ignoredArguments) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("\u00a7cOnly console can execute this command!");
            return;
        }

        File dumpFile = FilesKt.resolve(Matrix.Companion.matrixInstance().getDataFolder(), "dump.md");
        if (dumpFile.exists()) dumpFile.delete();
        try {
            dumpFile.createNewFile();
        } catch (IOException io) {
            DumpCommand.<RuntimeException>sneakyThrow(io);
        }

        writeText(dumpFile, "# Matrix Dump\n");
        appendText(dumpFile, "\n## Information\n");
        appendText(dumpFile, matrixVersion(Matrix.Companion.matrixInstance().getDescription().getVersion()));
        appendText(dumpFile, bukkitVersion(Bukkit.getBukkitVersion()));
        appendText(dumpFile, serverVersion(Bukkit.getVersion()));
        appendText(dumpFile, versionCode(Matrix.Companion.serverVersion().name()));
        appendText(dumpFile, javaVersion(SystemPropertyAccess.getProperty("java.version")));
        appendText(dumpFile, operatingSystem(
                SystemPropertyAccess.getProperty("os.name"),
                SystemPropertyAccess.getProperty("os.version"),
                SystemPropertyAccess.getProperty("os.arch")));
        appendText(dumpFile, maximumMemory(Runtime.getRuntime().maxMemory() / 1024L / 1024L));
        appendText(dumpFile, totalMemory(Runtime.getRuntime().totalMemory() / 1024L / 1024L));
        appendText(dumpFile, freeMemory(Runtime.getRuntime().freeMemory() / 1024L / 1024L));
        appendText(dumpFile, processorCount(Runtime.getRuntime().availableProcessors()));
        appendText(dumpFile, onlinePlayerCount(Bukkit.getOnlinePlayers().size()));
        appendText(dumpFile, onlineMode(Bukkit.getOnlineMode()));

        appendText(dumpFile, "\n## Checks\n");
        EnumEntries<?> checks = CheckCategory.enumEntries();
        Iterator<?> checkIterator = checks.iterator();
        while (checkIterator.hasNext()) {
            CheckCategory check = (CheckCategory) checkIterator.next();
            appendText(dumpFile,
                    booleanStatus(check.getDisplayName(), check.isEnabled()));
            Set<?> worlds = check.getDisabledWorlds();
            if (worlds != null && worlds.size() > 0) {
                Set<?> disabledWorlds = check.getDisabledWorlds();
                String joinedWorldNames = disabledWorlds == null ? null : CollectionsKt___CollectionsKt.joinToString(
                        disabledWorlds, ",", "", "", -1, "...", null);
                appendText(dumpFile, disabledWorlds(joinedWorldNames));
            }
            appendText(dumpFile, "\n");
        }

        appendText(dumpFile, "\n## Plugins\n");
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        for (Plugin plugin : plugins) {
            appendText(dumpFile,
                    pluginRow(plugin.getName(), plugin.getDescription().getVersion(), plugin.isEnabled()));
        }

        appendText(dumpFile, "\n## Worlds\n");
        for (World world : Bukkit.getWorlds()) {
            appendText(dumpFile, worldRow(world.getName(), world.getEnvironment().name()));
        }

        appendText(dumpFile, "\n## Players\n");
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCheckContext playerState = Matrix.Companion.matrixInstance()
                    .getPlayerDataManager().find(player.getUniqueId());
            Boolean permissionBypass = null;
            Integer protocolVersion = null;
            String clientVersion = null;
            Boolean bedrockClient = null;
            if (playerState != null) {
                permissionBypass = Boolean.valueOf(playerState.permissionBypass());
                protocolVersion = Integer.valueOf(playerState.protocolVersion());
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion version =
                        playerState.clientVersion();
                if (version != null) clientVersion = version.getReleaseName();
                bedrockClient = Boolean.valueOf(playerState.isBedrockClient());
            }
            String flags = playerFlags(permissionBypass, protocolVersion, clientVersion, bedrockClient);
            appendText(dumpFile,
                    playerRow(player.getName(), player.getUniqueId(), flags));
        }
        sender.sendMessage(dumpPath(dumpFile.getAbsolutePath()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
