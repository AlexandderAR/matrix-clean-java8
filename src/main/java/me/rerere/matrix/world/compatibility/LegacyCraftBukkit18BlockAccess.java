package me.rerere.matrix.world.compatibility;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.world.WorldAccess;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.MobEffectList;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.Vec3D;
import net.minecraft.server.v1_8_R3.WorldServer;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** Bukkit/NMS bridge for the 1.8_R3 server implementation. */
public final class LegacyCraftBukkit18BlockAccess implements WorldAccess {
    private static int chunkAccessFlag = 45;
    private static Material[] materialsById;

    public LegacyCraftBukkit18BlockAccess() {
        try {
            Field materialIdField = Material.class.getDeclaredField(
                    ReflectionNameResolver.mapFieldName(Material.class, "byId"));
            materialIdField.setAccessible(true);
            materialsById = (Material[]) materialIdField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException failure) {
            me.rerere.matrix.Matrix.logMessage("Failed to init fast chunk accessor!");
        }
    }

    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().bV();
    }



    @Override
    public float getMovementSpeed(LivingEntity entity) {
        return ((EntityHuman) ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity) entity).getHandle()).bI();
    }



    public static Material materialById(int id) {
        Material[] materialLookup = materialsById;
        return materialLookup.length > id && id >= 0 ? materialLookup[id] : null;
    }

    public float getBlockBreakSpeed(Player player, Block block) {
        net.minecraft.server.v1_8_R3.Block nmsBlock = org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers.getBlock(block);
        EntityHuman human = ((CraftPlayer) player).getHandle();
        WorldServer world = ((CraftWorld) player.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        return nmsBlock.getDamage(human, world, position);
    }



    public boolean isOnGround(Entity entity) {
        return ((CraftEntity) entity).getHandle().onGround;
    }



    private static BlockFace toBlockFace(EnumDirection direction) {
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

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        CraftWorld craftWorld = (CraftWorld) block.getWorld();
        net.minecraft.server.v1_8_R3.Block blockHandle = org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers.getBlock(block);
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.IBlockData state = craftWorld.getHandle().getType(position);
        AxisAlignedBB bounds = blockHandle.a(craftWorld.getHandle(), position, state);
        if (bounds == null) return AxisAlignedBox.at(block.getLocation());
        return new AxisAlignedBox(bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }

    @Override
    public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        Vec3D from = new Vec3D(start.getX(), start.getY(), start.getZ());
        Vector endpoint = start.clone().add(direction.clone().normalize().multiply(distance));
        Vec3D to = new Vec3D(endpoint.getX(), endpoint.getY(), endpoint.getZ());
        MovingObjectPosition rayTraceHit = ((CraftWorld) world).getHandle().rayTrace(from, to);
        if (rayTraceHit == null || rayTraceHit.type == MovingObjectPosition.EnumMovingObjectType.MISS) return null;
        net.minecraft.server.v1_8_R3.Entity hitEntity = rayTraceHit.entity;
        return new RaytraceResult(
                new Vector(rayTraceHit.pos.a, rayTraceHit.pos.b, rayTraceHit.pos.c),
                world.getBlockAt(rayTraceHit.a().getX(), rayTraceHit.a().getY(), rayTraceHit.a().getZ()),
                hitEntity == null ? null : hitEntity.getBukkitEntity(),
                toBlockFace(rayTraceHit.direction));
    }

    public AxisAlignedBox getEntityBounds(Entity entity) {
        net.minecraft.server.v1_8_R3.Entity handle = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bounds = handle.getBoundingBox();
        if (bounds == null) return AxisAlignedBox.at(entity.getLocation());
        return new AxisAlignedBox(bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }



    @Override
    public boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        return ((CraftWorld) world).getHandle().getChunkIfLoaded(chunkX, chunkZ) != null;
    }

    public Object getBlockState(Block block) {
        return org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers.getBlock(block);
    }



    @Override
    public int getItemUseDuration(ItemStack stack, LivingEntity ignoredEntity) {
        net.minecraft.server.v1_8_R3.ItemStack copy = CraftItemStack.asNMSCopy(stack);
        return copy.getItem().d(copy);
    }

    @Override
    public int getPotionAmplifierPlusOne(Player player, PotionEffectType effectType) {
        EntityHuman human = ((CraftPlayer) player).getHandle();
        MobEffect effect = human.getEffect(MobEffectList.byId[effectType.getId()]);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    @Override
    public boolean isUsingItem(Player player) {
        return ((EntityHuman) ((CraftEntity) player).getHandle()).bS();
    }

    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        CraftWorld craftWorld = (CraftWorld) block.getWorld();
        net.minecraft.server.v1_8_R3.Chunk chunk = ((CraftChunk) block.getChunk()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.IBlockData state = chunk.getBlockData(position);
        net.minecraft.server.v1_8_R3.Block blockHandle = state.getBlock();
        if (mode == BlockShapeMode.COLLISION) {
            List<AxisAlignedBB> collisionBoxes = new ArrayList<>();
            AxisAlignedBB blockBoundsQuery = AxisAlignedBB.a(block.getX(), block.getY(), block.getZ(),
                    (double) (block.getX() + 1), (double) (block.getY() + 1), (double) (block.getZ() + 1));
            blockHandle.a(craftWorld.getHandle(), position, state, blockBoundsQuery, collisionBoxes, null);
            AxisAlignedBox[] result = new AxisAlignedBox[collisionBoxes.size()];
            for (int i = 0; i < collisionBoxes.size(); i++) {
                AxisAlignedBB box = collisionBoxes.get(i);
                result[i] = new AxisAlignedBox(box.a, box.b, box.c, box.d, box.e, box.f);
            }
            return result;
        }
        if (mode == BlockShapeMode.INTERACT) {
            blockHandle.updateShape(craftWorld.getHandle(), position);
            AxisAlignedBox local = new AxisAlignedBox(
                    blockHandle.B(), blockHandle.D(), blockHandle.F(), blockHandle.C(), blockHandle.E(), blockHandle.G());
            local.translate((double) block.getX(), (double) block.getY(), (double) block.getZ());
            return new AxisAlignedBox[] {local};
        }
        return new AxisAlignedBox[0];
    }



    @Override
    public float getBlockFriction(Block block) {
        return org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers.getBlock(block).frictionFactor;
    }

    public void setOnGround(Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().onGround = onGround;
    }

    public static void setChunkAccessFlag(int value) {
        chunkAccessFlag = value;
    }

    public static int getChunkAccessFlag() {
        return chunkAccessFlag;
    }

    public static int getChunkAccessSentinel() {
        return getChunkAccessFlag() == 0 ? 114 : 0;
    }
}
