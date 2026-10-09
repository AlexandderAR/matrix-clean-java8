package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.MethodMatcherGroup;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.reflect.MethodProbeHarness;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Runtime adapters for the server methods used by collision and movement checks. */
public final class NmsMethodAccess implements WorldAccess {
    private final MethodHandle setOnGroundHandle;
    private final MethodHandle stopUsingItemHandle;
    private final MethodHandle getBlockStateHandle;
    private final MethodHandle getInteractionShapeHandle;
    private final MethodHandle getCollisionShapeHandle;
    private final MethodHandle getBlockHandle;
    private final MethodHandle blockFrictionHandle;
    private final MethodHandle itemUseDurationHandle;
    private final MethodHandle itemStackItemHandle;
    private final MethodHandle voxelShapeBoxesHandle;
    private final MethodHandle livingEntitySpeedHandle;
    private final MethodHandle entityBlockPosHandle;
    private final MethodHandle checkSupportingBlockHandle;
    private final MethodHandle blockPositionConstructor;

    public NmsMethodAccess() {
        this.stopUsingItemHandle = resolveMethodHandle(
                "world.entity.LivingEntity", NmsMethodAccess::bindStopUsingItem,
                "Cannot find method 'stopUsingItem' in LivingEntity");
        this.blockPositionConstructor = resolveConstructor("core.BlockPos");
        this.getBlockStateHandle = resolveMethodHandle(
                "world.level.World", NmsMethodAccess::bindBlockState,
                "Cannot find method 'getBlockState' in Level(World)");
        this.getInteractionShapeHandle = resolveMethodHandle(
                "world.level.block.state.BlockBehaviour$BlockStateBase", NmsMethodAccess::bindInteractionShape,
                "Cannot find method 'getInteractionShape' in BlockStateBase");
        this.getCollisionShapeHandle = resolveMethodHandle(
                "world.level.block.state.BlockBehaviour$BlockStateBase", NmsMethodAccess::bindCollisionShape,
                "Cannot find method 'getCollisionShape' in BlockStateBase");
        this.getBlockHandle = resolveMethodHandle(
                "world.level.block.state.BlockBehaviour$BlockStateBase", NmsMethodAccess::bindBlock,
                "Cannot find method 'getBlock' in BlockStateBase");
        this.setOnGroundHandle = resolveMethodHandle(
                "world.entity.Entity", NmsMethodAccess::bindGroundSetter,
                "Cannot find method 'setOnGround' in Entity");
        this.blockFrictionHandle = resolveMethodHandle(
                "world.level.block.Block", NmsMethodAccess::bindBlockFriction,
                "Cannot find method 'getFriction' in Block");
        this.itemUseDurationHandle = resolveMethodHandle(
                "world.item.Item", NmsMethodAccess::bindUseDuration,
                "Cannot find method 'getUseDuration' in Item");
        this.itemStackItemHandle = resolveMethodHandle(
                "world.item.ItemStack", NmsMethodAccess::bindItemSelector,
                "Cannot find method 'getItem' in ItemStack");
        this.voxelShapeBoxesHandle = resolveMethodHandle(
                "world.phys.shapes.VoxelShape", NmsMethodAccess::bindToAabbs,
                "Cannot find method 'toAabbs' in VoxelShape");
        this.livingEntitySpeedHandle = resolveMethodHandle(
                "world.entity.LivingEntity", NmsMethodAccess::bindMovementSpeed,
                "Cannot find method 'getSpeed' in LivingEntity");
        this.entityBlockPosHandle = resolveMethodHandle(
                "world.entity.Entity", NmsMethodAccess::bindBlockPosBelow,
                "Cannot find method 'aI' in Entity");
        this.checkSupportingBlockHandle = resolveMethodHandle(
                "world.entity.Entity", NmsMethodAccess::bindSupportCheck,
                "Cannot find method 'checkSupportingBlock' in Entity");
    }

    private static MethodHandle resolveMethodHandle(String ownerName,
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<MethodMatcherGroup, Unit> matcherInitializer,
            String failure) {
        try {
            Class<?> ownerClass = loadNmsClass(ownerName);
            Set<Method> matchingMethods = MethodProbeHarness.selectMethods(ownerClass, matcherInitializer);
            Method selectedMethod = (Method) me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt
                    .firstOrNull(matchingMethods);
            if (selectedMethod != null) {
                MethodHandle methodHandle = ReflectionLookup.methodHandle(selectedMethod);
                if (methodHandle != null) return methodHandle;
            }
        } catch (ReflectiveOperationException exception) {
            throw NmsMethodAccess.<RuntimeException>propagate(exception);
        }
        throw new IllegalStateException(failure);
    }

