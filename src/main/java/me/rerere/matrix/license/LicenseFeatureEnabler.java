package me.rerere.matrix.license;

import java.util.List;

public final class LicenseFeatureEnabler {
    private LicenseFeatureEnabler() {
    }

    public static void enablePremiumOrEnterprise(List<?> features) {
        if (!LicenseFeaturePredicates.hasPremiumOrEnterprise(features)) {
            return;
        }
        me.rerere.matrix.network.PlayerMetadataPacketListener.CALLBACK_ADAPTER.setEnabled(true);
        me.rerere.matrix.checks.combat.CombatReachCheck.volume.setEnabled(true);
        me.rerere.matrix.checks.block.BlockBreakRotationCheck.indexMarker.setEnabled(true);
        me.rerere.matrix.combat.CombatFlawCheck.HEALTH_MARKER.setEnabled(true);
        me.rerere.matrix.checks.movement.MovementRotationCheck.offset = true;
        me.rerere.matrix.checks.block.BaritoneActionCheck.enabled = true;
        me.rerere.matrix.checks.ElytraFlightCheck.enabled = true;
        me.rerere.matrix.checks.BlockBreakCheck.timeout = true;
        me.rerere.matrix.checks.AttackEntityCheck.timeout = true;
        me.rerere.matrix.checks.interaction.BlockInteractionVisibilityCheck.scale = true;
        me.rerere.matrix.checks.InteractionRayTraceCheck.Companion.setEnabled(true);
        me.rerere.matrix.checks.player.ScaffoldPlacementCheck.volumeCheckEnabled = true;
        me.rerere.matrix.checks.packet.BadPacketCheck.COMPANION.set(true);
    }

    public static void enableEnterprise(List<?> features) {
        if (!LicenseFeaturePredicates.hasEnterprise(features)) {
            return;
        }
        me.rerere.matrix.checks.player.RotationMiningCheck.angle.setEnabled(true);
        me.rerere.matrix.checks.movement.MovementPacketSequenceCheck.weight.setEnabled(true);
        me.rerere.matrix.checks.movement.FreecamMovementCheck.ANGLE.setEnabled(true);
    }
}
