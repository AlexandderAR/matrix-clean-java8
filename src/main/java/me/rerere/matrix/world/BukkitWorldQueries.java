package me.rerere.matrix.world;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.Matrix;
import net.minecraft.server.v1_12_R1.AxisAlignedBB;
import net.minecraft.server.v1_12_R1.BlockFence;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.BlockThin;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkProviderServer;
import net.minecraft.server.v1_12_R1.EnumDirection;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.Item;
import net.minecraft.server.v1_12_R1.MobEffect;
import net.minecraft.server.v1_12_R1.MobEffectList;
import net.minecraft.server.v1_12_R1.MovingObjectPosition;
import net.minecraft.server.v1_12_R1.Vec3D;
import net.minecraft.server.v1_12_R1.WorldServer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Bukkit and 1.12_R1 bridges for entity, block and collision geometry. */
public class BukkitWorldQueries implements WorldAccess {
    private static Material[] materialTable;

    public BukkitWorldQueries() {
        try {
            Field materialIdField = Material.class.getDeclaredField(
                    ReflectionNameResolver.mapFieldName(Material.class, "byId"));
            materialIdField.setAccessible(true);
            materialTable = (Material[]) materialIdField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException failure) {
            Matrix.Companion.logMessage("Failed to init fast chunk accessor!");
        }
    }

    public RaytraceResult rayTraceBlocks(
            org.bukkit.World world, Vector start, Vector direction, double distance) {
        Vec3D nmsStart = new Vec3D(start.getX(), start.getY(), start.getZ());
        Vector end = (Vector) start.clone().add(
                (Vector) direction.clone().normalize().multiply(distance));
        Vec3D nmsEnd = new Vec3D(end.getX(), end.getY(), end.getZ());
        WorldServer worldHandle = ((CraftWorld) world).getHandle();
        MovingObjectPosition hit = worldHandle.rayTrace(nmsStart, nmsEnd);
        if (hit == null || hit.type == MovingObjectPosition.EnumMovingObjectType.MISS) {
            return null;
        }

        net.minecraft.server.v1_12_R1.Entity hitEntity = hit.entity;
        Vector hitPosition = new Vector(hit.pos.x, hit.pos.y, hit.pos.z);
        Block hitBlock = world.getBlockAt(
                hit.a().getX(), hit.a().getY(), hit.a().getZ());
        Entity bukkitEntity = hitEntity == null ? null : hitEntity.getBukkitEntity();
        BlockFace face = blockFaceForDirection(hit.direction);
        return new RaytraceResult(hitPosition, hitBlock, bukkitEntity, face);
    }

    public boolean isOnGround(Entity entity) {
        return ((CraftEntity) entity).getHandle().onGround;
    }

