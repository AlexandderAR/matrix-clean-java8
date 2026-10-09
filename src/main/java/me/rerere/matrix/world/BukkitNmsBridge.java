package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.MethodMatcherGroup;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.reflect.MethodProbeHarness;

/** CraftBukkit and Mojang bridge operations recovered from the canonical ASM. */
public final class BukkitNmsBridge implements WorldAccess {

    private final MethodHandle stopUsingItemHandle;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockStateHandle;
    private final MethodHandle interactionShapeHandle;
    private final MethodHandle collisionShapeHandle;
    private final MethodHandle stateBlockHandle;
    private final MethodHandle entityGroundHandle;
    private final MethodHandle blockFrictionHandle;
    private final MethodHandle itemUseDurationHandle;
    private final MethodHandle itemStackItemHandle;
    private final MethodHandle voxelBoxesHandle;
    private final MethodHandle livingSpeedHandle;
    private final MethodHandle entitySupportPositionHandle;
    private final MethodHandle entitySupportCheckHandle;

    public BukkitNmsBridge() {
        Class<?> livingEntityClass = loadNmsClass(
                "world.entity.LivingEntity");
        this.stopUsingItemHandle = findMethodHandle(livingEntityClass,
                "Cannot find method 'stopUsingItem' in LivingEntity",
                BukkitNmsBridge::configureStopUsingItem);

        try {
            Class<?> blockPositionClass = loadNmsClass(
                    "core.BlockPos");
            Constructor<?> constructor = blockPositionClass.getDeclaredConstructor(
                    Integer.TYPE, Integer.TYPE, Integer.TYPE);
            this.blockPositionConstructor = ReflectionLookup.constructorHandle(constructor);
        } catch (Throwable failure) {
            throw propagate(failure);
        }

        Class<?> worldClass = loadNmsClass("world.level.World");
        this.worldBlockStateHandle = findMethodHandle(worldClass,
                "Cannot find method 'getBlockState' in Level(World)",
                BukkitNmsBridge::configureWorldBlockState);

        Class<?> stateClass = loadNmsClass(
                "world.level.block.state.BlockBehaviour$BlockStateBase");
        this.interactionShapeHandle = findMethodHandle(stateClass,
                "Cannot find method 'getInteractionShape' in BlockStateBase",
                BukkitNmsBridge::configureInteractionShape);

        stateClass = loadNmsClass(
                "world.level.block.state.BlockBehaviour$BlockStateBase");
        this.collisionShapeHandle = findMethodHandle(stateClass,
                "Cannot find method 'getCollisionShape' in BlockStateBase",
                BukkitNmsBridge::configureCollisionShape);

        stateClass = loadNmsClass(
                "world.level.block.state.BlockBehaviour$BlockStateBase");
        this.stateBlockHandle = findMethodHandle(stateClass,
                "Cannot find method 'getBlock' in BlockStateBase",
                BukkitNmsBridge::configureStateBlock);

        Class<?> entityClass = loadNmsClass("world.entity.Entity");
        this.entityGroundHandle = findMethodHandle(entityClass,
                "Cannot find method 'setOnGround' in Entity",
                BukkitNmsBridge::configureSetOnGround);

        Class<?> blockClass = loadNmsClass("world.level.block.Block");
        this.blockFrictionHandle = findMethodHandle(blockClass,
                "Cannot find method 'getFriction' in Block",
                BukkitNmsBridge::configureBlockFriction);

        Class<?> itemClass = loadNmsClass("world.item.Item");
        this.itemUseDurationHandle = findMethodHandle(itemClass,
                "Cannot find method 'getUseDuration' in Item",
                BukkitNmsBridge::configureItemUseDuration);

        Class<?> itemStackClass = loadNmsClass("world.item.ItemStack");
        this.itemStackItemHandle = findMethodHandle(itemStackClass,
                "Cannot find method 'getItem' in ItemStack",
                BukkitNmsBridge::configureItemStackItem);

        Class<?> voxelShapeClass = loadNmsClass(
                "world.phys.shapes.VoxelShape");
        this.voxelBoxesHandle = findMethodHandle(voxelShapeClass,
                "Cannot find method 'toAabbs' in VoxelShape",
                BukkitNmsBridge::configureVoxelBoxes);

        livingEntityClass = loadNmsClass(
                "world.entity.LivingEntity");
        this.livingSpeedHandle = findMethodHandle(livingEntityClass,
                "Cannot find method 'getSpeed' in LivingEntity",
                BukkitNmsBridge::configureLivingSpeed);

        entityClass = loadNmsClass("world.entity.Entity");
        this.entitySupportPositionHandle = findMethodHandle(entityClass,
                "Cannot find method 'aI' in Entity",
                BukkitNmsBridge::configureEntitySupportPosition);

        entityClass = loadNmsClass("world.entity.Entity");
        this.entitySupportCheckHandle = findMethodHandle(entityClass,
                "Cannot find method 'checkSupportingBlock' in Entity",
                BukkitNmsBridge::configureEntitySupportCheck);
    }

