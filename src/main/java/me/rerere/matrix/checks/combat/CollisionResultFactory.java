package me.rerere.matrix.checks.combat;

import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.interaction.InteractionData;
import me.rerere.matrix.network.InteractEntityObservation;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Creates interaction results for default, reach and miss outcomes. */
public final class CollisionResultFactory {
    private static boolean enabled;

    static {
        enabled = false;
    }

    private CollisionResultFactory() { }

    public CollisionResultFactory(DefaultConstructorMarker marker) {
        this();
    }

    public final InteractEntityObservation defaultResult(
            TargetTrackingSnapshot entity, InteractionData packet) {
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        AxisAlignedBox target = entity.copyCollisionBox();
        return new InteractEntityObservation(entity, action, target,
                true, false, "", 0.0, 0.0, 0.0,
                false, false, 1536, null);
    }

    public final InteractEntityObservation acceptedResult(
            TargetTrackingSnapshot entity, InteractionData packet, AxisAlignedBox target) {
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        return new InteractEntityObservation(entity, action, target,
                true, true, "", 0.0, 0.0, 1000.0,
                false, false, 1536, null);
    }

    public final InteractEntityObservation reachResult(
            TargetTrackingSnapshot entity, InteractionData packet,
            AxisAlignedBox target, double firstDistance, double secondDistance) {
        boolean reaches = firstDistance > secondDistance;
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        return new InteractEntityObservation(entity, action, target,
                true, reaches, reaches ? "hb.reach" : "",
                firstDistance, secondDistance,
                reaches ? firstDistance - secondDistance : 0.0,
                false, false, 1536, null);
    }

    public final InteractEntityObservation missResultWithCollisionCheck(
            TargetTrackingSnapshot entity, InteractionData packet, AxisAlignedBox target) {
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        return new InteractEntityObservation(entity, action, target,
                false, true, "hb.miss", 0.0, 0.0, 100.0, true, true);
    }

    public final InteractEntityObservation reachResultWithReferenceDistance(
            TargetTrackingSnapshot entity, InteractionData packet,
            AxisAlignedBox target, double distance, double referenceDistance) {
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        return new InteractEntityObservation(entity, action, target,
                true, true, "hb.reach", distance, referenceDistance,
                distance - referenceDistance, true, true);
    }

    public final InteractEntityObservation missResult(
            TargetTrackingSnapshot entity, InteractionData packet, AxisAlignedBox target) {
        WrapperPlayClientInteractEntity$InteractAction action = packet.getActionValue();
        return new InteractEntityObservation(entity, action, target,
                false, true, "hb.miss", 0.0, 0.0, 100.0,
                false, false, 1536, null);
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isDisabled() { return !isEnabled(); }
}
