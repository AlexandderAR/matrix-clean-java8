package me.rerere.matrix.world;

import java.util.EnumSet;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.ProtocolTranslationSupport;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

/** Resolves block collision and interaction shapes with legacy-server adjustments. */
public final class BlockCollisionGeometry {
    private static final EnumSet<Material> FULL_CUBE_MATERIALS;
    private static final ThreadLocal<AxisAlignedBox[]> BLOCK_BOUNDS_BUFFER;
    private static final AxisAlignedBox[] EMPTY_BOUNDS;
    private static final Material FARMLAND;
    @Nullable private static final Material DIRT_PATH;
    private static final Material LILY_PAD;

    static {
        ServerVersion server = Matrix.getServerVersion();
        Material grass = server.isOlderThan(ServerVersion.V_1_13)
                ? Material.valueOf("GRASS") : Material.valueOf("GRASS_BLOCK");
        Material endStone = server.isOlderThan(ServerVersion.V_1_13)
                ? Material.valueOf("ENDER_STONE") : Material.valueOf("END_STONE");
        FULL_CUBE_MATERIALS = EnumSet.of(Material.STONE, Material.SAND, Material.COBBLESTONE,
                grass, Material.NETHERRACK, endStone, Material.OBSIDIAN, Material.BEDROCK);
        EMPTY_BOUNDS = new AxisAlignedBox[0];
        BLOCK_BOUNDS_BUFFER = ThreadLocal.withInitial(BlockCollisionGeometry::unitBounds);
        FARMLAND = server.isOlderThan(ServerVersion.V_1_13)
                ? Material.valueOf("SOIL") : Material.FARMLAND;
        DIRT_PATH = server.isNewerThan(ServerVersion.V_1_8_8)
                ? (server.isOlderThan(ServerVersion.V_1_17)
                    ? Material.valueOf("GRASS_PATH") : Material.DIRT_PATH)
                : null;
        LILY_PAD = server.isOlderThan(ServerVersion.V_1_13)
                ? Material.valueOf("WATER_LILY") : Material.LILY_PAD;
    }

    public BlockCollisionGeometry() { }

    public static boolean isUnitCube(AxisAlignedBox[] boxes) {
        return boxes.length == 1
                && boxes[0].width() == 1.0D
                && boxes[0].height() == 1.0D
                && boxes[0].depth() == 1.0D;
    }

    public static AxisAlignedBox defaultBlockBounds(Block block) {
        return new AxisAlignedBox(
                (double) block.getX(), (double) block.getY(), (double) block.getZ(),
                (double) (block.getX() + 1), (double) (block.getY() + 1),
                (double) (block.getZ() + 1));
    }

    public static boolean hasNoCollision(Block block) {
        Material material = block.getType();
        if (MaterialPolicy.isAirMaterial(material)) return true;
        return getBlockShapes(block, BlockShapeMode.COLLISION).length == 0;
    }

    public static boolean isFullCubeMaterial(Material material) {
        return material != null && FULL_CUBE_MATERIALS.contains(material);
    }

    /** Returns the mutable one-element bounds buffer owned by the current thread. */
    public static AxisAlignedBox[] blockBoundsBuffer(Block block) {
        AxisAlignedBox[] boundsBuffer = BLOCK_BOUNDS_BUFFER.get();
        AxisAlignedBox bounds = boundsBuffer[0];
        bounds.setMinX(block.getX());
        bounds.setMinY(block.getY());
        bounds.setMinZ(block.getZ());
        bounds.setMaxX((double) (block.getX() + 1));
        bounds.setMaxY((double) (block.getY() + 1));
        bounds.setMaxZ((double) (block.getZ() + 1));
        return boundsBuffer;
    }

    public static boolean isStandingClearance(Block block, Location location) {
        AxisAlignedBox[] current = getBlockShapes(block, BlockShapeMode.COLLISION);
        if (current.length == 0) return false;
        if (!(location.getY() >= minimumTopY(current)
                && location.getY() <= maximumTopY(current) + 0.031D)) return false;

        Block above = BlockEnvironment.getBlockAt(
                block.getWorld(), block.getX(), block.getY() + 1, block.getZ());
        AxisAlignedBox[] upper = getBlockShapes(above, BlockShapeMode.COLLISION);
        if (upper.length == 0) return true;

        boolean fullClearance = false;
        double lowestUpper = upper[0].minY();
        for (AxisAlignedBox box : upper) {
            if (box.width() < 1.0D
                    || box.depth() < 1.0D
                    || box.maxY() <= location.getY()) {
                fullClearance = true;
            }
            double boxMinY = box.minY();
            if (boxMinY < lowestUpper) lowestUpper = boxMinY;
        }
        return fullClearance || lowestUpper > minimumTopY(current) + 1.0D;
    }

