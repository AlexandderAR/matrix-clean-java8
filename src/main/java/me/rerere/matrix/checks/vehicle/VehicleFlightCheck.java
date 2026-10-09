package me.rerere.matrix.checks.vehicle;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockGeometry;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Llama;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;

/** Checks vehicle movement and applies configured vehicle-entry restrictions. */
public final class VehicleFlightCheck extends CheckRuntimeState {
    private int movementCounter;
    private long lastMotionTime;
    private int setbackCount;
    private int airborneTicks;
    private static List<String> deniedVehicleTypes;
    private static boolean flightEnabled;
    private static int setbackViolationThreshold;
    private double observedMotion;
    private static int leaveVehicleThreshold;
    private static final long updateInterval = 2000L;

    public VehicleFlightCheck(PlayerCheckContext playerState) {
        super(playerState);
        this.movementCounter = 0;
        setCheckType(CheckCategory.VEHICLE);
    }

    public void reloadConfiguration() {
        flightEnabled = configuration().getBoolean("flight.enable", true);
        setbackViolationThreshold = configuration().getInt("flight.setback_vl", 4);
        leaveVehicleThreshold = configuration().getInt("flight.leave_vehicle", 10);
        deniedVehicleTypes = configuration().getStringList("deny_entity_types_as_vehicle");
        if (deniedVehicleTypes == null) deniedVehicleTypes = new ArrayList<>();
    }

    public void onVehicleEnter(VehicleEnterEvent event) {
        if (deniedVehicleTypes == null || deniedVehicleTypes.isEmpty()) return;
        Vehicle vehicle = event.getVehicle();
        String vehicleType = vehicle.getType().name();
        Player player = (Player) event.getEntered();
        for (String deniedType : deniedVehicleTypes) {
            if (vehicleType.equalsIgnoreCase(deniedType)) {
                event.setCancelled(true);
                player.sendMessage("You can't enter this vehicle!");
                return;
            }
        }
    }

    private static String formatVehicleFlight(double motion, String entity, int ticks, double jumpStrength) {
        return new StringBuilder("vehicle flight (mot: ").append(motion).append(", entity:")
                .append(entity).append(", t:").append(ticks).append(", js:").append(jumpStrength).append(")").toString();
    }

    private static String formatVehicleGlide(double motion, double expected, String entity, int ticks, int jumpStrength) {
        return new StringBuilder("glide with vehicle (mot: ").append(motion).append(", expect: ")
                .append(expected).append(", entity: ").append(entity).append(", t:").append(ticks)
                .append(",").append(jumpStrength).append(")").toString();
    }

    private static String formatHorseGlide(double motion) {
        return new StringBuilder("horse glide (mot: ").append(motion).append(")").toString();
    }

    private static String formatVehicleMotion(double motion, String entity, int ticks) {
        return new StringBuilder("vehicle flight (mot: ").append(motion).append(", entity:")
                .append(entity).append(", t:").append(ticks).append(")").toString();
    }

    private static String formatBoatFlight(double distance) {
        return new StringBuilder("move too fast in the air (dist: ").append(distance).append(", boat)").toString();
    }

    /** Updates the vehicle-flight and vehicle-glide checks for a movement event. */
    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        if (!flightEnabled || !player.isInsideVehicle() || player.getVehicle() == null) {
            return;
        }
        if (BlockEnvironment.hasSlimeBlockUnderPlayer(player)
                || BlockEnvironment.hasFa5kMaterialInMovementBox(from)) {
            movementCounter = 100;
        }
        if (movementCounter > 0) {
            movementCounter--;
            return;
        }

        Entity vehicle = player.getVehicle();
        if (BlockEnvironment.hasIceOnBlockBelowOrItsNeighbors(from)
                || BlockEnvironment.isSpecialPredicateAtOrAbove(player)
                || vehicle.getLocation().getBlock().isLiquid()) {
            lastMotionTime = System.currentTimeMillis();
        }

        double verticalMotion = to.getY() - from.getY();
        boolean reported = false;
        if (!vehicle.isOnGround()
                && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                && !BlockEnvironment.isSpecialPredicateAtOrAbove(player)
                && MaterialPolicy.isAirMaterial(
                        from.getBlock().getRelative(BlockFace.DOWN).getType())) {
            airborneTicks++;
            if (verticalMotion < 0.0D) {
                setbackCount++;
            }
        }
        if (vehicle.isOnGround() || BlockGeometry.hasNonOpenAdjacentMaterial(to)) {
            airborneTicks = 0;
            setbackCount = 0;
        }
        if (verticalMotion == 0.0D
                && vehicle.getLocation().getY() % 1.0D == 0.0D
                && vehicle.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
            airborneTicks = 0;
            setbackCount = 0;
        }
        if (verticalMotion >= 0.0D) {
            setbackCount = 0;
        }

