package me.rerere.matrix.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.combat.AttackReplaySnapshot;
import me.rerere.matrix.checks.combat.HeldAttack;
import me.rerere.matrix.checks.entity.TrackedEntityRegistry;
import me.rerere.matrix.checks.movement.PacketMovementCondition;
import me.rerere.matrix.util.collections.CollectionSupport;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.entity.EntityMetadataState;
import me.rerere.matrix.entity.EntityStateSnapshots;
import me.rerere.matrix.entity.TrackedEntity;
import me.rerere.matrix.network.packet.TransactionPingQueue;
import me.rerere.matrix.model.CheckState;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.player.VelocityHistory;
import me.rerere.matrix.player.model.HeldAttackReplayContext;
import me.rerere.matrix.player.model.EntityPositionUpdate;
import me.rerere.matrix.player.model.EntityPositionUpdateFactory;
import me.rerere.matrix.player.model.PacketMovementTracker;
import me.rerere.matrix.replay.PacketStateSnapshot;
import me.rerere.matrix.replay.PlayerMovementState;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.UserDisconnectEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.User;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.util.ProgressGate;
import me.rerere.matrix.geometry.Vector3;
import me.rerere.matrix.world.LocationFlagCache;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Tracks player packet state and reconciles client actions with queued entity updates. */
public final class PlayerPacketListener extends PacketListenerAbstract {
    private final ConcurrentHashMap<UUID, ArrayDeque<HeldAttack>> queuedState;
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> pendingPlayers;
    private final ConcurrentHashMap<UUID, AttackReplaySnapshot> playerState;

    public PlayerPacketListener() {
        this.queuedState = new ConcurrentHashMap<>();
        this.pendingPlayers = ConcurrentHashMap.newKeySet();
        this.playerState = new ConcurrentHashMap<>();
    }

    /** Returns whether both the client and server use the legacy 1.8 protocol range. */
    private boolean isLegacyProtocol(PlayerCheckContext session) {
        ClientVersion client = (ClientVersion) session.clientVersion();
        if (!client.isOlderThanOrEquals(ClientVersion.V_1_8)) return false;
        ServerVersion server = (ServerVersion) Matrix.Companion.serverVersion();
        return server.isOlderThanOrEquals(ServerVersion.V_1_8_8);
    }

    /** Detects a modern client connected to a server older than protocol 26.3. */
    private boolean hasModernClientOnOlderServer(PlayerCheckContext session) {
        ClientVersion client = (ClientVersion) session.clientVersion();
        if (client.compareTo(ClientVersion.V_26_3) < 0) return false;
        ServerVersion server = (ServerVersion) Matrix.Companion.serverVersion();
        return server.compareTo(ServerVersion.V_26_3) < 0;
    }

    /** Captures the target state when the nearby Bukkit entity is living. */
    private TargetTrackingSnapshot findLivingEntity(Player player, int entityId) {
        Entity entity = me.rerere.matrix.compat.FoliaSupport
                .nearbyEntityById(player, entityId);
        if (!(entity instanceof LivingEntity)) return null;
        return EntityStateSnapshots.fromLivingEntity((LivingEntity) entity);
    }

    /** Updates passenger-to-vehicle links and clears stale links for the supplied entity ids. */
    private void updateEffectEntries(
            PlayerCheckContext session, int vehicleEntityId, int[] passengerEntityIds) {
        TrackedEntityRegistry trackedEntities =
                (TrackedEntityRegistry) session.motionTracker();
        List<TrackedEntity> selectedEntities = new ArrayList<>(passengerEntityIds.length);
        for (int entityId : passengerEntityIds) {
            selectedEntities.add((TrackedEntity) trackedEntities.get(entityId));
        }
        for (TrackedEntity entity : selectedEntities) {
            if (entity != null) entity.setVehicleId(Integer.valueOf(vehicleEntityId));
        }
        Collection<?> allTrackedEntities = (Collection<?>) trackedEntities.values();
        for (Object value : allTrackedEntities) {
            TrackedEntity entity = (TrackedEntity) value;
            Integer currentVehicleId = entity.vehicleId();
            if (currentVehicleId != null && currentVehicleId.intValue() == vehicleEntityId
                    && !me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt
                            .contains(passengerEntityIds, entity.entityId())) {
                entity.setVehicleId(null);
            }
        }
    }

