package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

/** v1_20_R2 block and entity access backed by the original reflective handles. */
public final class CraftPhysicsAccess implements WorldAccess {
    private final MethodHandle entityGroundSetter;
    private final MethodHandle playerGroundAction;
    private final MethodHandle itemConsumeTicks;
    private final MethodHandle blockFriction;
    private final MethodHandle blockCollisionShape;
    private final MethodHandle blockOcclusionShape;
    private final MethodHandle blockState;
    private final MethodHandle effectAmplifier;
    private final MethodHandle entityMovementSpeed;
    private final MethodHandle entityCollision;
    private final MethodHandle entityBoundingBoxGetter;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockStateGetter;
    private final MethodHandle voxelShapeBoxes;
    private final MethodHandle itemAnimation;

    public CraftPhysicsAccess() {
        super();

        Class<?> livingClass = resolveClass("world.entity.EntityLiving");
        Method livingGround = resolveMappedMethod(livingClass, "F", new Class<?>[0]);
        this.playerGroundAction = requireMethodHandle(livingGround,
                "Cannot find method 'F' in EntityLiving");

        Class<?> positionClass = resolveClass("core.BlockPosition");
        Constructor<?> positionConstructor = resolveConstructor(positionClass,
                int.class, int.class, int.class);
        this.blockPositionConstructor = unreflectConstructor(positionConstructor);

        Class<?> worldClass = resolveClass("world.level.World");
        Method worldBlockState = null;
        for (Method candidate : worldClass.getDeclaredMethods()) {
            if ("a_".equals(candidate.getName())) {
                worldBlockState = candidate;
                break;
            }
        }
        this.worldBlockStateGetter = requireMethodHandle(worldBlockState,
                "Cannot find method 'a_' in World");

        Class<?> blockDataClass = resolveClass("world.level.block.state.BlockBase$BlockData");
        Method collisionShape = firstNamedMethodWithParameterCount(blockDataClass, "j", 2);
        this.blockCollisionShape = requireMethodHandle(collisionShape,
                "Cannot find method 'j' in BlockData");

        blockDataClass = resolveClass("world.level.block.state.BlockBase$BlockData");
        Method occlusionShape = firstNamedMethodWithParameterCount(blockDataClass, "k", 2);
        this.blockOcclusionShape = requireMethodHandle(occlusionShape,
                "Cannot find method 'k' in BlockData");

        blockDataClass = resolveClass("world.level.block.state.BlockBase$BlockData");
        this.blockState = requireMethodHandle(resolveMappedMethod(blockDataClass, "b", new Class<?>[0]),
                "Cannot find method 'b' in BlockData");

        Class<?> entityClass = resolveClass("world.entity.Entity");
        this.entityGroundSetter = requireMethodHandle(
                resolveMappedMethod(entityClass, "c", new Class<?>[] { boolean.class }),
                "Cannot find method 'c' in Entity");

        Class<?> blockClass = resolveClass("world.level.block.Block");
        Method frictionMethod = null;
        for (Method candidate : blockClass.getDeclaredMethods()) {
            if ("h".equals(candidate.getName()) && candidate.getReturnType() == float.class
                    && candidate.getParameterCount() == 0) {
                frictionMethod = candidate;
                break;
            }
        }
        this.blockFriction = requireMethodHandle(frictionMethod,
                "Cannot find method 'h' in Block");

        Class<?> itemClass = resolveClass("world.item.Item");
        Method animationMethod = null;
        for (Method candidate : itemClass.getDeclaredMethods()) {
            if ("EnumAnimation".equals(candidate.getReturnType().getSimpleName())) {
                animationMethod = candidate;
                break;
            }
        }
        this.itemAnimation = requireMethodHandle(animationMethod,
                "Cannot find method 'getAnimation' in Item");

        itemClass = resolveClass("world.item.Item");
        String consumeMethodName = ReflectionNameResolver.mapMethodName("b", itemClass,
                new Class<?>[] { net.minecraft.world.item.ItemStack.class });
        Method consumeMethod = null;
        for (Method candidate : itemClass.getDeclaredMethods()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (candidate.getName().equals(consumeMethodName)
                    && candidate.getReturnType() == int.class && parameters.length == 1
                    && parameters[0] == net.minecraft.world.item.ItemStack.class) {
                consumeMethod = candidate;
                break;
            }
        }
        this.itemConsumeTicks = requireMethodHandle(consumeMethod,
                "Cannot find method 'getConsumeTicks' in Item");

        Class<?> effectClass = resolveClass("world.effect.MobEffect");
        List<Field> instanceIntegerFields = new ArrayList<>();
        for (Field candidate : effectClass.getDeclaredFields()) {
            if (candidate.getType() == int.class && !Modifier.isStatic(candidate.getModifiers())) {
                instanceIntegerFields.add(candidate);
            }
        }
        Field amplifierField = instanceIntegerFields.size() > 1 ? instanceIntegerFields.get(1) : null;
        MethodHandle amplifierHandle = amplifierField == null ? null
                : unreflectFieldGetter(amplifierField);
        if (amplifierHandle == null) {
            throw new IllegalStateException("Cannot find field 'MobEffect.amplifier' in MobEffect");
        }
        this.effectAmplifier = amplifierHandle;

        Class<?> voxelShapeClass = resolveClass("world.phys.shapes.VoxelShape");
        Method boxesMethod = null;
        for (Method candidate : voxelShapeClass.getDeclaredMethods()) {
            if (Modifier.isPublic(candidate.getModifiers())
                    && candidate.getReturnType() == List.class
                    && candidate.getParameterCount() == 0) {
                boxesMethod = candidate;
                break;
            }
        }
        this.voxelShapeBoxes = requireMethodHandle(boxesMethod,
                "Cannot find method 'toAabbs' in VoxelShape");

        Class<?> livingClassForMovementSpeed = resolveClass("world.entity.EntityLiving");
        Method movementSpeedMethod = null;
        for (Method candidate : livingClassForMovementSpeed.getDeclaredMethods()) {
            if ("fe".equals(candidate.getName()) && candidate.getParameterCount() == 0) {
                movementSpeedMethod = candidate;
                break;
            }
        }
        this.entityMovementSpeed = requireMethodHandle(movementSpeedMethod,
                "Cannot find method 'getSpeed(fe())' in EntityLiving");

        entityClass = resolveClass("world.entity.Entity");
        Method boundingBoxMethod = null;
        for (Method candidate : entityClass.getDeclaredMethods()) {
            if ("aG".equals(candidate.getName()) && candidate.getParameterCount() == 0
                    && Modifier.isProtected(candidate.getModifiers())) {
                boundingBoxMethod = candidate;
                break;
            }
        }
        this.entityBoundingBoxGetter = requireMethodHandle(boundingBoxMethod,
                "Cannot find method 'aG' in Entity");

        entityClass = resolveClass("world.entity.Entity");
        Method collisionMethod = null;
        for (Method candidate : entityClass.getDeclaredMethods()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if ("b".equals(candidate.getName()) && parameters.length == 2
                    && Modifier.isProtected(candidate.getModifiers())
                    && candidate.getReturnType() == void.class
                    && parameters[1] == Vec3D.class) {
                collisionMethod = candidate;
                break;
            }
        }
        this.entityCollision = requireMethodHandle(collisionMethod,
                "Cannot find method 'b' in Entity");
    }

