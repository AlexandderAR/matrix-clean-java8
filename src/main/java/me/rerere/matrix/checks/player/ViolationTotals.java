package me.rerere.matrix.checks.player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.api.events.PlayerViolationCommandEvent;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.config.ConfigField;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.discord.DiscordMessage;
import me.rerere.matrix.license.identity.Author;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.network.KickReportService;
import me.rerere.matrix.player.AvatarUrlFactory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerPingAccessor;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$ObjectRef;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.Regex;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.violation.Violation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Per-player violation totals and notification commands. */
public final class ViolationTotals {
    private final UUID playerId;
    private final Map<CheckCategory, Violation> totals;

    public ViolationTotals(UUID playerId) {
        this.playerId = playerId;
        Map<CheckCategory, Violation> initialized = new LinkedHashMap<>();
        for (CheckCategory category : CheckCategory.values()) {
            initialized.put(category, new Violation(playerId, category));
        }
        this.totals = initialized;
    }

    public void updateEveryEntry() {
        for (Violation violation : totals.values()) {
            violation.clear();
        }
    }

    public Map<CheckCategory, Violation> positiveTotals() {
        Map<CheckCategory, Violation> selected = new LinkedHashMap<>();
        for (CheckCategory category : CheckCategory.values()) {
            if (totalFor(category) > 0) {
                Violation violation = totals.get(category);
                Intrinsics.checkNotNull(violation);
                selected.put(category, violation);
            }
        }
        return selected;
    }

    public int sumTotals() {
        int sum = 0;
        for (Violation violation : totals.values()) {
            sum += violation.getLevel();
        }
        return sum;
    }

    public int totalFor(CheckCategory category) {
        Violation violation = totals.get(category);
        return violation == null ? 0 : violation.getLevel();
    }

    public void updateViolationCommands(PlayerCheckContext playerContext, CheckCategory check,
                                        int increment, String component, String message) {
        if (increment == 0) return;

        Violation counter = totals.get(check);
        if (counter == null) return;
        int previousTotal = counter.getLevel();
        counter.add(increment);
        int currentTotal = counter.getLevel();

        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection commandSettings =
                LocaleMessages.componentAliases;
        String componentAlias = commandSettings == null
                ? component : commandSettings.getString(component, component);
        Map<?, ?> configuredCommands = check.getCommandOptions();
        Map<Object, Object> selectedCommands = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : configuredCommands.entrySet()) {
            int threshold = ((Number) entry.getKey()).intValue();
            if (previousTotal + 1 <= threshold && threshold <= currentTotal) {
                selectedCommands.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<Object, Object> entry : selectedCommands.entrySet()) {
            for (Object commandValue : (List<?>) entry.getValue()) {
                String template = (String) commandValue;
                Player player = playerForCommands(playerContext);
                Ref$ObjectRef<String> rendered = new Ref$ObjectRef<>();
                String command = template
                        .replace("%player%", player.getName())
                        .replace("%ping%", String.valueOf(PlayerPingAccessor.getPing(player)))
                        .replace("%world%", player.getWorld().getName())
                        .replace("%tps%", String.valueOf(RollingStatistics.metric()))
                        .replace("%version%", playerContext.clientVersion().getReleaseName())
                        .replace("%message%", java.util.Objects.requireNonNull(message))
                        .replace("%component%", java.util.Objects.requireNonNull(component))
                        .replace("%component_alias%", java.util.Objects.requireNonNull(componentAlias))
                        .replace("%vl%", String.valueOf(currentTotal));
                rendered.element = command;

                MatrixConfigBridge matrixAccess = Matrix.Companion;
                if (matrixAccess.isEnabled()) {
                    rendered.element = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, rendered.element);
                }

                PlayerViolationCommandEvent commandEvent = new PlayerViolationCommandEvent(
                        player, check.toApiHackType(), rendered.element);
                Bukkit.getPluginManager().callEvent(commandEvent);
                if (commandEvent.isCancelled()) continue;

                FoliaSupport.INSTANCE.runAtGlobalScheduler(() -> dispatchCommand(rendered));
                String scheduledCommand = rendered.element;
                matrixAccess.matrixInstance().getAsyncPool().enqueue(() -> logCommand(scheduledCommand));

                if (!template.startsWith("matrix notify")) continue;

                CharSequence commandText = scheduledCommand;
                Regex whitespace = new Regex("\\s+");
                List<String> words = whitespace.split(commandText, 0);
                List<String> remainingWords = me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt
                        .drop((Iterable<? extends String>) words, 2);
                String details = String.join(" ", remainingWords);

                Matrix matrix = matrixAccess.matrixInstance();
                KickReportService notifier = matrix.getKickReportService();
                AvatarUrlFactory authorFormatter = KickReportService.IDENTITY_FORMATTER;
                Author author = new Author(player.getName(), authorFormatter.createAvatarUrl(player.getUniqueId()));
                ConfigField[] embedFields = new ConfigField[] {
                    new ConfigField("Check", check.getDisplayName(), true),
                    new ConfigField("Component", component, true),
                    new ConfigField("Violation Level",
                            formatViolation(previousTotal, currentTotal, increment), true),
                    new ConfigField("Client Version", playerContext.clientVersion().getReleaseName(), true),
                    new ConfigField("World", player.getWorld().getName(), true),
                    new ConfigField("Ping", String.valueOf(PlayerPingAccessor.getPing(player)), true),
                    new ConfigField("TPS", String.valueOf(RollingStatistics.metric()), true)
                };
                List<ConfigField> fields =
                        me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.listOf(embedFields);
                me.rerere.matrix.api.notification.DiscordEmbed embed =
                        new me.rerere.matrix.api.notification.DiscordEmbed(
                                "rich", "Details", details, 16750848, null, author, fields);
                List<me.rerere.matrix.api.notification.DiscordEmbed> embeds =
                        me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.listOf(embed);
                DiscordMessage notification = new DiscordMessage(null, embeds, 1, null);
                notifier.postReport(notification);
            }
        }
    }

    private static Player playerForCommands(PlayerCheckContext playerContext) {
        try {
            return playerContext.getPlayer();
        } catch (Throwable failure) {
            ViolationTotals.<RuntimeException>sneakyThrow(failure);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }

    private static String formatViolation(int oldValue, int newValue, int increase) {
        return new StringBuilder().append(oldValue).append(" -> ").append(newValue)
                .append(" (+").append(increase).append(")").toString();
    }

    private static String formatCommand(String command) {
        return new StringBuilder("Execute command: ").append(command).toString();
    }

    private static void logCommand(String command) {
        MatrixConfigBridge matrixAccess = Matrix.Companion;
        matrixAccess.matrixInstance().getMatrixLogger().appendLog(formatCommand(command));
    }

    private static Unit dispatchCommand(Ref$ObjectRef<?> command) {
        CommandSender sender = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(sender, (String) command.element);
        return Unit.INSTANCE;
    }

    public Map<CheckCategory, Violation> totals() { return totals; }
    public UUID playerId() { return playerId; }
}
