package me.rerere.matrix.checks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.DuplicatePosition;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.runtime.EntityCategories;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.SafeBuffer;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.world.BlockGeometry;
import me.rerere.matrix.player.PlayerUtilities;

public class ElytraFlightCheck extends CheckRuntimeState {
    private int liquidEnergyTicks;
    private long lastGroundTime;
    public static boolean enabled;
    private long lastRiptideTime;
    private int glidingTicks;
    private Vector previousMovement;
    private Vector previousDirection;
    private long lastRapidGlideTime;
    private int groundGlideTicks;
    private static int unequipViolationLevel;
    private int rapidGlideRequests;
    private int placementAllowance;
    private static int setbackViolationLevel;
    private long lastBoostTime;
    private long speedFreezeTime;
    private SafeBuffer<DuplicatePosition> exemptLocations;
    private double previousKineticEnergy;
    private int frozenMoveTicks;
    private static boolean forceSetback;
    private long lastGlideRequestTime;

    public ElytraFlightCheck(PlayerCheckContext context) {
        super(context);
        placementAllowance = 0;
        exemptLocations = new SafeBuffer<DuplicatePosition>(5);
        speedFreezeTime = -1L;
        setCheckType(CheckCategory.ELYTRA);
    }

    public void reloadConfiguration() {
        setbackViolationLevel = configuration().getInt("setback_vl", 5);
        unequipViolationLevel = configuration().getInt("unequip_vl", 20);
        forceSetback = configuration().getBoolean("force_setback", true);
    }

    public void addExemptLocation(DuplicatePosition location) {
        exemptLocations.add(location);
    }

    private boolean consumePlacementAllowance() {
        if (placementAllowance > 0) {
            placementAllowance--;
            return false;
        }
        return true;
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent ignored, PacketWrapper<?> wrapper) {
        if (type == PacketType$Play$Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction$Action action = ((WrapperPlayClientEntityAction) wrapper).getAction();
            if (action == WrapperPlayClientEntityAction$Action.START_FLYING_WITH_ELYTRA) {
                if (DeveloperCommand.developerMode)
                    Bukkit.getScheduler().runTask(Matrix.getInstance(), this::broadcastGlideStart);
                long interval = System.currentTimeMillis() - lastGroundTime < 1000L ? 75L : 500L;
                if (MillisecondSampler.currentTimeMillis() - lastGlideRequestTime < interval) {
                    lastRapidGlideTime = MillisecondSampler.currentTimeMillis();
                    if (DeveloperCommand.developerMode)
                        Bukkit.getScheduler().runTask(Matrix.getInstance(), this::broadcastFrequentGliding);
                }
                if (MillisecondSampler.currentTimeMillis() - lastGlideRequestTime < 75L) {
                    if (++rapidGlideRequests >= 5 && enabled)
                        flag("ely.fq", "(elytra) toggle elytra gliding frequently: "
                                + (System.currentTimeMillis() - lastGlideRequestTime) + " (" + rapidGlideRequests + ")", 3);
                } else rapidGlideRequests = Math.max(0, rapidGlideRequests - 1);
                lastGlideRequestTime = System.currentTimeMillis();
            }
        }
        if (type == PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT
                && playerState().clientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            placementAllowance++;
            if (placementAllowance > 5) placementAllowance = 5;
        }
        return false;
    }

    private void broadcastGlideStart() {
        Bukkit.broadcastMessage(new StringBuilder(player().getName()).append(" start gliding").toString());
    }

    private void broadcastFrequentGliding() {
        Bukkit.broadcastMessage(new StringBuilder(player().getName()).append(" frequent gliding").toString());
    }

