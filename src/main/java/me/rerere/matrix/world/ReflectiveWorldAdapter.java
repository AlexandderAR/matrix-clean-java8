package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.reflect.MethodMatcherGroup;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.MethodProbeHarness;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.world.WorldAccess;

/** Handwritten from canonical ASM. Reflection is functional server-version compatibility. */
public final class ReflectiveWorldAdapter implements WorldAccess {
    private static int reflectionModeValue = 121;

    public static void setReflectionModeValue(int modeValue) { reflectionModeValue = modeValue; }
    public static int getReflectionModeValue() { return reflectionModeValue; }
    public static int reflectionModeCode() { return reflectionModeValue == 0 ? 33 : 0; }

    private final MethodHandle blockFromState;
    private final MethodHandle craftBlockState;
    private final MethodHandle itemFromStack;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle movementSupportPosition;
    private final MethodHandle checkSupportingBlock;
    private final MethodHandle movementSpeed;
    private final MethodHandle shapeBoxes;
    private final MethodHandle itemUseDuration;
    private final MethodHandle blockFriction;
    private final MethodHandle collisionShape;
    private final MethodHandle interactionShape;
    private final MethodHandle stopUsingItem;
    private final MethodHandle worldBlockState;
    private final MethodHandle setOnGround;

    public ReflectiveWorldAdapter() {
        try {
            stopUsingItem = find(nmsClass("world.entity.LivingEntity"), named("stopUsingItem", 0),
                    "Cannot find method 'stopUsingItem' in LivingEntity");
            blockPositionConstructor = ReflectionLookup.constructorHandle(
                    nmsClass("core.BlockPos").getDeclaredConstructor(int.class, int.class, int.class));
            worldBlockState = find(nmsClass("world.level.Level"), query -> {
                query.addMethodNamePredicate(name -> Intrinsics.areEqual(name, "getBlockState"));
                query.addPredicate(candidate -> {
                    ClassPredicateSet type = (ClassPredicateSet) candidate;
                    type.addPredicate(name -> Intrinsics.areEqual(name, "BlockState"));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            }, "Cannot find method 'getBlockState' in Level(World)");
            interactionShape = find(nmsClass("world.level.block.state.BlockBehaviour$BlockStateBase"),
                    named("getInteractionShape", 2), "Cannot find method 'getInteractionShape' in BlockStateBase");
            collisionShape = find(nmsClass("world.level.block.state.BlockBehaviour$BlockStateBase"),
                    named("getCollisionShape", 2), "Cannot find method 'getCollisionShape' in BlockStateBase");
            blockFromState = find(nmsClass("world.level.block.state.BlockBehaviour$BlockStateBase"),
                    named("getBlock", 0), "Cannot find method 'getBlock' in BlockStateBase");
            setOnGround = find(nmsClass("world.entity.Entity"), named("setOnGround", 1),
                    "Cannot find method 'setOnGround' in Entity");
            blockFriction = find(nmsClass("world.level.block.Block"), query -> {
                named("getFriction", 0).invoke(query);
                query.addPredicate(candidate -> {
                    ClassPredicateSet type = (ClassPredicateSet) candidate;
                    type.addClassPredicate(new Class<?>[] { float.class });
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            }, "Cannot find method 'getFriction' in Block");
            itemUseDuration = find(nmsClass("world.item.Item"), named("getUseDuration", 2),
                    "Cannot find method 'getUseDuration' in Item");
            itemFromStack = find(nmsClass("world.item.ItemStack"), named("getItem", 0),
                    "Cannot find method 'getItem' in ItemStack");
            shapeBoxes = find(nmsClass("world.phys.shapes.VoxelShape"), query -> {
                query.addMethodNamePredicate(name -> Intrinsics.areEqual(name, "toAabbs"));
                query.addModifierConstraint(1);
                query.addParameterCountConstraint(0);
                return Unit.INSTANCE;
            }, "Cannot find method 'toAabbs' in VoxelShape");
            movementSpeed = find(nmsClass("world.entity.LivingEntity"), named("getSpeed", 0),
                    "Cannot find method 'getSpeed' in LivingEntity");
            movementSupportPosition = find(nmsClass("world.entity.Entity"),
                    named("getBlockPosBelowThatAffectsMyMovement", 0),
                    "Cannot find method 'aI' in Entity");
            craftBlockState = find(CraftBlock.class, query -> {
                named("getBlockState", 0).invoke(query);
                query.addPredicate(candidate -> {
                    ClassPredicateSet type = (ClassPredicateSet) candidate;
                    type.addPredicate(name -> Intrinsics.areEqual(name, "BlockState"));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            }, "Cannot find method 'getBlockState' in CraftBlock");
            checkSupportingBlock = find(nmsClass("world.entity.Entity"), named("checkSupportingBlock", 2),
                    "Cannot find method 'checkSupportingBlock' in Entity");
        } catch (Throwable initializationFailure) {
            throw propagate(initializationFailure);
        }
    }

    private static Class<?> nmsClass(String className) throws ClassNotFoundException {
        return ReflectionLookup.loadVersionedNmsClass(className);
    }

    private static Function1<MethodMatcherGroup, Unit> named(String methodName, int argumentCount) {
        return query -> {
            query.addMethodNamePredicate(candidate -> Intrinsics.areEqual(candidate, methodName));
            query.addParameterCountConstraint(argumentCount);
            return Unit.INSTANCE;
        };
    }

    private static MethodHandle find(Class<?> owner, Function1<MethodMatcherGroup, Unit> query,
                                     String failureMessage) throws IllegalAccessException {
        Set<Method> methods = MethodProbeHarness.selectMethods(owner, query);
        Method selectedMethod = (Method) CollectionsKt.firstOrNull(methods);
        MethodHandle handle = selectedMethod == null ? null : ReflectionLookup.methodHandle(selectedMethod);
        if (handle == null) throw new IllegalStateException(failureMessage);
        return handle;
    }



    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }



    public void setOnGround(Entity entity, boolean grounded) {
        try { setOnGround.invoke(entityHandle(entity), grounded); }
        catch (Throwable failure) { throw propagate(failure); }
    }



    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.block.CraftBlock");
        CraftBlock craftBlock = (CraftBlock) block;
        try {
            Object nativeBlockState = craftBlockState.invoke(craftBlock);
            return (Object) blockFromState.invoke(nativeBlockState);
        } catch (Throwable failure) { throw propagate(failure); }
    }



    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftPlayer");
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        try { stopUsingItem.invoke(handle); }
        catch (Throwable failure) { throw propagate(failure); }
    }

    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    public AxisAlignedBox getBlockBounds(Block block) { return bounds(block.getBoundingBox()); }



    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeType) {
        World world = block.getWorld();
        Intrinsics.checkNotNull(world, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
        ServerLevel level = ((CraftWorld) world).getHandle();
        try {
            Object blockPosition = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            Object nativeBlockState = worldBlockState.invoke(level, blockPosition);
            VoxelShape shape = shapeType == BlockShapeMode.INTERACT
                    ? (VoxelShape) interactionShape.invoke(nativeBlockState, level, blockPosition)
                    : (VoxelShape) collisionShape.invoke(nativeBlockState, level, blockPosition);
            List<?> boxes = (List<?>) shapeBoxes.invoke(shape);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (Object boxValue : boxes) {
                AABB box = (AABB) boxValue;
                result[index++] = new AxisAlignedBox(block.getX() + box.minX, block.getY() + box.minY,
                        block.getZ() + box.minZ, block.getX() + box.maxX, block.getY() + box.maxY,
                        block.getZ() + box.maxZ);
            }
            return result;
        } catch (Throwable failure) { throw propagate(failure); }
    }

    public RaytraceResult rayTraceBlocks(World world, Vector origin, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(origin.toLocation(world), direction, distance);
        return hit == null ? null : new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
                hit.getHitEntity(), hit.getHitBlockFace());
    }

    public float updateSupportingBlock(Entity entity, boolean grounded, Vector movement) {
        net.minecraft.world.entity.Entity handle = entityHandle(entity);
        World world = ((CraftEntity) entity).getWorld();
        Intrinsics.checkNotNull(world, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
        ServerLevel level = ((CraftWorld) world).getHandle();
        try {
            checkSupportingBlock.invoke(handle, grounded, new Vec3(movement.getX(), movement.getY(), movement.getZ()));
            Object supportPosition = movementSupportPosition.invoke(handle);
            if (supportPosition == null) return 0.6f;
            Object nativeBlockState = worldBlockState.invoke(level, supportPosition);
            Object nativeBlock = blockFromState.invoke(nativeBlockState);
            return (float) blockFriction.invoke(nativeBlock);
        } catch (Throwable failure) { throw propagate(failure); }
    }



    public int getItemUseDuration(ItemStack stack, LivingEntity entity) {
        net.minecraft.world.item.ItemStack nativeStack = CraftItemStack.asNMSCopy(stack);
        net.minecraft.world.entity.Entity handle = entityHandle(entity);
        try {
            Object nativeItem = itemFromStack.invoke(nativeStack, handle);
            return (int) itemUseDuration.invoke(nativeItem, nativeStack);
        } catch (Throwable failure) { throw propagate(failure); }
    }

    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }

    public float getBlockFriction(Block block) {
        Object nativeBlock = getBlockState(block);
        try { return (float) blockFriction.invoke(nativeBlock); }
        catch (Throwable failure) { throw propagate(failure); }
    }



    public AxisAlignedBox getEntityBounds(Entity entity) { return bounds(entity.getBoundingBox()); }

    public float getMovementSpeed(LivingEntity entity) {
        net.minecraft.world.entity.Entity handle = entityHandle(entity);
        try { return (float) movementSpeed.invoke(handle); }
        catch (Throwable failure) { throw propagate(failure); }
    }

    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    private static net.minecraft.world.entity.Entity entityHandle(Entity entity) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
        return ((CraftEntity) entity).getHandle();
    }

    private static AxisAlignedBox bounds(BoundingBox bounds) {
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException propagate(Throwable failure) throws T {
        throw (T) failure;
    }
}
