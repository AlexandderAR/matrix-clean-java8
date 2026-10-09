package me.rerere.matrix.checks.movement;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Detects scaffold-like placements backed by repeated horizontal movement. */
public final class ScaffoldPlacementCheck extends CheckRuntimeState {
    private static final long PLACEMENT_WINDOW_MILLIS = 400L;
    private static boolean enabled;
    private static int violationWeight;

    @Nullable
    private Vector previousHorizontalMotion;
    private long lastSuspiciousMovementAt;
    private int suspiciousMovementCount;
    private int activeTicks;

    public ScaffoldPlacementCheck(PlayerCheckContext playerData) {
        super(playerData);
        activeTicks = 0;
        suspiciousMovementCount = 0;
        setCheckType(CheckCategory.SCAFFOLD);
    }

    public void onTick() {
        if (activeTicks > 0) {
            activeTicks--;
            if (activeTicks == 0) {
                previousHorizontalMotion = null;
            }
        }
    }

    public void onPlayerMove(@NotNull PlayerMoveEvent event, @NotNull Location from,
                             @NotNull Location to) {
        if (activeTicks <= 0) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isFlying()) {
            return;
        }
        ServerVersion serverVersion = Matrix.getServerVersion();
        if (serverVersion.isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) {
            return;
        }

        World world = from.getWorld();
        WorldAccess platformAccess = Matrix.getInstance().getNmsManager().visitor();
        Block blockBelow = world.getBlockAt(
                MovementMetrics.toInt(from.getX()),
                MovementMetrics.toInt(from.getY()) - 1,
                MovementMetrics.toInt(from.getZ()));
        float blockFriction = platformAccess.getBlockFriction(blockBelow);
        float horizontalScale = 0.91F;
        boolean useBlockFriction = PlayerUtilities.hasPlayerDataOrEntityFlag(player);
        if (useBlockFriction) {
            horizontalScale = blockFriction * 0.91F;
        }

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        if (previousHorizontalMotion == null) {
            previousHorizontalMotion = new Vector(
                    deltaX * horizontalScale, 0.0D, deltaZ * horizontalScale);
            return;
        }

        Vector unexplainedMotion = new Vector(
                deltaX - previousHorizontalMotion.getX(), 0.0D,
                deltaZ - previousHorizontalMotion.getZ());
        if (PlayerUtilities.hasPlayerDataOrEntityFlag(player) && to.getY() == from.getY()
                && unexplainedMotion.length() > 0.08D
                && unexplainedMotion.dot(previousHorizontalMotion) > 0.0D
                && to.getY() % 1.0D == 0.0D) {
            Vector horizontalLook = to.getDirection().setY(0);
            double angle = Math.toDegrees(unexplainedMotion.angle(horizontalLook)) % 45.0D;
            boolean outsideForwardCone = angle > 5.0D
                    && Math.abs(angle - 45.0D) > 5.0D;
            if (outsideForwardCone && BlockEnvironment.isPredicateMaterialAtAndAbove(from)) {
                suspiciousMovementCount++;
                if (suspiciousMovementCount >= 10) {
                    lastSuspiciousMovementAt = MillisecondSampler.currentTimeMillis();
                    suspiciousMovementCount = 10;
                }
            } else {
                suspiciousMovementCount = Math.max(0, suspiciousMovementCount - 2);
            }
        }

        previousHorizontalMotion = new Vector(
                deltaX * horizontalScale, 0.0D, deltaZ * horizontalScale);
    }

    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (!enabled) {
            return;
        }
        if (activeTicks == 0) {
            previousHorizontalMotion = null;
        }
        activeTicks = 15;

        Block placedBlock = event.getBlockPlaced();
        Block againstBlock = event.getBlockAgainst();
        if (placedBlock.getY() != againstBlock.getY()) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player.getLocation().getY() >= (double) (placedBlock.getY() + 1))) {
            return;
        }
        if (placedBlock.getRelative(BlockFace.DOWN).getType() != Material.AIR) {
            return;
        }
        if (PlayerUtilities.hasSwiftSneak(event.getPlayer())) {
            return;
        }
        if (MillisecondSampler.currentTimeMillis() - lastSuspiciousMovementAt
                > PLACEMENT_WINDOW_MILLIS) {
            return;
        }

        flag(
                "sfd.place.s", "scaffold-like placement | type S", violationWeight / 2);
        event.setCancelled(true);
    }

    public void reloadConfiguration() {
        enabled = configuration().getBoolean("modules.place.enable", true);
        violationWeight = configuration().getInt("modules.place.vl_weight", 5);
    }
}
