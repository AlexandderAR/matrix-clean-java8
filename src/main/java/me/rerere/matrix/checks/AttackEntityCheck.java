package me.rerere.matrix.checks;

import java.util.Locale;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.collections.MapsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.thirdparty.kotlin.sequences.Sequence;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.replay.PacketStateSnapshot;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.checks.CombatRayTraceCompanion;
import me.rerere.matrix.listeners.CallbackEventFactory;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.combat.snapshot.AttackSnapshot;
import me.rerere.matrix.config.DefaultSettingProperty;
import me.rerere.matrix.movement.PlayerSnapshot;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.combat.AttackReplaySnapshot;
import me.rerere.matrix.util.collections.CollectionSupport;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.entity.TrackedEntity;
import me.rerere.matrix.prediction.BlockedRay;
import me.rerere.matrix.checks.CheckCategory;

/** Checks entity attacks against block visibility along sampled player rays. */
public final class AttackEntityCheck extends CheckRuntimeState {
    private final DefaultSettingProperty checkAttackConfig;
    private final ArrayDeque<AttackSnapshot> pendingAttacks;
    private final Object queueLock;

    public static boolean timeout;
    public static final CombatRayTraceCompanion defaultMarker;
    public static final KProperty<?>[] checkAttackProperties;
    private static int rayCount;
    private static final long MAXIMUM_PENDING_ATTACKS = 20L;
    private static final long ATTACK_WINDOW_MILLIS = 1000L;

    static {
        setRayCount(49);
        checkAttackProperties = new KProperty<?>[] {
                Reflection.property1(new PropertyReference1Impl(
                        AttackEntityCheck.class, "checkAttack", "getCheckAttack()Z", 0))
        };
        defaultMarker = new CombatRayTraceCompanion();
    }

    public AttackEntityCheck(PlayerCheckContext context) {
        super(context, CheckCategory.INTERACT);
        checkAttackConfig = CallbackEventFactory.createSecondary(
                this, "check_attack", Boolean.TRUE);
        pendingAttacks = new ArrayDeque<>();
        queueLock = new Object();
    }

    private static String attackBlockDescription(String materialName) {
        return new StringBuilder("attack entity through ")
                .append(materialName).append(" block").toString();
    }

    @Override
    public boolean canHandleEntityState(int entityId, AttackReplaySnapshot packet) {
        PacketStateSnapshot packetState = packet.getPlayerState();
        PlayerSnapshot query = PlayerSnapshot.companion.createFromMovement(packetState);
        TargetTrackingSnapshot snapshot = packet.getTargetState();
        AxisAlignedBox lastKnown = snapshot == null ? null
                : snapshot.copyCertainCollisionBox();
        return isTargetCandidate(entityId, query, lastKnown);
    }

    private final boolean isBlockPassable(Vector position) {
        Player player = player();
        Block block = player.getWorld().getBlockAt(
                position.getBlockX(), position.getBlockY(), position.getBlockZ());
        return MaterialPolicy.isAirMaterial(block.getType())
                || MaterialPolicy.containsFluidBlock(block);
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        synchronized (queueLock) {
            pendingAttacks.clear();
        }
    }