        if (BlockEnvironment.isSpecialEntityType(vehicle.getType())
                && !BlockEnvironment.hasFaPredicateAroundCurrentAndAbove(from)) {
            if (airborneTicks > 2 && verticalMotion >= 0.0D) {
                flag("veh.fly", formatVehicleMotion(NumberUtils.round3(verticalMotion),
                        vehicle.getType().name().toLowerCase(), airborneTicks), 2);
                reported = true;
            }

            if (verticalMotion < 0.0D) {
                if (setbackCount > 1) {
                    double expectedMotion = observedMotion - 0.0399D;
                    if (expectedMotion < 0.0D
                            && Math.abs(verticalMotion) < Math.abs(expectedMotion) * 0.95D
                            && MaterialPolicy.isAirMaterial(to.getBlock().getType())
                            && MaterialPolicy.isAirMaterial(
                                    to.getBlock().getRelative(BlockFace.DOWN).getType())
                            && !BlockEnvironment.hasNearbyBubbleColumn(from)
                            && !BlockEnvironment.hasMaterialAtOrAdjacentTo(
                                    from, MaterialPolicy.cobwebMaterial())) {
                        flag("vel.fly", formatVehicleGlide(NumberUtils.round3(verticalMotion),
                                NumberUtils.round3(expectedMotion),
                                vehicle.getType().name().toLowerCase(), airborneTicks, setbackCount), 2);
                        reported = true;
                    }
                }

                if (MaterialPolicy.isAirMaterial(
                            from.getBlock().getRelative(BlockFace.DOWN).getType())
                        && System.currentTimeMillis() - lastMotionTime > updateInterval) {
                    AxisAlignedBox bounds = BlockCollisionGeometry.getEntityBounds(player)
                            .grow(0.5D, 1.0D, 0.5D);
                    if (!bounds.scanWorldBlocks(from.getWorld())) {
                        double distance = MovementMetrics.locationDistance(from, to);
                        if (distance > 0.42D) {
                            flag("vel.fly", formatBoatFlight(NumberUtils.round3(distance)), 2);
                            reported = true;
                            if (distance > 1.0D) {
                                event.setTo(from);
                            }
                        }
                    }
                }
            }
        }

        if ((vehicle.getType() == EntityType.HORSE || vehicle.getType() == EntityType.SKELETON_HORSE
                || vehicle.getType() == EntityType.ZOMBIE_HORSE || vehicle.getType() == EntityType.DONKEY)
                && !((LivingEntity) vehicle).isLeashed()) {
            AttributeInstance jumpAttribute = ((AbstractHorse) vehicle)
                    .getAttribute(BukkitAttributes.INDEX.jumpStrengthValue());
            double jumpStrength = jumpAttribute.getValue();
            if (airborneTicks > 15 && verticalMotion >= 0.0D
                    && !BlockEnvironment.isSpecialPredicateAtOrAbove(player)) {
                boolean onScaffolding = Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                        && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.SCAFFOLDING);
                if (!onScaffolding
                        && !BlockEnvironment.isOpenableOrHasSpecialLocationState(vehicle.getLocation(), false)
                        && !BlockEnvironment.hasFa8MaterialAroundOrBelow(vehicle.getLocation())
                        && !BlockEnvironment.hasNearbyBubbleColumn(from)) {
                    flag("veh.fly", formatVehicleFlight(NumberUtils.round3(verticalMotion),
                            vehicle.getType().name().toLowerCase(), airborneTicks, NumberUtils.round3(jumpStrength)), 2);
                    reported = true;
                }
            }
            if (verticalMotion < 0.0D && observedMotion < 0.0D && airborneTicks > 5
                    && !vehicle.isOnGround()
                    && !BlockEnvironment.hasFaPredicateAroundCurrentAndAbove(from)
                    && Math.abs(verticalMotion) < 0.075D && Math.abs(observedMotion) < 0.075D
                    && !BlockGeometry.hasNonOpenAdjacentMaterial(to)
                    && MaterialPolicy.nonAirBlockBridge(to.getBlock())
                    && MaterialPolicy.isAirMaterial(
                            to.getBlock().getRelative(BlockFace.DOWN).getType())
                    && !BlockEnvironment.hasMaterialAtOrAdjacentTo(
                            from, MaterialPolicy.cobwebMaterial())) {
                flag("veh.gli", formatHorseGlide(NumberUtils.round3(verticalMotion)), 1);
            }
        }

        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12) && vehicle instanceof Llama
                && airborneTicks > 20 && verticalMotion >= 0.0D
                && !BlockEnvironment.isSpecialPredicateAtOrAbove(player)) {
            boolean onScaffolding = Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                    && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.SCAFFOLDING);
            if (!onScaffolding
                    && !BlockEnvironment.isOpenableOrHasSpecialLocationState(vehicle.getLocation(), false)
                    && !BlockEnvironment.hasFa8MaterialAroundOrBelow(vehicle.getLocation())
                    && !BlockEnvironment.hasNearbyBubbleColumn(from)) {
                flag("veh.fly", formatVehicleMotion(NumberUtils.round3(verticalMotion),
                    vehicle.getType().name().toLowerCase(), airborneTicks), 2);
                reported = true;
            }
        }

        if (vehicle.getType() == EntityType.PIG && !((LivingEntity) vehicle).isLeashed()
                && airborneTicks > 5 && verticalMotion >= 0.0D
                && !BlockEnvironment.isOpenableOrHasSpecialLocationState(vehicle.getLocation(), false)
                && !BlockEnvironment.hasFa8MaterialAroundOrBelow(vehicle.getLocation())) {
            flag("veh.fly", formatVehicleMotion(NumberUtils.round3(verticalMotion),
                    vehicle.getType().name().toLowerCase(), airborneTicks), 2);
            reported = true;
        }

        if (reported && setbackViolationThreshold > 0
                && violationCount() >= setbackViolationThreshold) {
            event.setTo(from);
        }
        if (reported && leaveVehicleThreshold > 0 && violationCount() >= leaveVehicleThreshold) {
            player.leaveVehicle();
        }
        observedMotion = verticalMotion;
    }

}
