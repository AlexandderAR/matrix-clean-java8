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
import org.bukkit.craftbukkit.v1_20_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R4.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** Bukkit and NMS operations for the supported v1_20_R4 server. */
public final class NmsWorldAccess implements WorldAccess {
    private static boolean showEntityCollision;

    private final MethodHandle playerEntityAction;
    private final MethodHandle blockPositionConstructor;
    private final MethodHandle worldBlockState;
    private final MethodHandle blockCollisionShape;
    private final MethodHandle blockOutlineShape;
    private final MethodHandle blockFluidState;
    private final MethodHandle entityBooleanAction;
    private final MethodHandle blockDestroySpeed;
    private final MethodHandle itemAnimation;
    private final MethodHandle itemConsumeTicks;
    private final MethodHandle effectAmplifier;
    private final MethodHandle voxelShapeBoxes;
    private final MethodHandle entitySpeed;
    private final MethodHandle entityBlockPosition;
    private final MethodHandle entityVectorAction;

    static {
        setShowEntityCollision(false);
    }

    public NmsWorldAccess() {
        super();
        Class<?> livingClass = serverClass("world.entity.EntityLiving");
        this.playerEntityAction = directHandle(livingClass, "H");
        Class<?> blockPositionClass = serverClass("core.BlockPosition");
        this.blockPositionConstructor = constructorHandle(blockPositionClass,
            new Class<?>[] { int.class, int.class, int.class });

        Class<?> worldClass = serverClass("world.level.World");
        this.worldBlockState = requiredHandle(findMethod(worldClass,
            m -> m.getName().equals("a_"), "Cannot find method 'a_' in World"),
            "Cannot find method 'a_' in World");

        Class<?> blockDataClass = serverClass("world.level.block.state.BlockBase$BlockData");
        this.blockCollisionShape = requiredHandle(findMethod(blockDataClass,
            m -> m.getName().equals("j") && m.getParameterCount() == 2,
            "Cannot find method 'j' in BlockData"), "Cannot find method 'j' in BlockData");
        this.blockOutlineShape = requiredHandle(findMethod(blockDataClass,
            m -> m.getName().equals("k") && m.getParameterCount() == 2,
            "Cannot find method 'k' in BlockData"), "Cannot find method 'k' in BlockData");
        this.blockFluidState = directHandle(blockDataClass, "b");

        Class<?> entityClass = serverClass("world.entity.Entity");
        this.entityBooleanAction = directHandle(entityClass, "c", boolean.class);

        Class<?> blockClass = serverClass("world.level.block.Block");
        this.blockDestroySpeed = requiredHandle(findMethod(blockClass,
            m -> m.getName().equals("h") && m.getReturnType() == float.class && m.getParameterCount() == 0,
            "Cannot find method 'h' in Block"), "Cannot find method 'h' in Block");

        Class<?> itemClass = serverClass("world.item.Item");
        this.itemAnimation = requiredHandle(findMethod(itemClass,
            m -> m.getReturnType().getSimpleName().equals("EnumAnimation"),
            "Cannot find method 'getAnimation' in Item"), "Cannot find method 'getAnimation' in Item");
        this.itemConsumeTicks = requiredHandle(findMethod(itemClass,
            m -> m.getReturnType() == int.class && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == net.minecraft.world.item.ItemStack.class,
            "Cannot find method 'getConsumeTicks' in Item"), "Cannot find method 'getConsumeTicks' in Item");

        Class<?> effectClass = serverClass("world.effect.MobEffect");
        List<Field> integerInstanceFields = new ArrayList<>();
        for (Field field : effectClass.getDeclaredFields()) {
            if (field.getType() == int.class && !Modifier.isStatic(field.getModifiers()))
                integerInstanceFields.add(field);
        }
        Field amplifier = integerInstanceFields.size() > 1 ? integerInstanceFields.get(1) : null;
        this.effectAmplifier = amplifier == null ? null : fieldGetter(amplifier);
        if (this.effectAmplifier == null)
            throw new IllegalStateException("Cannot find field 'MobEffect.amplifier' in MobEffect");

        Class<?> voxelShapeClass = serverClass("world.phys.shapes.VoxelShape");
        this.voxelShapeBoxes = requiredHandle(findMethod(voxelShapeClass,
            m -> Modifier.isPublic(m.getModifiers()) && m.getReturnType() == List.class
                && m.getParameterCount() == 0,
            "Cannot find method 'toAabbs' in VoxelShape"), "Cannot find method 'toAabbs' in VoxelShape");

        Class<?> livingEntityClass = serverClass("world.entity.EntityLiving");
        this.entitySpeed = requiredHandle(findMethod(livingEntityClass,
            m -> m.getName().equals("ff") && m.getParameterCount() == 0,
            "Cannot find method 'getSpeed' in EntityLiving"), "Cannot find method 'getSpeed' in EntityLiving");

        this.entityBlockPosition = requiredHandle(findMethod(entityClass,
            m -> m.getName().equals("aI") && m.getParameterCount() == 0
                && Modifier.isProtected(m.getModifiers()),
            "Cannot find method 'aI' in Entity"), "Cannot find method 'aI' in Entity");
        this.entityVectorAction = requiredHandle(findMethod(entityClass,
            m -> m.getName().equals("b") && m.getParameterCount() == 2
                && Modifier.isProtected(m.getModifiers()) && m.getReturnType() == void.class
                && m.getParameterTypes()[1] == Vec3D.class,
            "Cannot find method 'b' in Entity"), "Cannot find method 'b' in Entity");
    }

