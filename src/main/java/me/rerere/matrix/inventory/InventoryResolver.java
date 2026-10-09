package me.rerere.matrix.inventory;

import org.bukkit.inventory.Inventory;

/** Provides the two inventory views used by Matrix inventory checks. */
public interface InventoryResolver {
    Inventory getTopInventory();
    Inventory getBottomInventory();
}
