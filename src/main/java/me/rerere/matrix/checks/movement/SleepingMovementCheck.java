package me.rerere.matrix.checks.movement;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.world.MaterialPolicy;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class SleepingMovementCheck extends CheckRuntimeState {
    private boolean bedEntered;

    public SleepingMovementCheck(PlayerCheckContext context) {
        super(context);
        bedEntered = false;
        setCheckType(CheckCategory.MOVE);
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        if (!bedEntered) {
            return;
        }
        if (player.isSleeping()) {
            double movement = MovementMetrics.locationDistance(from, to);
            if (movement > 0.0
                    && !MaterialPolicy.containsBedBridge(from.getBlock().getType())
                    && !MaterialPolicy.containsBedBridge(to.getBlock().getType())) {
                double roundedMovement = NumberUtils.round3(movement);
                String message = new StringBuilder("move when sleeping: ")
                        .append(roundedMovement)
                        .toString();
                flag("move.bed", message, 10);
                event.setTo(from);
            }
        } else {
            bedEntered = false;
        }
    }

    @Override
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        bedEntered = true;
    }
}