    private static MethodHandle resolveConstructor(String ownerName) {
        try {
            Class<?> ownerClass = loadNmsClass(ownerName);
            Constructor<?> blockPositionConstructor = ownerClass.getDeclaredConstructor(
                    int.class, int.class, int.class);
            return ReflectionLookup.constructorHandle(blockPositionConstructor);
        } catch (ReflectiveOperationException exception) {
            throw NmsMethodAccess.<RuntimeException>propagate(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException propagate(Throwable cause) throws T {
        throw (T) cause;
    }

    private static Class<?> loadNmsClass(String ownerName) throws ClassNotFoundException {
        return ReflectionLookup.loadVersionedNmsClass(ownerName);
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox bounds = block.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        net.minecraft.world.entity.Entity nativeEntity = craftHandle(entity);
        try {
            return (float) livingEntitySpeedHandle.invoke(nativeEntity);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    public int getItemUseDuration(ItemStack item, LivingEntity entity) {
        net.minecraft.world.item.ItemStack nativeItemStack = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(item);
        net.minecraft.world.entity.Entity nativeEntity = craftHandle(entity);
        try {
            Object nativeItem = (Object) itemStackItemHandle.invoke(nativeItemStack, nativeEntity);
            return (int) itemUseDurationHandle.invoke(nativeItem, nativeItemStack);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    @Override
    public float getBlockFriction(Block block) {
        Object nativeBlock = getBlockState(block);
        try {
            return (float) blockFrictionHandle.invoke(nativeBlock);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    @Override
    public boolean isOnGround(Entity entity) {
        return entity.isOnGround();
    }

    @Override
    public RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(start.toLocation(world), direction, distance);
        if (hit == null) {
            return null;
        }
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
                hit.getHitEntity(), hit.getHitBlockFace());
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        try {
            net.minecraft.server.level.ServerLevel nativeWorld = craftWorldHandle(block.getWorld());
            Object blockPosition = (Object) blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            Object blockState = (Object) getBlockStateHandle.invoke(nativeWorld, blockPosition);
            net.minecraft.world.phys.shapes.VoxelShape voxelShape;
            if (shapeKind == BlockShapeMode.INTERACT) {
                voxelShape = (net.minecraft.world.phys.shapes.VoxelShape) getInteractionShapeHandle.invoke(blockState, nativeWorld, blockPosition);
            } else {
                voxelShape = (net.minecraft.world.phys.shapes.VoxelShape) getCollisionShapeHandle.invoke(blockState, nativeWorld, blockPosition);
            }
            @SuppressWarnings("unchecked")
            List<net.minecraft.world.phys.AABB> localBoxes = (List<net.minecraft.world.phys.AABB>) voxelShapeBoxesHandle.invoke(voxelShape);
            AxisAlignedBox[] collisionBoxes = new AxisAlignedBox[localBoxes.size()];
            int index = 0;
            for (net.minecraft.world.phys.AABB localBox : localBoxes) {
                collisionBoxes[index++] = new AxisAlignedBox(
                        block.getX() + localBox.minX,
                        block.getY() + localBox.minY,
                        block.getZ() + localBox.minZ,
                        block.getX() + localBox.maxX,
                        block.getY() + localBox.maxY,
                        block.getZ() + localBox.maxZ);
            }
            return collisionBoxes;
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }



    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        try {
            setOnGroundHandle.invoke(craftHandle(entity), onGround);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }



    @Override
    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftPlayer");
        net.minecraft.server.level.ServerPlayer nativePlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
        try {
            stopUsingItemHandle.invoke(nativePlayer);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    @Override
    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    @Override
    public float updateSupportingBlock(Entity entity, boolean onGround, Vector movement) {
        net.minecraft.world.entity.Entity nmsEntity = craftHandle(entity);
        net.minecraft.server.level.ServerLevel nmsWorld = craftWorldHandle(((org.bukkit.craftbukkit.entity.CraftEntity) entity).getWorld());
        try {
            checkSupportingBlockHandle.invoke(nmsEntity, onGround,
                    new net.minecraft.world.phys.Vec3(movement.getX(), movement.getY(), movement.getZ()));
            Object supportPosition = (Object) entityBlockPosHandle.invoke(nmsEntity);
            if (supportPosition == null) return 0.6F;
            Object supportState = (Object) getBlockStateHandle.invoke(nmsWorld, supportPosition);
            Object supportBlock = (Object) getBlockHandle.invoke(supportState);
            return (float) blockFrictionHandle.invoke(supportBlock);
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        org.bukkit.util.BoundingBox bounds = entity.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @Override
    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.block.CraftBlock");
        org.bukkit.craftbukkit.block.CraftBlock craftBlock = (org.bukkit.craftbukkit.block.CraftBlock) block;
        try {
            return (Object) getBlockHandle.invoke(craftBlock.getNMS());
        } catch (Throwable failure) {
            throw NmsMethodAccess.<RuntimeException>propagate(failure);
        }
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }



    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    private static Unit bindItemSelector(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesItem((String) name));
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit bindInteractionShape(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesInteractionShape((String) name));
        target.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit bindSupportCheck(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesSupportingBlock((String) name));
        target.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit bindBlock(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesBlock((String) name));
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit bindBlockPosBelow(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesBlockPosBelow((String) name));
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit bindMovementSpeed(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesSpeed((String) name));
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit bindUseDuration(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesUseDuration((String) name));
        target.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit bindBlockState(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesBlockState((String) name));
        target.addPredicate(candidate -> bindBlockStateLookup((ClassPredicateSet) candidate));
        return Unit.INSTANCE;
    }

    private static Unit bindBlockStateLookup(ClassPredicateSet target) {
        target.addPredicate(name -> matchesBlockStateAlias((String) name));
        return Unit.INSTANCE;
    }

    private static Unit bindCollisionShape(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesCollisionShape((String) name));
        target.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static Unit bindStopUsingItem(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesStopUsingItem((String) name));
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static Unit bindGroundSetter(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesOnGround((String) name));
        target.addParameterCountConstraint(1);
        return Unit.INSTANCE;
    }

    private static Unit bindBlockFriction(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesFriction((String) name));
        target.addParameterCountConstraint(0);
        target.addPredicate(candidate -> {
            ((ClassPredicateSet) candidate).addClassPredicate(new Class<?>[] { float.class });
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    }

    private static Unit bindToAabbs(MethodMatcherGroup target) {
        target.addMethodNamePredicate(name -> matchesToAabbs((String) name));
        target.addModifierConstraint(1);
        target.addParameterCountConstraint(0);
        return Unit.INSTANCE;
    }

    private static boolean matchesToAabbs(String methodName) { return Objects.equals(methodName, "toAabbs"); }
    private static boolean matchesBlockState(String methodName) { return Objects.equals(methodName, "getBlockState"); }
    private static boolean matchesBlockStateAlias(String methodName) { return Objects.equals(methodName, "BlockState"); }
    private static boolean matchesCollisionShape(String methodName) { return Objects.equals(methodName, "getCollisionShape"); }
    private static boolean matchesStopUsingItem(String methodName) { return Objects.equals(methodName, "stopUsingItem"); }
    private static boolean matchesFriction(String methodName) { return Objects.equals(methodName, "getFriction"); }
    private static boolean matchesSpeed(String methodName) { return Objects.equals(methodName, "getSpeed"); }
    private static boolean matchesUseDuration(String methodName) { return Objects.equals(methodName, "getUseDuration"); }
    private static boolean matchesBlock(String methodName) { return Objects.equals(methodName, "getBlock"); }
    private static boolean matchesItem(String methodName) { return Objects.equals(methodName, "getItem"); }
    private static boolean matchesOnGround(String methodName) { return Objects.equals(methodName, "setOnGround"); }
    private static boolean matchesBlockPosBelow(String methodName) {
        return Objects.equals(methodName, "getBlockPosBelowThatAffectsMyMovement");
    }
    private static boolean matchesInteractionShape(String methodName) {
        return Objects.equals(methodName, "getInteractionShape");
    }
    private static boolean matchesSupportingBlock(String methodName) {
        return Objects.equals(methodName, "checkSupportingBlock");
    }

    private static net.minecraft.world.entity.Entity craftHandle(Entity entity) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
        return ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
    }

    private static net.minecraft.server.level.ServerLevel craftWorldHandle(World world) {
        Intrinsics.checkNotNull(world, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
        return ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
    }
}
