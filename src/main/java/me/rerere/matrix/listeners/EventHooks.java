package me.rerere.matrix.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/** Event callback surface with no-op defaults. */
public interface EventHooks {
    default void onFishPrimary(PlayerFishEvent event) { }
    default void onMovementCancelled(boolean cancelled) { }
    default void onPlayerToggleSprint(PlayerToggleSprintEvent event) { }
    default void onDrop(PlayerDropItemEvent event) { }
    default void onBlockDamage(BlockDamageEvent event) { }
    default void onPlayerAnimation(PlayerAnimationEvent event) { }
    default void onBlockPlace(BlockPlaceEvent event) { }
    default void onBlockBreak(BlockBreakEvent event) { }
    default void onPlayerMove(PlayerMoveEvent event, Location from, Location to) { }
    default void onPlayerInteract(PlayerInteractEvent event) { }
    default void onPlayerTeleport(PlayerTeleportEvent event) { }
    default void onItemConsume(PlayerItemConsumeEvent event) { }
    default void onDamagedByEntity(EntityDamageByEntityEvent event) { }
    default void onAttackWithContext(EntityDamageByEntityEvent event) { }
    default void onTickStart() { }
    default void onEntityDamage(EntityDamageEvent event) { }
    default void onTick() { }
    default void onPlayerBedEnter(PlayerBedEnterEvent event) { }
    default void onEntityDamageWithContext(EntityDamageByEntityEvent event) { }
    default void onVehicleEnter(VehicleEnterEvent event) { }
    default void onVehicleExit(VehicleExitEvent event) { }
    default void onEntitySpawn(Entity entity) { }
    default void onInventoryClick(InventoryClickEvent event) { }
    default void onPlayerToggleFlight(PlayerToggleFlightEvent event) { }
    default void onBowShoot(EntityShootBowEvent event) { }
    default void onPlayerChangedWorld(PlayerChangedWorldEvent event) { }
    default void onFishSecondary(PlayerFishEvent event) { }
    default void onEntityDamage(EntityDamageByEntityEvent event) { }
    default void onAsyncChat(AsyncPlayerChatEvent event) { }
    default void onVelocity(PlayerVelocityEvent event) { }
    default void onHealthRegain(EntityRegainHealthEvent event) { }
    default void onPlayerRespawn(PlayerRespawnEvent event) { }
    default void onHeldItemChange(PlayerItemHeldEvent event) { }
}
