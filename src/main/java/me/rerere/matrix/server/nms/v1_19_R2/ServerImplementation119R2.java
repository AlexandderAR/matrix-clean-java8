package me.rerere.matrix.server.nms.v1_19_R2;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** CraftBukkit 1.19_R2 bridge, including the reflection and MethodHandle lookup rules. */
public class ServerImplementation119R2 implements WorldAccess {
    private final MethodHandle stopUsingItemHandle;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockState;
    private final MethodHandle voxelShapeBy;
    private final MethodHandle voxelShapeCollision;
    private final MethodHandle blockStateByName;
    private final MethodHandle entityNoGravityHandle;
    private final MethodHandle blockDestroySpeedHandle;
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable exception) throws T {
        throw (T) exception;
    }

    public ServerImplementation119R2() {
        try {
            Class<?> livingEntityClass = ReflectionLookup.loadVersionedNmsClass("world.entity.EntityLiving");
            Class<?>[] noParameters = new Class<?>[0];
            String livingFlagName = ReflectionNameResolver.mapMethodName(
                    "A", livingEntityClass, noParameters);
            Method livingFlagMethod = livingEntityClass.getDeclaredMethod(livingFlagName, noParameters);
            this.stopUsingItemHandle = ReflectionLookup.methodHandle(livingFlagMethod);

            Class<?> blockPositionClass = ReflectionLookup.loadVersionedNmsClass("core.BlockPosition");
            Constructor<?> positionConstructor = blockPositionClass.getDeclaredConstructor(
                    int.class, int.class, int.class);
            this.blockPositionConstructor = ReflectionLookup.constructorHandle(positionConstructor);

            Class<?> worldClass = ReflectionLookup.loadVersionedNmsClass("world.level.World");
            Method worldStateMethod = null;
            for (Method method : worldClass.getDeclaredMethods()) {
                if ("a_".equals(method.getName())) { worldStateMethod = method; break; }
            }
            if (worldStateMethod == null)
                throw new NoSuchElementException("Array contains no element matching the predicate.");
            this.worldBlockState = ReflectionLookup.methodHandle(worldStateMethod);

            Class<?> blockDataClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            Method shapeByMethod = null;
            for (Method method : blockDataClass.getDeclaredMethods()) {
                if ("j".equals(method.getName()) && method.getParameterCount() == 2) {
                    shapeByMethod = method;
                    break;
                }
            }
            if (shapeByMethod == null)
                throw new NoSuchElementException("Array contains no element matching the predicate.");
            this.voxelShapeBy = ReflectionLookup.methodHandle(shapeByMethod);

            blockDataClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            Method shapeCollisionMethod = null;
            for (Method method : blockDataClass.getDeclaredMethods()) {
                if ("k".equals(method.getName()) && method.getParameterCount() == 2) {
                    shapeCollisionMethod = method;
                    break;
                }
            }
            if (shapeCollisionMethod == null)
                throw new NoSuchElementException("Array contains no element matching the predicate.");
            this.voxelShapeCollision = ReflectionLookup.methodHandle(shapeCollisionMethod);

            blockDataClass = ReflectionLookup.loadVersionedNmsClass(
                    "world.level.block.state.BlockBase$BlockData");
            noParameters = new Class<?>[0];
            String blockStateName = ReflectionNameResolver.mapMethodName(
                    "b", blockDataClass, noParameters);
            Method blockStateMethod = blockDataClass.getDeclaredMethod(blockStateName, noParameters);
            this.blockStateByName = ReflectionLookup.methodHandle(blockStateMethod);

            Class<?> entityClass = ReflectionLookup.loadVersionedNmsClass("world.entity.Entity");
            Method noGravityMethod = entityClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("c", entityClass,
                            new Class<?>[] { boolean.class }),
                    boolean.class);
            this.entityNoGravityHandle = ReflectionLookup.methodHandle(noGravityMethod);

            Class<?> blockClass = ReflectionLookup.loadVersionedNmsClass("world.level.block.Block");
            Method destroySpeedMethod = null;
            for (Method method : blockClass.getDeclaredMethods()) {
                if ("h".equals(method.getName()) && method.getReturnType() == float.class) {
                    destroySpeedMethod = method;
                    break;
                }
            }
            if (destroySpeedMethod == null)
                throw new NoSuchElementException("Array contains no element matching the predicate.");
            this.blockDestroySpeedHandle = ReflectionLookup.methodHandle(destroySpeedMethod);
        } catch (ReflectiveOperationException exception) {
            throw sneaky(exception);
        }
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(
            Block block, BlockShapeMode shapeKind) {
        org.bukkit.World world = block.getWorld();
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(world,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R2.CraftWorld");
        org.bukkit.craftbukkit.v1_19_R2.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_19_R2.CraftWorld) world;
        net.minecraft.server.level.WorldServer handleWorld = craftWorld.getHandle();
        Object blockPosition;
        net.minecraft.world.phys.shapes.VoxelShape shape;
        try {
            blockPosition = (Object) this.blockPositionConstructor.invoke(
                    block.getX(), block.getY(), block.getZ());
            Object blockData = (Object) this.worldBlockState.invoke(handleWorld, blockPosition);
            if (shapeKind == BlockShapeMode.INTERACT) {
                shape = (net.minecraft.world.phys.shapes.VoxelShape) this.voxelShapeBy.invoke(
                        blockData, handleWorld, blockPosition);
            } else {
                shape = (net.minecraft.world.phys.shapes.VoxelShape) this.voxelShapeCollision.invoke(
                        blockData, handleWorld, blockPosition);
            }
        } catch (Throwable exception) {
            throw sneaky(exception);
        }

        List<?> boxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int resultIndex = 0;
        Iterator<?> boxIterator = boxes.iterator();
        while (boxIterator.hasNext()) {
            net.minecraft.world.phys.AxisAlignedBB localBox =
                    (net.minecraft.world.phys.AxisAlignedBB) boxIterator.next();
            result[resultIndex++] = new AxisAlignedBox(
                    block.getX() + localBox.a, block.getY() + localBox.b, block.getZ() + localBox.c,
                    block.getX() + localBox.d, block.getY() + localBox.e, block.getZ() + localBox.f);
        }
        return result;
    }

    @Override
    public void stopUsingItem(Player player) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(player,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer");
        org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer craftPlayer =
                (org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer) player;
        net.minecraft.server.level.EntityPlayer handle = craftPlayer.getHandle();
        try {
            this.stopUsingItemHandle.invoke(handle);
        } catch (Throwable exception) {
            throw sneaky(exception);
        }
    }

    @Override
    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }
    @Override
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    @Override
    public Object getBlockState(Block block) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(block,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock");
        org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock craftBlock =
                (org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock) block;
        try {
            return (Object) this.blockStateByName.invoke(craftBlock.getNMS());
        } catch (Throwable exception) {
            throw sneaky(exception);
        }
    }

    @Override
    public RaytraceResult rayTraceBlocks(World world, Vector origin, Vector direction, double maxDistance) {
        RayTraceResult hit = world.rayTraceBlocks(origin.toLocation(world), direction, maxDistance);
        if (hit == null) return null;
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
                hit.getHitEntity(), hit.getHitBlockFace());
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.Attribute attribute = BukkitAttributes.INDEX.movementSpeedValue();
        org.bukkit.attribute.AttributeInstance instance = entity.getAttribute(attribute);
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(instance);
        return (float) instance.getValue();
    }

    @Override
    public int getItemUseDuration(ItemStack stack, LivingEntity user) {
        net.minecraft.world.item.ItemStack nmsStack =
                org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack.asNMSCopy(stack);
        net.minecraft.world.item.Item item = nmsStack.c();
        return item.b(nmsStack);
    }

    @Override
    public float getBlockFriction(Block block) {
        Object blockData = getBlockState(block);
        try {
            return (float) this.blockDestroySpeedHandle.invoke(blockData);
        } catch (Throwable exception) {
            throw sneaky(exception);
        }
    }

    @Override
    public void setOnGround(Entity entity, boolean noGravity) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(entity,
                "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity");
        org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity craftEntity =
                (org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity) entity;
        net.minecraft.world.entity.Entity handle = craftEntity.getHandle();
        try {
            this.entityNoGravityHandle.invoke(handle, noGravity);
        } catch (Throwable exception) {
            throw sneaky(exception);
        }
    }
}
