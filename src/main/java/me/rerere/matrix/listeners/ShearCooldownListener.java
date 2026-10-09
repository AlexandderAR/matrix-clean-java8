package me.rerere.matrix.listeners;

import me.rerere.matrix.license.LicenseResponseException;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.entity.Player;

public final class ShearCooldownListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player;
        boolean hasCooldown;
        try {
            player = event.getPlayer();
            hasCooldown = player.hasCooldown(Material.SHEARS);
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (hasCooldown) {
            event.setCancelled(true);
            return;
        }
        player = event.getPlayer();
        player.setCooldown(Material.SHEARS, 2);
    }
}
