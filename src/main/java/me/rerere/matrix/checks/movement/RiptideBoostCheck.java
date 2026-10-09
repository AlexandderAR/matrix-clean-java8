package me.rerere.matrix.checks.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class RiptideBoostCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private long lastViolationAtMillis;
    private int consecutiveRiptideMoves;
    private double previousDistance;
    private static int violationWeight;

    public RiptideBoostCheck(me.rerere.matrix.player.PlayerCheckContext context) {
        super(context);
        setCheckType(me.rerere.matrix.checks.CheckCategory.MOVE);
    }

    public void reloadConfiguration() {
        violationWeight = configuration().getInt("trident_boost_vl_weight", 20);
    }

    private static String formatViolationMessage(double previousDistance, double currentDistance) {
        return new StringBuilder("riptide boost in air (")
                .append(previousDistance)
                .append("->")
                .append(currentDistance)
                .append(")")
                .toString();
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        if (player.isRiptiding() && !player.isGliding()) {
            consecutiveRiptideMoves++;
            double distance = to.distance(from);
            if (consecutiveRiptideMoves > 3
                    && distance > previousDistance + 0.5
                    && distance > 2.5
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis()
                            - playerState().velocityTracker().latestSampleTime() > 100L) {
                double roundedPreviousDistance = me.rerere.matrix.util.NumberUtils.round3(previousDistance);
                double roundedDistance = me.rerere.matrix.util.NumberUtils.round3(distance);
                String message = formatViolationMessage(roundedPreviousDistance, roundedDistance);
                flag("move.riptide", message, violationWeight);
                event.setTo(from);
                player.setVelocity(new Vector(0, 0, 0));
                lastViolationAtMillis = System.currentTimeMillis();
            }
            previousDistance = distance;
            if (System.currentTimeMillis() - lastViolationAtMillis < 2000L) {
                event.setTo(from);
                if (player.isGliding()) {
                    player.setGliding(false);
                }
            }
        } else {
            consecutiveRiptideMoves = 0;
        }
    }
}
