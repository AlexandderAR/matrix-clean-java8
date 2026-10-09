package me.rerere.matrix.checks.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.CheckCategory;

/** Applies active movement-check responses to riptide and placed-entity events. */
public final class RiptideAndPlacementListener implements Listener {

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        PlayerCheckContext context = findPlayerState(player);
        if (context == null) return;
        context.velocityTracker().recordVelocity(1.5D, 4.0D, 3.0D);
        for (CheckRuntimeState check : context.checks().checks()) {
            if (isActiveForWorld(check, context, player)) check.onTickStart();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlaced(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        PlayerCheckContext context = findPlayerState(player);
        if (context == null) return;
        for (CheckRuntimeState check : context.checks().checks()) {
            if (isActiveForWorld(check, context, player)) {
                check.onEntitySpawn(event.getEntity());
            }
        }
    }

    private static PlayerCheckContext findPlayerState(Player player) {
        return Matrix.Companion.matrixInstance().getPlayerDataManager().find(player.getUniqueId());
    }

    private static boolean isActiveForWorld(CheckRuntimeState check,
                                            PlayerCheckContext context,
                                            Player player) {
        CheckCategory category = check.checkType();
        if (!category.isEnabled()) return false;
        if (context.shouldBypass(category)) return false;
        return category.isEnabledIn(player.getWorld());
    }

}
