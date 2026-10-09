package me.rerere.matrix.checks.movement;

import java.util.Optional;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.entity.MovementBlockResult;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockLocationCache;
import me.rerere.matrix.world.CollisionGeometry;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class PhaseCheck extends CheckRuntimeState {
    private long exemptionStartedMillis;
    private Location lastSafeLocation;
    private Vector expectedTeleport;
    private long freezeStartedMillis;
    private static int cancelViolationLevel;
    private boolean probingShortMove;
    private static long freezeTimeMillis;

    public PhaseCheck(PlayerCheckContext context) {
        super(context);
        setCheckType(CheckCategory.PHASE);
        exemptionStartedMillis = System.currentTimeMillis();
    }

    public void reloadConfiguration() {
        freezeTimeMillis = configuration().getLong("freeze_time", 500L);
        cancelViolationLevel = configuration().getInt("cancel_vl", 5);
    }

    private boolean isNearExemptBlock(Location location) {
        BlockLocationCache exemptions = Matrix.getInstance().getClientBlockManager();
        Block current = location.getBlock();
        Block below = current.getRelative(BlockFace.DOWN);
        if (exemptions.contains(current) || exemptions.contains(below)) return true;
        for (BlockFace face : BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES) {
            if (exemptions.contains(current.getRelative(face))
                    || exemptions.contains(below.getRelative(face))) return true;
        }
        return false;
    }

    public void onPlayerChangedWorld(PlayerChangedWorldEvent ignoredEvent) {
        lastSafeLocation = null;
        exemptionStartedMillis = System.currentTimeMillis();
    }

    public void onPlayerRespawn(PlayerRespawnEvent ignoredEvent) {
        exemptionStartedMillis = System.currentTimeMillis();
    }

    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        expectedTeleport = event.getTo().toVector();
        if (GeometryMath.horizontalManhattanDistance(event.getFrom(), event.getTo()) > 20.0) {
            exemptionStartedMillis = System.currentTimeMillis();
        }
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        World world = event.getFrom().getWorld();
        if (PlayerUtilities.isInVehicleOrPetVehicle(player)) return;
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && (player.isRiptiding() || player.isSwimming())) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && player.getPose() != Pose.STANDING && player.getPose() != Pose.SNEAKING) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (System.currentTimeMillis() - exemptionStartedMillis < 4000L) return;
        if (BlockEnvironment.hasFa8MaterialAroundOrBelow(from)) return;
        if (from.getBlock().getType() == Material.LADDER || to.getBlock().getType() == Material.LADDER) return;
        if (GeometryMath.horizontalManhattanDistance(from, to) > 16.0) return;
        if (System.currentTimeMillis() - freezeStartedMillis < freezeTimeMillis) {
            event.setTo(from);
            return;
        }
        if (expectedTeleport != null && expectedTeleport.equals(event.getTo().toVector())) {
            expectedTeleport = null;
            return;
        }
        boolean collided = false;
        double distance = MovementMetrics.locationDistance(from, to);
        if (!probingShortMove && distance > 0.4
                && (double) Matrix.getInstance().getNmsManager().visitor()
                        .getMovementSpeed(player) < 0.14
                && player.isSneaking()) {
            Vector direction = to.toVector().subtract(from.toVector()).normalize().multiply(0.05);
            Location shortTo = new Location(from.getWorld(), from.getX(), from.getY(), from.getZ()).add(direction);
            PlayerMoveEvent shortEvent = new PlayerMoveEvent(player, from, shortTo);
            probingShortMove = true;
            onPlayerMove(shortEvent, event.getFrom(), shortTo);
            probingShortMove = false;
            if (!shortEvent.getTo().equals(shortTo)) {
                event.setTo(from);
                freezeStartedMillis = System.currentTimeMillis();
                return;
            }
        }
        Vector movement = to.toVector().subtract(from.toVector());
        MovementBlockResult collision = CollisionGeometry.findFirstBlockingBlock(player, from.toVector(), to.toVector(), world,
                playerState());
        Optional<Block> block = collision.getOptionalBlock();
        if (collision.hasVolume()) {
            playerState().movementContext().volume = true;
        }
        if (block.isPresent() && !isNearExemptBlock(to)) {
            String blockName = block.get().getType().name().toLowerCase();
            String movementText = DiagnosticFormat.vector(movement);
            String locationText = DiagnosticFormat.formatLocation(to);
            double roundedDistance = NumberUtils.round3(distance);
            boolean probing = probingShortMove;
            String message = new StringBuilder("moved into block (block: ").append(blockName)
                    .append(", move: ").append(movementText).append(", at: ").append(locationText)
                    .append(", dist: ").append(roundedDistance).append(", loop: ").append(probing).append(")").toString();
            flag("phase.main", message, distance > 0.3 ? 4 : 2);
            if (movement.getY() >= 0.4 || movement.getY() < -0.08 || distance > 0.4
                    || player.getFallDistance() > 4.0f || probingShortMove
                    || violationCount() > cancelViolationLevel) {
                setBack(event, player, lastSafeLocation);
            }
            if (probingShortMove && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)) {
                freezeStartedMillis = System.currentTimeMillis();
            }
            if (violationCount() > 10) freezeStartedMillis = System.currentTimeMillis();
            collided = true;
        }
        if (!collided) lastSafeLocation = to.clone();
    }

    private void setBack(PlayerMoveEvent event, Player player, Location safe) {
        if (player.getFallDistance() > 3.0f) {
            if (playerState().movementContext().capacity != null) {
                event.setTo(playerState().movementContext().capacity);
            }
            return;
        }
        if (safe == null) {
            event.setTo(event.getFrom());
        } else if (safe.getWorld() != null && safe.getWorld().equals(player.getWorld())) {
            event.setTo(safe);
        }
    }
}