    private static MethodHandle findMethodHandle(Class<?> owner, String failureMessage,
                                                  Function1<MethodMatcherGroup, Unit> configure) {
        Set<?> candidates = MethodProbeHarness.selectMethods(owner, (Function1) configure);
        Method method = (Method) CollectionsKt.firstOrNull((Iterable<?>) candidates);
        if (method == null) throw new IllegalStateException(failureMessage);
        MethodHandle handle;
        try {
            handle = ReflectionLookup.methodHandle(method);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
        if (handle == null) throw new IllegalStateException(failureMessage);
        return handle;
    }

    private static Unit configureStopUsingItem(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isStopUsingItemName);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureWorldBlockState(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isGetBlockStateName);
        query.addPredicate((Function1<ClassPredicateSet, Unit>) BukkitNmsBridge::configureBlockStateReturnType);
        return Unit.INSTANCE;
    }

    private static Unit configureInteractionShape(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isInteractionShapeName);
        query.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit configureCollisionShape(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isCollisionShapeName);
        query.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit configureStateBlock(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isStateBlockName);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureSetOnGround(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isSetOnGroundName);
        query.addParameterCountConstraint(1);
        return Unit.INSTANCE;
    }

    private static Unit configureBlockFriction(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isBlockFrictionName);
        query.addParameterCountConstraint(0);
        query.addPredicate((Function1<ClassPredicateSet, Unit>) BukkitNmsBridge::configureFloatParameterType);
        return Unit.INSTANCE;
    }

    private static Unit configureItemUseDuration(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isItemUseDurationName);
        query.addParameterCountConstraint(1);
        return Unit.INSTANCE;
    }

    private static Unit configureItemStackItem(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isItemName);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureVoxelBoxes(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isToAabbsName);
        query.addModifierConstraint(1);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureLivingSpeed(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isLivingSpeedName);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureEntitySupportPosition(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isSupportPositionName);
        query.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit configureEntitySupportCheck(MethodMatcherGroup query) {
        query.addMethodNamePredicate((Function1<String, Boolean>) BukkitNmsBridge::isSupportCheckName);
        query.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit configureBlockStateReturnType(ClassPredicateSet returnTypeQuery) {
        returnTypeQuery.addPredicate((Function1<String, Boolean>) BukkitNmsBridge::isBlockStateName);
        return Unit.INSTANCE;
    }

    private static Unit configureFloatParameterType(ClassPredicateSet parameterTypeQuery) {
        parameterTypeQuery.addClassPredicate(new Class<?>[]{Float.TYPE});
        return Unit.INSTANCE;
    }

    private static boolean isBlockStateName(String className) { return "BlockState".equals(className); }
    private static boolean isToAabbsName(String methodName) { return "toAabbs".equals(methodName); }
    private static boolean isItemName(String methodName) { return "getItem".equals(methodName); }
    private static boolean isBlockFrictionName(String methodName) { return "getFriction".equals(methodName); }
    private static boolean isCollisionShapeName(String methodName) { return "getCollisionShape".equals(methodName); }
    private static boolean isStateBlockName(String methodName) { return "getBlock".equals(methodName); }
    private static boolean isItemUseDurationName(String methodName) { return "getUseDuration".equals(methodName); }
    private static boolean isSupportPositionName(String methodName) { return "getBlockPosBelowThatAffectsMyMovement".equals(methodName); }
    private static boolean isSupportCheckName(String methodName) { return "checkSupportingBlock".equals(methodName); }
    private static boolean isSetOnGroundName(String methodName) { return "setOnGround".equals(methodName); }
    private static boolean isStopUsingItemName(String methodName) { return "stopUsingItem".equals(methodName); }
    private static boolean isLivingSpeedName(String methodName) { return "getSpeed".equals(methodName); }
    private static boolean isGetBlockStateName(String methodName) { return "getBlockState".equals(methodName); }
    private static boolean isInteractionShapeName(String methodName) { return "getInteractionShape".equals(methodName); }

    @Override
    public void setOnGround(Entity entity, boolean using) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        try {
            entityGroundHandle.invoke(nmsEntity, using);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    /** Retains the recovered legacy descriptor for callers outside WorldAccess. */
    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.block.CraftBlock");
        net.minecraft.world.level.block.state.BlockState state = ((CraftBlock) block).getNMS();
        try {
            return stateBlockHandle.invoke(state);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    @Override
    public float getBlockFriction(Block block) {
        Object blockState = getBlockState(block);
        try {
            return (float) blockFrictionHandle.invoke(blockState);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }


    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    @Override
    public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(start.toLocation(world), direction, distance);
        if (hit == null) return null;
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(), hit.getHitEntity(), hit.getHitBlockFace());
    }


    @Override
    public float getMovementSpeed(LivingEntity entity) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        try {
            return (float) livingSpeedHandle.invoke(nmsEntity);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    @Override
    public int getItemUseDuration(ItemStack stack, LivingEntity ignoredUser) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        try {
            Object nmsItem = itemStackItemHandle.invoke(nmsStack);
            return (int) itemUseDurationHandle.invoke(nmsItem, nmsStack);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeMode) {
        World world = block.getWorld();
        Intrinsics.checkNotNull(world, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
        net.minecraft.server.level.ServerLevel nmsWorld = ((CraftWorld) world).getHandle();
        Object blockPosition;
        Object blockState;
        try {
            blockPosition = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            blockState = worldBlockStateHandle.invoke(nmsWorld, blockPosition);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
        net.minecraft.world.phys.shapes.VoxelShape shape;
        try {
            if (shapeMode == BlockShapeMode.INTERACT) {
                shape = (net.minecraft.world.phys.shapes.VoxelShape)
                        interactionShapeHandle.invoke(blockState, nmsWorld, blockPosition);
            } else {
                shape = (net.minecraft.world.phys.shapes.VoxelShape)
                        collisionShapeHandle.invoke(blockState, nmsWorld, blockPosition);
            }
            List<?> localBoxes = (List<?>) voxelBoxesHandle.invoke(shape);
            AxisAlignedBox[] collisionBoxes = new AxisAlignedBox[localBoxes.size()];
            int i = 0;
            for (Object value : localBoxes) {
                net.minecraft.world.phys.AABB localBox =
                        (net.minecraft.world.phys.AABB) value;
                collisionBoxes[i++] = new AxisAlignedBox(
                        block.getX() + localBox.minX, block.getY() + localBox.minY, block.getZ() + localBox.minZ,
                        block.getX() + localBox.maxX, block.getY() + localBox.maxY, block.getZ() + localBox.maxZ);
            }
            return collisionBoxes;
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    @Override
    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }


    @Override
    public float updateSupportingBlock(Entity entity, boolean onGround, Vector movement) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity nmsEntity = craftEntity.getHandle();
        World bukkitWorld = craftEntity.getWorld();
        Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
        net.minecraft.server.level.ServerLevel nmsWorld = ((CraftWorld) bukkitWorld).getHandle();
        try {
            entitySupportCheckHandle.invoke(nmsEntity, onGround,
                    new net.minecraft.world.phys.Vec3(movement.getX(), movement.getY(), movement.getZ()));
            Object supportPosition = entitySupportPositionHandle.invoke(nmsEntity);
            if (supportPosition == null) return 0.6f;
            Object supportBlockState = worldBlockStateHandle.invoke(nmsWorld, supportPosition);
            Object supportBlock = stateBlockHandle.invoke(supportBlockState);
            return (float) blockFrictionHandle.invoke(supportBlock);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    @Override
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    @Override
    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftPlayer");
        net.minecraft.server.level.ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        try {
            stopUsingItemHandle.invoke(serverPlayer);
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }

    private static RuntimeException propagate(Throwable failure) {
        BukkitNmsBridge.<RuntimeException>throwUnchecked(failure);
        return null;
    }

    private static Class<?> loadNmsClass(String className) {
        try {
            String versionedName;
            if (Matrix.getServerVersion().compareTo(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17) < 0) {
                versionedName = "net.minecraft.server."
                        + me.rerere.matrix.server.ServerNames.packageComponent() + "." + className;
            } else {
                versionedName = "net.minecraft." + className;
            }
            return Class.forName(me.rerere.matrix.util.reflect.ReflectionNameResolver
                    .mapDescriptorName(versionedName));
        } catch (Throwable failure) {
            throw propagate(failure);
        }
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }
}