    /** Resolves the canonical block shapes, including full-cube and shulker shortcuts. */
    public static AxisAlignedBox[] getWorldShapes(Block block, BlockShapeMode shapeMode) {
        return getBlockShapes(block, shapeMode);
    }

    private static AxisAlignedBox[] queryWorldShapes(Block block, BlockShapeMode shapeMode) {
        return Matrix.Companion.matrixInstance().getNmsManager().visitor()
                .getCollisionBoxes(block, shapeMode);
    }

    public static AxisAlignedBox[] getBlockShapes(Block block, BlockShapeMode shapeMode) {
        Material material = BlockEnvironment.getMaterial(block);
        if (block == null || MaterialPolicy.isAirMaterial(material)) return EMPTY_BOUNDS;
        if (isFullCubeMaterial(material)) return new AxisAlignedBox[] { defaultBlockBounds(block) };
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && MaterialPolicy.containsShulkerBox(material)) {
            return new AxisAlignedBox[] { defaultBlockBounds(block) };
        }
        return queryWorldShapes(block, shapeMode);
    }

    public static AxisAlignedBox getEntityBounds(LivingEntity entity) {
        return Matrix.Companion.matrixInstance().getNmsManager().visitor()
                .getEntityBounds(entity);
    }

    public static AxisAlignedBox getBlockBounds(Block block) {
        return Matrix.Companion.matrixInstance().getNmsManager().visitor()
                .getBlockBounds(block);
    }

    public static AxisAlignedBox[] unitBounds() {
        return new AxisAlignedBox[] {
                new AxisAlignedBox(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)
        };
    }

    public static AxisAlignedBox[] getBlockShapes(
            Block block, BlockShapeMode shapeMode, Material material) {
        if (block == null || MaterialPolicy.isAirMaterial(material)) return EMPTY_BOUNDS;
        if (isFullCubeMaterial(material)
                || (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                    && MaterialPolicy.containsShulkerBox(material))) {
            return blockBoundsBuffer(block);
        }
        return queryWorldShapes(block, shapeMode);
    }

    public static double maximumTopY(AxisAlignedBox[] boxes) {
        if (boxes.length == 0) return 0.0D;
        double maximum = boxes[0].maxY();
        for (int index = 1; index < boxes.length; index++) {
            double top = boxes[index].maxY();
            if (top > maximum) maximum = top;
        }
        return maximum;
    }

    public static double firstBoxHeight(AxisAlignedBox[] boxes) {
        if (boxes.length == 0) return 0.0D;
        return boxes[0].height();
    }

    public static double minimumTopY(AxisAlignedBox[] boxes) {
        if (boxes.length == 0) return 0.0D;
        double minimum = boxes[0].maxY();
        for (int index = 1; index < boxes.length; index++) {
            double top = boxes[index].maxY();
            if (top < minimum) minimum = top;
        }
        return minimum;
    }

    public static AxisAlignedBox[] getBlockShapes(
            Block block, BlockShapeMode shapeMode, PlayerCheckContext playerData) {
        Material material = BlockEnvironment.getMaterial(block);
        if (block == null || MaterialPolicy.isAirMaterial(material)) return EMPTY_BOUNDS;
        if (isFullCubeMaterial(material)) return new AxisAlignedBox[] { defaultBlockBounds(block) };

        if (ProtocolTranslationSupport.isAvailable()
                && shapeMode == BlockShapeMode.COLLISION
                && material == FARMLAND) {
            AxisAlignedBox bounds = defaultBlockBounds(block);
            if (playerData.protocolVersion() > ClientVersion.V_1_8.getProtocolVersion()) {
                bounds.setMaxY(bounds.minY() + 0.9375D);
            }
            return new AxisAlignedBox[] { bounds };
        }

        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && ProtocolTranslationSupport.isAvailable()
                && shapeMode == BlockShapeMode.COLLISION
                && material == DIRT_PATH
                && playerData.clientVersion() == ClientVersion.V_1_8) {
            return new AxisAlignedBox[] { defaultBlockBounds(block) };
        }

        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && material == Material.BAMBOO
                && shapeMode == BlockShapeMode.COLLISION) {
            return getBlockShapes(block, BlockShapeMode.INTERACT);
        }

        if (ProtocolTranslationSupport.isAvailable() && material == LILY_PAD) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            if (playerData.protocolVersion() > ClientVersion.V_1_8.getProtocolVersion()) {
                return new AxisAlignedBox[] { new AxisAlignedBox(
                        x + 0.0625D, y, z + 0.0625D,
                        x + 0.9375D, y + 0.09375D, z + 0.9375D) };
            }
            return new AxisAlignedBox[] { new AxisAlignedBox(
                    x, y, z, x + 1.0D, (double) ((float) y + 0.015625F), z + 1.0D) };
        }

        return queryWorldShapes(block, shapeMode);
    }
}
