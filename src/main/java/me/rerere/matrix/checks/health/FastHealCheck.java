package me.rerere.matrix.checks.health;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.Matrix;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffectType;

/** Detects unusually frequent satiation healing and optionally halves its amount. */
public class FastHealCheck extends CheckRuntimeState {
    private static final long RECENT_DAMAGE_WINDOW_MILLIS = 500L;
    private static boolean cancelHealingAmount;
    private static int healingCheckThreshold;

    private long previousRegainAt;
    private long previousDamageAt;

    public FastHealCheck(PlayerCheckContext playerData) {
        super(playerData);
        setCheckType(CheckCategory.DELAY);
    }

    public void reloadConfiguration() {
        ConfigurationSection configuration = configuration();
        healingCheckThreshold = configuration.getInt("check_healing", 1);
        cancelHealingAmount = configuration().getBoolean("cancel_action", true);
    }

    public void onHealthRegain(EntityRegainHealthEvent event) {
        long elapsedSincePreviousRegain = System.currentTimeMillis() - previousRegainAt;
        Player player = (Player) event.getEntity();
        Difficulty difficulty = player.getWorld().getDifficulty();

        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.REGENERATION)) {
            return;
        }
        if (RollingStatistics.metric() < 19.5D) {
            return;
        }

        double allowedDelayMillis = 495.0D;
        if (difficulty == Difficulty.PEACEFUL) {
            allowedDelayMillis = 450.0D;
        }
        if (difficulty == Difficulty.EASY || difficulty == Difficulty.NORMAL
                || difficulty == Difficulty.HARD) {
            allowedDelayMillis = 3800.0D;
        }

        ServerVersion serverVersion = Matrix.getServerVersion();
        if (serverVersion.isNewerThan(ServerVersion.V_1_8_8)) {
            allowedDelayMillis = 420.0D;
        }
        if (System.currentTimeMillis() - previousDamageAt < RECENT_DAMAGE_WINDOW_MILLIS) {
            allowedDelayMillis -= 100.0D;
        }

        if ((double) elapsedSincePreviousRegain < allowedDelayMillis
                && healingCheckThreshold >= 0) {
            double measuredDelay = NumberUtils.round3(
                    MillisecondSampler.nanosToMillis(elapsedSincePreviousRegain));
            ClientVersion clientVersion = Matrix.getServerVersion().toClientVersion();
            ClientVersion playerVersion = playerState().clientVersion();
            String report = formatHealingReport(difficulty.name().toLowerCase(),
                    Double.toString(measuredDelay), String.valueOf(clientVersion),
                    playerVersion.getReleaseName());
            flag("delay.heal", report, healingCheckThreshold);

            if (cancelHealingAmount) {
                event.setAmount(event.getAmount() / 2.0D);
            }
        }
        previousRegainAt = System.currentTimeMillis();
    }

    private static String formatHealingReport(String difficulty, String delay,
                                              String serverVersion, String playerVersion) {
        return new StringBuilder("perform actions faster than expected // heal (difficulty: ")
                .append(difficulty)
                .append(", delay: ").append(delay)
                .append(", ver: ").append(serverVersion)
                .append(", ver_p: ").append(playerVersion)
                .append(')').toString();
    }

    public void onEntityDamage(EntityDamageEvent event) {
        previousDamageAt = System.currentTimeMillis();
    }
}