    private static Class<?> serverClass(String encodedName) {
        try {
            return ReflectionLookup.loadVersionedNmsClass(encodedName);
        } catch (ClassNotFoundException failure) {
            return NmsWorldAccess.<RuntimeException, Class<?>>throwUnchecked(failure);
        }
    }
    private static MethodHandle directHandle(Class<?> owner, String encodedName, Class<?>... parameters) {
        try {
            String methodName = ReflectionNameResolver.mapMethodName(encodedName, owner, parameters);
            return ReflectionLookup.methodHandle(owner.getDeclaredMethod(methodName, parameters));
        } catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
    }
    private static MethodHandle constructorHandle(Class<?> owner, Class<?>[] parameters) {
        try {
            Constructor<?> constructor = owner.getDeclaredConstructor(parameters);
            return ReflectionLookup.constructorHandle(constructor);
        } catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
    }
    private static MethodHandle fieldGetter(Field field) {
        try { return ReflectionLookup.fieldGetter(field); }
        catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
    }
    private static Method findMethod(Class<?> owner, Predicate<Method> predicate, String error) {
        for (Method method : owner.getDeclaredMethods())
            if (predicate.test(method)) return method;
        throw new IllegalStateException(error);
    }
    private static MethodHandle requiredHandle(Method method, String error) {
        MethodHandle handle;
        try {
            handle = ReflectionLookup.methodHandle(method);
        } catch (IllegalAccessException failure) {
            return NmsWorldAccess.<RuntimeException, MethodHandle>throwUnchecked(failure);
        }
        if (handle == null) throw new IllegalStateException(error);
        return handle;
    }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
    private static void requireCraftEntity(Entity entity) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(entity,
            "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R4.entity.CraftEntity");
    }
    private static CraftWorld craftWorld(World world) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(world,
            "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R4.CraftWorld");
        return (CraftWorld) world;
    }

    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }

    public RaytraceResult rayTraceBlocks(
            org.bukkit.World world, Vector origin, Vector direction, double maximumDistance) {
        RayTraceResult hit = world.rayTraceBlocks(origin.toLocation(world), direction, maximumDistance);
        if (hit == null) return null;
        return new RaytraceResult(hit.getHitPosition(), hit.getHitBlock(),
            hit.getHitEntity(), hit.getHitBlockFace());
    }

    public float getMovementSpeed(LivingEntity entity) {
        requireCraftEntity(entity);
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        try { return (float) this.entitySpeed.invoke(handle); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(
            box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public Object getBlockState(Block block) {
        return blockState(block);
    }

    private Object blockState(Block block) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(block,
            "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R4.block.CraftBlock");
        CraftBlock craftBlock = (CraftBlock) block;
        try { return this.blockFluidState.invoke(craftBlock.getNMS()); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Object>throwUnchecked(failure); }
    }

    public float updateSupportingBlock(Entity entity, boolean onGround, Vector motion) {
        requireCraftEntity(entity);
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity handle = craftEntity.getHandle();
        CraftWorld craftWorld = craftWorld(craftEntity.getWorld());
        WorldServer world = craftWorld.getHandle();
        try { this.entityVectorAction.invoke(handle, onGround, new Vec3D(motion.getX(), motion.getY(), motion.getZ())); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
        Object position;
        try { position = this.entityBlockPosition.invoke(handle); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
        if (position == null) return 0x1.333334p-1f;
        Object state;
        try { state = this.worldBlockState.invoke(world, position); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
        Object block;
        try { block = this.blockFluidState.invoke(state); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
        try { return (float) this.blockDestroySpeed.invoke(block); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        WorldServer world = craftWorld(block.getWorld()).getHandle();
        Object blockPosition;
        Object blockState;
        try {
            blockPosition = this.blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            blockState = this.worldBlockState.invoke(world, blockPosition);
        } catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
        VoxelShape shape;
        try {
            if (shapeKind == BlockShapeMode.INTERACT)
                shape = (VoxelShape) this.blockCollisionShape.invoke(blockState, world, blockPosition);
            else
                shape = (VoxelShape) this.blockOutlineShape.invoke(blockState, world, blockPosition);
        } catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
        List<?> boxes;
        try { boxes = (List<?>) this.voxelShapeBoxes.invoke(shape); }
        catch (Throwable failure) {
            return NmsWorldAccess.<RuntimeException, AxisAlignedBox[]>throwUnchecked(failure);
        }
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int resultIndex = 0;
        java.util.Iterator<?> iterator = boxes.iterator();
        while (iterator.hasNext()) {
            AxisAlignedBB box = (AxisAlignedBB) iterator.next();
            result[resultIndex++] = new AxisAlignedBox(
                block.getX() + box.a, block.getY() + box.b, block.getZ() + box.c,
                block.getX() + box.d, block.getY() + box.e, block.getZ() + box.f);
        }
        return result;
    }

    public float getBlockFriction(Block block) {
        Object state = blockState(block);
        try { return (float) this.blockDestroySpeed.invoke(state); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Float>throwUnchecked(failure); }
    }

    @Override
    public void stopUsingItem(Player player) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(player,
            "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer");
        net.minecraft.server.level.EntityPlayer handle = ((CraftPlayer) player).getHandle();
        try { this.playerEntityAction.invoke(handle); }
        catch (Throwable failure) { NmsWorldAccess.<RuntimeException, Void>throwUnchecked(failure); }
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        return block.getBreakSpeed(player);
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(
            box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public void setOnGround(Entity entity, boolean enabled) {
        requireCraftEntity(entity);
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        try { this.entityBooleanAction.invoke(handle, enabled); }
        catch (Throwable failure) { NmsWorldAccess.<RuntimeException, Void>throwUnchecked(failure); }
    }

    public boolean isChunkLoaded(org.bukkit.World world, int x, int z) { return world.isChunkLoaded(x, z); }
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity ignoredUser) {
        net.minecraft.world.item.ItemStack handle = CraftItemStack.asNMSCopy(item);
        try { return (int) this.itemConsumeTicks.invoke(handle.d(), handle); }
        catch (Throwable failure) { return NmsWorldAccess.<RuntimeException, Integer>throwUnchecked(failure); }
    }

    public static void setShowEntityCollision(boolean enabled) { showEntityCollision = enabled; }
    public static boolean isShowEntityCollision() { return showEntityCollision; }
    public static boolean shouldRenderEntityCollision() {
        try { return !isShowEntityCollision(); }
        catch (IllegalStateException failure) { throw failure; }
    }
}
