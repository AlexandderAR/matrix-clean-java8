package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class V120R1WorldAdapter implements WorldAccess {
    private final MethodHandle stopUsingItem;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockState;
    private final MethodHandle interactionShape;
    private final MethodHandle collisionShape;
    private final MethodHandle blockFromState;
    private final MethodHandle setOnGround;
    private final MethodHandle blockFriction;
    private final MethodHandle itemAnimation;
    private final MethodHandle itemUseDuration;
    private final MethodHandle effectAmplifier;
    private final MethodHandle supportingBlockPosition;
    private final MethodHandle updateSupportingBlock;

    public V120R1WorldAdapter() {
        try {
            Class<?> nativeLivingEntity = nmsClass("world.entity.EntityLiving");
            stopUsingItem = ReflectionLookup.methodHandle(nativeLivingEntity.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("C", nativeLivingEntity, new Class<?>[0])));
            blockPositionConstructor = ReflectionLookup.constructorHandle(
                    nmsClass("core.BlockPosition").getDeclaredConstructor(int.class, int.class, int.class));
            worldBlockState = find(nmsClass("world.level.World"), method -> Intrinsics.areEqual(method.getName(), "a_"), "Cannot find method 'a_' in World");
            interactionShape = find(nmsClass("world.level.block.state.BlockBase$BlockData"),
                    method -> Intrinsics.areEqual(method.getName(), "j") && method.getParameterCount() == 2, "Cannot find method 'j' in BlockData");
            collisionShape = find(nmsClass("world.level.block.state.BlockBase$BlockData"),
                    method -> Intrinsics.areEqual(method.getName(), "k") && method.getParameterCount() == 2, "Cannot find method 'k' in BlockData");
            Class<?> nativeBlockState = nmsClass("world.level.block.state.BlockBase$BlockData");
            blockFromState = ReflectionLookup.methodHandle(nativeBlockState.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("b", nativeBlockState, new Class<?>[0])));
            Class<?> nativeEntity = nmsClass("world.entity.Entity");
            setOnGround = ReflectionLookup.methodHandle(nativeEntity.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("c", nativeEntity, new Class<?>[]{boolean.class}), boolean.class));
            blockFriction = find(nmsClass("world.level.block.Block"), method -> Intrinsics.areEqual(method.getName(), "h")
                    && Intrinsics.areEqual(method.getReturnType(), float.class) && method.getParameterCount() == 0, "Cannot find method 'h' in Block");
            itemAnimation = find(nmsClass("world.item.Item"),
                    method -> Intrinsics.areEqual(method.getReturnType().getSimpleName(), "EnumAnimation"),
                    "Cannot find method 'getAnimation' in Item");
            itemUseDuration = find(nmsClass("world.item.Item"), method -> Intrinsics.areEqual(method.getReturnType(), int.class)
                    && method.getParameterCount() == 1 && Intrinsics.areEqual(method.getParameterTypes()[0], net.minecraft.world.item.ItemStack.class),
                    "Cannot find method 'getConsumeTicks' in Item");
            List<Field> effectFields = new ArrayList<>();
            for (Field field : nmsClass("world.effect.MobEffect").getDeclaredFields()) {
                if (Intrinsics.areEqual(field.getType(), int.class) && !Modifier.isStatic(field.getModifiers())) {
                    effectFields.add(field);
                }
            }
            MethodHandle amplifier = effectFields.size() > 1 ? ReflectionLookup.fieldGetter(effectFields.get(1)) : null;
            if (amplifier == null) {
                throw new IllegalStateException("Cannot find field 'MobEffect.amplifier' in MobEffect");
            }
            effectAmplifier = amplifier;
            supportingBlockPosition = find(nativeEntity, method -> Intrinsics.areEqual(method.getName(), "aE")
                    && method.getParameterCount() == 0 && Modifier.isProtected(method.getModifiers()), "Cannot find method 'aE' in Entity");
            updateSupportingBlock = find(nativeEntity, method -> Intrinsics.areEqual(method.getName(), "b")
                    && method.getParameterCount() == 2 && Modifier.isProtected(method.getModifiers())
                    && Intrinsics.areEqual(method.getReturnType(), void.class) && Intrinsics.areEqual(method.getParameterTypes()[1], Vec3D.class),
                    "Cannot find method 'b' in Entity");
        } catch (Throwable failure) { throw propagate(failure); }
    }
    private static Class<?> nmsClass(String name) {
        try {
            return ReflectionLookup.loadVersionedNmsClass(name);
        } catch (ClassNotFoundException failure) {
            throw me.rerere.matrix.util.Exceptions.propagate(failure);
        }
    }
    private static MethodHandle find(Class<?> owner, Predicate<Method> matches, String message) throws IllegalAccessException {
        for (Method candidate : owner.getDeclaredMethods()) {
            if (matches.test(candidate)) {
                MethodHandle handle = ReflectionLookup.methodHandle(candidate);
                if (handle != null) return handle;
                break;
            }
        }
        throw new IllegalStateException(message);
    }
    @Override public RaytraceResult rayTraceBlocks(World world, Vector origin, Vector direction, double distance) {
        RayTraceResult hit = world.rayTraceBlocks(origin.toLocation(world), direction, distance);
        return hit == null ? null : new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(), hit.getHitEntity(), hit.getHitBlockFace());
    }
    @Override public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    @Override public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode kind) {
        try {
            World bukkitWorld = block.getWorld();
            Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.CraftWorld");
            WorldServer world = ((CraftWorld) bukkitWorld).getHandle();
            Object position = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            Object state = worldBlockState.invoke(world, position);
            VoxelShape voxelShape = kind == BlockShapeMode.INTERACT
                    ? (VoxelShape) interactionShape.invoke(state, world, position) : (VoxelShape) collisionShape.invoke(state, world, position);
            List<AxisAlignedBB> nativeBoxes = voxelShape.d();
            AxisAlignedBox[] result = new AxisAlignedBox[nativeBoxes.size()];
            int index = 0;
            for (AxisAlignedBB nativeBox : nativeBoxes) {
                result[index++] = new AxisAlignedBox(
                        block.getX() + nativeBox.a, block.getY() + nativeBox.b, block.getZ() + nativeBox.c,
                        block.getX() + nativeBox.d, block.getY() + nativeBox.e, block.getZ() + nativeBox.f);
            }
            return result;
        } catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
    @Override public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
    @Override public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock");
        try { return (Object) blockFromState.invoke(((CraftBlock) block).getNMS()); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public int getItemUseDuration(ItemStack item, LivingEntity ignoredUser) {
        net.minecraft.world.item.ItemStack nativeItem = CraftItemStack.asNMSCopy(item);
        try { return (int) itemUseDuration.invoke(nativeItem.d(), nativeItem); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public float getBlockFriction(Block block) {
        Object nativeBlock = getBlockState(block);
        try { return (float) blockFriction.invoke(nativeBlock); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer");
        try { stopUsingItem.invoke(((CraftPlayer) player).getHandle()); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public float updateSupportingBlock(Entity entity, boolean onGround, Vector displacement) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity");
        net.minecraft.world.entity.Entity nativeEntity = ((CraftEntity) entity).getHandle();
        World worldValue = ((CraftEntity) entity).getWorld();
        Intrinsics.checkNotNull(worldValue, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.CraftWorld");
        WorldServer world = ((CraftWorld) worldValue).getHandle();
        try {
            updateSupportingBlock.invoke(nativeEntity, onGround, new Vec3D(displacement.getX(), displacement.getY(), displacement.getZ()));
            Object position = supportingBlockPosition.invoke(nativeEntity);
            Object state = worldBlockState.invoke(world, position);
            return (float) blockFriction.invoke((Object) blockFromState.invoke(state));
        } catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public boolean isOnGround(Entity entity) { return entity.isOnGround(); }
    @Override public void setOnGround(Entity entity, boolean value) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity");
        try { setOnGround.invoke(((CraftEntity) entity).getHandle(), value); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @Override public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }
    @Override public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.AttributeInstance attribute = entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue());
        Intrinsics.checkNotNull(attribute);
        return (float) attribute.getValue();
    }
    @Override public float getBlockBreakSpeed(Player player, Block block) { return block.getBreakSpeed(player); }
    private static RuntimeException propagate(Throwable failure) { V120R1WorldAdapter.<RuntimeException>throwUnchecked(failure); return null; }
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T { throw (T) failure; }
}
