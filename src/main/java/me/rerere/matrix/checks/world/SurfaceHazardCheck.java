package me.rerere.matrix.checks.world;

import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockGeometry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/** Checks movement around cactus and sweet-berry-bush blocks. */
public class SurfaceHazardCheck extends CheckRuntimeState {
    public SurfaceHazardCheck(PlayerCheckContext manager) {
        super(manager);
        setCheckType(CheckCategory.MOVE);
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        boolean hasBypassFlag = PlayerUtilities.hasPlayerDataOrEntityFlag(event.getPlayer());
        Player player = event.getPlayer();
        GameMode initialGameMode = player.getGameMode();
        if (initialGameMode != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        if (hasBypassFlag
                && from.getY() % 1.0d == 0.0d
                && to.getY() % 1.0d == 0.0d
                && to.getY() == from.getY()) {
            Material fromBelow = from.getBlock().getRelative(BlockFace.DOWN).getType();
            if (fromBelow == Material.CACTUS
                    || to.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CACTUS) {
                Block belowDestination = to.getBlock().getRelative(BlockFace.DOWN);
                boolean sameHeightCollision = false;
                BlockFace[] neighbors = BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES;
                for (BlockFace face : neighbors) {
                    Block adjacent = belowDestination.getRelative(face);
                    AxisAlignedBox[] shapes = BlockCollisionGeometry.getBlockShapes(
                            adjacent, BlockShapeMode.COLLISION);
                    if (BlockCollisionGeometry.maximumTopY(shapes) == to.getY()) {
                        sameHeightCollision = true;
                        break;
                    }
                }
                if (!sameHeightCollision && player.getVelocity().getY() < 0.0d) {
                    flag("fly.cactus", "anti cactus", 10);
                    player.damage(2.0d);
                }
            }
        }

        if (Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_14)
                && hasBypassFlag
                && to.getY() >= from.getY()
                && from.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SWEET_BERRY_BUSH) {
            boolean obstructed = false;
            List<Block> sampledBlocks = BlockGeometry.sampledBlocks(from);
            for (Block block : sampledBlocks) {
                if (block.getType() != Material.AIR
                        && block.getType() != Material.SWEET_BERRY_BUSH) {
                    obstructed = true;
                    break;
                }
            }
            if (!obstructed) {
                String detail = DiagnosticFormat.formatLocation(from);
                String violation = "anti berry bush: " + detail;
                flag("fly.berry", violation, 5);
                event.setTo(event.getFrom().clone());
            }
        }
    }
}
