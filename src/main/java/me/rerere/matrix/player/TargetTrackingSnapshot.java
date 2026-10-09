package me.rerere.matrix.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import org.bukkit.util.Vector;
import me.rerere.matrix.geometry.AxisAlignedBox;

/** Immutable target tracking state captured for one entity. */
public final class TargetTrackingSnapshot {
    private final int id;
    private final EntityType type;
    private final EntityPose pose;
    private final Vector clientPos;
    private final Integer vehicleId;
    private final int backtrackTicks;
    private final AxisAlignedBox collisionBox;
    private final AxisAlignedBox certainCollisionBox;
    private final List<AxisAlignedBox> discreteCollisionBoxes;

    public TargetTrackingSnapshot(int id, EntityType type, EntityPose pose, Vector clientPos,
            Integer vehicleId, int backtrackTicks, AxisAlignedBox collisionBox,
            AxisAlignedBox certainCollisionBox,
            List<AxisAlignedBox> discreteCollisionBoxes) {
        this.id = id;
        this.type = type;
        this.pose = pose;
        this.clientPos = clientPos;
        this.vehicleId = vehicleId;
        this.backtrackTicks = backtrackTicks;
        this.collisionBox = collisionBox;
        this.certainCollisionBox = certainCollisionBox;
        this.discreteCollisionBoxes = discreteCollisionBoxes;
    }

    public TargetTrackingSnapshot(int id, EntityType type, EntityPose pose, Vector clientPos,
            Integer vehicleId, int backtrackTicks, AxisAlignedBox collisionBox,
            AxisAlignedBox certainCollisionBox, List<AxisAlignedBox> boxes,
            int mask, DefaultConstructorMarker marker) {
        this(id, type, pose, clientPos, vehicleId, backtrackTicks, collisionBox,
                certainCollisionBox,
                (mask & 256) != 0 ? Collections.singletonList(certainCollisionBox) : boxes);
    }

    public int getId() { return id; }
    public EntityType getType() { return type; }
    public EntityPose getPose() { return pose; }
    public Vector getClientPos() { return clientPos; }
    public Integer getVehicleId() { return vehicleId; }
    public int getBacktrackTicks() { return backtrackTicks; }
    public AxisAlignedBox getCollisionBox() { return collisionBox; }
    public AxisAlignedBox getCertainCollisionBox() { return certainCollisionBox; }
    public List<AxisAlignedBox> getDiscreteCollisionBoxes() { return discreteCollisionBoxes; }

    public boolean isConstant() { return true; }
    public AxisAlignedBox copyCollisionBox() { return collisionBox.copy(); }
    public AxisAlignedBox copyCertainCollisionBox() { return certainCollisionBox.copy(); }

    public List<AxisAlignedBox> copyDiscreteCollisionBoxes() {
        List<AxisAlignedBox> copiedBoxes =
                new ArrayList<AxisAlignedBox>(discreteCollisionBoxes.size());
        for (AxisAlignedBox box : discreteCollisionBoxes) {
            copiedBoxes.add(box.copy());
        }
        return copiedBoxes;
    }

    public TargetTrackingSnapshot copy(int id, EntityType type, EntityPose pose, Vector clientPos,
            Integer vehicleId, int backtrackTicks, AxisAlignedBox collisionBox,
            AxisAlignedBox certainCollisionBox, List<AxisAlignedBox> boxes) {
        return new TargetTrackingSnapshot(id, type, pose, clientPos, vehicleId, backtrackTicks,
                collisionBox, certainCollisionBox, boxes);
    }

    private static String format(int id, EntityType type, EntityPose pose, Vector clientPos,
            Integer vehicleId, int backtrackTicks, AxisAlignedBox collisionBox,
            AxisAlignedBox certainCollisionBox, List<AxisAlignedBox> discreteBoxes) {
        return "TargetTrackingSnapshot(id=" + id + ", type=" + type + ", pose=" + pose
                + ", clientPos=" + clientPos + ", vehicleId=" + vehicleId
                + ", backtrackTicks=" + backtrackTicks + ", collisionBox=" + collisionBox
                + ", certainCollisionBox=" + certainCollisionBox
                + ", discreteCollisionBoxes=" + discreteBoxes + ")";
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof TargetTrackingSnapshot)) return false;
        TargetTrackingSnapshot other = (TargetTrackingSnapshot) candidate;
        return id == other.id && Objects.equals(type, other.type) && pose == other.pose
                && Objects.equals(clientPos, other.clientPos)
                && Objects.equals(vehicleId, other.vehicleId)
                && backtrackTicks == other.backtrackTicks
                && Objects.equals(collisionBox, other.collisionBox)
                && Objects.equals(certainCollisionBox, other.certainCollisionBox)
                && Objects.equals(discreteCollisionBoxes, other.discreteCollisionBoxes);
    }

    @Override
    public int hashCode() {
        int hash = Integer.hashCode(id);
        hash = 31 * hash + type.hashCode();
        hash = 31 * hash + pose.hashCode();
        hash = 31 * hash + clientPos.hashCode();
        hash = 31 * hash + (vehicleId == null ? 0 : vehicleId.hashCode());
        hash = 31 * hash + Integer.hashCode(backtrackTicks);
        hash = 31 * hash + collisionBox.hashCode();
        hash = 31 * hash + certainCollisionBox.hashCode();
        hash = 31 * hash + discreteCollisionBoxes.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return format(id, type, pose, clientPos, vehicleId, backtrackTicks,
                collisionBox, certainCollisionBox, discreteCollisionBoxes);
    }
}
