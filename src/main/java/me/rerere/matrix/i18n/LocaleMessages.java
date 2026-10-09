package me.rerere.matrix.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import me.rerere.matrix.util.collections.MutableMapBuilder;
import me.rerere.matrix.config.ConfigurationManager;
import me.rerere.matrix.util.CollectionUtilities;
import me.rerere.matrix.util.text.ChatColorFormatter;
import me.rerere.matrix.Matrix;
import org.bukkit.plugin.java.JavaPlugin;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;

/** Locale file loading, placeholder decoding, and command/gui language defaults. */
public class LocaleMessages extends LocaleConfiguration {
    public static String kickFormat;
    private static final Map<String, String> commandDefaults;
    private static final Map<String, String> localeValueCache;
    public static String margin;
    public static ConfigurationSection componentAliases;
    public static String spamWarning;
    public static String verboseFormat;
    public static String relogMessage;
    public static boolean broadcastKick;
    public static final Map<String, String> globalPlaceholderValues;
    private static String defaultVerboseFormat;

    static {
        globalPlaceholderValues = new HashMap<>();
        localeValueCache = new HashMap<>();
        defaultVerboseFormat = "&7%player% failed &2%check% &7check &8| &7%message%(%tooltips%) &8| &7vl:%vl%&b(+%add_vl%) &7ping:%ping% tps:%tps% component: %component% ver: %version%";

        MutableMapBuilder<String, String> entries = CollectionUtilities.createResult();
        add(entries, "commands.verbose", "Turn on/off the verbose message output");
        add(entries, "commands.notify", "Send message to all online staff");
        add(entries, "commands.toggle-notify", "Toggle notify message output");
        add(entries, "commands.npc", "Spawn an KillAura detection NPC behind player");
        add(entries, "commands.kick", "Kick a player");
        add(entries, "commands.msg", "Send warning messages to a player");
        add(entries, "commands.gui", "Open Control Panel");
        add(entries, "commands.violations", "Open Violations GUI");
        add(entries, "commands.ping", "Get the player latency value");
        add(entries, "commands.cps", "Monitor player CPS data for seconds");
        add(entries, "commands.reload", "Reload Config");
        add(entries, "commands.lang", "Switch display language");
        add(entries, "misc.update.start-checking", "Start checking update....");
        add(entries, "misc.update.updater", "§a> Matrix Updater:");
        add(entries, "misc.update.has-update", "§cMatrix has an update: §f%latest%  §ccurrent version: §2%current%");
        add(entries, "misc.update.latest", "§aYou are using the latest version of Matrix-AntiCheat <3");
        add(entries, "misc.update.download-link", "§cDownload link");
        add(entries, "misc.premium.pass", "Premium version verification has passed!");
        add(entries, "misc.premium.expire-time", "Expire Time: %time%");
        add(entries, "misc.reload.start-reload", "Reloading Matrix...");
        add(entries, "misc.reload.reloaded", "Matrix reloaded.");
        add(entries, "gui.main.toggle-checks.name", "&aToggle Checks");
        add(entries, "gui.main.toggle-checks.description", "&7Click to open checks GUI");
        add(entries, "gui.main.show-violations.name", "&aShow Violations");
        add(entries, "gui.main.show-violations.description", "&7Click to open Violations GUI");
        add(entries, "gui.main.toggle-verbose.name", "&aToggle Verbose");
        add(entries, "gui.main.toggle-verbose.description", "&7Click to enable/disable verbose message");
        add(entries, "gui.main.reload-matrix.name", "&cReload Matrix");
        add(entries, "gui.main.reload-matrix.description", "&7Click to reload plugin");
        add(entries, "gui.main.server-status.name", "&bServer Status");
        add(entries, "gui.main.support-links.name", "&bSupport Links");
        add(entries, "gui.main.premium-user.name", "&bPremium User");
        add(entries, "gui.main.close-gui.name", "&cClose GUI");
        commandDefaults = entries.getEntries();
    }

    public LocaleMessages() {
        super("language.yml");
    }

    private static void add(MutableMapBuilder<String, String> entries, String key, String value) {
        entries.put(key, value);
    }