    private static Class<?> resolveClass(String encodedName) {
        try {
            return ReflectionLookup.loadVersionedNmsClass(encodedName);
        } catch (ClassNotFoundException failure) {
            return CraftPhysicsAccess.<RuntimeException, Class<?>>throwUnchecked(failure);
        }
    }

    private static Constructor<?> resolveConstructor(Class<?> owner, Class<?>... parameterTypes) {
        try {
            return owner.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException missing) {
            return CraftPhysicsAccess.<RuntimeException, Constructor<?>>throwUnchecked(missing);
        }
    }

    private static Method resolveMappedMethod(Class<?> owner, String encodedName,
                                              Class<?>[] parameterTypes) {
        String mappedName = ReflectionNameResolver.mapMethodName(encodedName, owner, parameterTypes);
        try {
            return owner.getDeclaredMethod(mappedName, parameterTypes);
        } catch (NoSuchMethodException missing) {
            return CraftPhysicsAccess.<RuntimeException, Method>throwUnchecked(missing);
        }
    }

    private static Method firstNamedMethodWithParameterCount(Class<?> owner, String name,
                                                             int parameterCount) {
        for (Method candidate : owner.getDeclaredMethods()) {
            if (name.equals(candidate.getName()) && candidate.getParameterCount() == parameterCount) {
                return candidate;
            }
        }
        return null;
    }

