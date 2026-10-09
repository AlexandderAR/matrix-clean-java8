package me.rerere.matrix.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.geometry.BoundingBoxExpansion;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.runtime.EntityCategories;
import me.rerere.matrix.util.GeometryMath;

/** Block queries used by movement checks and collision handling. */
public final class BlockEnvironment {
    public static final BlockFace[] HORIZONTAL_AND_DIAGONAL_FACES = {
        BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
    };
    private static Material[] VINE_MATERIALS;
    public static final int MODERN_MINIMUM_Y;
    private static final BlockFace[] CARDINAL_FACES;
    public static int MAXIMUM_BLOCK_Y;

    static {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
            MODERN_MINIMUM_Y = -64;
        } else {
            MODERN_MINIMUM_Y = 0;
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
            MAXIMUM_BLOCK_Y = 320;
        } else {
            MAXIMUM_BLOCK_Y = 256;
        }
        CARDINAL_FACES = new BlockFace[] {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
        };
    }

    public BlockEnvironment() {
        super();
    }

    /** Lazily caches the four vine materials introduced in the referenced server API. */
    public static Material[] getVineMaterials() {
        if (VINE_MATERIALS == null) {
            VINE_MATERIALS = new Material[] {
                Material.TWISTING_VINES_PLANT,
                Material.WEEPING_VINES_PLANT,
                Material.TWISTING_VINES,
                Material.WEEPING_VINES
            };
        }
        return VINE_MATERIALS;
    }

    public static List<Block> getBlocksInMovementWindow(Location location) {
        World world = location.getWorld();
        Objects.requireNonNull(world);
        List<Block> blocks = new ArrayList<>(256);
        for (int x = location.getBlockX() - 3; x <= location.getBlockX() + 4; x++) {
            for (int z = location.getBlockZ() - 3; z <= location.getBlockZ() + 4; z++) {
                for (int y = location.getBlockY() - 2; y <= location.getBlockY() + 1; y++) {
                    blocks.add(getBlockAt(world, x, y, z));
                }
            }
        }
        return blocks;
    }

    public static java.util.Optional<Block> findOaMatchingBlock(Location location) {
        for (Block block : getBlocksInMovementWindow(location)) {
            if (BlockCollisionGeometry.isStandingClearance(block, location)) {
                return java.util.Optional.of(block);
            }
        }
        return java.util.Optional.empty();
    }

    public static List<Block> getBlocksAroundLocation(Location location) {
        World world = location.getWorld();
        List<Block> blocks = new ArrayList<>();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        for (int x = blockX - 1; x <= blockX + 1; x++) {
            for (int y = blockY - 1; y <= blockY + 2; y++) {
                for (int z = blockZ - 1; z <= blockZ + 1; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public static List<Entity> getEntitiesWithinRadius(Location center, double radius) {
        List<Entity> entities = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            return new ArrayList<>();
        }
        int minChunkX = ChunkLoading.floorCoordinate(
            center.getX() - radius) >> 4;
        for (int chunkX = minChunkX;
             chunkX <= (ChunkLoading.floorCoordinate(
                 center.getX() + radius) >> 4);
             chunkX++) {
            int minChunkZ = ChunkLoading.floorCoordinate(
                center.getZ() - radius) >> 4;
            for (int chunkZ = minChunkZ;
                 chunkZ <= (ChunkLoading.floorCoordinate(
                     center.getZ() + radius) >> 4);
                 chunkZ++) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    Entity[] chunkEntities = world.getChunkAt(chunkX, chunkZ).getEntities();
                    for (Entity entity : chunkEntities) {
                        if (world == entity.getWorld()
                            && entity.getLocation().distanceSquared(center) <= radius * radius) {
                            entities.add(entity);
                        }
                    }
                }
            }
        }
        return entities;
    }

    public static boolean hasSpecialEntityWithinRadius(Location location) {
        for (Entity entity : getEntitiesWithinRadius(location, 2.0D)) {
            if (isSpecialEntityType(entity.getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConfiguredBlockInsideEntityBox(Location location) {
        BlockLocationCache blockCache = Matrix.getInstance()
            .getClientBlockManager();
        Vector position = location.toVector();
        for (Block block : getBlocksInMovementWindow(location)) {
            if (blockCache.contains(block)
                && isVectorStrictlyInsideBlockXZ(block, position)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBoxBlockMatchingPredicate(Location location) {
        World world = location.getWorld();
        AxisAlignedBox box = new AxisAlignedBox(
            location.toVector(), 0.4D, 2.0D);
        return box.cornerBlocks(world).stream()
            .anyMatch(BlockEnvironment::isNotPredicateMaterial);
    }

    /** Returns whether the material is one of the four vine variants. */
    public static boolean isVineMaterial(Material material) {
        return material == Material.TWISTING_VINES_PLANT
            || material == Material.WEEPING_VINES_PLANT
            || material == Material.TWISTING_VINES
            || material == Material.WEEPING_VINES;
    }

    /** Matches blue ice only on server versions that expose that material. */
    private static boolean isBlueIceAvailable(Material material) {
        return Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
            && material == Material.BLUE_ICE;
    }

    public static boolean isIceMaterial(Material material) {
        if (material == Material.ICE || material == Material.PACKED_ICE) {
            return true;
        }
        if (Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_12)
            && material == Material.FROSTED_ICE) {
            return true;
        }
        return Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_13)
            && material == Material.BLUE_ICE;
    }

    public static float frictionAtLocation(WorldAccess friction,
            PlayerCheckContext context, World world, Location location) {
        Block block = getBlockAt(world,
            MovementMetrics.toInt(location.getX()),
            MovementMetrics.toInt(location.getY() - 0x1.0000035afe535p-1),
            MovementMetrics.toInt(location.getZ()));
        return friction.getBlockFriction(block);
    }

    public static float maximumFrictionAroundBlock(Block block) {
        List<Block> neighbors = getBlocksAroundLocation(block.getLocation());
        float maximumFriction = 0.0F;
        for (Block neighbor : neighbors) {
            float friction = getConfiguredBlockFriction(neighbor);
            if (friction > maximumFriction) {
                maximumFriction = friction;
            }
        }
        return maximumFriction;
    }

    /** Matches the slime block material by identity. */
    public static boolean isSlimeBlock(Material material) {
        return material == Material.SLIME_BLOCK;
    }

    /** Delegates entity-type classification to the owning Matrix predicate. */
    public static boolean isSpecialEntityType(EntityType entityType) {
        return EntityCategories.isBoat(entityType);
    }

    public static Block getBlockAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z);
    }

    public static Material getMaterialAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType();
    }

    public static Material getMaterialAtViaAlias(World world, int x, int y, int z) {
        return getMaterialAt(world, x, y, z);
    }

    public static Material getMaterialAt(Location location) {
        return getMaterialAt(location.getWorld(), location.getBlockX(),
            location.getBlockY(), location.getBlockZ());
    }

    public static Material getMaterial(Block block) {
        if (block == null) {
            return Material.AIR;
        }
        return getMaterialAt(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockData getBlockData(Block block) {
        if (Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_13)) {
            throw new IllegalStateException("Can't use Block.getData in <1.13 servers!");
        }
        return block.getBlockData();
    }

    public static boolean isLiquidAtOrAbove(Location location) {
        Block block = location.getBlock();
        if (block.isLiquid()) {
            return true;
        }
        return block.getRelative(BlockFace.UP).isLiquid();
    }

    public static boolean hasAnyLiquidInMovementWindow(Location location) {
        for (Block block : getBlocksInMovementWindow(location)) {
            if (isLiquidWithPositiveLevel(block)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnyBlockMatchingConfiguredPredicate(Location location) {
        BlockLocationCache blockCache = Matrix.getInstance()
            .getClientBlockManager();
        for (Block block : getBlocksInMovementWindow(location)) {
            if (blockCache.contains(block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSolidNonHazardousBlock(Block block) {
        if (block.isLiquid()) {
            return false;
        }
        if (MaterialPolicy.isAirMaterial(block.getType())) {
            return false;
        }
        return block.getType().isSolid();
    }

    private static boolean isLiquidWithPositiveLevel(Block block) {
        if (!block.isLiquid()) {
            return false;
        }
        if (Matrix.getServerVersion()
                .isOlderThanOrEquals(ServerVersion.V_1_12_2)) {
            return block.getData() > 0;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Levelled) {
            return ((Levelled) block.getBlockData()).getLevel() > 0;
        }
        return getMaterial(block) == Material.BUBBLE_COLUMN;
    }

    public static boolean hasIceOnBlockBelowOrItsNeighbors(Location location) {
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        if (isIceMaterial(block.getType())) {
            return true;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (isIceMaterial(below.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isIceMaterial(block.getRelative(face).getType())) {
                return true;
            }
            if (isIceMaterial(below.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNearbyBubbleColumn(Location location) {
        if (Matrix.getServerVersion()
                .isOlderThan(ServerVersion.V_1_13)) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int minimumY = location.getBlockY() - 3;
        int y = location.getBlockY();
        while (y >= minimumY) {
            Block block = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (getMaterial(block) == Material.BUBBLE_COLUMN) {
                return true;
            }
            for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
                if (getMaterial(block.getRelative(face)) == Material.BUBBLE_COLUMN) {
                    return true;
                }
            }
            y--;
        }
        return false;
    }

    public static boolean hasFa5kMaterialInWideBox(Location location) {
        World world = location.getWorld();
        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int y = centerY - 3; y <= centerY + 2; y++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    Block block = getBlockAt(world, x, y, z);
                    if (MaterialPolicy.containsPiston(
                            getMaterial(block))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasFa5kMaterialInMovementBox(Location location) {
        World world = location.getWorld();
        Objects.requireNonNull(world);
        for (int x = location.getBlockX() - 3; x <= location.getBlockX() + 4; x++) {
            for (int z = location.getBlockZ() - 3; z <= location.getBlockZ() + 4; z++) {
                for (int y = location.getBlockY() - 3; y <= location.getBlockY() + 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (MaterialPolicy.containsPiston(
                            getMaterial(block))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void updatePlayerAdjacentBlockStates(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        updateBlockState(block);
        updateBlockState(below);
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            updateBlockState(block.getRelative(face));
            updateBlockState(below.getRelative(face));
        }
    }

    public static boolean hasBlueIceBelowPlayer(Player player) {
        Location location = player.getLocation();
        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        if (isBlueIceAvailable(below.getType())) {
            return true;
        }
        Block twoBelow = below.getRelative(BlockFace.DOWN);
        if (isBlueIceAvailable(twoBelow.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isBlueIceAvailable(below.getRelative(face).getType())) {
                return true;
            }
            if (isBlueIceAvailable(twoBelow.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasIceBelowPlayer(Player player) {
        Location location = player.getLocation();
        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        if (isIceMaterial(getMaterial(below))) {
            return true;
        }
        Block twoBelow = below.getRelative(BlockFace.DOWN);
        if (isIceMaterial(getMaterial(twoBelow))) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isIceMaterial(getMaterial(below.getRelative(face)))) {
                return true;
            }
            if (isIceMaterial(getMaterial(twoBelow.getRelative(face)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFa5mMaterialNearPlayer(Player player) {
        Block block = player.getLocation().getBlock();
        if (MaterialPolicy.containsBedBridge(getMaterial(block))) {
            return true;
        }
        if (MaterialPolicy.containsBedBridge(
                getMaterial(block.getRelative(BlockFace.DOWN)))) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (MaterialPolicy.containsBedBridge(
                    getMaterial(block.getRelative(face)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOpenableOrHasSpecialLocationState(Location location, boolean checkOpenable) {
        Block block = location.getBlock();
        Material material = block.getType();
        if (isLadderOrVineNearLocation(location)) {
            return true;
        }
        if (checkOpenable
            && MaterialPolicy.containsTrapdoor(material)) {
            if (Matrix.getServerVersion()
                    .isOlderThan(ServerVersion.V_1_13)) {
                return block.getData() > 3;
            }
            return ((Openable) block.getBlockData()).isOpen();
        }
        return false;
    }

    public static boolean isSpecialOrOpenableMaterial(Location location, boolean checkOpenable) {
        Block block = location.getBlock();
        Material material = block.getType();
        if (isLadderOrVineNearLocation(location)) {
            return true;
        }
        if (Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_16)
            && isVineMaterial(material)) {
            return true;
        }
        if (Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_17)
            && (material == Material.CAVE_VINES || material == Material.CAVE_VINES_PLANT)) {
            return true;
        }
        if (checkOpenable
            && MaterialPolicy.containsTrapdoor(material)) {
            if (Matrix.getServerVersion()
                    .isOlderThan(ServerVersion.V_1_13)) {
                return block.getData() > 3;
            }
            return ((Openable) block.getBlockData()).isOpen();
        }
        return false;
    }

    public static boolean hasMaterialAtOrAdjacentTo(Location location, Material target) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        Block below = block.getRelative(BlockFace.DOWN);
        if (getMaterial(block) == target || getMaterial(above) == target
            || getMaterial(below) == target) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (getMaterial(block.getRelative(face)) == target
                || getMaterial(above.getRelative(face)) == target
                || getMaterial(below.getRelative(face)) == target) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUnlevelledLiquidWithSolidCardinalNeighbor(Location location) {
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                for (int y = location.getBlockY() - 1; y <= location.getBlockY() + 1; y++) {
                    World world = Objects.requireNonNull(location.getWorld());
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isLiquid() && !isLiquidWithPositiveLevel(block)) {
                        for (BlockFace face : CARDINAL_FACES) {
                            if (MaterialPolicy.isAirMaterial(
                                    block.getRelative(face).getType())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static float getConfiguredBlockFriction(Block block) {
        return Matrix.getInstance()
            .getNmsManager()
            .visitor()
            .getBlockFriction(block);
    }

    private static boolean isNotPredicateMaterial(Block block) {
        return !MaterialPolicy.isAirMaterial(getMaterial(block));
    }

    public static int countAdjacentNonPredicateMaterials(Block block) {
        int count = 0;
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            Material neighborMaterial = block.getRelative(face).getType();
            if (!MaterialPolicy.isAirMaterial(neighborMaterial)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isPredicateMaterialAtAndAbove(Player player) {
        Location location = player.getLocation();
        if (!MaterialPolicy.isAirMaterial(getMaterial(location.getBlock()))) {
            return false;
        }
        Block above = location.getBlock().getRelative(BlockFace.UP);
        return MaterialPolicy.isAirMaterial(getMaterial(above));
    }

    public static boolean isSpecialPredicateAtOrAbove(Player player) {
        Location location = player.getLocation();
        Block block = location.getBlock();
        if (MaterialPolicy.containsFluidBlock(block)) {
            return true;
        }
        Block above = block.getRelative(BlockFace.UP);
        return MaterialPolicy.containsFluidBlock(above);
    }

    public static boolean hasNearbySolidGeometry(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (!MaterialPolicy.isWaterOrLavaAt(block)
            && !MaterialPolicy.isAirMaterial(block.getType())
            && block.getType().isSolid()) {
            return true;
        }
        if (!MaterialPolicy.isWaterOrLavaAt(above)
            && !MaterialPolicy.isAirMaterial(above.getType())
            && above.getType().isSolid()) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isSolidNonHazardousBlock(block.getRelative(face))) {
                return true;
            }
            if (isSolidNonHazardousBlock(above.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOaPredicateAtOrAbove(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        return BlockCollisionGeometry.hasNoCollision(block)
            || BlockCollisionGeometry.hasNoCollision(above);
    }

    public static boolean hasFaPredicateAroundCurrentAndAbove(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (MaterialPolicy.isWaterOrLavaAt(block)
            || MaterialPolicy.isWaterOrLavaAt(above)) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (MaterialPolicy.isWaterOrLavaAt(block.getRelative(face))) {
                return true;
            }
            if (MaterialPolicy.isWaterOrLavaAt(above.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLevelZeroWaterOrWaterlogged(Block block) {
        Material material = block.getType();
        BlockData data = block.getBlockData();
        if (material == Material.WATER) {
            if (data instanceof Levelled) {
                return ((Levelled) data).getLevel() == 0;
            }
            return true;
        }
        if (material == Material.LAVA) {
            return false;
        }
        if (!MaterialPolicy.isWaterOrLavaAt(block)) {
            return false;
        }
        if (data instanceof Waterlogged) {
            return ((Waterlogged) data).isWaterlogged();
        }
        return true;
    }

    private static boolean isVectorStrictlyInsideBlockXZ(Block block, Vector vector) {
        return vector.getX() > block.getX()
            && vector.getZ() > block.getZ()
            && vector.getX() < block.getX() + 1
            && vector.getZ() < block.getZ() + 1;
    }

    private static boolean isMaterialNotWaterWithOaPredicate(Block block) {
        Material material = block.getType();
        if (MaterialPolicy.isAirMaterial(material)) {
            return true;
        }
        if (material == Material.WATER) {
            return true;
        }
        return BlockCollisionGeometry.hasNoCollision(block);
    }

    public static boolean hasOaPredicateAcrossPlayerHead(Player player) {
        Location location = player.getLocation();
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (!MaterialPolicy.isWaterOrLavaAt(block)
            || !MaterialPolicy.isWaterOrLavaAt(above)) {
            return false;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!MaterialPolicy.isWaterOrLavaAt(above.getRelative(face))) {
                return false;
            }
            if (!MaterialPolicy.isWaterOrLavaAt(block.getRelative(face))) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSpecialFallingEntityAbove(Location location) {
        for (Entity entity : getEntitiesWithinRadius(location, 5.5D)) {
            EntityType type = entity.getType();
            double distance = location.distance(entity.getLocation());
            if (isSpecialEntityType(type)
                && entity.getLocation().getY() <= location.getY()
                && distance <= 0x1.2666666666666p1) {
                return true;
            }
            if (Matrix.getServerVersion()
                    .isNewerThanOrEquals(ServerVersion.V_1_12)
                && entity.getType() == EntityType.SHULKER
                && entity.getLocation().getY() <= location.getY()
                && distance <= 0x1.2666666666666p1) {
                return true;
            }
            if (Matrix.getServerVersion()
                    .isNewerThanOrEquals(ServerVersion.V_1_21_6)
                && entity.getType() == EntityType.HAPPY_GHAST
                && entity.getLocation().getY() <= location.getY()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFa8MaterialAroundOrBelow(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        if (MaterialPolicy.containsSupport(block.getType())) {
            return true;
        }
        if (MaterialPolicy.containsSupport(below.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (MaterialPolicy.containsSupport(
                    block.getRelative(face).getType())) {
                return true;
            }
            if (MaterialPolicy.containsSupport(
                    below.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOaWeightWithinUnitHeadVolume(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Block block = eyeLocation.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (!MaterialPolicy.isAirMaterial(block.getType())) {
            return true;
        }
        if (!MaterialPolicy.isAirMaterial(above.getType())
            && MaterialPolicy.isAirMaterial(block.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!MaterialPolicy.isAirMaterial(above.getRelative(face).getType())) {
                Block currentNeighbor = block.getRelative(face);
                if (MaterialPolicy.isAirMaterial(currentNeighbor.getType())) {
                    return true;
                }
            }
            Block shapeNeighbor = block.getRelative(face);
            if (!MaterialPolicy.isAirMaterial(shapeNeighbor.getType())) {
                AxisAlignedBox[] boxes = BlockCollisionGeometry.getBlockShapes(
                    block.getRelative(face), BlockShapeMode.COLLISION);
                double weight = BlockCollisionGeometry.firstBoxHeight(boxes);
                if (weight > 0.0D && weight < 1.0D) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void updateBlockState(Block block) {
        BlockState state = block.getState();
        state.update();
    }

    public static boolean isPlayerOnLadderOrVine(Player player) {
        Material material = player.getLocation().getBlock().getType();
        return material == Material.LADDER || material == Material.VINE;
    }

    public static java.util.Set<Block> getAxisOffsetBlocks(Location location, double radius) {
        double fractionX = location.getX() % 1.0D;
        double fractionZ = location.getZ() % 1.0D;
        java.util.LinkedHashSet<Block> blocks = new java.util.LinkedHashSet<>();
        if (Math.abs(fractionX + radius) > 0.0D) {
            blocks.add(location.clone().add(radius, 0.0D, 0.0D).getBlock());
        }
        if (Math.abs(fractionX - radius) > 0.0D) {
            blocks.add(location.clone().add(-radius, 0.0D, 0.0D).getBlock());
        }
        if (Math.abs(fractionZ + radius) > 0.0D) {
            blocks.add(location.clone().add(0.0D, 0.0D, radius).getBlock());
        }
        if (Math.abs(fractionZ - radius) > 0.0D) {
            blocks.add(location.clone().add(0.0D, 0.0D, -radius).getBlock());
        }
        return blocks;
    }

    public static boolean isLadderOrVineNearLocation(Location location) {
        Material material = location.getBlock().getType();
        if (material == Material.LADDER || material == Material.VINE) {
            return true;
        }
        boolean found = false;
        for (Block block : getAxisOffsetBlocks(location, 0.1D)) {
            Material offsetMaterial = block.getType();
            if (offsetMaterial == Material.LADDER || offsetMaterial == Material.VINE) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static boolean hasForbiddenMaterialNeighbor(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        if (!MaterialPolicy.isAirMaterial(block.getType())) {
            return true;
        }
        if (!MaterialPolicy.isAirMaterial(above.getType())
            && MaterialPolicy.isAirMaterial(block.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!MaterialPolicy.isAirMaterial(above.getRelative(face).getType())
                && MaterialPolicy.isAirMaterial(block.getType())
                && MaterialPolicy.isAirMaterial(block.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMatchingNeighborAroundCurrentAndAbove(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!MaterialPolicy.isAirMaterial(getMaterial(block.getRelative(face)))) {
                return true;
            }
            if (!MaterialPolicy.isAirMaterial(getMaterial(above.getRelative(face)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPlayerNeighborhoodCondition(Player player) {
        if (player == null) {
            return false;
        }
        Block block = player.getLocation().getBlock();
        Block above = block.getRelative(BlockFace.UP);
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (!MaterialPolicy.isAirMaterial(getMaterial(block.getRelative(face)))) {
                return true;
            }
            if (!MaterialPolicy.isAirMaterial(getMaterial(above.getRelative(face)))) {
                return true;
            }
        }
        return hasOaWeightWithinUnitHeadVolume(player);
    }

    public static boolean hasFa5qMaterialInSupportNeighborhood(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        if (MaterialPolicy.containsShulkerBox(block.getType())) {
            return true;
        }
        if (MaterialPolicy.containsShulkerBox(below.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (MaterialPolicy.containsShulkerBox(
                    below.getRelative(face).getType())) {
                return true;
            }
            if (MaterialPolicy.containsShulkerBox(
                    block.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOaLocationPredicateInMovementWindow(Location location) {
        for (Block block : getBlocksInMovementWindow(location)) {
            if (block.getY() > location.getBlockY()
                || block.getY() < location.getBlockY() - 1) {
                continue;
            }
            if (BlockCollisionGeometry.isStandingClearance(block, location)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFa5ySolidBlockNearOrBelow(Location location) {
        Block block = location.getBlock();
        if (!MaterialPolicy.isAirMaterial(block.getType())
            && block.getType().isSolid()
            && !MaterialPolicy.containsFluidMaterial(
                block.getType())) {
            return true;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (!MaterialPolicy.isAirMaterial(below.getType())
            && !MaterialPolicy.containsFluidMaterial(
                below.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            Block neighbor = block.getRelative(face);
            Block lowerNeighbor = neighbor.getRelative(BlockFace.DOWN);
            if (!MaterialPolicy.isAirMaterial(neighbor.getType())
                && neighbor.getType().isSolid()
                && !MaterialPolicy.containsFluidMaterial(
                    neighbor.getType())) {
                return true;
            }
            if (!MaterialPolicy.isAirMaterial(lowerNeighbor.getType())
                && !MaterialPolicy.containsFluidMaterial(
                    lowerNeighbor.getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFa5yBlockAtOrBelow(Location location) {
        Block block = location.getBlock();
        if (!MaterialPolicy.isAirMaterial(block.getType())
            && !MaterialPolicy.containsFluidMaterial(
                block.getType())) {
            return true;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (!MaterialPolicy.isAirMaterial(below.getType())
            && !MaterialPolicy.containsFluidMaterial(
                below.getType())) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            Block neighbor = block.getRelative(face);
            Block lowerNeighbor = neighbor.getRelative(BlockFace.DOWN);
            if (!MaterialPolicy.isAirMaterial(neighbor.getType())
                && !MaterialPolicy.containsFluidMaterial(
                    neighbor.getType())) {
                return true;
            }
            if (!MaterialPolicy.isAirMaterial(lowerNeighbor.getType())
                && !MaterialPolicy.containsFluidMaterial(
                    lowerNeighbor.getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasConfiguredBlockStateInMovementWindow(Location location) {
        BlockLocationCache blockCache = Matrix.getInstance()
            .getClientBlockManager();
        for (Block block : getBlocksInMovementWindow(location)) {
            ClientBlock cachedBlock = blockCache.get(block);
            if (cachedBlock != null
                && MaterialPolicy.containsFluidMaterial(
                    cachedBlock.getType())) {
                return true;
            }
        }
        return false;
    }

    private static BoundingBoxExpansion createLiquidColumnDescription(Block block) {
        Block currentBlock = block.getRelative(BlockFace.UP);
        int validLayerCount = 0;
        boolean foundSolidBoundary = false;
        for (int layer = 1; layer <= 5; layer++) {
            boolean waterLevelValid = isLevelZeroWaterOrWaterlogged(currentBlock);
            boolean materialBoundary = isMaterialNotWaterWithOaPredicate(currentBlock);
            if (waterLevelValid
                && (currentBlock.getType() == Material.WATER || materialBoundary)) {
                validLayerCount++;
                currentBlock = currentBlock.getRelative(BlockFace.UP);
            } else {
                if (validLayerCount > 0 && materialBoundary) {
                    foundSolidBoundary = true;
                }
                break;
            }
        }
        if (!foundSolidBoundary || validLayerCount < 1 || validLayerCount > 4) {
            return null;
        }
        int acceptedHeight = 0;
        int maximumHeight = validLayerCount * 6;
        currentBlock = block.getRelative(BlockFace.UP);
        while (acceptedHeight < maximumHeight
            && isMaterialNotWaterWithOaPredicate(currentBlock)) {
            acceptedHeight++;
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }
        if (acceptedHeight <= 0) {
            return null;
        }
        AxisAlignedBox box = new AxisAlignedBox(
            block.getX(), block.getY() + 1, block.getZ(),
            block.getX() + 1, block.getY() + 1 + acceptedHeight, block.getZ() + 1);
        return new BoundingBoxExpansion(box, validLayerCount);
    }

    public static boolean isPredicateMaterialAtAndAbove(Location location) {
        if (!MaterialPolicy.isAirMaterial(getMaterialAt(location))) {
            return false;
        }
        Location above = location.clone().add(0.0D, 1.0D, 0.0D);
        return MaterialPolicy.isAirMaterial(getMaterialAt(above));
    }

    public static BlockFace findNeighborFace(Block block, Block neighbor) {
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (block.getRelative(face).equals(neighbor)) {
                return face;
            }
        }
        return BlockFace.SELF;
    }

    public static boolean hasSlimeBlockUnderPlayer(Player player) {
        Block block = player.getLocation().getBlock();
        if (isSlimeBlock(getMaterial(block))) {
            return true;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (isSlimeBlock(getMaterial(below))) {
            return true;
        }
        Block twoBelow = below.getRelative(BlockFace.DOWN);
        if (isSlimeBlock(getMaterial(twoBelow))) {
            return true;
        }
        for (BlockFace face : HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isSlimeBlock(getMaterial(below.getRelative(face)))) {
                return true;
            }
            if (isSlimeBlock(getMaterial(twoBelow.getRelative(face)))) {
                return true;
            }
        }
        return false;
    }

    public static List<BoundingBoxExpansion> findWaterCollisionBoxes(World world,
                                                                  AxisAlignedBox scan) {
        if (Matrix.getServerVersion()
                .isOlderThan(ServerVersion.V_26_2)
            || MaterialPolicy.potentSulfur == null) {
            return java.util.Collections.emptyList();
        }
        int minX = GeometryMath.floorToInt(scan.minX());
        int maxX = (int) Math.ceil(scan.maxX()) - 1;
        int minZ = GeometryMath.floorToInt(scan.minZ());
        int maxZ = (int) Math.ceil(scan.maxZ()) - 1;
        int minY = Math.max(MODERN_MINIMUM_Y,
            GeometryMath.floorToInt(scan.minY()) - 24);
        int maxY = Math.min(MAXIMUM_BLOCK_Y - 1,
            (int) Math.ceil(scan.maxY() - 1.0D) - 1);
        List<BoundingBoxExpansion> collisions = new ArrayList<>(4);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == MaterialPolicy.potentSulfur
                        && blockDataHasWaterColumnState(block)) {
                        BoundingBoxExpansion liquidBox = createLiquidColumnDescription(block);
                        if (liquidBox != null
                            && liquidBox.getBoundingBox().intersects(scan)) {
                            collisions.add(liquidBox);
                        }
                    }
                }
            }
        }
        return collisions;
    }

    public static java.util.Optional<Block> findOaMatchingBlock(World world,
            AxisAlignedBox scan, Location location) {
        int minX = GeometryMath.floorToInt(scan.minX());
        int minY = GeometryMath.floorToInt(scan.minY());
        int minZ = GeometryMath.floorToInt(scan.minZ());
        int maxX = GeometryMath.floorToInt(scan.maxX() + 1.0D);
        int maxY = GeometryMath.floorToInt(scan.maxY() + 1.0D);
        int maxZ = GeometryMath.floorToInt(scan.maxZ() + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (BlockCollisionGeometry.isStandingClearance(block, location)) {
                        return java.util.Optional.of(block);
                    }
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean blockDataHasWaterColumnState(Block block) {
        String blockData = block.getBlockData().getAsString(false);
        return blockData.contains("potent_sulfur_state=erupting")
            || blockData.contains("potent_sulfur_state=continuous");
    }
}
