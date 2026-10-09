package me.rerere.matrix.listeners;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.EntityType;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/** Routes server events to checks and keeps player movement and client world state current. */
public class BlockWorldListener implements Listener {
    private static final long JOIN_NOTICE_DELAY_TICKS = 20L;
    private static final Set<Material> MOVEMENT_MARKERS = new HashSet<>();
    private static final Set<String> TRUSTED_NAMES = new HashSet<>();
    public static boolean movementPredictionEnabled;

    static {
        TRUSTED_NAMES.add("RE_OvO");
        TRUSTED_NAMES.add("Elaina_OvO");
        TRUSTED_NAMES.add("MIYUXD");
    }

    @EventHandler
    public void warnOnPlayerLimit(PlayerJoinEvent event) {
        if (!me.rerere.matrix.player.PlanState.INSTANCE
                .hasTrialOrNoPlans()) {
            return;
        }
        if (Bukkit.getOnlinePlayers().size() <= 50) {
            return;
        }
        String playerName = event.getPlayer().getName();
        me.rerere.matrix.Matrix.getInstance().getLogger()
                .warning(trialJoinMessage(playerName));
        me.rerere.matrix.Matrix.getInstance().getLogger()
                .warning("The maximum number of players allowed to join the server is 50!");
    }

    @EventHandler
    public void dispatchSprintToggleToChecks(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType().isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(check.checkType())) {
                continue;
            }
            if (!check.checkType().isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onPlayerToggleSprint(event);
        }
    }

    @EventHandler
    public void dispatchFishingEventToChecks(PlayerFishEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(check.checkType())) {
                continue;
            }
            if (!check.checkType().isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onFishSecondary(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dispatchBlockDamageToChecks(BlockDamageEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType().isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(check.checkType())) {
                continue;
            }
            if (!check.checkType().isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onBlockDamage(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void dispatchEntityDamageToChecks(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType().isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(check.checkType())) {
                continue;
            }
            if (!check.checkType().isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onEntityDamage(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dispatchInventoryClickToChecks(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType().isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(check.checkType())) {
                continue;
            }
            if (!check.checkType().isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onInventoryClick(event);
        }
    }

    @EventHandler
    public void dispatchDroppedItemToChecks(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onDrop(event);
        }
    }

    @EventHandler
    public void resetMotionAfterFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerToggleFlight(event);
        }
        playerData.velocityTracker().recordVelocity(0.0D, 1.0D, 0.0D);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dispatchHealthRegainToChecks(EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onHealthRegain(event);
        }
    }

    @EventHandler
    public void dispatchPlayerVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) return;
        Player player = (Player) event.getExited();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onVehicleExit(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void resetPlayerStateOnRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        me.rerere.matrix.replay.PlayerMovementState teleportState = playerData.movementState();
        teleportState.resetLocation(event.getRespawnLocation());
        me.rerere.matrix.entity.EntityMetadataState idState = playerData.packetState();
        idState.synchronizeFromPlayer(player);
        boolean hasAdjacentSolidMaterial = me.rerere.matrix.world.BlockGeometry
                .hasNonOpenAdjacentMaterial(event.getRespawnLocation());
        playerData.setCollidingBelow(hasAdjacentSolidMaterial);
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerRespawn(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void dispatchArmSwingAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        playerData.processActions();
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerAnimation(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void dispatchBlockBreakToChecks(BlockBreakEvent event) {
        if (me.rerere.matrix.commands.DeveloperCommand.denyBlockBreak) {
            event.setCancelled(true);
        }
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        if (event.getBlock().hasMetadata("block-ignore")) return;
        if (event.getClass() != BlockBreakEvent.class) return;
        playerData.processActions();
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onBlockBreak(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dispatchBedEntryToChecks(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerBedEnter(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void dispatchPlayerInteraction(PlayerInteractEvent event) {
        if (event.getClass() != PlayerInteractEvent.class) return;
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        playerData.processActions();
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerInteract(event);
        }
    }

    @EventHandler
    public void dispatchEntityDamageByEntityToChecks(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (event.getDamager().getType()
                != me.rerere.matrix.runtime.EntityCategories.legacyEndCrystal()) return;
        Player player = (Player) event.getEntity();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        playerData.velocityTracker().recordVelocity(0.5D, 1.0D, 0.5D);
    }

    @EventHandler
    public void updatePlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        if (event.getTo() == null) return;
        if (!event.isCancelled()) {
            playerData.movementState().updateServerLocation(event.getTo());
            if (me.rerere.matrix.compat.FoliaSupport.isLocationOwnedByCurrentRegion(event.getTo())) {
                boolean hasAdjacentSolidMaterial = me.rerere.matrix.world.BlockGeometry
                        .hasNonOpenAdjacentMaterial(event.getTo());
                playerData.setCollidingBelow(hasAdjacentSolidMaterial);
            }
        }
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onPlayerTeleport(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (!me.rerere.matrix.checks.network.IllegalPacketCheck.hasFiniteCoordinates(from)
                || !me.rerere.matrix.checks.network.IllegalPacketCheck.hasFiniteCoordinates(to)) {
            event.setCancelled(true);
            event.setTo(from);
            return;
        }
        if (!isFiniteFloat(from.getYaw()) || !isFiniteFloat(to.getYaw())) {
            event.setCancelled(true);
            event.setTo(from);
            return;
        }
        if (!to.getWorld().equals(from.getWorld()) || player.isDead()) {
            return;
        }
        if (!me.rerere.matrix.world.ChunkLoading.areSurroundingChunksLoaded(to.getWorld(), to.getX(), to.getZ())
                || !me.rerere.matrix.world.ChunkLoading.areSurroundingChunksLoaded(from.getWorld(), from.getX(), from.getZ())) {
            return;
        }
        me.rerere.matrix.player.PlayerCheckContext playerData =
                me.rerere.matrix.Matrix.getInstance()
                        .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        playerData.processActions();
        me.rerere.matrix.checks.MutableGeometryState movementState = playerData.movementContext();
        if (me.rerere.matrix.checks.CheckCategory.MOVE.isEnabled()) {
            movementState.index = me.rerere.matrix.entity.MovementCollisionEvaluator.evaluate(
                    from, to, player, playerData);
        } else {
            me.rerere.matrix.world.CollisionRegion collisionRegion = new me.rerere.matrix.world.CollisionRegion(
                    from.toVector(), to.toVector(), MOVEMENT_MARKERS, false, false, false, MOVEMENT_MARKERS);
            movementState.index = new me.rerere.matrix.player.model.PreparedMovementData(
                    false, false, false, collisionRegion);
        }
        movementState.volume = false;
        if (movementPredictionEnabled) {
            Vector displacement = to.toVector().subtract(from.toVector());
            me.rerere.matrix.player.model.MovementCheck movementCheck = playerData.movementCheck();
            me.rerere.matrix.player.model.PredictionContext predictionContext = new me.rerere.matrix.player.model.PredictionContext(
                    playerData, displacement, movementState.vector,
                    from, to, to.getYaw());
            me.rerere.matrix.prediction.PredictionResult predicted = movementCheck.evaluate(
                    predictionContext);
            movementState.vector = predicted.velocityUpdated();
        }
        try {
            for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
                if (!check.checkType().isEnabled()) continue;
                if (playerData.shouldBypass(check.checkType())) continue;
                if (!check.checkType().isEnabledIn(player.getWorld())) continue;
                check.onPlayerMove(event, from, to);
            }
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Location destination = event.getTo();
        playerData.movementState().updateServerLocation(destination);
        playerData.setCollidingBelow(playerData.locationFlags().getOrCompute(destination));
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit sendIdentityNotice(Player player) {
        String matrixVersion = me.rerere.matrix.Matrix.getInstance().getDescription().getVersion();
        me.rerere.matrix.license.LicenseMetadataProvider licenseMetadata = me.rerere.matrix.util.BuildMetadata.LICENSE_METADATA;
        String userName = licenseMetadata.getDisplayLabel();
        String userId = licenseMetadata.getBuildNumber();
        String plan = me.rerere.matrix.player.PlanState.INSTANCE
                .display();
        String serverVersion = Bukkit.getVersion();
        String serverName = Bukkit.getName();
        String serverBuild = String.valueOf(Bukkit.getServer());
        player.sendMessage(serverIdentityMessage(matrixVersion, userName, userId,
                plan, serverVersion, serverName, serverBuild));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static void scheduleIdentityNotice(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!TRUSTED_NAMES.contains(player.getName())) return;
        me.rerere.matrix.compat.FoliaSupport.INSTANCE.runTaskAsyncLater(
                () -> sendIdentityNotice(player), JOIN_NOTICE_DELAY_TICKS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleIdentityNotice(event);
    }

    @EventHandler
    public void dispatchHeldItemToChecks(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onHeldItemChange(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void dispatchConsumedItemToChecks(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onItemConsume(event);
        }
    }

    @EventHandler
    public void resetStateAfterWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        if (playerData == null) return;
        if (playerData.movementContext() != null) {
            playerData.movementContext().capacity = null;
            playerData.combatTracker().setLastLocation(null);
        }
        playerData.movementState()
                .resetLocation(player.getLocation());
        playerData.packetState()
                .synchronizeFromPlayer(player);
        boolean specialLocation = me.rerere.matrix.world.BlockGeometry.hasNonOpenAdjacentMaterial(player.getLocation());
        playerData.setCollidingBelow(specialLocation);
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            check.onPlayerChangedWorld(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dispatchBowShotToChecks(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        if (playerData == null) return;
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(player.getWorld())) continue;
            check.onBowShoot(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void dispatchBlockPlaceToChecks(BlockPlaceEvent event) {
        if (event.getClass() != BlockPlaceEvent.class) return;
        if (me.rerere.matrix.commands.DeveloperCommand.denyPlace) event.setCancelled(true);
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        if (playerData == null) return;
        playerData.processActions();
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(player.getWorld())) continue;
            check.onBlockPlace(event);
        }
    }

    private static void updatePistonAffectedBlock(Location location, Block block) {
        for (Player nearbyPlayer : (Set<Player>) me.rerere.matrix.player.PlayerUtilities.nearbyPlayers(location)) {
            me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                    .getPlayerDataManager()
                    .find(nearbyPlayer.getUniqueId());
            if (playerData == null) continue;
            playerData.movementContext()
                    .timeout
                    .add(block);
        }
    }

    @EventHandler
    public void updatePistonRetractRegions(BlockPistonRetractEvent event) {
        for (Block affectedBlock : event.getBlocks()) {
            updatePistonAffectedBlock(affectedBlock.getLocation(), affectedBlock);
        }
        Block piston = event.getBlock();
        updatePistonAffectedBlock(piston.getLocation(), piston);
    }

    @EventHandler
    public void updatePistonExtendRegions(BlockPistonExtendEvent event) {
        for (Block affectedBlock : event.getBlocks()) {
            updatePistonAffectedBlock(affectedBlock.getLocation(), affectedBlock);
        }
        Block piston = event.getBlock();
        updatePistonAffectedBlock(piston.getLocation(), piston);
    }

    @EventHandler
    public void dispatchVehicleEnterToChecks(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;
        Player player = (Player) event.getEntered();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        if (playerData == null) return;
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(player.getWorld())) continue;
            check.onVehicleEnter(event);
        }
    }

    @EventHandler
    public void handleFishingCatchOfPlayer(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        if (event.getCaught() == null) return;
        if (!(event.getCaught() instanceof Player)) return;
        Player caughtPlayer = (Player) event.getCaught();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(caughtPlayer.getUniqueId());
        if (playerData == null) return;
        Vector pullVector = event.getPlayer().getLocation().toVector()
                .subtract(event.getCaught().getLocation().toVector())
                .multiply(0.1D).multiply(1.2D);
        me.rerere.matrix.player.VelocityHistory velocityHistory = playerData.velocityTracker();
        velocityHistory.recordExplosion(pullVector);
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(caughtPlayer.getWorld())) continue;
            check.onFishPrimary(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void dispatchPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(attacker.getUniqueId());
        if (playerData == null) return;
        playerData.processActions();
        if (attacker.hasMetadata("IGNORE_ATTACK")) return;
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(attacker.getWorld())) continue;
            try {
                if (event.getEntity() instanceof LivingEntity) {
                    check.onAttackWithContext(event);
                }
                check.onEntityDamageWithContext(event);
            } catch (Exception failure) {
                failure.printStackTrace();
                System.out.println(errorAtMessage(
                        check.checkType().getDisplayName()));
            }
        }
    }

    @EventHandler
    public void dispatchVehiclePassengerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Vehicle)) return;
        Vehicle vehicle = (Vehicle) event.getEntity();
        Player passengerPlayer = null;
        if (me.rerere.matrix.Matrix.getServerVersion()
                .isNewerThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)) {
            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger.getType() == EntityType.PLAYER) {
                    passengerPlayer = (Player) passenger;
                }
            }
        } else {
            Entity passenger = vehicle.getPassenger();
            if (passenger != null && passenger.getType() == EntityType.PLAYER) {
                passengerPlayer = (Player) passenger;
            }
        }
        if (passengerPlayer == null) return;
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(passengerPlayer.getUniqueId());
        if (playerData == null) return;
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) continue;
            if (playerData.shouldBypass(
                    check.checkType())) continue;
            if (!check.checkType()
                    .isEnabledIn(passengerPlayer.getWorld())) continue;
            check.onDamagedByEntity(event);
        }
    }

    @EventHandler
    public void dispatchPlayerDamagedByLivingEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!(event.getDamager() instanceof LivingEntity)) return;
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            try {
                check.onEntityDamage(event);
            } catch (Exception failure) {
                failure.printStackTrace();
                System.out.println(errorContextMessage(
                        check.checkType().getDisplayName()));
            }
        }
    }

    public void handleAntiPiracyChat(AsyncPlayerChatEvent event) {
        if (!event.getMessage().contains("MatrixIDCheck1234")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.getName().contains("RE")) {
            return;
        }
        event.setCancelled(true);
        event.setMessage(":)");
        player.sendMessage(antiPiracySystemMessage(String.valueOf(ChatColor.GREEN)));

        TextComponent hardwareId = new TextComponent(hardwareIdMessage(
                me.rerere.matrix.license.EnvironmentFingerprint.fingerprint()));
        hardwareId.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                me.rerere.matrix.license.EnvironmentFingerprint.fingerprint()));
        player.spigot().sendMessage(hardwareId);

        TextComponent fileHash = new TextComponent(fileHashMessage(
                me.rerere.matrix.network.PacketOrderBuffer.pluginFileMd5()));
        fileHash.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                me.rerere.matrix.network.PacketOrderBuffer.pluginFileMd5()));
        player.spigot().sendMessage(fileHash);
        player.sendMessage("");

        String version = me.rerere.matrix.Matrix.getInstance()
                .getDescription().getVersion();
        player.sendMessage(versionAndServerMessage(version, Bukkit.getVersion()));

        String disabledCheckNames = Stream.of(
                        me.rerere.matrix.checks.CheckCategory.values())
                .filter(BlockWorldListener::isCheckDisabled)
                .map(check -> check.getDisplayName())
                .collect(Collectors.joining(","));
        player.sendMessage(disabledChecksMessage(disabledCheckNames));

        boolean aacEnabled = Bukkit.getPluginManager().getPlugin("AAC5") != null;
        boolean reflexEnabled = Bukkit.getPluginManager().getPlugin("Reflex") != null;
        player.sendMessage(otherAnticheatSettingsMessage(aacEnabled, reflexEnabled));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dispatchAsyncChatToChecks(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) return;
        for (me.rerere.matrix.checks.CheckRuntimeState check : playerData.checks().checks()) {
            if (!check.checkType().isEnabled()) continue;
            if (playerData.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(player.getWorld())) continue;
            check.onAsyncChat(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void dispatchVelocityToChecks(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager()
                .find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.checks.CheckRegistry checkSet = playerData.checks();
        for (me.rerere.matrix.checks.CheckRuntimeState check : checkSet.checks()) {
            if (!check.checkType()
                    .isEnabled()) {
                continue;
            }
            if (playerData.shouldBypass(
                    check.checkType())) {
                continue;
            }
            if (!check.checkType()
                    .isEnabledIn(player.getWorld())) {
                continue;
            }
            check.onVelocity(event);
        }
    }

    public void onPlayerEnterVehicle(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntered();
        me.rerere.matrix.player.PlayerCheckContext playerData = me.rerere.matrix.Matrix.getInstance()
                .getPlayerDataManager().find(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        me.rerere.matrix.player.VelocityHistory velocityHistory = playerData.velocityTracker();
        velocityHistory.recordVelocity(1.0D, 1.0D, 1.0D);
    }

    public BlockWorldListener() {
        Map<String, EventPriority> priorityOverrides = me.rerere.matrix.config.MatrixConfiguration.priorities;
        if (priorityOverrides.size() == 0) return;
        me.rerere.matrix.Matrix.getInstance().getLogger().warning(
                "You are customizing the priority of the check, which may cause some checks to fail to work properly.");
        Method[] declaredMethods = getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterCount() != 1) continue;
            Class<?> eventType = method.getParameterTypes()[0];
            String eventTypeName = eventType.getSimpleName();
            if (!priorityOverrides.containsKey(eventTypeName)) continue;
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(eventHandler);
            try {
                Class<?> handlerType = invocationHandler.getClass();
                String memberValuesName = me.rerere.matrix.util.reflect.ReflectionNameResolver
                        .mapFieldName(handlerType, "memberValues");
                Field memberValuesField = handlerType.getDeclaredField(memberValuesName);
                memberValuesField.setAccessible(true);
                Map annotationMemberValues = (Map) memberValuesField.get(invocationHandler);
                EventPriority priority = (EventPriority) priorityOverrides.get(eventTypeName);
                annotationMemberValues.put("priority", priority);
                String message = priorityMessage(eventTypeName, priority.name());
                me.rerere.matrix.Matrix
                        .logMessage(message);
            } catch (NoSuchFieldException | IllegalAccessException failure) {
                failure.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        me.rerere.matrix.Matrix.getInstance()
                .getClientBlockManager().cacheBlock(event.getBlock());
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        me.rerere.matrix.Matrix.getInstance()
                .getClientBlockManager().cacheBlock(event.getBlock());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> blocks = event.blockList();
        me.rerere.matrix.Matrix matrix = me.rerere.matrix.Matrix.getInstance();
        me.rerere.matrix.world.BlockLocationCache tracker = matrix.getClientBlockManager();
        Objects.requireNonNull(tracker);
        blocks.forEach(block -> tracker.cacheBlock(block));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        me.rerere.matrix.Matrix matrix = me.rerere.matrix.Matrix.getInstance();
        me.rerere.matrix.world.BlockLocationCache tracker = matrix.getClientBlockManager();
        Objects.requireNonNull(tracker);
        blocks.forEach(block -> tracker.cacheBlock(block));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        me.rerere.matrix.Matrix.getInstance()
                .getChunkManager().record(chunk);
    }

    private static String serverIdentityMessage(String matrixVersion, String userName,
            String userId, String plan, String serverVersion, String serverName,
            String serverBuild) {
        return "This server is running §bMatrix Anti Cheat§c (" + matrixVersion
                + ")\n§fUser:§b " + userName + " §f( " + userId + " )"
                + "§f\n§fPlan:§b " + plan
                + "\n§fServer:§b " + serverVersion + " §f/ §b" + serverName
                + " §f/ §b" + serverBuild;
    }

    private static String disabledChecksMessage(String check) {
        return "Disabled checks: §c" + check;
    }

    private static String priorityMessage(String check, String priority) {
        return "(Priority) " + check + " -> " + priority;
    }

    private static String versionAndServerMessage(String version, String server) {
        return "Version: §b" + version + " §fServer: §b" + server;
    }

    private static String errorAtMessage(String location) {
        return "[Matrix/Error] @" + location;
    }

    private static String otherAnticheatSettingsMessage(boolean aacEnabled,
            boolean reflexEnabled) {
        return "Other anticheat §7// §fAAC5: §b" + aacEnabled
                + " §7// §fReflex: §b" + reflexEnabled;
    }

    private static String antiPiracySystemMessage(String detail) {
        return new StringBuilder(detail).append("§lAnti-piracy System:").toString();
    }

    private static String fileHashMessage(String hash) {
        return "File Hash: §b" + hash;
    }

    private static String trialJoinMessage(String player) {
        return "Player " + player
                + " tried to join the server but server is using a trail version of anti cheat!";
    }

    private static String errorContextMessage(String context) {
        return "[Matrix/Error] c: " + context + " please report this to author!";
    }

    private static String hardwareIdMessage(String hardwareId) {
        return "HWID: §b" + hardwareId;
    }

    private static boolean isFiniteFloat(float value) {
        return Double.NEGATIVE_INFINITY < (double) value
                && (double) value < Double.POSITIVE_INFINITY;
    }

    private static boolean isCheckDisabled(me.rerere.matrix.checks.CheckCategory check) {
        return !check.isEnabled();
    }
}
