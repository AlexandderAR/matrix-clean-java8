package me.rerere.matrix.world;

import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IBlockAccess;
import net.minecraft.server.v1_13_R2.VoxelShape;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class V113R2WorldAdapter implements WorldAccess {
    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData state = world.getType(position);
        VoxelShape shape = shapeKind == BlockShapeMode.INTERACT
                ? (VoxelShape) invokeApi(state, IBlockData.class, "getShape",
                        new Class<?>[] {IBlockAccess.class, BlockPosition.class}, world, position)
                : state.getCollisionShape(world, position);
        List<AxisAlignedBB> boxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int index = 0;
        for (AxisAlignedBB box : boxes) {
            result[index++] = new AxisAlignedBox(
                    block.getX() + box.minX, block.getY() + box.minY, block.getZ() + box.minZ,
                    block.getX() + box.maxX, block.getY() + box.maxY, block.getZ() + box.maxZ);
        }
        return result;
    }
    @Override
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData state = world.getType(position);
        return state.getDamage(((CraftPlayer) player).getHandle(), world, position);
    }
    @Override
    public Object getBlockState(Block block) {
        return ((CraftBlock) block).getNMS().getBlock();
    }
    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        CraftWorld craftWorld = (CraftWorld) world;
        WorldServer nativeWorld = craftWorld.getHandle();
        return ((net.minecraft.server.v1_13_R2.ChunkProviderServer)
                nativeWorld.getChunkProvider()).isLoaded(x, z);
    }
    @Override
    public float getMovementSpeed(LivingEntity entity) {
        return (float) entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue()).getValue();
    }
    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().onGround = onGround;
    }
    @Override
    public float getBlockFriction(Block block) {
        return ((net.minecraft.server.v1_13_R2.Block) getBlockState(block)).n();
    }
    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        AxisAlignedBB box = ((CraftEntity) entity).getHandle().getBoundingBox();
        return new AxisAlignedBox(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity ignoredUser) {
        net.minecraft.server.v1_13_R2.ItemStack nativeItem = CraftItemStack.asNMSCopy(item);
        return nativeItem.getItem().c(nativeItem);
    }
    @Override
    public boolean isOnGround(Entity entity) {
        return ((CraftEntity) entity).getHandle().onGround;
    }
    @Override
    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().clearActiveItem();
    }
    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        Object box = invokeApi(block, Block.class, "getBoundingBox", new Class<?>[0]);
        Class<?> boxType = apiMethod(Block.class, "getBoundingBox", new Class<?>[0]).getReturnType();
        return new AxisAlignedBox(
                (Double) invokeApi(box, boxType, "getMinX", new Class<?>[0]),
                (Double) invokeApi(box, boxType, "getMinY", new Class<?>[0]),
                (Double) invokeApi(box, boxType, "getMinZ", new Class<?>[0]),
                (Double) invokeApi(box, boxType, "getMaxX", new Class<?>[0]),
                (Double) invokeApi(box, boxType, "getMaxY", new Class<?>[0]),
                (Double) invokeApi(box, boxType, "getMaxZ", new Class<?>[0]));
    }

    // The supplied 1.13.2 compile SDK lacks these reference API names.
    private static Object invokeApi(Object target, Class<?> owner, String name,
            Class<?>[] parameterTypes, Object... arguments) {
        try {
            return apiMethod(owner, name, parameterTypes).invoke(target, arguments);
        } catch (IllegalAccessException failure) {
            IllegalAccessError error = new IllegalAccessError(owner.getName() + "." + name);
            error.initCause(failure);
            throw error;
        } catch (InvocationTargetException failure) {
            return V113R2WorldAdapter.<RuntimeException, Object>rethrow(failure.getCause());
        }
    }

    private static Method apiMethod(Class<?> owner, String name, Class<?>[] parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException failure) {
            NoSuchMethodError error = new NoSuchMethodError(owner.getName() + "." + name);
            error.initCause(failure);
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R rethrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
