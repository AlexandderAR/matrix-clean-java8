package me.rerere.matrix.network;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.checks.combat.CollisionResultFactory;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.player.TargetTrackingSnapshot;

/** Immutable result of an entity-interaction reach observation. */
public final class InteractEntityObservation {
    public static final CollisionResultFactory companion =
            new CollisionResultFactory((DefaultConstructorMarker) null);
    private static String textConfiguration;

    private final TargetTrackingSnapshot target;
    private final WrapperPlayClientInteractEntity$InteractAction action;
    private final AxisAlignedBox boundingBox;
    private final boolean hit;
    private final boolean cancelHit;
    private final String component;
    private final double reach;
    private final double allowedReach;
    private final double score;
    private final boolean zeroVl;
    private final boolean vehicle;

    public InteractEntityObservation(TargetTrackingSnapshot target, WrapperPlayClientInteractEntity$InteractAction action,
            AxisAlignedBox boundingBox, boolean hit, boolean cancelHit, String component,
            double reach, double allowedReach, double score, boolean zeroVl, boolean vehicle) {
        this.target = target;
        this.action = action;
        this.boundingBox = boundingBox;
        this.hit = hit;
        this.cancelHit = cancelHit;
        this.component = component;
        this.reach = reach;
        this.allowedReach = allowedReach;
        this.score = score;
        this.zeroVl = zeroVl;
        this.vehicle = vehicle;
    }

    public TargetTrackingSnapshot target() {
        return target;
    }

    public WrapperPlayClientInteractEntity$InteractAction action() {
        return action;
    }

    public AxisAlignedBox boundingBox() {
        return boundingBox;
    }

    public boolean hit() {
        return hit;
    }

    public boolean cancelHit() {
        return cancelHit;
    }

    public String component() {
        return component;
    }

    public double reach() {
        return reach;
    }

    public double allowedReach() {
        return allowedReach;
    }

    public double score() {
        return score;
    }

    public boolean zeroVl() {
        return zeroVl;
    }

    public boolean vehicle() {
        return vehicle;
    }

    public InteractEntityObservation copy(TargetTrackingSnapshot target, WrapperPlayClientInteractEntity$InteractAction action,
            AxisAlignedBox boundingBox, boolean hit, boolean cancelHit, String component,
            double reach, double allowedReach, double score, boolean zeroVl, boolean vehicle) {
        return new InteractEntityObservation(target, action, boundingBox, hit, cancelHit, component,
                reach, allowedReach, score, zeroVl, vehicle);
    }

    public InteractEntityObservation(TargetTrackingSnapshot target, WrapperPlayClientInteractEntity$InteractAction action,
            AxisAlignedBox boundingBox, boolean hit, boolean cancelHit, String component,
            double reach, double allowedReach, double score, boolean zeroVl, boolean vehicle,
            int defaultMask, DefaultConstructorMarker marker) {
        this(target, action, boundingBox, hit, cancelHit, component, reach, allowedReach, score,
                (defaultMask & 512) == 0 && zeroVl,
                (defaultMask & 1024) == 0 && vehicle);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InteractEntityObservation)) {
            return false;
        }
        InteractEntityObservation observation = (InteractEntityObservation) other;
        return Intrinsics.areEqual(target, observation.target)
                && action == observation.action
                && Intrinsics.areEqual(boundingBox, observation.boundingBox)
                && hit == observation.hit
                && cancelHit == observation.cancelHit
                && Intrinsics.areEqual(component, observation.component)
                && Double.compare(reach, observation.reach) == 0
                && Double.compare(allowedReach, observation.allowedReach) == 0
                && Double.compare(score, observation.score) == 0
                && zeroVl == observation.zeroVl
                && vehicle == observation.vehicle;
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = result * 31 + action.hashCode();
        result = result * 31 + boundingBox.hashCode();
        result = result * 31 + Boolean.hashCode(hit);
        result = result * 31 + Boolean.hashCode(cancelHit);
        result = result * 31 + component.hashCode();
        result = result * 31 + Double.hashCode(reach);
        result = result * 31 + Double.hashCode(allowedReach);
        result = result * 31 + Double.hashCode(score);
        result = result * 31 + Boolean.hashCode(zeroVl);
        result = result * 31 + Boolean.hashCode(vehicle);
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder("ReachResult(target=").append(target)
                .append(", action=").append(action).append(", boundingBox=").append(boundingBox)
                .append(", hit=").append(hit).append(", cancelHit=").append(cancelHit)
                .append(", component=").append(component).append(", reach=").append(reach)
                .append(", allowedReach=").append(allowedReach).append(", score=").append(score)
                .append(", zeroVl=").append(zeroVl).append(", vehicle=").append(vehicle).append(")")
                .toString();
    }

    public static void setTextConfiguration(String value) {
        textConfiguration = value;
    }

    public static String textConfiguration() {
        return textConfiguration;
    }
}
