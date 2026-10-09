package me.rerere.matrix.checks.movement;

import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.checks.ElytraFlightCheck;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionTypes;

/** Handles collision-related movement checks and the associated damage/config state. */
public final class CobwebCollisionHandler extends CheckRuntimeState {
    private long lastDamageTimeMillis;
    private static final Material cobwebMaterial;
    private double previousVerticalDelta;
    static int cancelViolationThreshold;

    static {
        cancelViolationThreshold = 30;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            cobwebMaterial = Material.COBWEB;
        } else {
            cobwebMaterial = Material.valueOf("WEB");
        }
    }

    public CobwebCollisionHandler(PlayerCheckContext playerState) {
        super(playerState);
        this.previousVerticalDelta = 0.0;
        setCheckType(CheckCategory.MOVE);
    }

    public void onEntityDamage(EntityDamageEvent event) {
        lastDamageTimeMillis = System.currentTimeMillis();
    }

    public void reloadConfiguration() {
        ConfigurationSection section = configuration();
        cancelViolationThreshold = section.getInt("cancel_vl", 30);
    }

    /** Applies the cobweb movement checks to one player move. */
    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) {
            // The reference queries this state but continues into the web checks.
        }
        if (player.isFlying() || player.getAllowFlight() || player.getVehicle() != null) {
            return;
        }
        if (Math.abs(player.getWalkSpeed() - 0.2F) > 0.02F) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL
                && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        Block fromBlock = from.getBlock();
        Block aboveFrom = from.getBlock().getRelative(BlockFace.UP);
        if (fromBlock.getType() == Material.AIR
                && fromBlock.getRelative(BlockFace.DOWN).getType() == cobwebMaterial) {
            boolean grounded = PlayerUtilities.hasPlayerDataOrEntityFlag(player);
            Block support = from.getBlock().getRelative(BlockFace.DOWN);
            if (grounded) {
                boolean obstructed = false;
                for (BlockFace face : BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES) {
                    Block adjacent = support.getRelative(face);
                    if (adjacent.getType() != Material.AIR
                            && adjacent.getType() != cobwebMaterial) {
                        obstructed = true;
                    }
                }
                if (!(to.getY() >= from.getY())) {
                    obstructed = true;
                }
                if (from.getY() % 1.0D != 0.0D) {
                    obstructed = true;
                }
                if (!obstructed) {
                    flag("move.web",
                            "fast web | type: block walk | tag: #a | val: " + (to.getY() - from.getY()), 3);
                    event.setTo(from);
                }
            }
        }

        if (fromBlock.getType() == cobwebMaterial || aboveFrom.getType() == cobwebMaterial) {
            double verticalDelta = to.getY() - from.getY();
            if (verticalDelta != 0.0D) {
                double baseThreshold = 0.1D;
                if (MillisecondSampler.currentTimeMillis()
                        - lastDamageTimeMillis <= 1200L) {
                    baseThreshold += playerState().velocityTracker().maximumRecentMagnitude();
                }
                if (Math.abs(verticalDelta) > baseThreshold
                        && Math.abs(verticalDelta) >= Math.abs(previousVerticalDelta)
                        && !BlockEnvironment.hasFa8MaterialAroundOrBelow(to)
                        && !BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.SOUL_SAND)
                        && verticalDelta != 0.188D) {
                    flag("move.web",
                            "fast web | move: " + NumberUtils.round3(verticalDelta)
                                    + " | tag: ver", 2);
                    if (Math.random() < 0.5D || verticalDelta > 0.5D) {
                        event.setTo(from);
                    }
                }
            }

            double distance = MovementMetrics.locationDistance(from, to);
            double threshold = 0.121D;
            boolean specialSurface = PlayerUtilities.hasPlayerDataOrEntityFlag(player);
            if (System.currentTimeMillis() - lastDamageTimeMillis < 1500L) {
                threshold += 0.9D;
            }
            PlayerCheckContext playerData = playerState();
            if (playerData.hasPotionEffect(PotionTypes.SPEED)) {
                threshold += PlayerUtilities.potionLevel(playerData, PotionTypes.SPEED) * 0.005D;
            }
            if (System.currentTimeMillis() - playerData.velocityTracker().latestSampleTime() <= 1500L) {
                threshold += playerData.velocityTracker().sumRecentMagnitudes();
            }
            if (player.isSneaking() && specialSurface) {
                threshold -= 0.06D;
            }
            if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8) && playerData.isGliding()) {
                threshold += 0.2D;
                if (ElytraFlightCheck.hasNearbyBoostEntity(player, from)) {
                    threshold += 0.2D;
                }
            }
            float walkSpeed = Matrix.getInstance().getNmsManager().visitor().getMovementSpeed(player);
            if (!player.isSprinting()) {
                walkSpeed *= 1.3F;
            }
            if ((double) walkSpeed > 0.14D) {
                threshold += 0.3D;
            }

            if (to.getBlock().getType() == cobwebMaterial) {
                if (distance > threshold) {
                    flag("move.web",
                            "fast web | " + NumberUtils.round3(distance) + " | tag: a", 4);
                    if (playerData.playerStorage().totalFor(checkType())
                            >= cancelViolationThreshold && cancelViolationThreshold != -1) {
                        Location adjusted = from.clone();
                        adjusted.setY(to.getY());
                        event.setTo(adjusted);
                    }
                }
                if (!specialSurface && !(to.getY() >= from.getY())) {
                    threshold = 0.075D;
                    if (System.currentTimeMillis() - lastDamageTimeMillis < 1500L) {
                        threshold += 0.9D;
                    }
                    if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
                        threshold += 0.01D;
                        if (playerData.isGliding()) {
                            threshold += 0.2D;
                            if (ElytraFlightCheck.hasNearbyBoostEntity(player, from)) {
                                threshold += 0.2D;
                            }
                        }
                    }
                    if (distance > threshold) {
                        flag("move.web",
                                "fast web | " + NumberUtils.round3(distance) + " | tag: b", 4);
                        if (playerData.playerStorage().totalFor(checkType())
                                >= cancelViolationThreshold && cancelViolationThreshold != -1) {
                            Location adjusted = from.clone();
                            adjusted.setY(to.getY());
                            event.setTo(adjusted);
                        }
                    }
                }
            } else if (distance > threshold + 0.2D) {
                flag("move.web",
                        "fast web | " + NumberUtils.round3(distance) + " | tag: c", 4);
                event.setTo(from);
            }
        }
        previousVerticalDelta = to.getY() - from.getY();
    }

}
