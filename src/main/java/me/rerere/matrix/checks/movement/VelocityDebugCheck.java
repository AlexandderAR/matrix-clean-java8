package me.rerere.matrix.checks.movement;

import java.util.List;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.listeners.VelocityExplosionType;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.network.VelocityHandler;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.VelocityData;
import me.rerere.matrix.runtime.MatrixTasks;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Emits detailed movement-velocity diagnostics for the owning check. */
public final class VelocityDebugCheck extends CheckRuntimeState {
    public VelocityDebugCheck(PlayerCheckContext user) {
        super(user, CheckCategory.VELOCITY);
    }

    private static String movementLabel(ChatColor color, String source, String from,
                                        String to) {
        return color + "move: " + source + ", " + from + ", " + to;
    }

    private static String velocityDetails(String x, String y, String z,
                                          VelocityExplosionType category) {
        return new StringBuilder(x).append(", ").append(y).append(", ")
                .append(z).append(" | ").append(category).toString();
    }

    private static String velocityCount(int count) {
        return "Found " + count + " velocities";
    }

    private void emitReport(List<?> velocities, Location from, Location to) {
        if (!velocities.isEmpty()) {
            String count = velocityCount(velocities.size());
            MatrixTasks.broadcast(count);

            for (Object value : velocities) {
                VelocityData velocity = (VelocityData) value;
                String x = DiagnosticFormat.formatDouble(
                        velocity.getX());
                String y = DiagnosticFormat.formatDouble(
                        velocity.getY());
                String z = DiagnosticFormat.formatDouble(
                        velocity.getZ());
                VelocityExplosionType category = velocity.getType();
                String details = velocityDetails(x, y, z, category);
                MatrixTasks.broadcast(details);
            }
            Vector displacement = to.toVector().subtract(from.toVector());
            String movement = movementLabel(ChatColor.BLUE,
                    DiagnosticFormat.formatDouble(displacement.getX()),
                    DiagnosticFormat.formatDouble(displacement.getY()),
                    DiagnosticFormat.formatDouble(displacement.getZ()));
            MatrixTasks.broadcast(movement);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        PlayerCheckContext playerContext = playerState();
        VelocityHandler velocityHandler = playerContext.inventoryTracker();
        List<?> velocities = (List<?>) velocityHandler.getAuxiliaryValues();
        emitReport(velocities, from, to);
    }
}


