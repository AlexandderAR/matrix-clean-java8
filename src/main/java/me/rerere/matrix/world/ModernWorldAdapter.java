package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.MethodMatcherGroup;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.reflect.MethodProbeHarness;

/** Runtime method lookup for servers that expose the modern unversioned NMS packages. */
public final class ModernWorldAdapter implements WorldAccess {
    private final MethodHandle stopUsingItem;
    private final MethodHandle createBlockPosition;
    private final MethodHandle getBlockState;
    private final MethodHandle getInteractionShape;
    private final MethodHandle getCollisionShape;
    private final MethodHandle getBlock;
    private final MethodHandle setOnGround;
    private final MethodHandle getFriction;
    private final MethodHandle getUseDuration;
    private final MethodHandle getItem;
    private final MethodHandle toBoundingBoxes;
    private final MethodHandle getSpeed;
    private final MethodHandle getSupportingBlockPosition;
    private final MethodHandle checkSupportingBlock;

    public ModernWorldAdapter() throws Throwable {
        stopUsingItem = requireMethod("world.entity.LivingEntity", named("stopUsingItem", 0),
                "Cannot find method 'stopUsingItem' in LivingEntity");
        createBlockPosition = ReflectionLookup.constructorHandle(
                ReflectionLookup.loadVersionedNmsClass("core.BlockPos")
                        .getDeclaredConstructor(int.class, int.class, int.class));
        getBlockState = requireMethod("world.level.Level", builder -> {
            builder.addMethodNamePredicate(name -> Intrinsics.areEqual(name, "getBlockState"));
            builder.addPredicate((Function1<ClassPredicateSet, Unit>) parameters -> {
                parameters.addPredicate(name -> Intrinsics.areEqual(name, "BlockState"));
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        }, "Cannot find method 'getBlockState' in Level(World)");
        getInteractionShape = requireMethod("world.level.block.state.BlockBehaviour$BlockStateBase",
                named("getInteractionShape", 2), "Cannot find method 'getInteractionShape' in BlockStateBase");
        getCollisionShape = requireMethod("world.level.block.state.BlockBehaviour$BlockStateBase",
                named("getCollisionShape", 2), "Cannot find method 'getCollisionShape' in BlockStateBase");
        getBlock = requireMethod("world.level.block.state.BlockBehaviour$BlockStateBase",
                named("getBlock", 0), "Cannot find method 'getBlock' in BlockStateBase");
        setOnGround = requireMethod("world.entity.Entity", named("setOnGround", 1),
                "Cannot find method 'setOnGround' in Entity");
        getFriction = requireMethod("world.level.block.Block", builder -> {
            builder.addMethodNamePredicate(name -> Intrinsics.areEqual(name, "getFriction"));
            builder.addParameterCountConstraint(0);
            builder.addPredicate((Function1<ClassPredicateSet, Unit>) parameters -> {
                parameters.addClassPredicate(new Class<?>[] { float.class });
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        }, "Cannot find method 'getFriction' in Block");
        getUseDuration = requireMethod("world.item.Item", named("getUseDuration", 2),
                "Cannot find method 'getUseDuration' in Item");
        getItem = requireMethod("world.item.ItemStack", named("getItem", 0),
                "Cannot find method 'getItem' in ItemStack");
        toBoundingBoxes = requireMethod("world.phys.shapes.VoxelShape", builder -> {
            builder.addMethodNamePredicate(name -> Intrinsics.areEqual(name, "toAabbs"));
            builder.addModifierConstraint(1);
            builder.addParameterCountConstraint(0);
            return Unit.INSTANCE;
        }, "Cannot find method 'toAabbs' in VoxelShape");
        getSpeed = requireMethod("world.entity.LivingEntity", named("getSpeed", 0),
                "Cannot find method 'getSpeed' in LivingEntity");
        getSupportingBlockPosition = requireMethod("world.entity.Entity",
                named("getBlockPosBelowThatAffectsMyMovement", 0), "Cannot find method 'aI' in Entity");
        checkSupportingBlock = requireMethod("world.entity.Entity", named("checkSupportingBlock", 2),
                "Cannot find method 'checkSupportingBlock' in Entity");
    }

    private static Function1<MethodMatcherGroup, Unit> named(String expectedName, int parameterCount) {
        return builder -> {
            builder.addMethodNamePredicate(name -> Intrinsics.areEqual(name, expectedName));
            builder.addParameterCountConstraint(parameterCount);
            return Unit.INSTANCE;
        };
    }

    private static MethodHandle requireMethod(String nmsClassName,
            Function1<MethodMatcherGroup, Unit> configure, String failureMessage) throws Throwable {
        Class<?> ownerClass = ReflectionLookup.loadVersionedNmsClass(nmsClassName);
        Iterable<Method> matchingMethods = MethodProbeHarness.selectMethods(ownerClass, configure);
        Method method = CollectionsKt.firstOrNull(matchingMethods);
        if (method != null) {
            MethodHandle handle = ReflectionLookup.methodHandle(method);
            if (handle != null) return handle;
        }
        throw new IllegalStateException(failureMessage.toString());
    }

    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    public int getItemUseDuration(ItemStack stack, LivingEntity entity) {
        try {
            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
            Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
            net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
            Object item = getItem.invoke(nmsStack, handle);
            return (int) getUseDuration.invoke(item, nmsStack);
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, Integer>throwUnchecked(failure);
        }
    }

    public void stopUsingItem(Player player) {
        try {
            Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftPlayer");
            stopUsingItem.invoke(((CraftPlayer) player).getHandle());
        } catch (Throwable failure) {
            ModernWorldAdapter.<RuntimeException, Void>throwUnchecked(failure);
        }
    }

    public void setOnGround(Entity entity, boolean onGround) {
        try {
            Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
            setOnGround.invoke(((CraftEntity) entity).getHandle(), onGround);
        } catch (Throwable failure) {
            ModernWorldAdapter.<RuntimeException, Void>throwUnchecked(failure);
        }
    }

    public float updateSupportingBlock(Entity entity, boolean onGround, Vector movement) {
        try {
            Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
            net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
            World entityWorld = ((CraftEntity) entity).getWorld();
            Intrinsics.checkNotNull(entityWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
            ServerLevel world = ((CraftWorld) entityWorld).getHandle();
            checkSupportingBlock.invoke(handle, onGround,
                    new Vec3(movement.getX(), movement.getY(), movement.getZ()));
            Object position = getSupportingBlockPosition.invoke(handle);
            if (position == null) return 0.6f;
            Object state = getBlockState.invoke(world, position);
            Object block = getBlock.invoke(state);
            return (float) getFriction.invoke(block);
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public float getBlockFriction(Block block) {
        try {
            Object nmsBlock = getBlockState(block);
            return (float) getFriction.invoke(nmsBlock);
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public float getMovementSpeed(LivingEntity entity) {
        try {
            Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.entity.CraftEntity");
            net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
            return (float) getSpeed.invoke(handle);
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, Float>throwUnchecked(failure);
        }
    }

    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        try {
            World blockWorld = block.getWorld();
            Intrinsics.checkNotNull(blockWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.CraftWorld");
            ServerLevel world = ((CraftWorld) blockWorld).getHandle();
            Object position = createBlockPosition.invoke(block.getX(), block.getY(), block.getZ());
            Object state = getBlockState.invoke(world, position);
            VoxelShape shape;
            if (mode == BlockShapeMode.INTERACT) {
                shape = (VoxelShape) getInteractionShape.invoke(state, world, position);
            } else {
                shape = (VoxelShape) getCollisionShape.invoke(state, world, position);
            }
            List<?> boxes = (List<?>) toBoundingBoxes.invoke(shape);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (Object boxValue : boxes) {
                AABB box = (AABB) boxValue;
                result[index++] = new AxisAlignedBox(block.getX() + box.minX, block.getY() + box.minY,
                        block.getZ() + box.minZ, block.getX() + box.maxX, block.getY() + box.maxY,
                        block.getZ() + box.maxZ);
            }
            return result;
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
    }



    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }



    public Object getBlockState(Block block) {
        try {
            Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.block.CraftBlock");
            return getBlock.invoke(((CraftBlock) block).getNMS());
        } catch (Throwable failure) {
            return ModernWorldAdapter.<RuntimeException, Object>throwUnchecked(failure);
        }
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public RaytraceResult rayTraceBlocks(World world, Vector origin, Vector direction, double distance) {
        RayTraceResult result = world.rayTraceBlocks(origin.toLocation(world), direction, distance);
        if (result == null) return null;
        return new RaytraceResult(result.getHitPosition(), result.getHitBlock(),
                result.getHitEntity(), result.getHitBlockFace());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
}
