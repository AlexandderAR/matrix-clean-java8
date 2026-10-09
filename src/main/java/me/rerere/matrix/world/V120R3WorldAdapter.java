package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.util.reflect.ReflectionLookup;

/** Bukkit/NMS bridge for the CraftBukkit 1.20_R3 layout. */
public final class V120R3WorldAdapter implements WorldAccess {
    private final MethodHandle stopUsingItemHandle;
    private final MethodHandle entityBlockPositionHandle;
    private final MethodHandle itemUseDuration;
    private final MethodHandle itemUseAnimation;
    private final MethodHandle effectAmplifier;
    private final MethodHandle blockFromState;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle interactionShapeHandle;
    private final MethodHandle collisionShapeHandle;
    private final MethodHandle worldBlockState;
    private final MethodHandle entityGroundFlag;
    private final MethodHandle entitySetPosition;
    private final MethodHandle blockFriction;
    private final MethodHandle livingEntitySpeed;
    private final MethodHandle voxelShapeBoxes;

    public V120R3WorldAdapter() {
        try {
            Class<?> livingClass = loadNmsClass("world.entity.EntityLiving");
            stopUsingItemHandle = methodHandle(livingClass,
                    mappedName("H", livingClass, new Class<?>[0]), new Class<?>[0]);

            Class<?> positionClass = loadNmsClass("core.BlockPosition");
            Constructor<?> positionConstructor = positionClass.getDeclaredConstructor(int.class, int.class, int.class);
            blockPositionConstructor = ReflectionLookup.constructorHandle(positionConstructor);

            Class<?> worldClass = loadNmsClass("world.level.World");
            Method worldState = firstNamedMethod(worldClass, "a_");
            worldBlockState = requireHandle(worldState, "Cannot find method 'a_' in World");

            Class<?> blockDataAngle = loadNmsClass("world.level.block.state.BlockBase$BlockData");
            interactionShapeHandle = requireHandle(firstNamedMethod(blockDataAngle, "j", 2),
                    "Cannot find method 'j' in BlockData");
            Class<?> blockDataOther = loadNmsClass("world.level.block.state.BlockBase$BlockData");
            collisionShapeHandle = requireHandle(firstNamedMethod(blockDataOther, "k", 2),
                    "Cannot find method 'k' in BlockData");
            Class<?> blockDataBlock = loadNmsClass("world.level.block.state.BlockBase$BlockData");
            blockFromState = methodHandle(blockDataBlock,
                    mappedName("b", blockDataBlock, new Class<?>[0]), new Class<?>[0]);

            Class<?> entityGround = loadNmsClass("world.entity.Entity");
            entityGroundFlag = methodHandle(entityGround,
                    mappedName("c", entityGround, new Class<?>[] { boolean.class }),
                    new Class<?>[] { boolean.class });

            Class<?> blockClass = loadNmsClass("world.level.block.Block");
            blockFriction = requireHandle(firstMethod(blockClass, method ->
                    method.getName().equals("i") && method.getReturnType() == float.class
                            && method.getParameterCount() == 0), "Cannot find method 'h' in Block");

            Class<?> itemAnimationOwner = loadNmsClass("world.item.Item");
            itemUseAnimation = requireHandle(firstMethod(itemAnimationOwner, method ->
                    method.getReturnType().getSimpleName().equals("EnumAnimation")),
                    "Cannot find method 'getAnimation' in Item");

            Class<?> itemConsumeOwner = loadNmsClass("world.item.Item");
            Class<?> itemStackClass = net.minecraft.world.item.ItemStack.class;
            String consumeMethodName = mappedName("b", itemConsumeOwner,
                    new Class<?>[] { itemStackClass });
            itemUseDuration = requireHandle(firstMethod(itemConsumeOwner, method ->
                    method.getName().equals(consumeMethodName)
                            && method.getReturnType() == int.class && method.getParameterCount() == 1
                            && method.getParameterTypes()[0] == itemStackClass),
                    "Cannot find method 'getConsumeTicks' in Item");

            Class<?> effectClass = loadNmsClass("world.effect.MobEffect");
            List<Field> effectFields = new ArrayList<>();
            for (Field field : effectClass.getDeclaredFields()) {
                if (field.getType() == int.class && !Modifier.isStatic(field.getModifiers())) {
                    effectFields.add(field);
                }
            }
            Field amplifierField = effectFields.size() > 1 ? effectFields.get(1) : null;
            if (amplifierField == null) {
                throw new IllegalStateException("Cannot find field 'MobEffect.amplifier' in MobEffect");
            }
            MethodHandle amplifierHandle = ReflectionLookup.fieldGetter(amplifierField);
            if (amplifierHandle == null) {
                throw new IllegalStateException("Cannot find field 'MobEffect.amplifier' in MobEffect");
            }
            effectAmplifier = amplifierHandle;

            Class<?> shapeClass = loadNmsClass("world.phys.shapes.VoxelShape");
            voxelShapeBoxes = requireHandle(firstMethod(shapeClass, method ->
                    Modifier.isPublic(method.getModifiers()) && method.getReturnType() == List.class
                            && method.getParameterCount() == 0), "Cannot find method 'toAabbs' in VoxelShape");

            Class<?> livingEntityClass = loadNmsClass("world.entity.EntityLiving");
            livingEntitySpeed = requireHandle(firstNamedMethod(livingEntityClass, "ff", 0),
                    "Cannot find method 'getSpeed' in EntityLiving");

            Class<?> entityBlockPositionOwner = loadNmsClass("world.entity.Entity");
            Method protectedGroundFlag = firstMethod(entityBlockPositionOwner, method ->
                    method.getName().equals("aI") && method.getParameterCount() == 0
                            && Modifier.isProtected(method.getModifiers()));
            entityBlockPositionHandle = requireHandle(protectedGroundFlag, "Cannot find method 'aI' in Entity");

            Class<?> entitySetPositionOwner = loadNmsClass("world.entity.Entity");
            Method setPosition = firstMethod(entitySetPositionOwner, method ->
                method.getName().equals("b") && method.getParameterCount() == 2
                        && Modifier.isProtected(method.getModifiers())
                        && method.getReturnType() == void.class
                        && method.getParameterTypes()[1] == net.minecraft.world.phys.Vec3D.class);
            entitySetPosition = requireHandle(setPosition, "Cannot find method 'b' in Entity");
        } catch (Throwable failure) {
            V120R3WorldAdapter.<RuntimeException, Void>throwUnchecked(failure);
            throw new AssertionError();
        }
    }

