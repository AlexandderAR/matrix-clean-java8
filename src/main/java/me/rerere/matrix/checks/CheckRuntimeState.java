package me.rerere.matrix.checks;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.api.HackType;
import me.rerere.matrix.api.events.PlayerViolationEvent;
import me.rerere.matrix.checks.AbstractCheckAction;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.VerboseDetail;
import me.rerere.matrix.checks.movement.PacketMovementHooks;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.config.CheckSetting;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.listeners.EventHooks;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.notification.ViolationNotificationTask;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.util.CollectionsSupport;
import me.rerere.matrix.util.ObjectSampleBuffer;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;

/** Shared state and violation-reporting pipeline for individual checks. */
public abstract class CheckRuntimeState implements EventHooks, PacketMovementHooks {
    private long lastResetAt;
    private int localViolations;
    private final PlayerCheckContext playerState;
    private final Set<AbstractCheckAction> subscriptions = new HashSet<>();
    private CheckCategory checkType;

    private static final int LIMIT;
    private static final long resetWindowMillis;

    protected CheckRuntimeState(PlayerCheckContext playerState, CheckCategory checkType) {
        this.playerState = playerState;
        this.checkType = checkType;
    }

    protected CheckRuntimeState(PlayerCheckContext playerState) {
        this.playerState = playerState;
    }

    public final int violationCount() {
        return playerState().playerStorage().totalFor(checkType());
    }

    public CheckCategory checkType() { return checkType; }

    public boolean canEqual(Object other) { return other instanceof CheckRuntimeState; }

    public void loadConfiguration() {
        subscriptions.forEach(AbstractCheckAction::loadFromConfig);
    }

    public Player player() { return playerState.getPlayer(); }

    private static String lowTpsMessage(String checkName, String playerName) {
        return new StringBuilder("(Check) Ignored a ").append(checkName).append(" flag from ")
                .append(playerName).append(" because TPS is low").toString();
    }

    public final ConfigurationSection configuration() {
        CheckSetting config = (CheckSetting) Objects.requireNonNull(
                checkType().getConfiguration());
        return config.getConfigurationSection();
    }

    public static Unit runViolationTask(ViolationNotificationTask task) {
        task.run();
        return Unit.INSTANCE;
    }

    public boolean flag(String componentName, String violationMessage, int violationWeight) {
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) (Map<?, ?>)
                CollectionsSupport.linkedMapOf(new Object[0]);
        return flag(componentName, violationMessage, details, violationWeight);
    }

    public boolean flag(String componentName, String violationMessage,
                        Map<String, Object> details, int violationWeight) {
        if (violationWeight < 0) return false;
        ++localViolations;
        long now = MillisecondSampler.currentTimeMillis();
        if (now - lastResetAt > resetWindowMillis) {
            localViolations = 0;
            lastResetAt = MillisecondSampler.currentTimeMillis();
        }
        if (localViolations > LIMIT) return true;
        if (playerState() == null) return false;
        if (MatrixConfiguration.disabledComponents != null
                && MatrixConfiguration.disabledComponents.contains(componentName)) return false;
        if (MatrixConfiguration.customViolationWeights.containsKey(componentName))
            violationWeight = ((Integer) MatrixConfiguration.customViolationWeights
                    .get(componentName)).intValue();
        if (playerState.shouldBypass(checkType)) return false;

        if (MatrixConfiguration.tpsProtectionEnabled
                && RollingStatistics.metric() <= MatrixConfiguration.minimumTps) {
            if (checkType == CheckCategory.MOVE
                    || checkType == CheckCategory.BADPACKETS
                    || checkType == CheckCategory.INTERACT
                    || checkType == CheckCategory.BLOCK
                    || checkType == CheckCategory.PHASE) violationWeight /= 2;
            DebugLogger.printDebug(lowTpsMessage(
                    checkType.getDisplayName(), playerState().playerName()));
        }

        Matrix matrix = Matrix.getInstance();
        PlayerSessionRegistry reporter = matrix.getPlayerDataManager();
        ViolationTotals violationManager = playerState.playerStorage();
        CheckRegistry checkManager = playerState.checks();
        Objects.requireNonNull(violationManager, "null vl manager");
        Objects.requireNonNull(checkManager, "null check manager");
        Player eventPlayer = playerState().getPlayer();
        HackType hackType = checkType().toApiHackType();
        PlayerViolationEvent event = new PlayerViolationEvent(
                eventPlayer, componentName, hackType, violationMessage,
                (Map<String, String>) (Map<?, ?>) details, violationWeight);
        PluginManager plugins = Bukkit.getPluginManager();
        plugins.callEvent((Event) event);
        if (event.isCancelled()) return false;
        violationWeight = event.getViolations();

        for (Object candidate : checkManager.checks()) {
            CheckRuntimeState other = (CheckRuntimeState) candidate;
            if (other != this && other != null) {
                other.onCheckNotice(this, componentName, violationMessage, violationWeight);
            }
        }

        Player notificationPlayer = playerState().getPlayer();
        ObjectSampleBuffer history = playerState.packetHistory();
        String label = skippedMessage(checkType.getDisplayName(), violationMessage);
        history.add(new VerboseDetail(label, (Map<String, String>) (Map<?, ?>) details));
        ViolationNotificationTask task = new ViolationNotificationTask(
                notificationPlayer, checkType, violationWeight, componentName, violationMessage,
                details, playerState, reporter, violationManager);
        me.rerere.matrix.compat.FoliaSupport.INSTANCE.runSyncForPlayer(notificationPlayer, (Function0<Unit>) () -> {
            task.run();
            return Unit.INSTANCE;
        });
        return true;
    }

    public void setCheckType(CheckCategory category) { checkType = category; }

    private static String skippedMessage(String prefix, String detail) {
        return new StringBuilder(prefix).append(": ").append(detail).toString();
    }

    @Override public int hashCode() {
        int result = 1;
        result = 59 * result + localViolations;
        result = 59 * result + (int) (lastResetAt ^ (lastResetAt >>> 32));
        PlayerCheckContext context = playerState();
        result = 59 * result + (context == null ? 43 : context.hashCode());
        CheckCategory category = checkType();
        result = 59 * result + (category == null ? 43 : category.hashCode());
        result = 59 * result + (subscriptions == null ? 43 : subscriptions.hashCode());
        return result;
    }

    public void onCheckNotice(CheckRuntimeState sourceCheck, String componentName,
                              String detail, int violationWeight) {
        // This callback currently records no additional state.
    }

    public void reloadConfiguration() {
        // The base check has no configuration values to reload.
    }

    public PlayerCheckContext playerState() { return playerState; }
    public void addSubscription(AbstractCheckAction listener) { subscriptions.add(listener); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CheckRuntimeState)) return false;
        CheckRuntimeState that = (CheckRuntimeState) other;
        if (!that.canEqual(this)) return false;
        if (localViolations != that.localViolations || lastResetAt != that.lastResetAt) return false;
        PlayerCheckContext context = playerState();
        PlayerCheckContext otherContext = that.playerState();
        if (context == null ? otherContext != null : !context.equals(otherContext)) return false;
        CheckCategory category = checkType();
        CheckCategory otherCategory = that.checkType();
        return (category == null ? otherCategory == null : category.equals(otherCategory))
                && Objects.equals(subscriptions, that.subscriptions);
    }

    static {
        LIMIT = 100;
        resetWindowMillis = 1000L;
    }
}
