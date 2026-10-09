package me.rerere.matrix.interaction;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction;

/** Recovered interaction observation tuple: origins, target and action. */
public final class InteractionData {
    private final java.util.List<?> origins;
    private final TargetTrackingSnapshot target;
    private final WrapperPlayClientInteractEntity$InteractAction action;

    public InteractionData(java.util.List<?> origins,
                           TargetTrackingSnapshot target,
                           WrapperPlayClientInteractEntity$InteractAction action) {
        this.origins = origins;
        this.target = target;
        this.action = action;
    }

    public TargetTrackingSnapshot getTarget() { return target; }
    public static InteractionData create(java.util.List<?> origins,
                                          TargetTrackingSnapshot target,
                                          WrapperPlayClientInteractEntity$InteractAction action) {
        return new InteractionData(origins, target, action);
    }
    private InteractionData copyValues(java.util.List<?> origins, TargetTrackingSnapshot target,
            WrapperPlayClientInteractEntity$InteractAction action) {
        return new InteractionData(origins, target, action);
    }
    public static InteractionData copyWithDefaults(InteractionData original,
            java.util.List<?> origins, TargetTrackingSnapshot target,
            WrapperPlayClientInteractEntity$InteractAction action, int mask) {
        return copyWithDefaultsValues(original, origins, target, action, mask);
    }

    private static InteractionData copyWithDefaultsValues(InteractionData original,
            java.util.List<?> origins, TargetTrackingSnapshot target,
            WrapperPlayClientInteractEntity$InteractAction action, int mask) {
        if ((mask & 1) != 0) origins = original.origins;
        if ((mask & 2) != 0) target = original.target;
        if ((mask & 4) != 0) action = original.action;
        return original.copyValues(origins, target, action);
    }
    public java.util.List<?> getOrigins() { return origins; }
    public TargetTrackingSnapshot getTargetValue() { return target; }

    @Override
    public String toString() {
        return format(origins, target, action);
    }

    private static String format(java.util.List<?> origins, TargetTrackingSnapshot target,
                                 WrapperPlayClientInteractEntity$InteractAction action) {
        return new StringBuilder("InteractionData(origins=")
                .append(origins)
                .append(", target=")
                .append(target)
                .append(", action=")
                .append(action)
                .append(")")
                .toString();
    }

    @Override
    public int hashCode() {
        int result = origins.hashCode();
        result = 31 * result + (target == null ? 0 : target.hashCode());
        result = 31 * result + action.hashCode();
        return result;
    }

    public WrapperPlayClientInteractEntity$InteractAction getAction() { return action; }
    public WrapperPlayClientInteractEntity$InteractAction getActionValue() { return action; }
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InteractionData)) return false;
        InteractionData that = (InteractionData) other;
        return Intrinsics.areEqual(origins, that.origins)
                && Intrinsics.areEqual(target, that.target)
                && action == that.action;
    }
    public java.util.List<?> getOriginsValue() { return origins; }

}
