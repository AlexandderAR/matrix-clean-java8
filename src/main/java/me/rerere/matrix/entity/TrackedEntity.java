package me.rerere.matrix.entity;

import java.util.Map;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import me.rerere.matrix.util.Exceptions;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.attribute.Attribute;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.resources.ResourceLocation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.rerere.matrix.compat.Mainthread;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import me.rerere.matrix.world.EntityBoundsInterpolator;
import me.rerere.matrix.player.model.MovementPredictionHistory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.geometry.ObservedEntityDimensions;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.model.AttributeInstance;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.entity.EntityPoseDimensions;
import me.rerere.matrix.debug.BoundingBoxParticles;
import me.rerere.matrix.checks.movement.MovementStep;
import me.rerere.matrix.util.cache.CachePolicy;
import me.rerere.matrix.player.model.EntityPositionUpdate;

/** Tracks an entity's position, bounds, pose, and packet-updated attributes. */
public class TrackedEntity {
    private final int entityId;
    private EntityBoundsInterpolator currentBounds;
    private long timestamp;
    private Integer vehicleId;
    private EntityBoundsInterpolator previousBounds;
    private final EntityType entityType;
    private final PlayerCheckContext playerData;
    private EntityPose pose;
    private int positionUpdateDelay;
    private int flags;
    private Vector position;
    private final MovementPredictionHistory vehicleState;
    private long epoch;
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    private final Map<Attribute, AttributeInstance> attributes;


    public TrackedEntity(int entityId, EntityType entityType, PlayerCheckContext playerData, Vector position, float width, float height) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.playerData = playerData;
        this.timestamp = 0L;
        this.epoch = 0L;
        this.pose = EntityPose.STANDING;
        this.attributes = new java.util.LinkedHashMap();
        this.position = position;
        float initialWidth = EntityPoseDimensions.resolveSecond(entityType, pose);
        float initialHeight = EntityPoseDimensions.resolveFirst(entityType, pose);
        AxisAlignedBox firstBounds = AxisAlignedBox.aroundVector(position, initialWidth, initialHeight);
        float secondWidth = EntityPoseDimensions.resolveSecond(entityType, pose);
        float secondHeight = EntityPoseDimensions.resolveFirst(entityType, pose);
        AxisAlignedBox secondBounds = AxisAlignedBox.aroundVector(position, secondWidth, secondHeight);
        this.currentBounds = new EntityBoundsInterpolator(firstBounds, secondBounds, entityType);

