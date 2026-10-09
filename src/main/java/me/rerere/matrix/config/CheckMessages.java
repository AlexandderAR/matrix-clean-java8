package me.rerere.matrix.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.i18n.LocaleConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.world.CollisionGeometry;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;

/** Loads per-check configuration and formats check status messages. */
public final class CheckMessages extends LocaleConfiguration {
    public static String message;
    public static boolean enabled;
    public static boolean alternateEnabled;
    public static boolean warningEnabled;
    private static final int DEFAULT_DECAY_INTERVAL = 30;
    private static final long CHECK_WARNING_INTERVAL = 3000L;

    public CheckMessages() { super("checks.yml"); }

    @Override
    public void loadSettings() {
        super.loadSettings();

        int enabledCount = 0;
        int disabledCount = 0;
        YamlFile configuration = this.configuration();
        for (Object key : configuration.getKeys(false)) {
            String checkName = (String) key;
            CheckCategory check = CheckCategory.fromSettingName(checkName);
            if (check == null || !configuration.isConfigurationSection(checkName)) {
                continue;
            }

            ConfigurationSection section = configuration.getConfigurationSection(checkName);
            if (section == null) continue;
            check.setConfiguration(new CheckSetting(check, section));

            boolean checkEnabled = section.getBoolean("enable", true);
            if (checkEnabled) enabledCount++;
            else disabledCount++;
            check.setEnabled(checkEnabled);

            if (check == CheckCategory.BADPACKETS
                    && !checkEnabled) {
                Matrix.getInstance().getLogger()
                        .warning("BadPackets check is disabled in the configuration file!");
                Matrix.getInstance().getLogger()
                        .warning("Please don't disable it, or it will cause Timer bypass!");
                Matrix.waitForServer(CHECK_WARNING_INTERVAL);
            }

            if (section.contains("name")) {
                check.setDisplayName(section.getString("name", "null"));
            }
            int decayInterval = section.getInt("decay.interval",
                    (int) DEFAULT_DECAY_INTERVAL);
            check.setDecayInterval(Integer.valueOf(decayInterval));
            int defaultDecayAmount = check.getCategoryWeight();
            int decayAmount = section.getInt("decay.amount", defaultDecayAmount);
            check.setDecayAmount(Integer.valueOf(decayAmount));

            if (section.contains("disable_worlds")) {
                Set<String> disabledWorlds = new HashSet<String>(
                        section.getStringList("disable_worlds"));
                check.setDisabledWorlds(disabledWorlds);
                if (!disabledWorlds.isEmpty()) {
                    String messageText = disabledWorldMessage(
                            check.getDisplayName(), String.join(",", disabledWorlds));
                    Matrix.logMessage(messageText);
                }
            } else {
                check.setDisabledWorlds(null);
            }

            Map<Integer, List<String>> commandOptions = new HashMap<Integer, List<String>>();
            ConfigurationSection commands = section.getConfigurationSection("commands");
            for (String commandKey : commands.getKeys(false)) {
                ConfigurationSection commandSection = section.getConfigurationSection("commands");
                if (!NumberUtils.isInteger(commandKey)) {
                    Matrix.logMessage(invalidCheckMessage(checkName));
                    continue;
                }

                Integer commandId = Integer.valueOf(Integer.parseInt(commandKey));
                if (commandSection.isString(commandKey)) {
                    List<String> names = Collections.singletonList(
                            commandSection.getString(commandKey));
                    commandOptions.put(commandId, translateNames(names));
                } else if (commandSection.isList(commandKey)) {
                    commandOptions.put(commandId,
                            translateNames(commandSection.getStringList(commandKey)));
                }
            }
            // Canonical bytecode stores Integer keys and List<String> values. The
            // recovered CheckCategory declaration has broader, incompatible generics.
            check.setCommandOptions((Map) commandOptions);
            int onlineAmount = section.getInt("activate_online_amount", 0);
            check.setActivateOnlineAmount(Integer.valueOf(onlineAmount));
        }

        Matrix.logMessage(enabledCountsMessage(enabledCount, disabledCount));

        alternateEnabled = configuration.getBoolean("move.ignore_unloaded_chunks", true);
        enabled = configuration.getBoolean("badpackets.timer.use_legacy_timer_check", false);
        message = configuration.getString("interact.mode", "v1");
        warningEnabled = configuration.getBoolean("hitbox.strict_mode", false);
        if (warningEnabled) {
            Matrix.getInstance().getLogger().warning(
                    "You have enabled the `hitbox.strict_mode` option, which is experimental and may cause false positives, please use it carefully!");
        }
        if (message.equals("v2")) {
            Matrix.logMessage(
                    "Interact mode is set to v2, this mode is experimental, use it at your own risk!");
        }
        if (enabled) {
            Matrix.getInstance().getLogger().warning(
                    "You are using legacy timer check, it's best to stop using it as soon as possible");
        }
        if (configuration.getBoolean("move.allow_bpe_bridge", false)) {
            Matrix.getInstance().getLogger().warning(
                    "You have enabled the `move.allow_bpe_bridge` option, which will cause the server to be more easy to bypass, please use it carefully!");
        }
        CollisionGeometry.loadExcludedMaterials();
    }

    private static String disabledWorldMessage(String check, String worlds) {
        return "* Disabled " + check + " check for world(s): " + worlds;
    }

    private static String enabledCountsMessage(int enabledCount, int disabledCount) {
        return "Enabled " + enabledCount + " checks, Disabled " + disabledCount + " checks";
    }

    private static String invalidCheckMessage(String path) {
        return "* warn: You put a illegal number in checks.yml @" + path + ".commands";
    }

    public static List<String> translateNames(List<String> input) {
        if (input == null) return null;
        return input.stream().map(CheckMessages::translate).collect(Collectors.toList());
    }

    public static String translate(String input) {
        Map replacements = LocaleMessages.globalPlaceholderValues;
        if (replacements.size() == 0) return input;
        String translated = input;
        for (Object entryObject : replacements.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            translated = translated.replace((CharSequence) entry.getKey(),
                    (CharSequence) entry.getValue());
        }
        return translated;
    }

}