    /** Clears queued attacks and pending movement work for this player. */
    private void clearPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        queuedState.remove(playerId);
        pendingPlayers.remove(playerId);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        UUID playerId = user.getUUID();
        if (playerId == null) return;
        queuedState.remove(playerId);
        pendingPlayers.remove(playerId);
        playerState.remove(playerId);
    }

    private static void replacePendingData(PlayerCheckContext session, java.util.List<?> values) {
        VelocityHandler tracking = (VelocityHandler) session.inventoryTracker();
        tracking.setAuxiliaryValues(values);
    }

    private void recordSendPosition(PlayerCheckContext session, double x, double y, double z,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent event) {
        VelocityHistory velocityHistory = (VelocityHistory) session.velocityTracker();
        velocityHistory.recordVelocity(x, y, z);
        ((VelocityHandler) session.inventoryTracker()).onPacketSend(event);
    }

    private void recordRelativePosition(PlayerCheckContext session, double x, double y, double z,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent event) {
        VelocityHistory velocityHistory = (VelocityHistory) session.velocityTracker();
        velocityHistory.recordAppliedVelocity(x, y, z);
        ((VelocityHandler) session.inventoryTracker()).onPacketSend(event);
    }

    private void updateMetadataPosition(PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata packet) {
        if (((EntityMetadataState) session.packetState())
                .applyMetadata(packet)) {
            ((PlayerMovementState) session.movementState())
                    .setEyeHeight(session.eyeHeight());
        }
    }

    private static void rememberPotionEffect(PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect packet) {
        session.potionEffects().put(packet.getPotionType(), Integer.valueOf(packet.getEffectAmplifier()));
    }

    private static void forgetPotionEffect(PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect packet) {
        session.potionEffects().remove(packet.getPotionType());
    }

    private void sendPrediction(Player player,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper packet,
            AttackReplaySnapshot prediction) {
        UUID playerId = player.getUniqueId();
        playerState.put(playerId, prediction);
        try {
            PacketEventSupport.receivePacket(player, packet);
        } finally {
            playerState.remove(playerId);
        }
    }

    private void captureClientInput(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper wrapper,
            PlayerCheckContext session) {
        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action action = null;
            if (wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction) {
                action = ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction) wrapper).getAction();
            }
            ((EntityMetadataState) session.packetState()).applyEntityAction(action);
            if (action == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.START_SNEAKING
                    || action == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.STOP_SNEAKING) {
                ((PlayerMovementState) session.movementState()).setEyeHeight(session.eyeHeight());
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_INPUT) {
            if (!(wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput)) return;
            ((EntityMetadataState) session.packetState()).updateInput(
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput) wrapper);
            ((PlayerMovementState) session.movementState()).setEyeHeight(session.eyeHeight());
        }
    }

    private boolean acceptPredictedEntity(Player player, PlayerCheckContext session,
            int entityId, AttackReplaySnapshot prediction) {
        for (Object value : ((CheckRegistry) session.checks()).checks()) {
            CheckRuntimeState check = (CheckRuntimeState) value;
            if (check != null) {
                CheckCategory condition = (CheckCategory) check.checkType();
                if (condition.isEnabled()
                        && !session.shouldBypass(condition)
                        && condition.isEnabledIn(player.getWorld())
                        && !check.checkTrackedEntity(entityId, prediction)) return false;
            }
        }
        return true;
    }

    private boolean acceptReceivedEntity(Player player, PlayerCheckContext session,
            int entityId, AttackReplaySnapshot prediction) {
        for (Object value : ((CheckRegistry) session.checks()).checks()) {
            CheckRuntimeState check = (CheckRuntimeState) value;
            if (check != null) {
                CheckCategory condition = (CheckCategory) check.checkType();
                if (condition.isEnabled()
                        && !session.shouldBypass(condition)
                        && condition.isEnabledIn(player.getWorld())
                        && !check.canHandleEntityState(entityId, prediction)) return false;
            }
        }
        return true;
    }

    private static void processEntityById(Player player, int entityId, PlayerCheckContext session) {
        try {
            org.bukkit.entity.Entity entity = me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotConversionUtil
                    .getEntityById(player.getWorld(), entityId);
            if (entity == null) return;
            for (Object value : ((CheckRegistry) session.checks()).checks()) {
                CheckRuntimeState check = (CheckRuntimeState) value;
                if (check != null) {
                    CheckCategory condition = (CheckCategory) check.checkType();
                    if (condition.isEnabled()
                            && !session.shouldBypass(condition)
                            && condition.isEnabledIn(player.getWorld())) {
                        check.onEntityAction(entity,
                                me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.emptyList());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void scheduleMovementUpdates(Player player, PlayerCheckContext session) {
        if (!pendingPlayers.add(player.getUniqueId())) return;
        me.rerere.matrix.compat.FoliaSupport.INSTANCE.runDelayedSyncForPlayer(
                player, () -> movementUpdateCallback(this, player, session), 1L);
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit movementUpdateCallback(
            PlayerPacketListener listener, Player player, PlayerCheckContext session) {
        listener.updatePlayerMovement(player, session);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit enqueueEntityProcessing(
            Player player, int entityId, PlayerCheckContext session) {
        me.rerere.matrix.compat.Mainthread.executeOnEventLoop(player,
                () -> processEntityById(player, entityId, session));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private void updatePlayerMovement(Player player, PlayerCheckContext session) {
        UUID playerId = player.getUniqueId();
        pendingPlayers.remove(playerId);
        ArrayDeque<HeldAttack> queuedAttacks = queuedState.get(playerId);
        if (queuedAttacks == null) return;
        ArrayList<HeldAttack> recentAttacks = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (queuedAttacks) {
            while (!queuedAttacks.isEmpty()) {
                HeldAttack queuedAttack = (HeldAttack) CollectionSupport.removeFirst(queuedAttacks);
                if (now - queuedAttack.getCreatedAt() <= 1000L) recentAttacks.add(queuedAttack);
            }
            if (queuedAttacks.isEmpty()) queuedState.remove(playerId, queuedAttacks);
            // Preserve Kotlin Unit initialization at this synchronized point.
            me.rerere.matrix.thirdparty.kotlin.Unit unitInitialization =
                    me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        }
        if (recentAttacks.isEmpty()) return;
        ArrayList<HeldAttackReplayContext> replayContexts =
                new ArrayList<>(recentAttacks.size());
        for (HeldAttack queuedAttack : recentAttacks) {
            PacketStateSnapshot playerSnapshot = (PacketStateSnapshot) session.movementState()
                    .snapshot();
            TrackedEntity trackedEntity = (TrackedEntity) session.motionTracker()
                    .get(queuedAttack.entityIdValue());
            TargetTrackingSnapshot targetSnapshot = trackedEntity == null ? null
                    : EntityStateSnapshots.fromUpdateSnapshot(trackedEntity);
            HeldAttackReplayContext replayContext =
                    new HeldAttackReplayContext(queuedAttack, playerSnapshot, targetSnapshot);
            replayContexts.add(replayContext);
        }
        me.rerere.matrix.compat.FoliaSupport.INSTANCE.runSyncForPlayer(
                player, () -> applyQueuedUpdates(replayContexts, this, player, session));
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit applyQueuedUpdates(
            List<HeldAttackReplayContext> replayContexts, PlayerPacketListener listener, Player player,
            PlayerCheckContext session) {
        for (HeldAttackReplayContext replayContext : replayContexts) {
            HeldAttack heldAttack = (HeldAttack) replayContext.getAttack();
            AttackReplaySnapshot queuedPrediction =
                    (AttackReplaySnapshot) heldAttack.snapshotValue();
            PacketStateSnapshot playerSnapshot =
                    (PacketStateSnapshot) replayContext.getPlayerState();
            TargetTrackingSnapshot targetSnapshot =
                    (TargetTrackingSnapshot) replayContext.getTargetState();
            AttackReplaySnapshot mergedPrediction =
                    queuedPrediction.update(playerSnapshot, targetSnapshot);
            TargetTrackingSnapshot currentTargetSnapshot = listener.findLivingEntity(player,
                    heldAttack.entityIdValue());
            mergedPrediction = mergedPrediction.addTargetIfMissing(currentTargetSnapshot);
            mergedPrediction = mergedPrediction.withCancelledOriginal(heldAttack.isShouldReplay());
            if (listener.acceptReceivedEntity(player, session, heldAttack.entityIdValue(), mergedPrediction)
                    && heldAttack.isShouldReplay()) {
                listener.sendPrediction(player, heldAttack.getPacket(), mergedPrediction);
            }
        }
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    @Override
    public void onPacketReceive(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent event) {
        Object rawPlayer = event.getPlayer();
        if (rawPlayer == null || !(event.getPlayer() instanceof Player)) return;
        if (event.isCancelled()) return;
        Object checkedPlayer = event.getPlayer();
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                checkedPlayer, "null cannot be cast to non-null type org.bukkit.entity.Player");
        Player player = (Player) checkedPlayer;
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType =
                event.getPacketType();
        PlayerSessionRegistry registry = Matrix.Companion
                .matrixInstance().getPlayerDataManager();
        PlayerCheckContext session = (PlayerCheckContext) registry
                .find(player.getUniqueId());
        if (session == null || session.isBedrockClient()) return;

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper wrapper =
                PacketWrapperFactory.wrapClientReceive(event);
        boolean movementChanged = false;
        AttackReplaySnapshot prediction = playerState.get(player.getUniqueId());
        captureClientInput(packetType, wrapper, session);
        if (prediction == null && wrapper != null
                && PacketEventSupport.isAttackWrapper(wrapper)) {
            int entityId = PacketEventSupport.getAttackEntityId(wrapper);
            AttackReplaySnapshot queuedPrediction = createPrediction(session, entityId);
            boolean accepted = acceptPredictedEntity(player, session, entityId, queuedPrediction);
            enqueuePrediction(player, wrapper, queuedPrediction, !accepted);
            if (!accepted) {
                event.setCancelled(true);
                scheduleMovementUpdates(player, session);
                return;
            }
        }

        if (PacketEventSupport.isPlayerFlying(packetType)) {
            if (!(wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation)) {
                throw new IllegalArgumentException("Failed requirement.");
            }
            boolean handled = ((PacketMovementCondition) session.interactionTracker())
                    .processPacket(event);
            if (handled || event.isCancelled()) return;
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation movement =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper;
            ((PlayerMovementState) session.movementState()).updatePacket(movement);

            org.bukkit.Location oldLocation = ((PacketMovementTracker)
                    session.combatTracker()).getLastLocation();
            Double oldX = oldLocation == null ? null : Double.valueOf(oldLocation.getX());
            oldLocation = ((PacketMovementTracker)
                    session.combatTracker()).getLastLocation();
            Double oldY = oldLocation == null ? null : Double.valueOf(oldLocation.getY());
            oldLocation = ((PacketMovementTracker)
                    session.combatTracker()).getLastLocation();
            Double oldZ = oldLocation == null ? null : Double.valueOf(oldLocation.getZ());
            oldLocation = ((PacketMovementTracker)
                    session.combatTracker()).getLastLocation();
            Float oldYaw = oldLocation == null ? null : Float.valueOf(oldLocation.getYaw());
            oldLocation = ((PacketMovementTracker)
                    session.combatTracker()).getLastLocation();
            Float oldPitch = oldLocation == null ? null : Float.valueOf(oldLocation.getPitch());
            boolean hasBaseline = oldX != null && oldY != null && oldZ != null
                    && oldYaw != null && oldPitch != null;
            boolean anyDifference = false;
            if (movement.hasPositionChanged()) {
                double packetX = movement.getPosition().x;
                oldLocation = ((PacketMovementTracker) session.combatTracker()).getLastLocation();
                boolean positionDifferent = !me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        packetX, oldLocation == null ? null : Double.valueOf(oldLocation.getX()));
                if (!positionDifferent) {
                    double packetY = movement.getPosition().y;
                    oldLocation = ((PacketMovementTracker) session.combatTracker()).getLastLocation();
                    positionDifferent = !me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                            packetY, oldLocation == null ? null : Double.valueOf(oldLocation.getY()));
                }
                if (!positionDifferent) {
                    double packetZ = movement.getPosition().z;
                    oldLocation = ((PacketMovementTracker) session.combatTracker()).getLastLocation();
                    positionDifferent = !me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                            packetZ, oldLocation == null ? null : Double.valueOf(oldLocation.getZ()));
                }
                if (positionDifferent) {
                    anyDifference = true;
                    movementChanged = true;
                }
            }
            if (movement.hasRotationChanged()) {
                float packetYaw = movement.getYaw();
                oldLocation = ((PacketMovementTracker) session.combatTracker()).getLastLocation();
                boolean rotationDifferent = !me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        packetYaw, oldLocation == null ? null : Float.valueOf(oldLocation.getYaw()));
                if (!rotationDifferent) {
                    float packetPitch = movement.getPitch();
                    oldLocation = ((PacketMovementTracker) session.combatTracker()).getLastLocation();
                    rotationDifferent = !me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                            packetPitch, oldLocation == null ? null : Float.valueOf(oldLocation.getPitch()));
                }
                if (rotationDifferent) anyDifference = true;
            }
            if (movement.isOnGround() != ((PacketMovementTracker)
                    session.combatTracker()).isOnGround()) {
                anyDifference = true;
                movementChanged = true;
            }
            if (!hasBaseline) movementChanged = false;
            if (hasBaseline && anyDifference) {
                List<?> pending = ((VelocityHandler) session.inventoryTracker()).getRecentImpulses();
                me.rerere.matrix.compat.Mainthread.executeOnEventLoop(player,
                        () -> replacePendingData(session, pending));
            }
        }

        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_POSITION
                || packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d packetPosition =
                    ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper).getPosition();
            ((LocationFlagCache) session.locationFlags())
                    .put(new Vector3(
                            packetPosition.getX(), packetPosition.getY(), packetPosition.getZ()),
                            ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper).isOnGround());
        }

        if (PacketEventSupport.isAttackEvent(event)) {
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(wrapper);
            int entityId = PacketEventSupport.getAttackEntityId(wrapper);
            if (prediction == null) prediction = createPrediction(session, entityId);
            for (Object value : ((CheckRegistry) session.checks()).checks()) {
                CheckRuntimeState check = (CheckRuntimeState) value;
                if (check != null) {
                    CheckCategory condition = (CheckCategory) check.checkType();
                    if (condition.isEnabled()
                            && !session.shouldBypass(condition)
                            && condition.isEnabledIn(player.getWorld())) {
                        check.applyStateUpdate(entityId, prediction);
                    }
                }
            }
            ProgressGate.runCallbacksDefault(
                    (ProgressGate) session.packetBuffer(),
                    () -> enqueueEntityProcessing(player, entityId, session), null, 2, null);
        }

        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.TELEPORT_CONFIRM) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm confirm =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm) wrapper;
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(confirm);
            ((PacketMovementTracker) session.combatTracker())
                    .confirmTeleport(confirm.getTeleportId());
            clearPlayer(player);
        }
        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT) {
            ((PacketMovementCondition) session.interactionTracker()).setEnabled(true);
        }

        for (Object value : ((CheckRegistry) session.checks()).checks()) {
            CheckRuntimeState check = (CheckRuntimeState) value;
            if (check != null) {
                CheckCategory condition = (CheckCategory) check.checkType();
                if (condition.isEnabled()
                        && !session.shouldBypass(condition)
                        && condition.isEnabledIn(player.getWorld())) {
                    boolean cancelled = check.onPacketReceive(packetType, event, wrapper);
                    if (cancelled) event.setCancelled(true);
                    if (!cancelled && PacketEventSupport.isPlayerRotationPacket(packetType)) {
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation look =
                                (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper;
                        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(look);
                        check.onPacketRotation(packetType, look.getYaw(), look.getPitch());
                    }
                }
            }
        }

        boolean vehicleOrFlying = me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(packetType)
                || packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.VEHICLE_MOVE;
        boolean continueMovement = false;
        if (vehicleOrFlying) {
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(wrapper);
            ((PacketMovementTracker) session.combatTracker())
                    .onMovement(event, player, wrapper);
            continueMovement = !event.isCancelled();
        }
        if (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            ((TrackedEntityRegistry) session.motionTracker()).updateAll();
            ((VelocityHandler) session.inventoryTracker()).update(movementChanged);
        }
        if (continueMovement) updatePlayerMovement(player, session);
        if (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            ((EntityMetadataState) session.packetState()).advanceTick(
                    wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation
                    && ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper).hasPositionChanged());
        }
        session.setLastActivityTime(System.currentTimeMillis());
    }

    @Override
    public void onPacketSend(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent event) {
        Object rawPlayer = event.getPlayer();
        if (rawPlayer == null || !(event.getPlayer() instanceof Player)) return;
        if (event.isCancelled()) return;
        Object checkedPlayer = event.getPlayer();
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                checkedPlayer, "null cannot be cast to non-null type org.bukkit.entity.Player");
        Player player = (Player) checkedPlayer;
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType =
                event.getPacketType();
        PlayerSessionRegistry registry = Matrix.Companion
                .matrixInstance().getPlayerDataManager();
        PlayerCheckContext session = (PlayerCheckContext) registry
                .find(player.getUniqueId());
        if (session == null) return;
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper serverPacket =
                PacketWrapperFactory.wrapServerSend(event);

        if (serverPacket instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
                && ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity) serverPacket).getEntityId() == player.getEntityId()) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d velocity =
                    ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity) serverPacket).getVelocity();
            recordSendPosition(session, velocity.x, velocity.y, velocity.z, event);
        } else if (serverPacket instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d knockback =
                    ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion) serverPacket).getKnockback();
            if (knockback != null) recordRelativePosition(session, knockback.x, knockback.y, knockback.z, event);
        } else if (serverPacket instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook teleport =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook) serverPacket;
            org.bukkit.Location location = new org.bukkit.Location(player.getWorld(),
                    teleport.getX(), teleport.getY(), teleport.getZ());
            clearPlayer(player);
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion server =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion)
                            Matrix.Companion.serverVersion();
            PacketMovementTracker locationState =
                    (PacketMovementTracker) session.combatTracker();
            if (server.isOlderThan(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)) {
                locationState.addUnconfirmedTeleport(location);
            } else {
                locationState.addTeleport(location, teleport.getTeleportId());
            }
        }

        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.SPAWN_ENTITY) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity spawn =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity(event);
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d position = spawn.getPosition();
            ((TrackedEntityRegistry) session.motionTracker()).put(
                    new TrackedEntity(spawn.getEntityId(), spawn.getEntityType(), session,
                            new org.bukkit.util.Vector(position.x, position.y, position.z),
                            spawn.getYaw(), spawn.getPitch()));
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.SPAWN_PLAYER) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer spawn =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer(event);
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d position = spawn.getPosition();
            ((TrackedEntityRegistry) session.motionTracker()).put(
                    new TrackedEntity(spawn.getEntityId(),
                            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER,
                            session, new org.bukkit.util.Vector(position.x, position.y, position.z),
                            spawn.getYaw(), spawn.getPitch()));
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.SPAWN_LIVING_ENTITY) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity spawn =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity(event);
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d position = spawn.getPosition();
            ((TrackedEntityRegistry) session.motionTracker()).put(
                    new TrackedEntity(spawn.getEntityId(), spawn.getEntityType(), session,
                            new org.bukkit.util.Vector(position.x, position.y, position.z),
                            spawn.getYaw(), spawn.getPitch()));
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.DESTROY_ENTITIES) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities destroy =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities(event);
            int[] entityIds = destroy.getEntityIds();
            for (int entityId : entityIds) updateEffectEntries(session, entityId, new int[0]);
            ((TrackedEntityRegistry) session.motionTracker()).removeAll(destroy.getEntityIds());
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.UPDATE_ATTRIBUTES) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes attributes =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(attributes.getEntityId());
            if (entity != null) entity.updateAttributes(attributes);
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_METADATA) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata metadata =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata(event);
            if (metadata.getEntityId() == player.getEntityId()) updateMetadataPosition(session, metadata);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(metadata.getEntityId());
            if (entity != null) entity.updateMetadata(metadata);
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_TELEPORT) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport teleport =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(teleport.getEntityId());
            if (entity != null) {
                EntityPositionUpdate update = EntityPositionUpdateFactory.fromPathDefaults(
                        EntityPositionUpdate.FACTORY,
                        new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath(teleport.getPosition()),
                        teleport.getYaw(), teleport.getPitch(), false, 8, null);
                updateEffectState(session, entity, update);
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_POSITION_SYNC) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync sync =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(sync.getId());
            if (entity != null) {
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath path =
                        toSteppedPathIfNeeded(session, sync.getPosition());
                updateEffectState(session, entity, EntityPositionUpdate.FACTORY
                        .fromPath(path, sync.getYRot(), sync.getXRot(), true));
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.SET_PASSENGERS) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers passengers =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers(event);
            updateEffectEntries(session, passengers.getEntityId(), passengers.getPassengers());
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ATTACH_ENTITY) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity attach =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity(event);
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion server =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion)
                            Matrix.Companion.serverVersion();
            if (server.compareTo(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8) <= 0
                    && !attach.isLeash()) {
                TrackedEntity attached = (TrackedEntity) ((TrackedEntityRegistry)
                        session.motionTracker()).get(attach.getAttachedId());
                TrackedEntity holder = (TrackedEntity) ((TrackedEntityRegistry)
                        session.motionTracker()).get(attach.getHoldingId());
                if (attached != null) attached.setVehicleId(
                        holder == null ? null : Integer.valueOf(holder.entityId()));
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_RELATIVE_MOVE) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove move =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(move.getEntityId());
            if (entity != null) {
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion client =
                        (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion)
                                session.clientVersion();
                EntityPositionUpdate update;
                if (client.compareTo(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_3) >= 0) {
                    update = EntityPositionUpdateFactory.fromDeltaDefaults(
                            EntityPositionUpdate.FACTORY,
                            toSteppedDeltaIfNeeded(session, move.getDelta()), null, null, 6, null);
                } else {
                    update = EntityPositionUpdateFactory.fromOffsetDefaults(
                            EntityPositionUpdate.FACTORY,
                            move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), null, null, 24, null);
                }
                updateEffectState(session, entity, update);
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation move =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(move.getEntityId());
            if (entity != null) {
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion client =
                        (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion)
                                session.clientVersion();
                EntityPositionUpdate update;
                if (client.compareTo(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_3) >= 0) {
                    update = EntityPositionUpdate.FACTORY.fromDelta(
                            toSteppedDeltaIfNeeded(session, move.getDelta()),
                            Float.valueOf(move.getYaw()), Float.valueOf(move.getPitch()));
                } else {
                    update = EntityPositionUpdate.FACTORY.fromOffset(
                            move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(),
                            Float.valueOf(move.getYaw()), Float.valueOf(move.getPitch()));
                }
                updateEffectState(session, entity, update);
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_ROTATION) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation rotation =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation(event);
            TrackedEntity entity = (TrackedEntity) ((TrackedEntityRegistry)
                    session.motionTracker()).get(rotation.getEntityId());
            if (entity != null) updateEffectState(session, entity,
                    EntityPositionUpdate.FACTORY.rotationOnly(rotation.getYaw(), rotation.getPitch()));
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.ENTITY_EFFECT) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect effect =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect(event);
            if (effect.getEntityId() == ((PlayerCheckContext) session).getPlayer().getEntityId()) {
                ((TransactionPingQueue) session.transactionQueue()).workWithCallbacks(
                        () -> schedulePotionAdd(player, session, effect), PlayerPacketListener::unitCallback);
            }
        } else if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server.REMOVE_ENTITY_EFFECT) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect effect =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect(event);
            if (effect.getEntityId() == ((PlayerCheckContext) session).getPlayer().getEntityId()) {
                ((TransactionPingQueue) session.transactionQueue()).workWithCallbacks(
                        () -> schedulePotionRemove(player, session, effect), PlayerPacketListener::secondUnitCallback);
            }
        }

        for (Object value : ((CheckRegistry) session.checks()).checks()) {
            CheckRuntimeState check = (CheckRuntimeState) value;
            if (check != null) {
                CheckCategory condition = (CheckCategory) check.checkType();
                if (condition.isEnabled()
                        && !session.shouldBypass(condition)
                        && condition.isEnabledIn(player.getWorld())
                        && check.onPacketSend(packetType, event, serverPacket)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private AttackReplaySnapshot createPrediction(PlayerCheckContext session, int entityId) {
        PlayerMovementState movement = (PlayerMovementState) session.movementState();
        PacketStateSnapshot playerSnapshot = (PacketStateSnapshot) movement.snapshot();
        TrackedEntityRegistry trackedEntities = (TrackedEntityRegistry) session.motionTracker();
        TrackedEntity trackedEntity = (TrackedEntity) trackedEntities.get(entityId);
        TargetTrackingSnapshot targetSnapshot = trackedEntity == null ? null
                : EntityStateSnapshots.fromUpdateSnapshot(trackedEntity);
        return new AttackReplaySnapshot(playerSnapshot, targetSnapshot, null, null, null, false, 60,
                (DefaultConstructorMarker) null);
    }

    private HeldAttack normalizeQueuedPacket(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper packet,
            AttackReplaySnapshot prediction, boolean restore) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper normalized;
        if (packet instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack) {
            int entityId = ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack) packet)
                    .getEntityId();
            normalized = new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack(entityId);
        } else if (packet instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity interaction =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity) packet;
            if (interaction.getAction() != me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction.ATTACK) {
                return null;
            }
            boolean sneaking = interaction.isSneaking().orElse(Boolean.FALSE).booleanValue();
            normalized = new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity(
                    interaction.getEntityId(), interaction.getAction(), interaction.getLocation(),
                    interaction.getHand(), sneaking);
        } else {
            return null;
        }
        int entityId = PacketEventSupport.getAttackEntityId(packet);
        return new HeldAttack(normalized, entityId, prediction, restore,
                System.currentTimeMillis());
    }

    private void enqueuePrediction(Player player,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper packet,
            AttackReplaySnapshot prediction, boolean restore) {
        HeldAttack queued = normalizeQueuedPacket(packet, prediction, restore);
        if (queued == null) return;
        UUID playerId = player.getUniqueId();
        ArrayDeque<HeldAttack> queue = queuedState.computeIfAbsent(
                playerId, ignoredId -> new ArrayDeque<HeldAttack>());
        synchronized (queue) {
            CollectionSupport.append(queue, queued);
            while (queue.size() > 20) CollectionSupport.removeFirst(queue);
            // Preserve Kotlin Unit initialization at this synchronized point.
            me.rerere.matrix.thirdparty.kotlin.Unit unitInitialization =
                    me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        }
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit invokeEffectAction(
            TrackedEntity trackedEntity, EntityPositionUpdate positionUpdate) {
        trackedEntity.updatePositionPath(positionUpdate);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit clearEffect(TrackedEntity trackedEntity) {
        trackedEntity.tick();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit unitCallback() {
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit secondUnitCallback() {
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit schedulePotionAdd(Player player,
            PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect packet) {
        me.rerere.matrix.compat.Mainthread.executeOnEventLoop(player,
                () -> rememberPotionEffect(session, packet));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit schedulePotionRemove(Player player,
            PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect packet) {
        me.rerere.matrix.compat.Mainthread.executeOnEventLoop(player,
                () -> forgetPotionEffect(session, packet));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private void updateEffectState(PlayerCheckContext session, TrackedEntity trackedEntity,
            EntityPositionUpdate positionUpdate) {
        int pendingWorkId = trackedEntity.flags();
        TransactionPingQueue transactionQueue =
                (TransactionPingQueue) session.transactionQueue();
        if (pendingWorkId == ((CheckState)
                transactionQueue.currentWork()).primaryValue()) {
            transactionQueue.flushPendingWork();
        }
        trackedEntity.setFlags(
                ((CheckState) transactionQueue.currentWork())
                        .primaryValue());
        transactionQueue.workWithCallbacks(
                () -> invokeEffectAction(trackedEntity, positionUpdate),
                () -> clearEffect(trackedEntity));
    }

    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta
            toSteppedDeltaIfNeeded(PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta delta) {
        if (delta instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta
                && hasModernClientOnOlderServer(session)) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta linear =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta) delta;
            return new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.SteppedVecDelta(
                    me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.listOf(
                            new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.SteppedVecDelta$DeltaStep(
                                    3, linear.dxRaw(), linear.dyRaw(), linear.dzRaw())));
        }
        return delta;
    }

    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath
            toSteppedPathIfNeeded(PlayerCheckContext session,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath path) {
        if (path instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath
                && hasModernClientOnOlderServer(session)) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath linear =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath) path;
            return new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath(
                    me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.listOf(
                            new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath$Step(
                                    linear.getEndPosition(), 3)));
        }
        return path;
    }
}
