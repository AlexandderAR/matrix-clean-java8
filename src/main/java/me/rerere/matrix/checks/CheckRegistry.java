package me.rerere.matrix.checks;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

/** Builds and initializes the ordered Matrix check set for the active server version. */
public final class CheckRegistry {
    public static boolean loaded;
    private static String registrationMarker;

    private final List<me.rerere.matrix.checks.CheckRuntimeState> checks = new ArrayList<>();
    private final me.rerere.matrix.player.PlayerCheckContext config;

    static {
        setRegistrationMarker("XZmV2b");
        loaded = false;
    }

    public CheckRegistry(me.rerere.matrix.player.PlayerCheckContext config) {
        this.config = config;
        registerConfiguredChecks(config);
        loadChecks();
        checks.forEach(me.rerere.matrix.checks.CheckRuntimeState::loadConfiguration);
    }

    public List<me.rerere.matrix.checks.CheckRuntimeState> checks() {
        return checks;
    }

    private void registerConfiguredChecks(me.rerere.matrix.player.PlayerCheckContext config) {
        addCheck(new me.rerere.matrix.checks.movement.PositionPacketFlyCheck(config));
        addCheck(new me.rerere.matrix.checks.network.IllegalPacketCheck(config));
        try {
            addCheck(new me.rerere.matrix.checks.block.BlockPlacementChecks(config));
            addCheck(new me.rerere.matrix.checks.player.ChatSpamCheck(config));
            if (me.rerere.matrix.config.CheckMessages.enabled) {
                addCheck(new me.rerere.matrix.checks.packet.TimerPacketCheck(config));
            } else {
                addCheck(new me.rerere.matrix.checks.TimerAdvantageCheck(config));
            }
        } catch (me.rerere.matrix.license.LicenseResponseException failure) {
            throw failure;
        }

        if (me.rerere.matrix.config.CheckMessages.warningEnabled) {
            addCheck(new me.rerere.matrix.checks.combat.CombatReachCheck(config));
        } else {
            addCheck(new me.rerere.matrix.checks.MeleeReachCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.CriticalHitCheck(config));
        addCheck(new me.rerere.matrix.checks.combat.CombatTargetTracker(config));
        addCheck(new me.rerere.matrix.combat.CombatFlawCheck(config));
        addCheck(new me.rerere.matrix.checks.combat.CombatClickRateCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.MovementRotationCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.MovementSampleCheck(config));

        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            addCheck(new me.rerere.matrix.checks.combat.CrystalAttackCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.movement.StrafeCheck(config));
        addCheck(new me.rerere.matrix.commands.condition.ConditionListener(config));
        addCheck(new me.rerere.matrix.checks.packet.PacketPayloadGuard(config));
        addCheck(new me.rerere.matrix.checks.network.ClientSkinSettingsCheck(config));
        addCheck(new me.rerere.matrix.checks.packet.BadPacketCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.PhaseCheck(config));

        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            addCheck(new me.rerere.matrix.checks.movement.BoatMovementCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.GroundSpoofCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.SleepingMovementCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.NoSlowCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.CobwebCollisionHandler(config));
        addCheck(new me.rerere.matrix.checks.movement.RepeatedYawStepCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.FlightAbilityPacketCheck(config));
        addCheck(new me.rerere.matrix.checks.MovementPhysicsCheck(config));
        addCheck(new me.rerere.matrix.checks.world.SurfaceHazardCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.CreativeFlightCheck(config));

        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            addCheck(new me.rerere.matrix.checks.ElytraFlightCheck(config));
            addCheck(new me.rerere.matrix.checks.vehicle.VehicleFlightCheck(config));
            addCheck(new me.rerere.matrix.checks.movement.VehicleSpeedCheck(config));
        }
        if (me.rerere.matrix.Matrix.getServerVersion() == ServerVersion.V_1_8_8) {
            addCheck(new me.rerere.matrix.checks.ItemUseDelayCheck(config));
            addCheck(new me.rerere.matrix.checks.FastBowCheck(config));
            addCheck(new me.rerere.matrix.checks.health.FastHealCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.combat.AutoSplashCheck(config));
        addCheck(new me.rerere.matrix.checks.InventoryActionDelayCheck(config));
        addCheck(new me.rerere.matrix.checks.player.FastThrowCheck(config));

        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) {
            addCheck(new me.rerere.matrix.checks.inventory.AutoTotemCheck(config));
        }
        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
            addCheck(new me.rerere.matrix.checks.combat.AutoCrossbowListener(config));
        }
        addCheck(new me.rerere.matrix.checks.block.FastBreakCheck(config));
        addCheck(new me.rerere.matrix.checks.block.BlockBreakRotationCheck(config));
        addCheck(new me.rerere.matrix.checks.block.BlockBreakCheck(config));
        addCheck(new me.rerere.matrix.checks.placement.FastPlaceCheck(config));
        addCheck(new me.rerere.matrix.checks.packet.BlockNukerCheck(config));
        addCheck(new me.rerere.matrix.checks.combat.NoSwingCheck(config));

        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            addCheck(new me.rerere.matrix.checks.block.BaritoneActionCheck(config));
            addCheck(new me.rerere.matrix.checks.player.RotationMiningCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.ScaffoldTowerCheck(config));
        addCheck(new me.rerere.matrix.checks.player.ScaffoldPlacementCheck(config));
        addCheck(new me.rerere.matrix.checks.RightClickCpsCheck(config));
        addCheck(new me.rerere.matrix.checks.rotation.RotationPlacementCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.ScaffoldPlacementCheck(config));
        addCheck(new me.rerere.matrix.checks.block.ScaffoldRayTraceCheck(config));
        addCheck(new me.rerere.matrix.checks.placement.PlacementCheck(config));
        addCheck(new me.rerere.matrix.checks.player.VelocityResponseCheck(config));
        addCheck(new me.rerere.matrix.checks.movement.VerticalVelocityCheck(config));

        String variant = me.rerere.matrix.config.CheckMessages.message;
        if (variant.equals("v2")) {
            addCheck(new me.rerere.matrix.checks.InteractionRayTraceCheck(config));
        } else {
            addCheck(new me.rerere.matrix.checks.interaction.BlockInteractionVisibilityCheck(config));
            addCheck(new me.rerere.matrix.checks.BlockBreakCheck(config));
        }
        addCheck(new me.rerere.matrix.checks.AttackEntityCheck(config));
        addCheck(new me.rerere.matrix.gui.LicensePanel(config));
    }

    public me.rerere.matrix.player.PlayerCheckContext getConfiguration() {
        return config;
    }

    public me.rerere.matrix.checks.CheckRuntimeState firstCheckOfType(Class<?> type) {
        for (me.rerere.matrix.checks.CheckRuntimeState check : checks) {
            try {
                if (check.getClass() == type) return check;
            } catch (me.rerere.matrix.license.LicenseResponseException failure) {
                throw failure;
            }
        }
        return null;
    }

    public void loadChecks() {
        if (loaded) return;
        try {
            checks.forEach(me.rerere.matrix.checks.CheckRuntimeState::reloadConfiguration);
        } catch (Exception failure) {
            failure.printStackTrace();
            me.rerere.matrix.Matrix.logMessage("(!) Error when load checks, please check your config files!");
        }
        loaded = true;
    }

    private void addCheck(me.rerere.matrix.checks.CheckRuntimeState check) {
        checks.add(check);
    }

    public static void constructThrowaway() {
        new CheckRegistry(null);
    }

    public static void setRegistrationMarker(String marker) {
        registrationMarker = marker;
    }

    public static String getRegistrationMarker() {
        return registrationMarker;
    }
}