    private static MethodHandle requireMethodHandle(Method method, String failureMessage) {
        if (method == null) throw new IllegalStateException(failureMessage);
        MethodHandle handle;
        try {
            handle = ReflectionLookup.methodHandle(method);
        } catch (IllegalAccessException failure) {
            return CraftPhysicsAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
        if (handle == null) throw new IllegalStateException(failureMessage);
        return handle;
    }

    private static MethodHandle unreflectConstructor(Constructor<?> constructor) {
        try {
            return ReflectionLookup.constructorHandle(constructor);
        } catch (IllegalAccessException failure) {
            return CraftPhysicsAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
    }

    private static MethodHandle unreflectFieldGetter(Field field) {
        try {
            return ReflectionLookup.fieldGetter(field);
        } catch (IllegalAccessException failure) {
            return CraftPhysicsAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
    }

    public void setOnGround(Entity entity, boolean onGround) {
        try {
            Intrinsics.checkNotNull(entity,
                    "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity");
            CraftEntity craft = (CraftEntity) entity;
            entityGroundSetter.invoke(craft.getHandle(), onGround);
        } catch (Throwable failure) {
            CraftPhysicsAccess.<RuntimeException, Object>throwUnchecked(failure);
        }
    }

    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    public int getItemUseDuration(ItemStack item, LivingEntity user) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        try {
            return (int) itemConsumeTicks.invoke(nms.d(), nms);
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, Integer>throwUnchecked(failure);
        }
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public float getBlockFriction(Block block) {
        Object blockState = getBlockState(block);
        try {
            return (float) blockFriction.invoke(blockState);
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(start.toLocation(world), direction, distance);
        if (hit == null) return null;
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
                hit.getHitEntity(), hit.getHitBlockFace());
    }

    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer");
        CraftPlayer craft = (CraftPlayer) player;
        EntityPlayer handle = craft.getHandle();
        try {
            playerGroundAction.invoke(handle);
        } catch (Throwable failure) {
            CraftPhysicsAccess.<RuntimeException, Object>throwUnchecked(failure);
        }
    }

    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public float getMovementSpeed(LivingEntity entity) {
        Intrinsics.checkNotNull(entity,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity");
        CraftEntity craft = (CraftEntity) entity;
        try {
            return (float) entityMovementSpeed.invoke(craft.getHandle());
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.block.CraftBlock");
        CraftBlock craft = (CraftBlock) block;
        try {
            return blockState.invoke(craft.getNMS());
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, Object>throwUnchecked(failure);
        }
    }

    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }

    public float updateSupportingBlock(Entity entity, boolean flag, Vector motion) {
        Intrinsics.checkNotNull(entity,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity");
        CraftEntity craft = (CraftEntity) entity;
        net.minecraft.world.entity.Entity handle = craft.getHandle();
        World entityWorld = craft.getWorld();
        Intrinsics.checkNotNull(entityWorld,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.CraftWorld");
        CraftWorld craftWorld = (CraftWorld) entityWorld;
        WorldServer world = craftWorld.getHandle();
        try {
            entityCollision.invoke(handle, flag, new Vec3D(motion.getX(), motion.getY(), motion.getZ()));
            Object entityBounds = entityBoundingBoxGetter.invoke(handle);
            Object nativeBlockState = worldBlockStateGetter.invoke(world, entityBounds);
            Object nativeBlockData = blockState.invoke(nativeBlockState);
            return (float) blockFriction.invoke(nativeBlockData);
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        World blockWorld = block.getWorld();
        Intrinsics.checkNotNull(blockWorld,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R2.CraftWorld");
        CraftWorld craftWorld = (CraftWorld) blockWorld;
        WorldServer world = craftWorld.getHandle();
        Object position;
        Object state;
        VoxelShape shape;
        try {
            position = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            state = worldBlockStateGetter.invoke(world, position);
            if (shapeKind == BlockShapeMode.INTERACT) {
                shape = (VoxelShape) blockCollisionShape.invoke(state, world, position);
            } else {
                shape = (VoxelShape) blockOcclusionShape.invoke(state, world, position);
            }
            List<AxisAlignedBB> boxes = (List<AxisAlignedBB>) voxelShapeBoxes.invoke(shape);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (AxisAlignedBB box : boxes) {
                result[index++] = new AxisAlignedBox(
                        block.getX() + box.a, block.getY() + box.b, block.getZ() + box.c,
                        block.getX() + box.d, block.getY() + box.e, block.getZ() + box.f);
            }
            return result;
        } catch (Throwable failure) {
            return CraftPhysicsAccess.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
    }















    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
}
