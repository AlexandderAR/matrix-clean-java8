package me.rerere.matrix.compat.nms;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.NoSuchElementException;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.world.WorldAccess;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import me.rerere.matrix.geometry.BlockShapeMode;

/** CraftBukkit v1_19_R1 accessors and collision-shape conversion. */
public final class V119R1NmsAccess implements WorldAccess {
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockStateLookup;
    private final MethodHandle angleCollisionShape;
    private final MethodHandle weightedCollisionShape;
    private final MethodHandle blockDataGetter;
    private final MethodHandle entityBlockFlagSetter;
    private final MethodHandle blockFrictionGetter;
    private final MethodHandle playerUpdate;

    public V119R1NmsAccess() {
        Class<?> livingEntityClass = loadNmsClass("world.entity.EntityLiving");
        Method livingEntityFlag = declaredMethod(livingEntityClass,
                decodeMemberName("A", livingEntityClass));
        playerUpdate = methodHandle(livingEntityFlag);

        Class<?> blockPositionClass = loadNmsClass("core.BlockPosition");
        MethodHandle resolvedBlockPositionConstructor;
        try {
            Constructor<?> constructor = blockPositionClass.getDeclaredConstructor(
                    int.class, int.class, int.class);
            resolvedBlockPositionConstructor = constructorHandle(constructor);
        } catch (ReflectiveOperationException exception) {
            resolvedBlockPositionConstructor = V119R1NmsAccess
                    .<RuntimeException, MethodHandle>throwUnchecked(exception);
        }
        blockPositionConstructor = resolvedBlockPositionConstructor;

        Class<?> worldClass = loadNmsClass("world.level.World");
        worldBlockStateLookup = methodHandle(
                declaredMethodByName(worldClass, "a_"));
        Class<?> blockDataClass = loadNmsClass("world.level.block.state.BlockBase$BlockData");
        angleCollisionShape = methodHandle(
                declaredMethodByNameAndParameterCount(blockDataClass, "j", 2));
        weightedCollisionShape = methodHandle(
                declaredMethodByNameAndParameterCount(blockDataClass, "k", 2));
        blockDataGetter = methodHandle(
                declaredMethod(blockDataClass, decodeMemberName("b", blockDataClass)));
        Class<?> nmsEntityClass = loadNmsClass("world.entity.Entity");
        entityBlockFlagSetter = methodHandle(
                declaredMethod(nmsEntityClass, decodeMemberName("c", nmsEntityClass,
                        boolean.class)));
        Class<?> nmsBlockClass = loadNmsClass("world.level.block.Block");
        blockFrictionGetter = methodHandle(
                declaredMethodByNameAndReturnType(nmsBlockClass, "h", float.class));
    }

    private static Method declaredMethod(Class<?> owner, String methodName,
                                         Class<?>... parameterTypes) {
        try {
            return owner.getDeclaredMethod(methodName, parameterTypes);
        } catch (ReflectiveOperationException exception) {
            return V119R1NmsAccess.<RuntimeException, Method>throwUnchecked(exception);
        }
    }

    private static Method declaredMethodByName(Class<?> owner, String methodName) {
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return method;
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }

    private static Method declaredMethodByNameAndParameterCount(Class<?> owner, String methodName,
                                                                  int parameterCount) {
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }

    private static Method declaredMethodByNameAndReturnType(Class<?> owner, String methodName,
                                                              Class<?> returnType) {
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getReturnType() == returnType) {
                return method;
            }
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }

    private static Class<?> loadNmsClass(String name) {
        try { return ReflectionLookup.loadVersionedNmsClass(name); }
        catch (ClassNotFoundException exception) {
            return V119R1NmsAccess.<RuntimeException, Class<?>>throwUnchecked(exception);
        }
    }

    private static MethodHandle methodHandle(Method method) {
        try { return ReflectionLookup.methodHandle(method); }
        catch (IllegalAccessException exception) {
            return V119R1NmsAccess.<RuntimeException, MethodHandle>throwUnchecked(exception);
        }
    }

    private static MethodHandle constructorHandle(Constructor<?> constructor) {
        try { return ReflectionLookup.constructorHandle(constructor); }
        catch (IllegalAccessException exception) {
            return V119R1NmsAccess.<RuntimeException, MethodHandle>throwUnchecked(exception);
        }
    }
    private static String decodeMemberName(String methodName, Class<?> owner,
                                           Class<?>... parameterTypes) {
        return ReflectionNameResolver.mapMethodName(methodName, owner, parameterTypes);
    }

    private static Object invoke(MethodHandle handle, Object... arguments) {
        try {
            return handle.invokeWithArguments(arguments);
        } catch (Throwable throwable) {
            return V119R1NmsAccess.<RuntimeException, Object>throwUnchecked(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable throwable) throws E {
        throw (E) throwable;
    }

    @Override public void setOnGround(Entity entity, boolean enabled) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(entity,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity");
        CraftEntity craftEntity = (CraftEntity) entity;
        invoke(entityBlockFlagSetter, craftEntity.getHandle(), Boolean.valueOf(enabled));
    }

    @Override public int getItemUseDuration(ItemStack item, LivingEntity ignoredUser) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        return nmsStack.c().b(nmsStack);
    }

    @Override public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    @Override public float getBlockFriction(Block block) {
        Object nmsBlock = getNmsBlock(block);
        return ((Number) invoke(blockFrictionGetter, nmsBlock)).floatValue();
    }

    @Override public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction,
                                      double maximumDistance) {
        RayTraceResult hit = world.rayTraceBlocks(start.toLocation(world), direction,
                maximumDistance);
        return hit == null ? null : new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
                hit.getHitEntity(), hit.getHitBlockFace());
    }

    @Override public Object getBlockState(Block block) {
        return getNmsBlock(block);
    }

    private Object getNmsBlock(Block block) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(block,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock");
        return invoke(blockDataGetter, ((CraftBlock) block).getNMS());
    }

    @Override public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    @Override public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override public float getMovementSpeed(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(
                BukkitAttributes.INDEX.movementSpeedValue());
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(attribute);
        return (float) attribute.getValue();
    }

    @Override public void stopUsingItem(Player player) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(player,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer");
        invoke(playerUpdate, ((CraftPlayer) player).getHandle());
    }

    @Override public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    @Override public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }

    @Override public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        World world = block.getWorld();
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(world,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R1.CraftWorld");
        CraftWorld craftWorld = (CraftWorld) world;
        Object nmsWorld = craftWorld.getHandle();
        Object position = invoke(blockPositionConstructor, Integer.valueOf(block.getX()),
                Integer.valueOf(block.getY()), Integer.valueOf(block.getZ()));
        Object blockData = invoke(worldBlockStateLookup, nmsWorld, position);
        Object shape = invoke(mode == BlockShapeMode.INTERACT
                ? angleCollisionShape : weightedCollisionShape, blockData, nmsWorld, position);
        List<?> boxes = (List<?>) callVoxelShapeBoxes(shape);
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int index = 0;
        for (Object value : boxes) {
            net.minecraft.world.phys.AxisAlignedBB box =
                    (net.minecraft.world.phys.AxisAlignedBB) value;
            result[index++] = new AxisAlignedBox(
                    block.getX() + box.a, block.getY() + box.b, block.getZ() + box.c,
                    block.getX() + box.d, block.getY() + box.e, block.getZ() + box.f);
        }
        return result;
    }

    private static Object callVoxelShapeBoxes(Object shape) {
        return ((net.minecraft.world.phys.shapes.VoxelShape) shape).d();
    }

    @Override public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

}
