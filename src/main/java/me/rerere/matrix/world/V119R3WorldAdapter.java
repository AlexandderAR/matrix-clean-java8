package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class V119R3WorldAdapter implements WorldAccess {
    private final MethodHandle stopUsingItem;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockState;
    private final MethodHandle interactionShape;
    private final MethodHandle collisionShape;
    private final MethodHandle blockFromState;
    private final MethodHandle setOnGround;
    private final MethodHandle blockFriction;

    public V119R3WorldAdapter() {
        try {
            Class<?> livingEntityClass = nmsClass("world.entity.EntityLiving");
            stopUsingItem = ReflectionLookup.methodHandle(livingEntityClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("C", livingEntityClass, new Class<?>[0])));
            blockPositionConstructor = ReflectionLookup.constructorHandle(
                    nmsClass("core.BlockPosition").getDeclaredConstructor(int.class, int.class, int.class));
            worldBlockState = find(nmsClass("world.level.World"), "a_", -1, null, "Cannot find method 'a_' in World");
            Class<?> blockStateClass = nmsClass("world.level.block.state.BlockBase$BlockData");
            interactionShape = find(blockStateClass, "j", 2, null, "Cannot find method 'j' in BlockData");
            collisionShape = find(nmsClass("world.level.block.state.BlockBase$BlockData"), "k", 2, null, "Cannot find method 'k' in BlockData");
            Class<?> blockStateClassAgain = nmsClass("world.level.block.state.BlockBase$BlockData");
            blockFromState = ReflectionLookup.methodHandle(blockStateClassAgain.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("b", blockStateClassAgain, new Class<?>[0])));
            Class<?> entityClass = nmsClass("world.entity.Entity");
            setOnGround = ReflectionLookup.methodHandle(entityClass.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("c", entityClass, new Class<?>[]{boolean.class}), boolean.class));
            blockFriction = find(nmsClass("world.level.block.Block"), "i", -1, float.class, "Cannot find method 'i' in Block");
        } catch (Throwable failure) { throw propagate(failure); }
    }
    private static Class<?> nmsClass(String name) throws ClassNotFoundException {
        return ReflectionLookup.loadVersionedNmsClass(name);
    }
    private static MethodHandle find(Class<?> owner, String name, int parameterCount,
            Class<?> returnType, String message) throws IllegalAccessException {
        for (Method method : owner.getDeclaredMethods()) {
            if (Intrinsics.areEqual(method.getName(), name)
                    && (parameterCount < 0 || method.getParameterCount() == parameterCount)
                    && (returnType == null || Intrinsics.areEqual(method.getReturnType(), returnType))) {
                MethodHandle handle = ReflectionLookup.methodHandle(method);
                if (handle != null) return handle;
                break;
            }
        }
        throw new IllegalStateException(message);
    }
    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        try {
            World blockWorld = block.getWorld();
            Intrinsics.checkNotNull(blockWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R3.CraftWorld");
            WorldServer world = ((CraftWorld) blockWorld).getHandle();
            Object position = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            Object blockState = worldBlockState.invoke(world, position);
            VoxelShape shape = mode == BlockShapeMode.INTERACT
                    ? (VoxelShape) interactionShape.invoke(blockState, world, position)
                    : (VoxelShape) collisionShape.invoke(blockState, world, position);
            List<AxisAlignedBB> boxes = shape.d();
            AxisAlignedBox[] collisionBoxes = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (AxisAlignedBB box : boxes) {
                collisionBoxes[index++] = new AxisAlignedBox(block.getX() + box.a, block.getY() + box.b, block.getZ() + box.c,
                        block.getX() + box.d, block.getY() + box.e, block.getZ() + box.f);
            }
            return collisionBoxes;
        } catch (Throwable failure) { throw propagate(failure); }
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox bounds = entity.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox bounds = block.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }
    @Override
    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }
    @Override
    public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.AttributeInstance attribute = entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue());
        Intrinsics.checkNotNull(attribute);
        return (float) attribute.getValue();
    }
    @Override
    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }
    @Override
    public float getBlockBreakSpeed(Player player, Block block) { return block.getBreakSpeed(player); }

    @Override
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity ignoredEntity) {
        net.minecraft.world.item.ItemStack nativeItem = CraftItemStack.asNMSCopy(item);
        return nativeItem.c().b(nativeItem);
    }

    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity");
        try { setOnGround.invoke(((CraftEntity) entity).getHandle(), onGround); }
        catch (Throwable failure) { throw propagate(failure); }
    }

    @Override
    public RaytraceResult rayTraceBlocks(World world, Vector origin, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(origin.toLocation(world), direction, distance);
        return hit == null ? null : new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(), hit.getHitEntity(), hit.getHitBlockFace());
    }
    @Override
    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock");
        try { return (Object) blockFromState.invoke(((CraftBlock) block).getNMS()); }
        catch (Throwable failure) { throw propagate(failure); }
    }

    @Override
    public float getBlockFriction(Block block) {
        Object nativeBlock = getBlockState(block);
        try { return (float) blockFriction.invoke(nativeBlock); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override
    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer");
        try { stopUsingItem.invoke(((CraftPlayer) player).getHandle()); }
        catch (Throwable failure) { throw propagate(failure); }
    }

    private static RuntimeException propagate(Throwable failure) { V119R3WorldAdapter.<RuntimeException>throwUnchecked(failure); return null; }
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T { throw (T) failure; }
}