    private static String localePath(String localeName) {
        return "locale/" + localeName + "/language.yml";
    }

    private static String backupPath(String fileName) {
        return new StringBuilder(fileName).append(".bak").toString();
    }

    private static String loadedPlaceholderCount(int placeholderCount) {
        return "Loaded " + placeholderCount + " global placeholders";
    }

    private static String globalPlaceholder(String placeholderName) {
        return "%gp_" + placeholderName + "%";
    }

    private static String missingLocaleResource(String resourcePath) {
        return "Missing locale resource: " + resourcePath;
    }

    private static String localeMigrationFailure(String locale) {
        return "Failed to migrate language.yml to locale: " + locale;
    }

    private static String formatLocaleText(String text) {
        return ChatColorFormatter.translateWithDefaultColorCode(text);
    }

    public static String getLocaleValue(String path) {
        boolean cached;
        try {
            cached = localeValueCache.containsKey(path);
        } catch (IllegalArgumentException failure) {
            throw failure;
        }
        if (cached) return localeValueCache.get(path);

        Matrix matrix = Matrix.getInstance();
        ConfigurationManager provider = matrix.getConfigManager();
        LocaleMessages locale = provider.getLanguageConfiguration();
        String value = locale.configuration().getString(path);
        if (value == null) return commandDefaults.get(path);
        value = formatLocaleText(value);
        localeValueCache.put(path, value);
        return value;
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        localeValueCache.clear();
        YamlFile yaml = configuration();
        ConfigurationSection section = yaml.getConfigurationSection("global_placeholder");
        if (section != null) {
            Set<String> keys = section.getKeys(false);
            Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()) {
                String placeholderName = iterator.next();
                String token = globalPlaceholder(placeholderName);
                String value = section.getString(placeholderName);
                if (value != null) {
                    globalPlaceholderValues.put(token,
                            ChatColorFormatter.translateWithDefaultColorCode(value));
                }
            }
            String countMessage = loadedPlaceholderCount(globalPlaceholderValues.size());
            Matrix.logMessage(countMessage);
        }

        margin = formatLocaleText(yaml.getString("prefix"));
        kickFormat = formatLocaleText(yaml.getString("kick-format"));
        spamWarning = formatLocaleText(yaml.getString("spam-warning"));
        broadcastKick = yaml.getBoolean("kick-broadcast", true);
        relogMessage = formatLocaleText(yaml.getString("relog-message"));
        verboseFormat = formatLocaleText(yaml.getString("verbose-format", defaultVerboseFormat));
        componentAliases = yaml.getConfigurationSection("component_alias");

        if (!verboseFormat.contains("%tooltips%")) {
            yaml.set("verbose-format", defaultVerboseFormat);
            JavaPlugin plugin = Matrix.getInstance();
            Logger logger = plugin.getLogger();
            logger.warning("(!) | (Config) Updated your language.yml, add `%tooltips%` to verbose-format");
            save();
            verboseFormat = formatLocaleText(yaml.getString("verbose-format", defaultVerboseFormat));
        }
    }

    @Override public void migrateLocaleResource(String fileName, int shouldCopy, byte closeMode) {
        String resourcePath = localePath(fileName);
        JavaPlugin plugin = Matrix.getInstance();
        InputStream input = plugin.getResource(resourcePath);
        if (input == null) {
            throw new IllegalArgumentException(missingLocaleResource(resourcePath));
        }
        File target = dataFile();
        try {
            try {
                if (target.exists()) {
                    File backup = new File(target.getParentFile(), backupPath(target.getName()));
                    Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                if (shouldCopy >= 0) {
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable failure) {
                if (closeMode <= 0 && input != null) {
                    try {
                        input.close();
                    } catch (Throwable closeFailure) {
                        failure.addSuppressed(closeFailure);
                    }
                }
                LocaleMessages.<RuntimeException>sneakyThrow(failure);
            }
            if (shouldCopy >= 0) input.close();
        } catch (IOException failure) {
            throw new RuntimeException(localeMigrationFailure(fileName), failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
