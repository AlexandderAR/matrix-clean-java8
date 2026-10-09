package me.rerere.matrix.checks.movement;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.VelocityHistory;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.player.PlayerUtilities;

/** Vehicle speed check and per-vehicle movement limits. */
public class VehicleSpeedCheck extends CheckRuntimeState {
    private long lastVehicleTimestamp;
    private long lastExternalMotionTimestamp;
    private static boolean speedEnabled;
    private long liquidWindowTimestamp;
    private int vehicleTicks;
    private boolean vehicleEntered;
    private static int setbackViolationLimit;
    private double previousDistance;

    public VehicleSpeedCheck(PlayerCheckContext playerState) {
        super(playerState);
        this.vehicleEntered = false;
        setCheckType(CheckCategory.VEHICLE);
    }

    public void onVehicleEnter(VehicleEnterEvent event) {
        vehicleEntered = true;
    }

    public void onDamagedByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getUniqueId().equals(player().getUniqueId())) return;
        lastExternalMotionTimestamp = System.currentTimeMillis();
    }

    public void onVelocity(PlayerVelocityEvent event) {
        if (event.getVelocity().getX() == 0.0D && event.getVelocity().getZ() == 0.0D) return;
        lastExternalMotionTimestamp = System.currentTimeMillis();
    }

    @Override
    public void reloadConfiguration() {
        CheckCategory configurationGroup = checkType();
        me.rerere.matrix.config.CheckSetting checkConfiguration =
                configurationGroup.getConfiguration();
        ConfigurationSection nested = checkConfiguration.getConfigurationSection();
        speedEnabled = nested.getBoolean("speed.enable", true);
        ConfigurationSection ownConfig = configuration();
        setbackViolationLimit = ownConfig.getInt("speed.setback_vl", 3);
    }

    private static String formatHorseNoSaddle(double distance) {
        return "vehicle speed | horse | no saddle | " + distance;
    }
    private static String formatPig(double distance) {
        return "vehicle speed | type: pig | distance: " + distance;
    }
    private static String formatHorse(double first, double second) {
        return "vehicle speed | type: horse | distance: " + first + "/" + second;
    }
    private static String formatDonkey(double distance) {
        return "vehicle speed | type: donkey | distance: " + distance;
    }
    private static String formatBoatIce(double distance, double best) {
        return "vehicle speed | type: boat | tag: ice | distance: " + distance + " bst: " + best;
    }
    private static String formatStrider(double distance) {
        return "vehicle speed | type: strider | distance: " + distance;
    }
    private static String formatNautilus(double distance) {
        return "vehicle speed | type: nautilus | distance: " + distance;
    }
    private static String formatLlama(double distance) {
        return "vehicle speed | type: llama | distance: " + distance;
    }
    private static String formatBoatLiquid(double distance) {
        return "vehicle speed | type: boat | tag: liquid | distance: " + distance;
    }
    private static String formatStep(double distance) {
        return "vehicle speed | type: step | distance: " + distance;
    }
    private static String formatBoatSuper(double distance) {
        return "vehicle speed | type: boat | tag: super | distance: " + distance;
    }
    private static String formatAcceleration(double distance) {
        return "accelerating too fast: " + distance;
    }
    private static String formatCamel(double distance) {
        return "vehicle speed | type: camel | distance: " + distance;
    }
    private static String formatDonkeyNoSaddle(double distance) {
        return "vehicle speed | donkey | no saddle | " + distance;
    }
    private static String formatPigNoCarrot(double distance) {
        return "vehicle speed | type: pig-no-carrot | distance: " + distance;
    }
    private static String formatUnknown(String type, double distance) {
        return "vehicle speed | type: unknown(" + type + ") | distance: " + distance;
    }
    private static String formatBoatSolid(double distance) {
        return "vehicle speed | type: boat | tag: solid | distance: " + distance;
    }

    @Override
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event,
                       org.bukkit.Location from, org.bukkit.Location to) {
        Player player = event.getPlayer();
        if (!speedEnabled) return;
        if (vehicleEntered) {
            vehicleEntered = false;
            return;
        }
        if (!player.isInsideVehicle()) {
            vehicleTicks = 0;
            return;
        }
        Entity vehicleEntity = player.getVehicle();
        if (!(vehicleEntity instanceof org.bukkit.entity.Vehicle)) return;
        if (!PlayerUtilities.isPlayerFirstPassenger(
                player, (org.bukkit.entity.Vehicle) player.getVehicle())) return;

        vehicleTicks++;
        if (vehicleTicks > 20) vehicleTicks = 20;
        double distance = MovementMetrics.locationDistance(from, to);
        double verticalChange = to.getY() - from.getY();
        org.bukkit.entity.EntityType vehicleType = player.getVehicle().getType();
        if (BlockEnvironment.isSpecialEntityType(vehicleType)
                && distance > previousDistance + 0.2D) {
            long current = MillisecondSampler.currentTimeMillis();
            PlayerCheckContext playerState = playerState();
            VelocityHistory movementState = playerState.velocityTracker();
            if (current - movementState.latestSampleTime() > 1500L
                    && !BlockEnvironment.hasFa5kMaterialInWideBox(from)) {
                String detail = formatAcceleration(NumberUtils.round3(distance - previousDistance));
                flag("vehicle.acce", detail, 2);
                if (setbackViolationLimit > 0) event.setTo(from);
            }
        }

        if (vehicleTicks > 1
                && System.currentTimeMillis() - lastExternalMotionTimestamp > 3000L) {
            org.bukkit.entity.Vehicle vehicle = (org.bukkit.entity.Vehicle) player.getVehicle();
            org.bukkit.entity.EntityType type = vehicle.getType();
            boolean reported = false;
            boolean recognized = false;
            PlayerCheckContext playerState = playerState();
            VelocityHistory movementState = playerState.velocityTracker();
            double stateAllowance = movementState.sumRecentVerticalValues();
            if (BlockEnvironment.isSpecialEntityType(type)) {
                recognized = true;
                org.bukkit.block.Block supportBlock =
                        from.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                if (BlockEnvironment.hasIceOnBlockBelowOrItsNeighbors(from)) {
                    double iceAllowance = 0.0D;
                    if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_13)
                            && BlockEnvironment.hasBlueIceBelowPlayer(player)) {
                        iceAllowance = 0.75D;
                    }
                    if (distance > 2.9D + iceAllowance) {
                        String detail = formatBoatIce(NumberUtils.round3(distance),
                                NumberUtils.round3(iceAllowance));
                        flag("vehicle.speed", detail, 1);
                        reported = true;
                    }
                    lastVehicleTimestamp = System.currentTimeMillis();
                } else {
                    if (supportBlock.isLiquid()
                            && System.currentTimeMillis() - lastVehicleTimestamp > 1500L) {
                        double allowance = 0.5D;
                        if (System.currentTimeMillis() - liquidWindowTimestamp < 700L) allowance += 0.5D;
                        if (BlockEnvironment.hasAnyLiquidInMovementWindow(from)) {
                            allowance += 0.5D;
                            liquidWindowTimestamp = System.currentTimeMillis();
                        }
                        if (distance > allowance) {
                            String detail = formatBoatLiquid(distance);
                            flag("vehicle.speed", detail, 1);
                            reported = true;
                        }
                    }

                    if (!supportBlock.isLiquid()
                            && distance > 0.39D + stateAllowance
                            && supportBlock.getType() != org.bukkit.Material.AIR
                            && supportBlock.getType().isSolid()
                            && from.getBlock().getType() == org.bukkit.Material.AIR
                            && System.currentTimeMillis() - lastVehicleTimestamp > 1500L) {
                        boolean nearbyLiquid = false;
                        org.bukkit.block.BlockFace[] faces = BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES;
                        for (org.bukkit.block.BlockFace face : faces) {
                            if (supportBlock.getRelative(face).isLiquid()) nearbyLiquid = true;
                        }
                        if (BlockEnvironment.hasFaPredicateAroundCurrentAndAbove(from)
                                || BlockEnvironment.hasFa5kMaterialInWideBox(from)) {
                            nearbyLiquid = true;
                        }
                        if (System.currentTimeMillis() - liquidWindowTimestamp < 1000L) nearbyLiquid = true;
                        if (System.currentTimeMillis() - lastVehicleTimestamp < 3000L) nearbyLiquid = true;
                        if (!nearbyLiquid) {
                            reported = true;
                            String detail = formatBoatSolid(distance);
                            flag("vehicle.speed", detail, 1);
                        }
                    }
                }

                if (distance > 3.7D) {
                    String detail = formatBoatSuper(distance);
                    flag("vehicle.speed", detail, 10);
                    player.leaveVehicle();
                    reported = true;
                }
            }

            if (type == org.bukkit.entity.EntityType.HORSE
                    || type == org.bukkit.entity.EntityType.ZOMBIE_HORSE
                    || type == org.bukkit.entity.EntityType.SKELETON_HORSE) {
                recognized = true;
                org.bukkit.entity.AbstractHorse horse = (org.bukkit.entity.AbstractHorse) vehicle;
                org.bukkit.attribute.AttributeInstance speedAttribute = java.util.Objects.requireNonNull(
                        horse.getAttribute(BukkitAttributes.INDEX.movementSpeedValue()));
                double horseSpeed = speedAttribute.getValue();
                double normalizedSpeed = horseSpeed / 0x1.916872b020c4ap-3D;
                org.bukkit.inventory.Inventory inventory =
                        ((org.bukkit.inventory.InventoryHolder) vehicle).getInventory();
                if ((inventory.getItem(0) == null || inventory.getItem(0).getType() == org.bukkit.Material.AIR)
                        && distance > 0.33D + (horse.isLeashed() ? 1.0D : 0.0D)) {
                    String detail = formatHorseNoSaddle(NumberUtils.round3(distance));
                    flag("horse.nosaddle", detail, 1);
                    reported = true;
                }
                double horseAllowance = 0.8D * normalizedSpeed
                        + (to.getY() > from.getY() + 0.25D ? 0.4D : 0.0D)
                        + (vehicleTicks > 10 || vehicleTicks == 1
                            ? playerState().velocityTracker().sumRecentVerticalValues() : 0.0D)
                        + GeometryMath.horizontalLength(vehicle.getVelocity());
                if (BlockEnvironment.isLiquidAtOrAbove(from)
                        && !BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                        && !vehicle.isOnGround()) {
                    horseAllowance /= type == org.bukkit.entity.EntityType.HORSE ? 3.0D : 2.3D;
                }
                if (distance > horseAllowance) {
                    String detail = formatHorse(NumberUtils.round3(distance),
                            NumberUtils.round3(horseAllowance));
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                }
            }

            if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)
                    && type == org.bukkit.entity.EntityType.DONKEY) {
                recognized = true;
                org.bukkit.inventory.Inventory inventory =
                        ((org.bukkit.inventory.InventoryHolder) vehicle).getInventory();
                if ((inventory.getItem(0) == null || inventory.getItem(0).getType() == org.bukkit.Material.AIR)
                        && distance > 0.33D + (((org.bukkit.entity.AbstractHorse) vehicle).isLeashed() ? 1.0D : 0.0D)) {
                    String detail = formatDonkeyNoSaddle(NumberUtils.round3(distance));
                    flag("horse.nosaddle", detail, 1);
                    reported = true;
                }
                if (to.getY() == from.getY() && distance > 0.8D
                        && !((org.bukkit.entity.LivingEntity) vehicle).hasPotionEffect(
                                org.bukkit.potion.PotionEffectType.SPEED)) {
                    String detail = formatDonkey(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                }
            }

            if (type == org.bukkit.entity.EntityType.PIG) {
                recognized = true;
                org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
                if (inventory.getItemInMainHand().getType() != MaterialPolicy.carrotOnAStick
                        && inventory.getItemInOffHand().getType() != MaterialPolicy.carrotOnAStick
                        && MaterialPolicy.carrotOnAStick != null
                        && distance > 0.3D
                        && !((org.bukkit.entity.LivingEntity) vehicle).hasPotionEffect(
                                org.bukkit.potion.PotionEffectType.SPEED)) {
                    String detail = formatPigNoCarrot(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                }
                if (to.getY() == from.getY() && distance > 0.5D
                        && !((org.bukkit.entity.LivingEntity) vehicle).hasPotionEffect(
                                org.bukkit.potion.PotionEffectType.SPEED)) {
                    String detail = formatPig(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                }
            }

            if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_16)
                    && type == org.bukkit.entity.EntityType.STRIDER) {
                recognized = true;
                double allowance = 0.45D + playerState().velocityTracker().sumRecentVerticalValues();
                if (distance > allowance) {
                    String detail = formatStrider(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                }
            }

            if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)
                    && vehicle instanceof org.bukkit.entity.Llama) {
                recognized = true;
                if (distance > 0.4D) {
                    String detail = formatLlama(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                    player.leaveVehicle();
                }
            }

            if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_20)
                    && vehicle instanceof org.bukkit.entity.Camel) {
                recognized = true;
                if (distance > 2.35D) {
                    String detail = formatCamel(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                    player.leaveVehicle();
                }
            }

            if (atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_21_11)
                    && vehicle instanceof org.bukkit.entity.Nautilus) {
                recognized = true;
                if (distance > 1.55D) {
                    String detail = formatNautilus(distance);
                    flag("vehicle.speed", detail, 1);
                    reported = true;
                    player.leaveVehicle();
                }
            }

            if (!recognized && distance > 1.0D) {
                reported = true;
                String detail = formatUnknown(vehicle.getType().name().toLowerCase(), distance);
                flag("vehicle.speed", detail, 1);
            }

            if (verticalChange >= 1.95D
                    && !BlockEnvironment.hasNearbyBubbleColumn(from)
                    && MillisecondSampler.currentTimeMillis()
                        - playerState().velocityTracker().latestSampleTime() >= 1000L) {
                String detail = formatStep(verticalChange);
                flag("vehicle.speed", detail, 5);
                reported = true;
                player.leaveVehicle();
                event.setTo(from);
            }

            if (reported) {
                int violations = violationCount();
                if (setbackViolationLimit != -1 && violations >= setbackViolationLimit) {
                    event.setTo(from);
                }
            }
        }
        previousDistance = distance;
    }

    private static boolean atLeast(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion required) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion server =
                Matrix.getServerVersion();
        return server.isNewerThanOrEquals(required);
    }
}
