package me.rerere.matrix.checks.movement;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.GeometryMath;

public class RepeatedYawStepCheck extends CheckRuntimeState {
    private int repeatedSteps;
    private double previousYawStep;

    public RepeatedYawStepCheck(PlayerCheckContext context) {
        super(context);
        setCheckType(CheckCategory.MOVE);
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (event.getPlayer().isInsideVehicle()) {
            return;
        }
        double yawStep = GeometryMath.angularDistance(from.getYaw(), to.getYaw());
        if (yawStep == previousYawStep && yawStep % 1.0 == 0.0 && yawStep > 5.0
                && from.getYaw() * to.getYaw() > 0.0f) {
            if (++repeatedSteps > 2) {
                String message = new StringBuilder("t: spin (").append(from.getYaw())
                        .append("->").append(to.getYaw()).append(")").toString();
                flag("move.spin", message, 10);
            }
        } else {
            repeatedSteps = 0;
        }
        previousYawStep = yawStep;
    }
}
