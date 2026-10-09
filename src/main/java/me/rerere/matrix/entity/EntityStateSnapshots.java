package me.rerere.matrix.entity;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** Converts Bukkit entities and update snapshots into packet entity state. */
public final class EntityStateSnapshots {
    private EntityStateSnapshots() { }

    public static TargetTrackingSnapshot fromLivingEntity(LivingEntity entity) {
        MatrixConfigBridge height = Matrix.Companion;
        ServerVersion serverVersion = height.serverVersion();
        EntityPose pose;
        if (serverVersion.compareTo(ServerVersion.V_1_14) < 0) {
            pose = EntityPose.STANDING;
        } else {
            pose = SpigotConversionUtil.fromBukkitPose(entity.getPose());
        }

        int entityId = entity.getEntityId();
        EntityType entityType = SpigotConversionUtil.fromBukkitEntityType(entity.getType());
        Location location = entity.getLocation();
        Vector position = location.toVector();
        Entity vehicle = entity.getVehicle();
        Integer vehicleId = vehicle == null ? null : Integer.valueOf(vehicle.getEntityId());
        AxisAlignedBox firstState = BlockCollisionGeometry.getEntityBounds(entity)
                .copy();
        AxisAlignedBox secondState = BlockCollisionGeometry.getEntityBounds(entity)
                .copy();
        return new TargetTrackingSnapshot(
                entityId, entityType, pose, position, vehicleId, 0,
                firstState, secondState, null, 256, (DefaultConstructorMarker) null);
    }

    public static TargetTrackingSnapshot fromUpdateSnapshot(TrackedEntity update) {
        int entityId = update.entityId();
        EntityType entityType = update.entityType();
        EntityPose pose = update.pose();
        Vector position = update.position().clone();
        Integer vehicleId = update.vehicleId();
        int backtrackTicks = update.positionUpdateDelay();
        AxisAlignedBox firstState = update.currentBounds().copy();
        AxisAlignedBox secondState = update.previousBounds().copy();
        List<AxisAlignedBox> sourceStates = update.nearbyPoints();

        List<AxisAlignedBox> copiedStates = new ArrayList<>(sourceStates.size());
        for (AxisAlignedBox sourceState : sourceStates) {
            copiedStates.add(sourceState.copy());
        }
        return new TargetTrackingSnapshot(entityId, entityType, pose,
                position, vehicleId, backtrackTicks, firstState, secondState, copiedStates);
    }
}
