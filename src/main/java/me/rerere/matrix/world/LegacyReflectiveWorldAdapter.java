package me.rerere.matrix.world;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.server.ServerNames;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

public class LegacyReflectiveWorldAdapter implements WorldAccess {
    private static MethodHandle entityHandle;
    private static MethodHandle worldHandle;
    private static MethodHandle releaseActiveItem;
    private static MethodHandle blockPositionConstructor;
    private static MethodHandle chunkHandle;
    private static Method blockStateMethod;
    private static MethodHandle chunkBlockState;
    private static MethodHandle interactionShape;
    private static MethodHandle collisionShape;
    private static MethodHandle shapeBoundingBoxes;
    private static MethodHandle[] boundingBoxCoordinateHandles;
    private static MethodHandle craftBlockState;
    private static MethodHandle blockFromState;
    private static MethodHandle blockFriction;
    private static MethodHandle itemFromStack;
    private static MethodHandle itemAnimation;
    private static MethodHandle nativeItemCopy;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            entityHandle = lookup.unreflect(select(craftClass("entity.CraftEntity").getDeclaredMethods(), named("getHandle")));
            worldHandle = lookup.unreflect(select(craftClass("CraftWorld").getDeclaredMethods(), named("getHandle")));
            releaseActiveItem = lookup.unreflect(select(nmsClass("server.level.EntityPlayer").getMethods(), named("clearActiveItem")));
            blockPositionConstructor = lookup.unreflectConstructor(nmsClass("core.BlockPosition").getConstructor(int.class, int.class, int.class));
            chunkHandle = lookup.unreflect(select(craftClass("CraftChunk").getDeclaredMethods(), named("getHandle")));
            Class<?> chunkType = nmsClass("world.level.chunk.Chunk");
            Class<?>[] positionParameter = {nmsClass("core.BlockPosition")};
            blockStateMethod = chunkType.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("getType", chunkType, positionParameter),
                    positionParameter);
            Class<?> chunkTypeAgain = nmsClass("world.level.chunk.Chunk");
            Class<?>[] positionParameterAgain = {nmsClass("core.BlockPosition")};
            chunkBlockState = lookup.unreflect(chunkTypeAgain.getDeclaredMethod(
                    ReflectionNameResolver.mapMethodName("getType", chunkTypeAgain, positionParameterAgain),
                    positionParameterAgain));
            interactionShape = lookup.unreflect(select(nmsClass("world.level.block.state.IBlockData").getMethods(), named("getShape")));
            collisionShape = lookup.unreflect(select(nmsClass("world.level.block.state.IBlockData").getMethods(), named("getCollisionShape")));
            shapeBoundingBoxes = lookup.unreflect(select(
                    nmsClass("world.phys.shapes.VoxelShape").getMethods(),
                    method -> method.getReturnType() == List.class));
            String[] names = {"a", "b", "c", "d", "e", "f"};
            MethodHandle[] coordinateHandles = new MethodHandle[6];
            int index = 0;
            for (String name : names) {
                coordinateHandles[index++] = MethodHandles.lookup().unreflectGetter(
                        Arrays.stream(nmsClass("world.phys.AxisAlignedBB").getDeclaredFields())
                                .filter(field -> field.getName().equals(name))
                                .findFirst().get());
            }
            boundingBoxCoordinateHandles = coordinateHandles;
            craftBlockState = lookup.unreflect(select(craftClass("block.CraftBlock").getDeclaredMethods(), named("getNMS")));
            blockFromState = lookup.unreflect(select(nmsClass("world.level.block.state.IBlockData").getMethods(), named("getBlock")));
            blockFriction = lookup.unreflect(select(nmsClass("world.level.block.Block").getDeclaredMethods(), named("getFrictionFactor")));
            itemFromStack = lookup.unreflect(select(nmsClass("world.item.ItemStack").getDeclaredMethods(), named("getItem")));
            itemAnimation = lookup.unreflect(select(nmsClass("world.item.Item").getMethods(), method -> method.getReturnType().getSimpleName().equals("EnumAnimation")));
            nativeItemCopy = lookup.unreflect(select(craftClass("inventory.CraftItemStack").getDeclaredMethods(), named("asNMSCopy")));
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException failure) {
            failure.printStackTrace();
        }
    }
    private static Predicate<Method> named(String name) {
        return method -> method.getName().equals(name);
    }

    private static Method select(Method[] methods, Predicate<Method> predicate) {
        return Arrays.stream(methods).filter(predicate).findFirst().get();
    }

    private static Class<?> craftClass(String name) throws ClassNotFoundException {
        return Class.forName(ReflectionNameResolver.mapDescriptorName(ServerNames.joinViaInstance(name)));
    }
    private static Class<?> nmsClass(String name) throws ClassNotFoundException {
        return Class.forName(ReflectionNameResolver.mapDescriptorName(
                ServerNames.versionedSince117(name)));
    }
    public int getItemUseDuration(ItemStack item, LivingEntity entity) {
        throw new RuntimeException("TODO!");
    }
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
    public AxisAlignedBox getEntityBounds(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
    public Object getBlockState(Block block) {
        try {
            Object state = craftBlockState.invoke(block);
            return (Object) blockFromState.invoke(state);
        } catch (Throwable failure) { failure.printStackTrace(); return null; }
    }
    public boolean isOnGround(Entity entity) { return entity.isOnGround(); }
    public float getBlockFriction(Block block) {
        Object nativeBlock = getBlockState(block);
        try { return (float) blockFriction.invoke(nativeBlock); }
        catch (Throwable failure) { failure.printStackTrace(); return 0x1.333334p-1f; }
    }
    public void stopUsingItem(Player player) {
        try {
            Object nativePlayer = entityHandle.invoke(player);
            releaseActiveItem.invoke(nativePlayer);
        }
        catch (Throwable failure) { failure.printStackTrace(); }
    }
    public boolean isChunkLoaded(World world, int x, int z) { return world.isChunkLoaded(x, z); }
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }
    public void setOnGround(Entity entity, boolean enabled) { }
    public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.AttributeInstance attribute = entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue());
        return (float) Objects.requireNonNull(attribute, "ERROR#P21MA").getValue();
    }
    public float getBlockBreakSpeed(Player player, Block block) { return block.getBreakSpeed(player); }
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode kind) {
        try {
            Object world = worldHandle.invoke(block.getWorld());
            Object position = blockPositionConstructor.invoke(block.getX(), block.getY(), block.getZ());
            Object chunk = chunkHandle.invoke(block.getChunk());
            Object state = chunkBlockState.invoke(chunk, position);
            Object shape = kind == BlockShapeMode.INTERACT
                    ? (Object) interactionShape.invoke(state, world, position) : (Object) collisionShape.invoke(state, world, position);
            List<?> boxes = (List<?>) shapeBoundingBoxes.invoke(shape);
            AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
            int index = 0;
            for (Object box : boxes) {
                double minX = (double) boundingBoxCoordinateHandles[0].invoke(box);
                double minY = (double) boundingBoxCoordinateHandles[1].invoke(box);
                double minZ = (double) boundingBoxCoordinateHandles[2].invoke(box);
                double maxX = (double) boundingBoxCoordinateHandles[3].invoke(box);
                double maxY = (double) boundingBoxCoordinateHandles[4].invoke(box);
                double maxZ = (double) boundingBoxCoordinateHandles[5].invoke(box);
                result[index++] = new AxisAlignedBox(block.getX() + minX, block.getY() + minY, block.getZ() + minZ,
                        block.getX() + maxX, block.getY() + maxY, block.getZ() + maxZ);
            }
            return result;
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println(blockStateMethod.getDeclaringClass().getName());
            System.out.println(blockStateMethod.getName());
            for (Class<?> parameter : blockStateMethod.getParameterTypes()) System.out.println(parameter.getName());
            return null;
        }
    }
}