    private final BlockedRay findBlockedRay(PlayerSnapshot candidateContext,
                                            AxisAlignedBox startPosition) {
        WorldAccess rayGeometry =
                me.rerere.matrix.Matrix.Companion.matrixInstance()
                        .getNmsManager()
                        .visitor();
        Collection<?> sampledHeights = (Collection<?>) playerState()
                .possibleEyeHeights();
        List<?> allHeights = CollectionsKt.plus(
                sampledHeights, Double.valueOf(player().getEyeHeight()));
        Set<Integer> uniqueMillimetres = new HashSet<>();
        List<Double> uniqueHeights = new ArrayList<>();
        for (Object sampledHeight : allHeights) {
            double height = ((Number) sampledHeight).doubleValue();
            Integer heightInMillimetres = Integer.valueOf(
                    me.rerere.matrix.thirdparty.kotlin.math.MathKt.roundToInt(height * 1000.0d));
            if (uniqueMillimetres.add(heightInMillimetres)) {
                uniqueHeights.add((Double) sampledHeight);
            }
        }

        Sequence<?> rays = candidateContext.sequence(
                playerState(), uniqueHeights);
        Iterator<?> rayIterator = rays.iterator();
        int checkedRays = 0;
        BlockedRay nearest = null;
        while (rayIterator.hasNext()) {
            Ray ray = (Ray) rayIterator.next();
            Vector rayOrigin = ray.getOrigin();
            if (!isBlockPassable(rayOrigin)) continue;

            Vector targetIntersection = startPosition.clipSegmentAgainstBox(
                    ray, 0.0f, 6.0f);
            if (targetIntersection == null) continue;
            checkedRays++;
            double targetDistance = targetIntersection.distance(rayOrigin);
            RaytraceResult rayTrace =
                    rayGeometry.rayTraceBlocks(
                            player().getWorld(),
                            rayOrigin,
                            ray.getDirection(), targetDistance + 0.08d);
            Block hitBlock = rayTrace == null ? null
                    : rayTrace.getHitBlock();
            if (hitBlock == null || MaterialPolicy.isAirMaterial(hitBlock.getType())
                    || MaterialPolicy.containsFluidBlock(hitBlock)) {
                return null;
            }

            double blockDistance = rayTrace.getHitPosition().distance(rayOrigin);
            if (blockDistance >= targetDistance - 0.08d) return null;
            BlockedRay blockedRay = new BlockedRay(
                    hitBlock, blockDistance, targetDistance, checkedRays);
            if (nearest == null || blockedRay.blockDistance()
                    < nearest.blockDistance()) {
                nearest = blockedRay;
            }
        }
        return checkedRays > 0 ? nearest : null;
    }

