package me.rerere.matrix.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.rerere.matrix.gui.InventoryLayoutMenu;

/** Tracks the Matrix menu associated with each player. */
public final class PlayerInventoryMenuListener implements Listener {
    public static final PlayerInventoryMenuListener INSTANCE;
    private static final HashMap<UUID, InventoryLayoutMenu> OPEN_MENUS;

    static {
        INSTANCE = new PlayerInventoryMenuListener();
        OPEN_MENUS = new HashMap<>();
    }

    private PlayerInventoryMenuListener() {
    }

    public void closeTrackedInventories() {
        for (Map.Entry<UUID, InventoryLayoutMenu> entry : new HashMap<>(OPEN_MENUS).entrySet()) {
            org.bukkit.entity.Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
        }
        OPEN_MENUS.clear();
    }

    public void remove(UUID playerId) {
        OPEN_MENUS.remove(playerId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryLayoutMenu menu = OPEN_MENUS.get(event.getWhoClicked().getUniqueId());
        if (menu == null || !menu.isInventory(event.getInventory())) {
            return;
        }
        // Cancel all clicks while this menu is open, including transfers from the lower inventory.
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < event.getInventory().getSize()
                && menu.isInventory(event.getClickedInventory())) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryLayoutMenu menu = OPEN_MENUS.get(event.getWhoClicked().getUniqueId());
        if (menu == null || !menu.isInventory(event.getInventory())) return;
        int menuSize = event.getInventory().getSize();
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < menuSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void put(UUID playerId, InventoryLayoutMenu menu) {
        OPEN_MENUS.put(playerId, menu);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        InventoryLayoutMenu menu = OPEN_MENUS.get(playerId);
        if (menu != null && menu.isInventory(event.getInventory())) {
            remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    public InventoryLayoutMenu get(UUID playerId) {
        return OPEN_MENUS.get(playerId);
    }

}