    public static boolean hasNearbyBoostEntity(Player player, Location location) {
        return player.getWorld().getNearbyEntities(location, 4.0, 5.0, 4.0).stream()
                .filter(entity -> entity.getType() == EntityCategories.firework())
                .count() > 0L;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (frozenMoveTicks > 0) {
            frozenMoveTicks--;
            event.setTo(from);
            return;
        }
        if (!exemptLocations.isEmpty()) {
            DuplicatePosition location = DuplicatePosition.fromLocation(event.getTo());
            if (exemptLocations.contains(location)) {
                exemptLocations.remove(location);
                return;
            }
        }
        Player player = event.getPlayer();
        boolean gliding = playerState().isGliding();
        if (gliding && playerState().locationFlags().getOrCompute(to)
                && BlockEnvironment.isPredicateMaterialAtAndAbove(to)
                && !BlockGeometry.hasNonOpenAdjacentMaterial(to)
                && to.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType() == Material.AIR
                && glidingTicks == 0 && enabled) {
            event.setTo(from);
            flag("ely.str", "weird elytra jump | move: " + DiagnosticFormat.vector(to.toVector().subtract(from.toVector())), 1);
        }
        boolean riptiding = false;
        long now = MillisecondSampler.currentTimeMillis();
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            riptiding = player.isRiptiding();
            if (riptiding) lastRiptideTime = now;
        }
        double verticalMove = to.getY() - from.getY();
        if (gliding) glidingTicks++; else glidingTicks = 0;
        boolean violation = false;
        boolean severe = false;
        if (speedFreezeTime > 0L) {
            if (MillisecondSampler.currentTimeMillis() - speedFreezeTime <= 500L) event.setTo(from);
            else speedFreezeTime = -1L;
            return;
        }
        if (!gliding || PlayerUtilities.hasPlayerDataOrEntityFlag(player)) groundGlideTicks = 0;
        if (playerState().locationFlags().getOrCompute(from)) lastGroundTime = now;
        if (gliding && !riptiding) {
            boolean boosted = hasNearbyBoostEntity(player, from);
            if (boosted) lastBoostTime = now;
            double distance = to.distance(from);
            double kineticEnergy = 0.5 * Math.pow(distance, 2.0);
            double energyChange = kineticEnergy - previousKineticEnergy;
            if (glidingTicks < 5 && !boosted) {
                double horizontalMove = MovementMetrics.locationDistance(from, to);
                double velocityAllowance = playerState().velocityTracker().sumRecentVerticalValues();
                if (glidingTicks > 1) {
                    if (greater(horizontalMove, 0.4 + velocityAllowance) && greater(energyChange, 0.2) && enabled) {
                        flag("ely.ace", "(elytra) move too fast: " + rounded(horizontalMove)
                                + " | val: " + rounded(energyChange) + ", et: " + glidingTicks, 5);
                        violation = severe = true;
                    }
                } else if (glidingTicks == 1 && greater(horizontalMove, 0.4 + velocityAllowance)
                        && greater(energyChange, 0.5) && enabled) {
                    flag("ely.ace", "(elytra) move too fast: " + rounded(horizontalMove)
                            + " | val: " + rounded(energyChange) + ", et: 1$", 5);
                    violation = severe = true;
                }
            }
            if (glidingTicks > 5 && greater(distance, 0.5)
                    && (BlockEnvironment.hasOaPredicateAcrossPlayerHead(player)
                        || (to.getBlock().isLiquid() && from.getBlock().isLiquid()))
                    && !boosted && now - lastBoostTime > 500L
                    && now - playerState().velocityTracker().latestSampleTime() >= 1000L
                    && now - lastRiptideTime > 9000L) {
                if (energyChange >= 0.0) {
                    boolean dolphinGrace = Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                            && player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
                    if (!dolphinGrace && (++liquidEnergyTicks > 2 || greater(energyChange, 0.6))
                            && consumePlacementAllowance() && enabled) {
                        flag("ely.rel", "(elytra) reverse energy: " + rounded(kineticEnergy)
                                + " dif: " + rounded(energyChange) + " tag: liquid", 2);
                        violation = true;
                    }
                } else liquidEnergyTicks = 0;
            } else liquidEnergyTicks = 0;

            if (glidingTicks > 5 && !boosted
                    && now - playerState().velocityTracker().latestSampleTime() >= 1500L
                    && now - lastRiptideTime > 1000L) {
                if (less(MovementMetrics.locationDistance(from, to), 0.02)
                        && greater(verticalMove, 0.43) && greater(from.getPitch(), -80.0f)
                        && consumePlacementAllowance() && enabled) {
                    flag("ely.mvy", "(elytra) impossible elytra movement (move: "
                            + DiagnosticFormat.vector(to.toVector().subtract(from.toVector())) + ", mot: " + verticalMove + ")", 2);
                    violation = true;
                }
                if (verticalMove == 0.0 && less(player.getVelocity().getY(), -1.0)
                        && greater(MovementMetrics.locationDistance(from, to), 0.2)
                        && MaterialPolicy.isAirMaterial(from.getBlock().getType()) && enabled)
                    flag("ely.mvv", "(elytra) weird movement (move: "
                            + DiagnosticFormat.vector(to.toVector().subtract(from.toVector())) + ", vel: " + rounded(verticalMove) + ")", 2);
            }
            if (boosted) {
                double horizontalMove = MovementMetrics.locationDistance(from, to);
                if (greater(horizontalMove, 2.0) && consumePlacementAllowance() && enabled) {
                    flag("ely.bst", "(elytra) move too fast when boosting: " + rounded(horizontalMove), 3);
                    violation = true;
                    if (greater(horizontalMove, 3.0)) severe = true;
                }
            }
            if (!PlayerUtilities.hasPlayerDataOrEntityFlag(player) && verticalMove == 0.0
                    && BlockEnvironment.isPredicateMaterialAtAndAbove(from) && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                    && !BlockEnvironment.hasMaterialAtOrAdjacentTo(from, MaterialPolicy.cobwebMaterial())) {
                groundGlideTicks++;
                if (groundGlideTicks > 2 && enabled) {
                    boolean berryBush = Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                            && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.SWEET_BERRY_BUSH);
                    if (!berryBush && consumePlacementAllowance()) {
                        flag("ely.grd", "(elytra) keep glide pose on ground: " + groundGlideTicks, 4);
                        violation = true;
                    }
                }
            }
            if (DeveloperCommand.developerMode)
                Bukkit.broadcastMessage(String.format("%s: eng_dif: %s, mot: %s, vy: %s, t: %s",
                        player.getName(), energyChange, verticalMove, player.getVelocity().getY(), glidingTicks));
            if (energyChange >= 0.0 && now - lastRiptideTime > 9000L && !boosted && now - lastBoostTime > 1000L
                    && now - playerState().velocityTracker().latestSampleTime() >= 1000L
                    && !BlockEnvironment.isSpecialPredicateAtOrAbove(player)) {
                double horizontalMove = MovementMetrics.locationDistance(from, to);
                if (greater(horizontalMove, 3.2) && consumePlacementAllowance() && enabled) {
                    flag("ely.spd", "(elytra) move too fast with elytra (speed: " + rounded(horizontalMove)
                            + ", tick: " + glidingTicks + ", yAxis: " + rounded(verticalMove) + ")", 2);
                    violation = severe = true;
                    speedFreezeTime = MillisecondSampler.currentTimeMillis();
                }
                if (less(verticalMove, 0.0)) {
                    double ratio = energyChange / -verticalMove;
                    if ((ratio == 0.0 && greater(Math.abs(verticalMove + 0.072), 0.001) || greater(ratio, 0.13))
                            && (glidingTicks > 5 || greater(energyChange, 1.0))) {
                        boolean exempt = Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                                && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.SWEET_BERRY_BUSH);
                        if (!exempt && Math.abs(verticalMove) <= 0.1
                                && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, MaterialPolicy.cobwebMaterial())) exempt = true;
                        if (!exempt && Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_15)
                                && less(verticalMove, -0.09) && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.HONEY_BLOCK)) exempt = true;
                        if (!exempt && Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
                                && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.POWDER_SNOW)) exempt = true;
                        if (!exempt && consumePlacementAllowance() && enabled) {
                            flag("ely.me", "(elytra) more energy: " + rounded(kineticEnergy)
                                    + " dif: " + rounded(energyChange) + " motion: " + rounded(verticalMove)
                                    + " ratio: " + rounded(ratio), greater(ratio, 1.0) ? 3 : 1);
                            violation = true;
                            if (greater(ratio, 1.0) && MillisecondSampler.currentTimeMillis() - lastBoostTime >= 4000L) severe = true;
                        }
                    }
                } else if ((glidingTicks > 4 || less(player.getVelocity().getY(), -0.7) || now - lastRapidGlideTime < 1000L)
                        && !BlockEnvironment.hasFaPredicateAroundCurrentAndAbove(from)) {
                    boolean blocked = BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                            && (!less(horizontalMove, 0.25) || !greater(verticalMove, 0.421))
                            && !less(player.getVelocity().getY(), -1.0);
                    if (!blocked && !BlockEnvironment.hasMaterialAtOrAdjacentTo(from, MaterialPolicy.cobwebMaterial())
                            && consumePlacementAllowance() && enabled) {
                        boolean fromGround = playerState().locationFlags().getOrCompute(from);
                        boolean toGround = playerState().locationFlags().getOrCompute(to);
                        flag("ely.re", "(elytra) reverse energy: " + rounded(kineticEnergy)
                                + " dif: " + rounded(energyChange) + " motion: " + rounded(verticalMove)
                                + " t: " + glidingTicks + " g: " + DiagnosticFormat.formatBooleanArray(new boolean[]{fromGround, toGround})
                                + " v: " + DiagnosticFormat.formatDouble(player.getVelocity().getY()), glidingTicks <= 4 ? 4 : 2);
                        violation = severe = true;
                        if (glidingTicks <= 4) frozenMoveTicks = 10;
                    }
                }
            }
            previousKineticEnergy = kineticEnergy;
            previousMovement = to.toVector().subtract(from.toVector());
            previousDirection = from.getDirection();
        }
        if (violation) {
            int violationLevel = violationCount();
            if (setbackViolationLevel != -1 && violationLevel >= setbackViolationLevel) {
                event.setTo(from);
            }
            if (unequipViolationLevel != -1 && violationLevel >= unequipViolationLevel) {
                player.setGliding(false);
            }
            if (severe && forceSetback && setbackViolationLevel != -1) {
                event.setTo(from);
                player.setGliding(false);
                previousKineticEnergy = 0.0;
            }
        }
    }

    private static double rounded(double value) {
        return NumberUtils.round3(value);
    }

    // The reference skips greater/less branches for unordered (NaN) operands.
    private static boolean greater(double value, double threshold) {
        return value > threshold;
    }

    private static boolean less(double value, double threshold) {
        return value < threshold;
    }

    private Vector simulateGlideStep(Object[] arguments) {
        Vector velocity = (Vector) arguments[0];
        Vector direction = (Vector) arguments[1];
        float pitch = (Float) arguments[2];
        double gravity = 0.08;
        Vector result = velocity.clone();
        double horizontalDirection = Math.sqrt(direction.getX() * direction.getX()
                + direction.getZ() * direction.getZ());
        double speed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getY() * velocity.getY()
                + velocity.getZ() * velocity.getZ());
        float pitchRadians = (float) Math.toRadians((double) pitch);
        float lift = (float) Math.pow(Math.cos((double) pitchRadians), 2.0);
        lift *= (float) Math.min(1.0, speed / 0.4);
        result.add(new Vector(0.0, gravity * (-1.0 + (double) lift * 0.75), 0.0));
        if (result.getY() < 0.0 && horizontalDirection > 0.0) {
            double descentLift = result.getY() * -0.1 * (double) lift;
            result.add(new Vector(direction.getX() * descentLift / horizontalDirection, descentLift,
                    direction.getZ() * descentLift / horizontalDirection));
        }
        if (pitch < 0.0F && horizontalDirection > 0.0) {
            double climb = speed * -Math.sin((double) pitchRadians) * 0.04;
            result.add(new Vector(-direction.getX() * climb / horizontalDirection, climb * 3.2,
                    -direction.getZ() * climb / horizontalDirection));
        }
        if (horizontalDirection > 0.0) {
            result.add(new Vector((direction.getX() / horizontalDirection * speed - result.getX()) * 0.1,
                    0.0, (direction.getZ() / horizontalDirection * speed - result.getZ()) * 0.1));
        }
        result.multiply(new Vector(0.99, 0.98, 0.99));
        return result;
    }

    private Vector estimateGlideAcceleration(Object[] arguments) {
        Vector previous = (Vector) arguments[0];
        Vector current = (Vector) arguments[1];
        return current.clone().multiply(0.85).subtract(previous.clone().multiply(0.5));
    }
}