    public AxisAlignedBox getEntityBounds(Entity entity) {
        net.minecraft.server.v1_12_R1.Entity entityHandle = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bounds = entityHandle.getBoundingBox();
        if (bounds == null) {
            return AxisAlignedBox.at(entity.getLocation());
        }
        return new AxisAlignedBox(
                bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }

    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    public void setOnGround(Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().onGround = onGround;
    }

    public float getBlockBreakSpeed(Player player, Block block) {
        WorldServer worldHandle = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = worldHandle.getType(position);
        EntityHuman human = ((CraftPlayer) player).getHandle();
        return blockData.a(human, worldHandle, position);
    }

    public float getBlockFriction(Block block) {
        net.minecraft.server.v1_12_R1.Block blockHandle =
                (net.minecraft.server.v1_12_R1.Block) getBlockState(block);
        return blockHandle.frictionFactor;
    }

    private static int directionMask(EnumDirection direction) {
        return 1 << direction.get2DRotationValue();
    }

    private static int fenceConnectionMask(IBlockData data) {
        int connectionMask = 0;
        if (((Boolean) (Object) data.get(BlockFence.NORTH)).booleanValue()) {
            connectionMask |= 1 << EnumDirection.NORTH.get2DRotationValue();
        }
        if (((Boolean) (Object) data.get(BlockFence.EAST)).booleanValue()) {
            connectionMask |= 1 << EnumDirection.EAST.get2DRotationValue();
        }
        if (((Boolean) (Object) data.get(BlockFence.SOUTH)).booleanValue()) {
            connectionMask |= 1 << EnumDirection.SOUTH.get2DRotationValue();
        }
        if (((Boolean) (Object) data.get(BlockFence.WEST)).booleanValue()) {
            connectionMask |= 1 << EnumDirection.WEST.get2DRotationValue();
        }
        return connectionMask;
    }

    private static int thinConnectionMask(IBlockData data) {
        int connectionMask = 0;
        if (((Boolean) (Object) data.get(BlockThin.NORTH)).booleanValue()) {
            connectionMask |= directionMask(EnumDirection.NORTH);
        }
        if (((Boolean) (Object) data.get(BlockThin.EAST)).booleanValue()) {
            connectionMask |= directionMask(EnumDirection.EAST);
        }
        if (((Boolean) (Object) data.get(BlockThin.SOUTH)).booleanValue()) {
            connectionMask |= directionMask(EnumDirection.SOUTH);
        }
        if (((Boolean) (Object) data.get(BlockThin.WEST)).booleanValue()) {
            connectionMask |= directionMask(EnumDirection.WEST);
        }
        return connectionMask;
    }

    public boolean isChunkLoaded(org.bukkit.World world, int chunkX, int chunkZ) {
        return ((CraftWorld) world).getHandle()
                .getChunkProviderServer().isLoaded(chunkX, chunkZ);
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        WorldServer worldHandle = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = worldHandle.getType(position);
        AxisAlignedBB bounds = blockData.e(worldHandle, position).a(position);
        if (bounds == null) {
            return AxisAlignedBox.at(block.getLocation());
        }
        return new AxisAlignedBox(
                bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }

    private static BlockFace blockFaceForDirection(EnumDirection direction) {
        if (direction == null) return null;
        switch (direction) {
            case UP: return BlockFace.UP;
            case DOWN: return BlockFace.DOWN;
            case NORTH: return BlockFace.NORTH;
            case SOUTH: return BlockFace.SOUTH;
            case WEST: return BlockFace.WEST;
            case EAST: return BlockFace.EAST;
            default: return null;
        }
    }

    public static Material materialById(int index) {
        if (materialTable.length <= index || index < 0) return null;
        return materialTable[index];
    }

    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode collisionMode) {
        WorldServer worldHandle = ((CraftWorld) block.getWorld()).getHandle();
        ChunkProviderServer chunkProvider = worldHandle.getChunkProviderServer();
        Chunk chunk = chunkProvider.getChunkIfLoaded(block.getX() >> 4, block.getZ() >> 4);
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = chunk.getBlockData(position);
        net.minecraft.server.v1_12_R1.Block nmsBlock = blockData.getBlock();

        if (collisionMode == BlockShapeMode.INTERACT) {
            if (!nmsBlock.a(blockData, false)) return new AxisAlignedBox[0];
            AxisAlignedBB bounds = blockData.e(worldHandle, position);
            AxisAlignedBox box = new AxisAlignedBox(
                    bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
            box.translate((double) block.getX(), (double) block.getY(), (double) block.getZ());
            return new AxisAlignedBox[] { box };
        }
        if (collisionMode == BlockShapeMode.COLLISION) {
            List<AxisAlignedBB> boxes = new ArrayList<>();
            AxisAlignedBB unitBlock = new AxisAlignedBB(position);
            blockData.a(worldHandle, position, unitBlock, boxes, null, false);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            for (int index = 0; index < boxes.size(); index++) {
                AxisAlignedBB bounds = boxes.get(index);
                result[index] = new AxisAlignedBox(
                        bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
            }
            return result;
        }
        return new AxisAlignedBox[0];
    }

    public int getPotionAmplifierPlusOne(Player player, PotionEffectType effectType) {
        EntityLiving entityHandle = ((org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity) player).getHandle();
        MobEffectList effectList = MobEffectList.fromId(effectType.getId());
        MobEffect effect = entityHandle.getEffect(effectList);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }


    public float getMovementSpeed(LivingEntity entity) {
        Attribute attribute = BukkitAttributes.INDEX.movementSpeedValue();
        AttributeInstance instance = entity.getAttribute(attribute);
        return (float) instance.getValue();
    }

    public Object getBlockState(Block block) {
        WorldServer worldHandle = ((CraftWorld) block.getWorld()).getHandle();
        ChunkProviderServer chunkProvider = worldHandle.getChunkProviderServer();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        Chunk chunk = chunkProvider.getChunkIfLoaded(block.getX() >> 4, block.getZ() >> 4);
        if (chunk == null) return Material.AIR;
        return chunk.getBlockData(new BlockPosition(x, y, z)).getBlock();
    }

    public int getItemUseDuration(ItemStack item, LivingEntity user) {
        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        Item nmsType = nmsItem.getItem();
        return nmsType.e(nmsItem);
    }

    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().clearActiveItem();
    }















}