        if (playerData.clientVersion().compareTo(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_3) >= 0) {
            AxisAlignedBox modernBounds = interpolatedBounds();
            CachePolicy path = MovementPredictionHistory.COMPANION.getMode(entityType);
            int pathFlags = MovementPredictionHistory.COMPANION.getInterpolationSteps(entityType);
            this.vehicleState = new MovementPredictionHistory(modernBounds, path, pathFlags, width, height);
        } else {
            this.vehicleState = null;
        }
        Mainthread.executeOnEventLoop(playerData.getPlayer(), () -> refreshBukkitEntity(this));
    }

    public TrackedEntity(int entityId, EntityType entityType, PlayerCheckContext playerData, Vector position, float width, float height, int defaultsMask, me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this(entityId, entityType, playerData, position,
             (defaultsMask & 16) != 0 ? 0.0F : width,
             (defaultsMask & 32) != 0 ? 0.0F : height);
    }

    public int entityId() { return entityId; }
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable
    public Integer vehicleId() { return vehicleId; }
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public EntityPose pose() { return pose; }
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public Vector position() { return position; }
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public EntityType entityType() { return entityType; }
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public PlayerCheckContext playerData() { return playerData; }
    public int flags() { return flags; }
    public int positionUpdateDelay() { return positionUpdateDelay; }
    public void setFlags(int value) { flags = value; }
    public void setVehicleId(Integer value) { vehicleId = value; }

    @Override
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public String toString() {
        ResourceLocation name = entityType.getName();
        return "TrackingEntity(id=" + entityId + ", type=" + name + ", vehicle=" + vehicleId
                + ", pose=" + pose.name() + ")";
    }

    public boolean isWithinCoordinateTolerance() {
        double current = interpolatedBounds().horizontalDiagonal();
        double previous = currentBounds().horizontalDiagonal();
        return Math.abs(current - previous) < 0.01D;
    }





    public void refreshTrackingState() {
        if (vehicleState != null) {
            vehicleState.advance();
        } else {
            currentBounds.advance(previousBounds == null);
            if (previousBounds != null) {
                previousBounds.advance(true);
                AxisAlignedBox adjusted = previousBounds.interpolateToIndex();
                currentBounds.unionBounds(adjusted);
            }
        }
        if (playerData.debugEnabled()) {
            double distance = playerData.movementState().distanceToPacketPosition(position);
            if (distance < 16.0D) {
                BoundingBoxParticles.drawDefaults(currentBounds(), playerData.getPlayer(),
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.COMPOSTER, 0.0D, 4);
            }
        }
    }

    public void tick() {
        if (vehicleState != null) vehicleState.clearPriorPredictions();
        previousBounds = null;
        if (playerData.debugEnabled()) {
            double distance = playerData.movementState().distanceToPacketPosition(position);
            if (distance < 16.0D) {
                BoundingBoxParticles.drawDefaults(currentBounds(), playerData.getPlayer(),
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.COMPOSTER, 0.0D, 4);
            }
        }
    }

    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public java.util.List<AxisAlignedBox> nearbyPoints() {
        if (vehicleState != null) {
            java.util.List<AxisAlignedBox> path = vehicleState.getBounds();
            java.util.ArrayList<AxisAlignedBox> copied = new java.util.ArrayList<>(path.size());
            for (AxisAlignedBox value : path) copied.add(value.copy());
            return copied;
        }
        java.util.ArrayList<AxisAlignedBox> points = new java.util.ArrayList<>();
        if (previousBounds != null) points.addAll(previousBounds.interpolatedBounds());
        points.addAll(currentBounds.interpolatedBounds());
        return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(points));
    }

    public AxisAlignedBox currentBounds() {
        if (vehicleState != null) {
            java.util.Iterator<AxisAlignedBox> iterator = vehicleState.getBounds().iterator();
            if (!iterator.hasNext()) throw new UnsupportedOperationException("Empty collection can't be reduced.");
            AxisAlignedBox combined = iterator.next();
            while (iterator.hasNext()) {
                AxisAlignedBox next = iterator.next();
                combined = combined.union(next);
            }
            return combined.copy();
        }
        if (previousBounds == null) return currentBounds.interpolateToIndex();
        AxisAlignedBox previous = previousBounds.interpolateToIndex();
        AxisAlignedBox current = currentBounds.interpolateToIndex();
        return previous.union(current);
    }

    public AxisAlignedBox previousBounds() {
        if (vehicleState != null) return interpolatedBounds();
        return currentBounds.getEndBounds();
    }

    public AxisAlignedBox interpolatedBounds() {
        float width = EntityPoseDimensions.resolveSecond(entityType, pose);
        float height = EntityPoseDimensions.resolveFirst(entityType, pose);
        return AxisAlignedBox.aroundVector(position, width, height);
    }

    private static void refreshBukkitEntity(TrackedEntity tracked) {
        Player player = tracked.playerData.getPlayer();
        Entity entity = SpigotConversionUtil.getEntityById(player.getWorld(), tracked.entityId);
        if (entity != null) {
            ObservedEntityDimensions.INSTANCE.scheduleObservation(entity);
        }
    }

    public void updateAttributes(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes packet) {
        if (packet.getEntityId() != entityId) {
            throw new IllegalArgumentException("Invalid entity id " + packet.getEntityId() + " != " + entityId);
        }
        for (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes$Property property : packet.getProperties()) {
            Attribute attribute = property.getAttribute();
            AttributeInstance attributeInstance = attributes.get(attribute);
            if (attributeInstance == null) {
                attributeInstance = new AttributeInstance(attribute, property.getValue(), new java.util.ArrayList<>());
                attributes.put(attribute, attributeInstance);
            }
            attributeInstance.setBaseValue(property.getValue());
            java.util.ArrayList modifiers = attributeInstance.modifiers();
            modifiers.clear();
            for (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes$PropertyModifier modifier : property.getModifiers()) {
                modifiers.add(modifier);
            }
            attributeInstance.markTouched();
        }
    }

    public void updatePositionPath(EntityPositionUpdate pathSource) {

        double oldDistance = playerData.movementState().distanceToPacketPosition(position);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath path = pathSource.resolvePath(
            new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d(position.getX(), position.getY(), position.getZ()));
        if (path != null) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d end = path.getEndPosition();
            if (end != null) position = new Vector(end.x, end.y, end.z);
        }

        double newDistance = playerData.movementState().distanceToPacketPosition(position);
        if (newDistance > oldDistance) {
            long nowForDelta = playerData.latency();
            long elapsed = KotlinTimeAccess.subtractDurations(nowForDelta, epoch);
            long interval = DurationKt.toDuration(50, DurationUnit.MILLISECONDS);
            if (KotlinTimeAccess.compareDurations(elapsed, interval) > 0) {
                long nowForRatio = playerData.latency();
                double ratio = divideDurations(nowForRatio, epoch);
                if (ratio > 3.0D) {
                    int added = (int) Math.ceil(divideDurations(elapsed, interval));
                    positionUpdateDelay = Math.min(5, positionUpdateDelay + added);
                }
            }
        }
        timestamp = epoch;
        epoch = playerData.latency();

        if (vehicleState != null) {
            java.util.List<MovementStep> steps;
            if (path == null) {
                steps = null;
            } else if (path instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath) {
                java.util.List<?> sourceSteps = ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath) path).getSteps();
                java.util.ArrayList<MovementStep> mapped = new java.util.ArrayList<>(sourceSteps.size());
                for (Object raw : sourceSteps) {
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath$Step step = (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath$Step) raw;
                    Vector stepPosition = new Vector(step.x(), step.y(), step.z());
                    float stepWidth = EntityPoseDimensions.resolveSecond(entityType, pose);
                    float stepHeight = EntityPoseDimensions.resolveFirst(entityType, pose);
                    AxisAlignedBox box = AxisAlignedBox.aroundVector(stepPosition, stepWidth, stepHeight);
                    mapped.add(new MovementStep(box, step.tickOffset()));
                }
                steps = mapped;
            } else {
                AxisAlignedBox box = interpolatedBounds();
                steps = java.util.Collections.singletonList(new MovementStep(box, vehicleState.getCapacity()));
            }
            vehicleState.updateSamples(steps, pathSource.getYaw(),
                pathSource.getPitch(), pathSource.isPositionSync());
        } else {
            previousBounds = currentBounds;
            AxisAlignedBox start = previousBounds.interpolateToIndex();
            AxisAlignedBox end = interpolatedBounds();
            currentBounds = new EntityBoundsInterpolator(start, end, entityType);
        }

        if (positionUpdateDelay > 0) positionUpdateDelay--;
        if (playerData.debugEnabled()) {
            double distance = playerData.movementState().distanceToPacketPosition(position);
            if (distance < 16.0D) {
                BoundingBoxParticles.drawDefaults(interpolatedBounds(), playerData.getPlayer(),
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.FLAME, 0.0D, 4);
                BoundingBoxParticles.drawDefaults(currentBounds(), playerData.getPlayer(),
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.COMPOSTER, 0.0D, 4);
            }
        }
    }

    private static final class DurationDivision {
        private static final MethodHandle METHOD = resolve();

        private static MethodHandle resolve() {
            try {
                return MethodHandles.publicLookup().findStatic(Duration.class, "div-LRDsOJo",
                        MethodType.methodType(double.class, long.class, long.class));
            } catch (Throwable failure) {
                throw Exceptions.propagate(failure);
            }
        }
    }

    private static double divideDurations(long first, long second) {
        try {
            return (double) DurationDivision.METHOD.invokeExact(first, second);
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    public void updateMetadata(WrapperPlayServerEntityMetadata packet) {
        for (EntityData data : packet.getEntityMetadata()) {
            if (data.getIndex() != 0 && data.getIndex() != 6) continue;
            if (data.getIndex() == 6 && Matrix.Companion.serverVersion().compareTo(ServerVersion.V_1_14) >= 0) {
                Object value = data.getValue();
                me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(value, "null cannot be cast to non-null type com.github.retrooper.packetevents.protocol.entity.pose.EntityPose");
                pose = (EntityPose) value;
            }
            Mainthread.executeOnEventLoop(playerData.getPlayer(), () -> applyMetadataOnMainThread(this, packet));
        }
    }

    private static void applyMetadataOnMainThread(TrackedEntity tracked, WrapperPlayServerEntityMetadata packet) {
        Player player = tracked.playerData.getPlayer();
        Entity entity = SpigotConversionUtil.getEntityById(player.getWorld(), packet.getEntityId());
        if (entity != null) {
            ObservedEntityDimensions.INSTANCE.scheduleObservation(entity);
        }
    }


}