    private static Class<?> loadNmsClass(String name) throws ClassNotFoundException {
        return ReflectionLookup.loadVersionedNmsClass(name);
    }

    private static String mappedName(String name, Class<?> owner, Class<?>[] parameters) {
        return ReflectionNameResolver.mapMethodName(name, owner, parameters);
    }

    private static MethodHandle methodHandle(Class<?> owner, String name, Class<?>[] parameters)
            throws NoSuchMethodException, IllegalAccessException {
        Method method = owner.getDeclaredMethod(name, parameters);
        return ReflectionLookup.methodHandle(method);
    }

    private static Method firstNamedMethod(Class<?> owner, String name) {
        for (Method method : owner.getDeclaredMethods()) if (method.getName().equals(name)) return method;
        return null;
    }

    private static Method firstNamedMethod(Class<?> owner, String name, int parameterCount) {
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) return method;
        }
        return null;
    }

    private static Method firstMethod(Class<?> owner, Predicate<Method> predicate) {
        for (Method method : owner.getDeclaredMethods()) if (predicate.test(method)) return method;
        return null;
    }

    private static MethodHandle requireHandle(Method method, String message) throws IllegalAccessException {
        if (method == null) throw new IllegalStateException(message);
        MethodHandle handle = ReflectionLookup.methodHandle(method);
        if (handle == null) throw new IllegalStateException(message);
        return handle;
    }

    @Override public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(start.toLocation(world), direction, distance);
        if (hit == null) return null;
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(), hit.getHitEntity(), hit.getHitBlockFace());
    }

    @Override public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    @Override public float updateSupportingBlock(Entity entity, boolean onGround, Vector movement) {
        try {
            Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity");
            org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity craft =
                    (org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity) entity;
            net.minecraft.world.entity.Entity handle = craft.getHandle();
            org.bukkit.World bukkitWorld = craft.getWorld();
            Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.CraftWorld");
            net.minecraft.server.level.WorldServer world =
                    ((org.bukkit.craftbukkit.v1_20_R3.CraftWorld) bukkitWorld).getHandle();
            net.minecraft.world.phys.Vec3D vector = new net.minecraft.world.phys.Vec3D(
                    movement.getX(), movement.getY(), movement.getZ());
            invokeEntityVector(entitySetPosition, handle, onGround, vector);
            Object position = invokeObjectEntity(entityBlockPositionHandle, handle);
            Object blockState = invokeWorldPosition(worldBlockState, world, position);
            Object supportingBlock = invokeObject(blockFromState, blockState);
            return invokeFloat(blockFriction, supportingBlock);
        } catch (Throwable failure) {
            return V120R3WorldAdapter.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    @Override public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override public int getItemUseDuration(ItemStack stack, LivingEntity ignoredUser) {
        try {
            net.minecraft.world.item.ItemStack copy =
                    org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack.asNMSCopy(stack);
            return invokeItemUseDuration(itemUseDuration, copy.d(), copy);
        } catch (Throwable failure) {
            return V120R3WorldAdapter.<RuntimeException, Integer>throwUnchecked(failure);
        }
    }

    @Override public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock");
        org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock craft =
                (org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock) block;
        try { return invokeBlockState(blockFromState, craft.getNMS()); }
        catch (Throwable failure) { return V120R3WorldAdapter.<RuntimeException, Object>throwUnchecked(failure); }
    }



    @Override public float getBlockFriction(Block block) {
        try {
            Object state = getBlockState(block);
            return invokeFloat(blockFriction, state);
        } catch (Throwable failure) {
            return V120R3WorldAdapter.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    @Override public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    @Override public void setOnGround(Entity entity, boolean onGround) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity");
        net.minecraft.world.entity.Entity handle =
                ((org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity) entity).getHandle();
        try { invokeEntityBoolean(entityGroundFlag, handle, onGround); }
        catch (Throwable failure) { V120R3WorldAdapter.<RuntimeException, Void>throwUnchecked(failure); }
    }



    @Override public boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }



    @Override public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer");
        org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer craft =
                (org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer) player;
        try { invokePlayer(stopUsingItemHandle, craft.getHandle()); }
        catch (Throwable failure) { V120R3WorldAdapter.<RuntimeException, Void>throwUnchecked(failure); }
    }



    @Override public float getMovementSpeed(LivingEntity entity) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity");
        net.minecraft.world.entity.Entity handle =
                ((org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity) entity).getHandle();
        try { return invokeFloatEntity(livingEntitySpeed, handle); }
        catch (Throwable failure) { return V120R3WorldAdapter.<RuntimeException, Float>throwUnchecked(failure); }
    }

    @Override public AxisAlignedBox[] getCollisionBoxes(
            Block block, BlockShapeMode shapeKind) {
        org.bukkit.World bukkitWorld = block.getWorld();
        Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R3.CraftWorld");
        org.bukkit.craftbukkit.v1_20_R3.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_20_R3.CraftWorld) bukkitWorld;
        net.minecraft.server.level.WorldServer world = craftWorld.getHandle();
        try {
            Object position = invokePosition(blockPositionConstructor, block.getX(), block.getY(), block.getZ());
            Object state = invokeWorldPosition(worldBlockState, world, position);
            net.minecraft.world.phys.shapes.VoxelShape shape;
            if (shapeKind == BlockShapeMode.INTERACT) {
                shape = invokeVoxelShape(interactionShapeHandle, state, world, position);
            } else {
                shape = invokeVoxelShape(collisionShapeHandle, state, world, position);
            }
            List<?> boxes = invokeVoxelList(voxelShapeBoxes, shape);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int i = 0;
            for (Object value : boxes) {
                net.minecraft.world.phys.AxisAlignedBB box =
                        (net.minecraft.world.phys.AxisAlignedBB) value;
                result[i++] = new AxisAlignedBox(
                        block.getX() + box.a, block.getY() + box.b, block.getZ() + box.c,
                        block.getX() + box.d, block.getY() + box.e, block.getZ() + box.f);
            }
            return result;
        } catch (Throwable failure) {
            return V120R3WorldAdapter.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
    }



    @Override public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }



    private static Object invokeObject(MethodHandle handle, Object receiver) throws Throwable {
        return (Object) handle.invoke(receiver);
    }
    private static Object invokeBlockState(MethodHandle handle,
                                           net.minecraft.world.level.block.state.IBlockData state) throws Throwable {
        return (Object) handle.invoke(state);
    }
    private static float invokeFloat(MethodHandle handle, Object receiver) throws Throwable {
        return (float) handle.invoke(receiver);
    }
    private static Object invokeObjectEntity(MethodHandle handle, net.minecraft.world.entity.Entity entity) throws Throwable {
        return (Object) handle.invoke(entity);
    }
    private static float invokeFloatEntity(MethodHandle handle, net.minecraft.world.entity.Entity entity) throws Throwable {
        return (float) handle.invoke(entity);
    }
    private static void invokeEntityBoolean(MethodHandle handle, net.minecraft.world.entity.Entity entity, boolean onGround) throws Throwable {
        handle.invoke(entity, onGround);
    }
    private static void invokeEntityVector(MethodHandle handle, net.minecraft.world.entity.Entity entity,
                                           boolean onGround, net.minecraft.world.phys.Vec3D vector) throws Throwable {
        handle.invoke(entity, onGround, vector);
    }
    private static Object invokeWorldPosition(MethodHandle handle, net.minecraft.server.level.WorldServer world,
                                              Object blockPosition) throws Throwable {
        return (Object) handle.invoke(world, blockPosition);
    }
    private static Object invokePosition(MethodHandle handle, int x, int y, int z) throws Throwable {
        return (Object) handle.invoke(x, y, z);
    }
    private static net.minecraft.world.phys.shapes.VoxelShape invokeVoxelShape(
            MethodHandle handle, Object state, net.minecraft.server.level.WorldServer world, Object position) throws Throwable {
        return (net.minecraft.world.phys.shapes.VoxelShape) handle.invoke(state, world, position);
    }
    private static List<?> invokeVoxelList(MethodHandle handle,
                                           net.minecraft.world.phys.shapes.VoxelShape shape) throws Throwable {
        return (List<?>) handle.invoke(shape);
    }
    private static int invokeItemUseDuration(MethodHandle handle, net.minecraft.world.item.Item item,
                                             net.minecraft.world.item.ItemStack stack) throws Throwable {
        return (int) handle.invoke(item, stack);
    }
    private static void invokePlayer(MethodHandle handle, net.minecraft.server.level.EntityPlayer player) throws Throwable {
        handle.invoke(player);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R throwUnchecked(Throwable failure) throws E { throw (E) failure; }
}