    @Override
    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        if (!timeout || !getCheckAttack()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        Entity attackedEntity = event.getEntity();
        if (!(attackedEntity instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) attackedEntity;
        if (player().getGameMode() != GameMode.SURVIVAL
                && player().getGameMode() != GameMode.ADVENTURE) return;
        if (player().isInsideVehicle() || target.isInsideVehicle()) return;

        AttackSnapshot attackSnapshot = findAttackHistory(target.getEntityId());
        if (attackSnapshot == null) {
            attackSnapshot = new AttackSnapshot(
                    target.getEntityId(),
                    PlayerSnapshot.companion.createFromPlayer(playerState()),
                    System.currentTimeMillis());
        }
        AxisAlignedBox targetBounds = currentPosition(target);
        PlayerSnapshot attackContext = attackSnapshot.snapshot();
        BlockedRay blockedRay = findBlockedRay(attackContext, targetBounds);
        if (blockedRay == null) return;

        Block block = blockedRay.block();
        String blockName = block.getType().name().toLowerCase(Locale.ROOT);
        String message = attackBlockDescription(blockName);
        Map<String, Object> details = MapsKt.<String, Object>mapOf(new Pair[] {
                new Pair<>("block", block.getType().name()),
                new Pair<>("dist", DiagnosticFormat.formatDouble(
                        blockedRay.targetDistance())),
                new Pair<>("block_dist", DiagnosticFormat.formatDouble(
                        blockedRay.blockDistance())),
                new Pair<>("rays", String.valueOf(blockedRay.getCheckedRays()))
        });
        flag("interact.attack", message, details, 1);
        event.setCancelled(true);
    }

    public boolean getCheckAttack() {
        return ((Boolean) checkAttackConfig.getValue(
                this, checkAttackProperties[0])).booleanValue();
    }

    @Override
    public void recordEntityState(int entityId, PacketStateSnapshot packet) {
        if (!timeout || !getCheckAttack()) return;
        synchronized (queueLock) {
            PlayerSnapshot query =
                    PlayerSnapshot.companion.createFromMovement(packet);
            AttackSnapshot entry = new AttackSnapshot(
                    entityId,
                    query,
                    System.currentTimeMillis());
            CollectionSupport.append(pendingAttacks, entry);
            while (pendingAttacks.size() > MAXIMUM_PENDING_ATTACKS) {
                CollectionSupport.removeFirst(pendingAttacks);
            }
        }
    }

    private final AxisAlignedBox currentPosition(LivingEntity entity) {
        AxisAlignedBox currentBounds =
                BlockCollisionGeometry.getEntityBounds(entity);
        TrackedEntity history = playerState()
                .motionTracker().get(entity.getEntityId());
        AxisAlignedBox previous = history == null ? null
                    : history.currentBounds();
        AxisAlignedBox sweptBounds = previous == null ? null
                : previous.union(currentBounds);
        if (sweptBounds == null) sweptBounds = currentBounds;
        return sweptBounds.grow(0.001d, 0.001d, 0.001d);
    }

    private final boolean isTargetCandidate(int entityId,
                                            PlayerSnapshot candidateContext,
                                            AxisAlignedBox targetBounds) {
        if (!timeout || !getCheckAttack()) return true;

        TrackedEntity entityState = playerState()
                .motionTracker().get(entityId);
        if (entityState == null) return true;
        if (playerState().gameMode() != GameMode.SURVIVAL
                && playerState().gameMode()
                != GameMode.ADVENTURE) return true;
        if (playerState().isInVehicle()
                || entityState.vehicleId() != null) return true;

        if (targetBounds == null) {
            targetBounds = entityState.currentBounds().copy();
        }
        targetBounds = targetBounds.grow(0.001d, 0.001d, 0.001d);
        BlockedRay blockedRay = findBlockedRay(candidateContext, targetBounds);
        if (blockedRay == null) return true;

        String materialName = blockedRay.block()
                .getType().name().toLowerCase(Locale.ROOT);
        String message = attackBlockDescription(materialName);
        Map<String, Object> details = MapsKt.<String, Object>mapOf(new Pair[] {
                new Pair<>("block", blockedRay.block().getType().name()),
                new Pair<>("dist", DiagnosticFormat.formatDouble(
                        blockedRay.targetDistance())),
                new Pair<>("block_dist", DiagnosticFormat.formatDouble(
                        blockedRay.blockDistance())),
                new Pair<>("rays", String.valueOf(blockedRay.getCheckedRays()))
        });
        flag("interact.attack", message, details, 1);
        return false;
    }

    private final AttackSnapshot findAttackHistory(int entityId) {
        long now = System.currentTimeMillis();
        synchronized (queueLock) {
            while (!pendingAttacks.isEmpty()) {
                AttackSnapshot oldest =
                        (AttackSnapshot)
                                CollectionSupport.first(pendingAttacks);
                if (now - oldest.createdAt()
                        <= ATTACK_WINDOW_MILLIS) break;
                CollectionSupport.removeFirst(pendingAttacks);
            }
            Iterator<AttackSnapshot> iterator = pendingAttacks.iterator();
            while (iterator.hasNext()) {
                AttackSnapshot entry = iterator.next();
                if (entry.entityId() == entityId) {
                    iterator.remove();
                    return entry;
                }
            }
        }
        return null;
    }

    @Override
    public boolean checkBlockInteraction(int entityId, PacketStateSnapshot packet) {
        PlayerSnapshot candidateContext = PlayerSnapshot.companion.createFromMovement(packet);
        return isTargetCandidate(entityId, candidateContext, null);
    }

    public static void setRayCount(int value) { rayCount = value; }
    public static int getRayCount() { return rayCount; }
    public static int getDefaultRayCount() { return getRayCount() == 0 ? 80 : 0; }
}
