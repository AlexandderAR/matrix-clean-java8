package me.rerere.matrix.platform.bukkit.v1_18_R2;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.world.WorldAccess;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Version-specific bridge between Bukkit and CraftBukkit 1.18 R2. */
public final class CraftBukkit118R2Access implements WorldAccess {
    private final MethodHandle collisionPositionConstructor;
    private final MethodHandle interactionShapeMethod;
    private final MethodHandle otherShapeMethod;
    private final MethodHandle blockDataMethod;
    private final MethodHandle entityGroundFlagMethod;
    private final MethodHandle blockFrictionMethod;
    private final MethodHandle stopUsingItemMethod;
    private final MethodHandle worldBlockStateMethod;

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T raise(Throwable failure) throws E {
        throw (E) failure;
    }

    private static Method firstMatchingMethod(Class<?> owner, Predicate<Method> predicate) {
        for (Method method : owner.getDeclaredMethods()) {
            if (predicate.test(method)) {
                return method;
            }
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }

    public CraftBukkit118R2Access() {
        try {
            Class<?> livingEntityClass = ReflectionLookup.loadVersionedNmsClass("world.entity.EntityLiving");
            Class<?>[] stopUsingItemParameters = new Class<?>[0];
            Method stopUsingItem = livingEntityClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("eS", livingEntityClass,
                            stopUsingItemParameters), stopUsingItemParameters);
            stopUsingItemMethod = ReflectionLookup.methodHandle(stopUsingItem);

            Class<?> blockPositionClass = ReflectionLookup.loadVersionedNmsClass("core.BlockPosition");
            Constructor<?> blockPositionConstructor = blockPositionClass.getDeclaredConstructor(
                    int.class, int.class, int.class);
            collisionPositionConstructor = ReflectionLookup.constructorHandle(blockPositionConstructor);

            Class<?> worldClass = ReflectionLookup.loadVersionedNmsClass("world.level.World");
            Method getBlockState = firstMatchingMethod(worldClass,
                    method -> Intrinsics.areEqual(method.getName(), "a_"));
            worldBlockStateMethod = ReflectionLookup.methodHandle(getBlockState);

            Class<?> blockDataClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            Method interactionShape = firstMatchingMethod(blockDataClass,
                    method -> Intrinsics.areEqual(method.getName(), "j")
                            && method.getParameterCount() == 2);
            interactionShapeMethod = ReflectionLookup.methodHandle(interactionShape);
            Class<?> otherShapeClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            Method otherShape = firstMatchingMethod(otherShapeClass,
                    method -> Intrinsics.areEqual(method.getName(), "k")
                            && method.getParameterCount() == 2);
            otherShapeMethod = ReflectionLookup.methodHandle(otherShape);

            Class<?> blockDataAccessClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            Method getBlockData = blockDataAccessClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("b", blockDataAccessClass, new Class<?>[0]),
                    new Class<?>[0]);
            blockDataMethod = ReflectionLookup.methodHandle(getBlockData);

            Class<?> entityClass = ReflectionLookup.loadVersionedNmsClass("world.entity.Entity");
            Method setGroundFlag = entityClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("c", entityClass,
                            new Class<?>[] { boolean.class }),
                    new Class<?>[] { boolean.class });
            entityGroundFlagMethod = ReflectionLookup.methodHandle(setGroundFlag);

            Class<?> blockClass = ReflectionLookup.loadVersionedNmsClass("world.level.block.Block");
            Method getFriction = firstMatchingMethod(blockClass,
                    method -> Intrinsics.areEqual(method.getName(), "i")
                            && Intrinsics.areEqual(method.getReturnType(), float.class));
            blockFrictionMethod = ReflectionLookup.methodHandle(getFriction);
        } catch (Throwable failure) {
            throw CraftBukkit118R2Access.<RuntimeException, RuntimeException>raise(failure);
        }
    }

    @Override
    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock");
        CraftBlock craftBlock = (CraftBlock) block;
        try {
            return blockDataMethod.invoke(craftBlock.getNMS());
        } catch (Throwable failure) {
            return raise(failure);
        }
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer");
        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        try {
            stopUsingItemMethod.invoke(nmsPlayer);
        } catch (Throwable failure) {
            raise(failure);
        }
    }

    @Override
    public int getItemUseDuration(ItemStack stack, LivingEntity user) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        return nmsStack.c().b(nmsStack);
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        World world = block.getWorld();
        Intrinsics.checkNotNull(world,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R2.CraftWorld");
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        try {
            Object blockPosition = collisionPositionConstructor.invoke(
                    block.getX(), block.getY(), block.getZ());
            Object blockState = worldBlockStateMethod.invoke(nmsWorld, blockPosition);
            VoxelShape shape = mode == BlockShapeMode.INTERACT
                    ? (VoxelShape) interactionShapeMethod.invoke(blockState, nmsWorld, blockPosition)
                    : (VoxelShape) otherShapeMethod.invoke(blockState, nmsWorld, blockPosition);
            List<?> boxes = shape.d();
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (Object box : boxes) {
                AxisAlignedBB bounds = (AxisAlignedBB) box;
                result[index++] = new AxisAlignedBox(
                        block.getX() + bounds.a, block.getY() + bounds.b, block.getZ() + bounds.c,
                        block.getX() + bounds.d, block.getY() + bounds.e, block.getZ() + bounds.f);
            }
            return result;
        } catch (Throwable failure) {
            return raise(failure);
        }
    }

    @Override
    public boolean isOnGround(Entity entity) {
        return entity.isOnGround();
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        org.bukkit.util.BoundingBox bounds = entity.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.AttributeInstance attribute =
                entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue());
        Intrinsics.checkNotNull(attribute);
        return (float) attribute.getValue();
    }

    @Override
    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        Intrinsics.checkNotNull(entity,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity");
        try {
            entityGroundFlagMethod.invoke(((CraftEntity) entity).getHandle(), onGround);
        } catch (Throwable failure) {
            raise(failure);
        }
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        org.bukkit.util.BoundingBox bounds = block.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @Override
    public float getBlockFriction(Block block) {
        Object blockState = getBlockState(block);
        try {
            return (float) blockFrictionMethod.invoke(blockState);
        } catch (Throwable failure) {
            return raise(failure);
        }
    }
}
