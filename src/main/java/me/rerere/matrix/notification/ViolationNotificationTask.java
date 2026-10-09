package me.rerere.matrix.notification;

import java.util.Map;
import org.bukkit.entity.Player;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.player.PlayerPingAccessor;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.text.ChatColorFormatter;

/** Sends a formatted violation notification and updates the player's violation state. */
public final class ViolationNotificationTask implements Runnable {
    private final Player player;
    private final CheckCategory checkType;
    private final int addedViolations;
    private final String component;
    private final String message;
    private final Map<?, ?> tooltips;
    private final PlayerCheckContext playerData;
    private final PlayerSessionRegistry playerDataManager;
    private final ViolationTotals violationManager;

    public ViolationNotificationTask(Player player, CheckCategory checkType, int addedViolations,
            String component, String message, Map<?, ?> tooltips, PlayerCheckContext playerData,
            PlayerSessionRegistry playerDataManager, ViolationTotals violationManager) {
        super();
        if (player == null) throw new NullPointerException("player is marked non-null but is null");
        if (checkType == null) throw new NullPointerException("checkType is marked non-null but is null");
        if (component == null) throw new NullPointerException("component is marked non-null but is null");
        if (message == null) throw new NullPointerException("message is marked non-null but is null");
        if (tooltips == null) throw new NullPointerException("tooltips is marked non-null but is null");
        if (playerData == null) throw new NullPointerException("playerData is marked non-null but is null");
        if (playerDataManager == null) throw new NullPointerException("playerDataManager is marked non-null but is null");
        if (violationManager == null) throw new NullPointerException("violationManager is marked non-null but is null");
        this.player = player;
        this.checkType = checkType;
        this.addedViolations = addedViolations;
        this.component = component;
        this.message = message;
        this.tooltips = tooltips;
        this.playerData = playerData;
        this.playerDataManager = playerDataManager;
        this.violationManager = violationManager;
    }

    public CheckCategory getCheckType() { return checkType; }
    public int getAddedViolations() { return addedViolations; }
    public Player getPlayer() { return player; }
    public PlayerSessionRegistry getPlayerDataManager() { return playerDataManager; }
    public ViolationTotals getViolationManager() { return violationManager; }
    public PlayerCheckContext getPlayerData() { return playerData; }
    public String getMessage() { return message; }
    public Map<?, ?> getTooltips() { return tooltips; }
    public String getComponent() { return component; }

    public static String prefixBullet(Object value) {
        return " ▪ " + value;
    }

    public static void logDebugMessage(String value) {
        Matrix.getInstance().getMatrixLogger().appendLog(value);
    }

    @Override
    public void run() {
        String playerName = player.getName();
        String checkName = checkType.getDisplayName();
        int ping = PlayerPingAccessor.getPing(player);
        double tps = NumberUtils.roundPowerOfTen(RollingStatistics.metric(), 2);
        int newViolationLevel = addedViolations + violationManager.totalFor(checkType);
        String tooltipText = DiagnosticFormat.joinMap(tooltips);

        String configuredComponent = component;
        if (LocaleMessages.componentAliases != null) {
            configuredComponent = LocaleMessages.componentAliases.getString(component, component);
        }

        String formatted = ChatColorFormatter.translateWithDefaultColorCode(LocaleMessages.verboseFormat)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%message%", message)
                .replace("%tooltips%", tooltipText)
                .replace("%add_vl%", String.valueOf(addedViolations))
                .replace("%vl%", String.valueOf(newViolationLevel))
                .replace("%ping%", String.valueOf(ping))
                .replace("%tps%", String.valueOf(tps))
                .replace("%component%", component)
                .replace("%component_alias%", configuredComponent)
                .replace("%version%", playerData.clientVersion().getReleaseName());

        if (Matrix.isRuntimeEnabled()) {
            Matrix.logMessage(formatted);
        }

        for (Object session : playerDataManager.sessions().values()) {
            PlayerCheckContext recipient = (PlayerCheckContext) session;
            if (!recipient.verboseEnabled()) continue;
            if (recipient.verboseRecipient() != null) {
                if (recipient.verboseRecipient() == null) continue;
                if (!player.getUniqueId().equals(recipient.verboseRecipient())) continue;
            }
            recipient.getPlayer().sendMessage(prefixBullet(formatted));
        }

        if (MatrixConfiguration.logFile) {
            Matrix.getInstance().getAsyncPool().enqueue(() -> logDebugMessage(formatted));
        }
        violationManager.updateViolationCommands(playerData, checkType, addedViolations, component, message);
    }
}
