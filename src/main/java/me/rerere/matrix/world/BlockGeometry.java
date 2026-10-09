package me.rerere.matrix.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

/** Block sampling, fluid support, and collision-volume helpers. */
public class BlockGeometry {
    private static final Vector[] SAMPLE_OFFSETS;
    private static final double HALF_WIDTH = 0.45;

    static {
        SAMPLE_OFFSETS = new Vector[] {
            new Vector(0, 0, 0),
            new Vector(0.45, 0, 0.45), new Vector(0.45, 0, -0.45),
            new Vector(-0.45, 0, 0.45), new Vector(-0.45, 0, -0.45),
            new Vector(0.45, 0, 0), new Vector(0, 0, 0.45),
            new Vector(-0.45, 0, 0), new Vector(0, 0, -0.45)
        };
    }

    public BlockGeometry() { }

    /** Returns true when the center, block below, or a face neighbor is non-air. */
    public static boolean hasNonOpenAdjacentMaterial(Location location) {
        Block center = location.getBlock();
        Block below = center.getRelative(BlockFace.DOWN);
        boolean centerOpen = isAirMaterial(center.getType());
        if (!centerOpen) return true;
        if (!isAirMaterial(below.getType())) return true;
        for (BlockFace face : BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!isAirMaterial(center.getRelative(face).getType())) return true;
            if (!isAirMaterial(below.getRelative(face).getType())) return true;
        }
        return false;
    }

    /** Checks center-or-below liquid and then all four lower diagonal samples. */
    public static boolean hasLiquidSupport(Location location) {
        Block center = location.getBlock();
        Block below = center.getRelative(BlockFace.DOWN);
        boolean centerLiquid = center.isLiquid();
        if (!centerLiquid && !below.isLiquid()) return false;
        return location.clone().add(HALF_WIDTH, -0.5, HALF_WIDTH).getBlock().isLiquid()
                && location.clone().add(HALF_WIDTH, -0.5, -HALF_WIDTH).getBlock().isLiquid()
                && location.clone().add(-HALF_WIDTH, -0.5, HALF_WIDTH).getBlock().isLiquid()
                && location.clone().add(-HALF_WIDTH, -0.5, -HALF_WIDTH).getBlock().isLiquid();
    }

    public static int floorCoordinate(double coordinate) {
        return NumberConversions.floor(coordinate);
    }

    public static Block blockAtOffset(Location location, Vector offset) {
        World world = location.getWorld();
        int x = floorCoordinate(location.getX() + offset.getX());
        int y = floorCoordinate(location.getY() + offset.getY());
        int z = floorCoordinate(location.getZ() + offset.getZ());
        return world.getBlockAt(x, y, z);
    }

    /** Full block-volume collision scan using the fixed player-like box and shape defaults. */
    public static boolean collides(Location location) {
        World world = Objects.requireNonNull(location.getWorld());
        AxisAlignedBox target = new AxisAlignedBox(
                location.toVector(), (double) 0.3F, 1.5);
        AxisAlignedBox scan = new AxisAlignedBox(
                location.toVector().add(new Vector(0, -1, 0)), (double) 0.3F, (double) 2.8F);
        return scanCollisionVolume(
                world, target, scan, null, false);
    }

    /** Counts matching collisions over the radius-expanded block volume. */
    public static boolean collidesWithin(Location location, double radius) {
        World world = Objects.requireNonNull(location.getWorld());
        AxisAlignedBox target = new AxisAlignedBox(
                location.toVector(), radius, 1.5);
        AxisAlignedBox scan = new AxisAlignedBox(
                location.toVector().add(new Vector(0, -1, 0)), radius, (double) 2.8F);
        int matches = 0;
        for (int x = (int) scan.minX() - 1; x <= scan.maxX() + 1.0; x++) {
            for (int y = (int) scan.minY() - 1; y <= scan.maxY() + 1.0; y++) {
                for (int z = (int) scan.minZ() - 1; z <= scan.maxZ() + 1.0; z++) {
                    Block block = BlockEnvironment.getBlockAt(world, x, y, z);
                    if (shouldSkip(block)) continue;
                    AxisAlignedBox[] boxes = fixedBoxes(block);
                    for (AxisAlignedBox box : boxes) {
                        if (passesContactFilter(box, target, scan)) {
                            matches++;
                            break;
                        }
                    }
                }
            }
        }
        return matches > 0;
    }

    /** AIR shortcut followed by the radius collision scan. */
    public static boolean hasSpaceOrCollision(Location location, double radius) {
        Material current = BlockEnvironment.getMaterialAt(location);
        if (current == Material.AIR) {
            Material below = BlockEnvironment.getMaterialAt(
                    location.clone().add(0.0, -1.0, 0.0));
            if (below != Material.AIR) return true;
        }
        return collidesWithin(location, radius);
    }

    /** For exact integer Y, returns the nine blocks below the location; otherwise applies the original material gate. */
    public static List<Block> sampledBlocks(Location location) {
        List<Block> result = new ArrayList<>();
        double y = location.getY();
        location.getWorld();
        if (y % 1.0 == 0.0) {
            Location lower = location.clone().add(0.0, -1.0, 0.0);
            for (Vector offset : SAMPLE_OFFSETS) {
                result.add(blockAtOffset(lower, offset));
            }
            return result;
        }

        Location current = location.clone();
        Location lower = current.clone().add(0.0, -1.0, 0.0);
        boolean includeLower = false;
        double fraction = current.getY() - current.getBlockY();
        if (fraction <= 0.5 && isAirMaterial(current.getBlock().getType())) {
            for (Vector offset : SAMPLE_OFFSETS) {
                Block sample = blockAtOffset(lower, offset);
                if (MaterialPolicy.containsFenceOrWall(sample.getType())) {
                    includeLower = true;
                }
            }
        }
        for (Vector offset : SAMPLE_OFFSETS) {
            if (includeLower) result.add(blockAtOffset(lower, offset));
            else result.add(blockAtOffset(current, offset));
        }
        return result;
    }

    /** Weighted collision scan using the player's real block-shape context. */
    public static boolean collidesWithShape(Location location, PlayerCheckContext shape) {
        World world = Objects.requireNonNull(location.getWorld());
        AxisAlignedBox target = new AxisAlignedBox(
                location.toVector(), (double) 0.3F, 1.5);
        AxisAlignedBox scan = new AxisAlignedBox(
                location.toVector().add(new Vector(0, -1, 0)), (double) 0.3F, (double) 2.8F);
        return scanCollisionVolume(world, target, scan, shape, true);
    }

    private static boolean isAirMaterial(Material material) {
        ServerVersion serverVersion = Matrix.getServerVersion();
        if (serverVersion.isOlderThan(ServerVersion.V_1_13)) {
            return material == Material.AIR;
        }
        return material == Material.AIR || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private static boolean shouldSkip(Block block) {
        return isAirMaterial(BlockEnvironment.getMaterial(block));
    }

    private static AxisAlignedBox[] fixedBoxes(Block block) {
        return BlockCollisionGeometry.getBlockShapes(block, BlockShapeMode.COLLISION);
    }

    private static AxisAlignedBox[] shapeBoxes(
            Block block, PlayerCheckContext shape) {
        return BlockCollisionGeometry.getBlockShapes(
                block, BlockShapeMode.COLLISION, shape);
    }

    private static boolean passesContactFilter(AxisAlignedBox box,
            AxisAlignedBox target,
            AxisAlignedBox scan) {
        if (box.intersects(target) || !box.intersects(scan)) return false;
        double top = target.minY();
        double lowerBound = top - 0.001;
        double boxY = box.maxY();
        return boxY <= top && boxY >= lowerBound;
    }

    private static boolean passesAboveBlockRule(Block block,
            AxisAlignedBox box, AxisAlignedBox target) {
        Block above = block.getRelative(BlockFace.UP);
        AxisAlignedBox[] upperBoxes = fixedBoxes(above);
        if (upperBoxes.length == 0 || target.intersectsAny(upperBoxes)) return true;
        if (upperBoxes.length == 1) {
            AxisAlignedBox upper = upperBoxes[0];
            if (upper.width() < box.width()) return true;
            if (upper.depth() < box.depth()) return true;
            if (upper.minY() - box.maxY() >= 1.5) return true;
        }
        return upperBoxes.length > 1;
    }

    private static boolean scanCollisionVolume(World world,
            AxisAlignedBox target, AxisAlignedBox scan, PlayerCheckContext shape,
            boolean usePlayerShapes) {
        for (int x = (int) scan.minX() - 1; x <= (int) scan.maxX() + 1; x++) {
            for (int y = (int) scan.minY() - 1; y <= (int) scan.maxY() + 1; y++) {
                for (int z = (int) scan.minZ() - 1; z <= (int) scan.maxZ() + 1; z++) {
                    Block block = BlockEnvironment.getBlockAt(world, x, y, z);
                    if (shouldSkip(block)) continue;
                    AxisAlignedBox[] boxes = usePlayerShapes
                            ? shapeBoxes(block, shape) : fixedBoxes(block);
                    for (AxisAlignedBox box : boxes) {
                        if (passesContactFilter(box, target, scan)
                                && passesAboveBlockRule(block, box, target)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}


