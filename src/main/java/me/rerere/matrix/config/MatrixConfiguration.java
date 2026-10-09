package me.rerere.matrix.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.event.EventPriority;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.i18n.LocaleConfiguration;
import me.rerere.matrix.util.NumberUtils;

/** Matrix YAML settings, including the priority and custom violation-weight tables. */
public class MatrixConfiguration extends LocaleConfiguration {
    public static final Map<String, EventPriority> priorities;
    public static boolean showPrefixInKick;
    public static int permissionRefreshInterval;
    public static boolean geyserSupport;
    public static double minimumTps;
    public static boolean scaffoldMitigation;
    public static boolean pingObfuscation;
    public static boolean disableBypassPermission;
    public static boolean hideLocation;
    public static boolean logFile;
    public static boolean hidePrefixInKick;
    public static boolean discordWebhookEnabled;
    public static boolean kickStrikeLightning;
    public static final Map<String, Integer> customViolationWeights;
    public static boolean tpsProtectionEnabled;
    public static List<String> disabledComponents;
    public static boolean antiHealthIndicator;
    public static int relogTime;
    public static String discordWebhookUrl;
    public static boolean attackMitigation;
    public static long lagThreshold;
    public static boolean offlinePlan;
    public static boolean checkUpdate;
    public static int deleteTime;
    public static String authLine;
    public static String accessToken;

    static {
        priorities = new LinkedHashMap<>();
        customViolationWeights = new LinkedHashMap<>();
        showPrefixInKick = false;
    }

    public MatrixConfiguration() { super("config.yml"); }

    private static String disabledComponentsMessage(String components) {
        return new StringBuilder(" (!) | (Config) Disabled components: ")
            .append(components).toString();
    }

    private static String malformedWeightMessage(String entry) {
        return new StringBuilder("Malformed custom vl weight setting: ")
            .append(entry).toString();
    }

    private static String unknownPriorityMessage(String value) {
        return new StringBuilder("(Config Error) Unknown Event Priority: ")
            .append(value).toString();
    }

    private static String priorityChangedMessage(String name, String newValue) {
        return new StringBuilder(" (!) | (Priority) ").append(name)
            .append(" -> ").append(newValue).toString();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        priorities.clear();
        customViolationWeights.clear();

        showPrefixInKick = yaml().getBoolean("show_prefix_in_kick", false);
        logFile = yaml().getBoolean("log_file", false);
        tpsProtectionEnabled = yaml().getBoolean("tps_protection.enable");
        minimumTps = yaml().getDouble("tps_protection.min_tps", 17.0D);
        lagThreshold = yaml().getLong("tps_protection.lag_threshold", 155L);
        disableBypassPermission = yaml().getBoolean("disable_bypass_permission", false);
        disabledComponents = yaml().getStringList("disabled_components");
        antiHealthIndicator = yaml().getBoolean("anti-health-indicator", true);
        pingObfuscation = yaml().getBoolean("ping-obfuscation", true);
        scaffoldMitigation = yaml().getBoolean("mitigation.scaffold", true);
        attackMitigation = yaml().getBoolean("mitigation.attack", true);

        if (disabledComponents.size() > 1) {
            Matrix.logMessage(disabledComponentsMessage(String.valueOf(disabledComponents)));
            Matrix.logMessage(
                " (!) | (Config) Do not use `disabled_components` in config.yml, it will be removed in the future");
            Matrix.waitForServer(1500L);
        }

        offlinePlan = yaml().getBoolean("offline-plan", true);
        checkUpdate = yaml().getBoolean("check_update", true);
        hideLocation = yaml().getBoolean("hide_loc", false);
        relogTime = yaml().getInt("relog_time", -1);
        geyserSupport = yaml().getBoolean("bungee_geysermc_support", false);
        permissionRefreshInterval = yaml().getInt("permission_refresh_interval", 15);
        kickStrikeLightning = yaml().getBoolean("kick_strike_lightning", true);
        hidePrefixInKick = yaml().getBoolean("hide_prefix_in_kick", false);
        deleteTime = yaml().getInt("delete_time", 30);
        discordWebhookEnabled = yaml().getBoolean("discord_webhook.enable", false);
        discordWebhookUrl = yaml().getString("discord_webhook.url", "");
        authLine = readAuthLine();
        accessToken = yaml().getString("access_token", "");

        ConfigurationSection prioritySection = yaml().getConfigurationSection("priority");
        if (prioritySection != null) {
            for (String componentName : prioritySection.getKeys(false)) {
                String priorityName = prioritySection.getString(componentName);
                try {
                    EventPriority priority = EventPriority.valueOf(priorityName);
                    priorities.put(componentName, priority);
                    Matrix.logMessage(
                        priorityChangedMessage(componentName, priority.name()));
                } catch (Exception failure) {
                    failure.printStackTrace();
                    Matrix.logMessage(
                        unknownPriorityMessage(priorityName));
                }
            }
        }

        List<String> weightEntries = yaml().getStringList("custom_vl_weight");
        for (String entry : weightEntries) {
            String[] parts = entry.split(":");
            if (parts.length < 2) {
                Logger logger = Matrix.getInstance().getLogger();
                logger.warning(malformedWeightMessage(entry));
                continue;
            }
            String component = parts[0];
            String weightText = parts[1];
            boolean isInteger = NumberUtils.isInteger(weightText);
            int weight = isInteger ? Integer.parseInt(weightText) : 1;
            customViolationWeights.put(component, Integer.valueOf(weight));
        }
    }

    private YamlFile yaml() { return this.configuration(); }

    private static String readAuthLine() {
        try (InputStream input = Matrix.getInstance().getResource(".auth")) {
            if (input != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    return reader.readLine();
                }
            }
        } catch (IOException failure) {
            failure.printStackTrace();
        }
        return null;
    }

}
