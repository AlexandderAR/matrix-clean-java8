package me.rerere.matrix.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import me.rerere.matrix.thirdparty.com.google.gson.Gson;
import me.rerere.matrix.thirdparty.okhttp3.Call;
import me.rerere.matrix.thirdparty.okhttp3.MediaType;
import me.rerere.matrix.thirdparty.okhttp3.Request;
import me.rerere.matrix.thirdparty.okhttp3.Request$Builder;
import me.rerere.matrix.thirdparty.okhttp3.Callback;
import me.rerere.matrix.thirdparty.okhttp3.RequestBody;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.api.notification.DiscordEmbed;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.config.ConfigField;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.discord.DiscordMessage;
import me.rerere.matrix.license.identity.Author;
import me.rerere.matrix.player.AvatarUrlFactory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.violation.Violation;
import me.rerere.matrix.webhook.DiscordWebhookCallback;
import org.bukkit.entity.Player;

/** Builds and asynchronously posts the rich report for a player kick event. */
public final class KickReportService {
    public static final AvatarUrlFactory IDENTITY_FORMATTER =
            new AvatarUrlFactory();

    private final Lazy<Gson> gson;

    public KickReportService() {
        this.gson = LazyKt.lazy((Function0<Gson>) KickReportService::newGson);
    }

    private static String formatReason(String reason) {
        return new StringBuilder("Reason: ").append(reason).toString();
    }

    private static Gson newGson() {
        return new Gson();
    }

    private final Gson gsonValue() {
        return gson.getValue();
    }

    public void postReport(DiscordMessage report) {
        if (!MatrixConfiguration.discordWebhookEnabled) {
            return;
        }

        Request request = new Request$Builder()
                .url(MatrixConfiguration.discordWebhookUrl)
                .post(RequestBody.Companion.create(
                        gsonValue().toJson(report),
                        MediaType.Companion.get("application/json")))
                .build();
        Call call = Matrix.getInstance().getRuntimeServices().httpClient()
                .newCall(request);
        call.enqueue((Callback) new DiscordWebhookCallback());
    }

    public void reportKick(Player player, String reason) {
        PlayerCheckContext playerContext = Matrix.Companion.matrixInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        List<ConfigField> fields;
        if (playerContext == null) {
            fields = Collections.emptyList();
        } else {
            ViolationTotals totals = playerContext.playerStorage();
            Map<CheckCategory, Violation> positiveViolations = totals.positiveTotals();
            if (positiveViolations == null) {
                fields = Collections.emptyList();
            } else {
                fields = new ArrayList<>(positiveViolations.size());
                for (Map.Entry<CheckCategory, Violation> entry : positiveViolations.entrySet()) {
                    String checkName = entry.getKey().name();
                    int violationLevel = entry.getValue().getLevel();
                    String displayLevel = formatViolationLevel(violationLevel);
                    fields.add(new ConfigField(checkName, displayLevel, true));
                }
            }
        }

        String formattedReason = formatReason(reason);
        String playerName = player.getName();
        String avatarUrl = IDENTITY_FORMATTER.createAvatarUrl(player.getUniqueId());
        Author author = new Author(playerName, avatarUrl);
        DiscordEmbed embed = new DiscordEmbed(
                "rich", "Player was kicked", formattedReason, 16_711_680,
                null, author, fields, 16, null);
        DiscordMessage report = new DiscordMessage(
                null, Collections.singletonList(embed), 1, null);
        postReport(report);
    }

    private static String formatViolationLevel(int level) {
        return new StringBuilder().append(level).append(" VL").toString();
    }

}
