package me.rerere.matrix.listeners;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Updates a player's remembered world and notifies eligible checks after a move. */
public final class WorldChangeListener implements org.bukkit.event.Listener {
    public WorldChangeListener() { }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerCheckContext state = Matrix.getInstance().getPlayerDataManager()
                .find(event.getPlayer().getUniqueId());
        if (state == null) return;

        Location destination = event.getTo();
        World world = destination == null ? null : destination.getWorld();
        String worldName = world == null ? null : world.getName();
        if (worldName == null) return;

        String rememberedWorldName = state.clientBrand();
        if (rememberedWorldName == null) {
            state.setClientBrand(worldName);
            return;
        }
        if (Intrinsics.areEqual(state.clientBrand(), worldName)) return;

        state.setClientBrand(worldName);
        List<?> checks = state.checks().checks();
        for (Object checkEntry : checks) {
            CheckRuntimeState check = (CheckRuntimeState) checkEntry;
            CheckCategory category = check.checkType();
            if (category.isEnabled() && !state.permissionBypass()) {
                org.bukkit.entity.Player player = event.getPlayer();
                Location currentDestination = event.getTo();
                Intrinsics.checkNotNull(currentDestination);
                World currentWorld = currentDestination.getWorld();
                Intrinsics.checkNotNull(currentWorld);
                check.onPlayerChangedWorld(new PlayerChangedWorldEvent(player, currentWorld));
            }
        }
    }
}
