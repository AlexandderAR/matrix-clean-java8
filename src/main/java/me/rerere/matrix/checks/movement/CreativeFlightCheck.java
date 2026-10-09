package me.rerere.matrix.checks.movement;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Detects excessive movement while a player is flying. */
public class CreativeFlightCheck extends CheckRuntimeState {
    private static boolean enabled;
    private static final long interval;
    private boolean wasFlying;

    static {
        interval = 1200L;
    }

    public CreativeFlightCheck(PlayerCheckContext context) {
        super(context);
        this.wasFlying = false;
        this.setCheckType(CheckCategory.MOVE);
    }

    public void reloadEnabled() {
        enabled = this.configuration().getBoolean("check_flying_players", false);
    }

    private static String formatFlightReason(double actual, double expected) {
        return new StringBuilder("creative fly (move: ").append(actual)
                .append(", exp: ").append(expected).append(")").toString();
    }

    private double expectedSpeed(Player player) {
        double expectedSpeed = (double) (player.getFlySpeed() * 10.0F) * 1.1D;
        long elapsed = MillisecondSampler.currentTimeMillis()
                - this.playerState().velocityTracker().latestSampleTime();
        if (elapsed <= interval) {
            expectedSpeed += this.playerState().velocityTracker().sumRecentMagnitudes();
        }
        return expectedSpeed + 0.1D;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (!player.isFlying()) {
            wasFlying = false;
            return;
        }
        if (Matrix.getServerVersion().isNewerThan(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8)
                && player.isGliding()) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_13)
                && (player.isSwimming() || player.isRiptiding())) return;

        double movementDistance = MovementMetrics.locationDistance(from, to);
        double allowedDistance = expectedSpeed(player);
        if (movementDistance > allowedDistance && wasFlying) {
            double roundedMovement = NumberUtils.round3(movementDistance);
            double roundedExpected = NumberUtils.round3(allowedDistance);
            String reason = formatFlightReason(roundedMovement, roundedExpected);
            if (this.flag("move.cre", reason, 10)) event.setTo(from);
        }
        wasFlying = true;
    }
}




