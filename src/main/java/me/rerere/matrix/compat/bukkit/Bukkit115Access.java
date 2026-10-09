package me.rerere.matrix.compat.bukkit;

import java.util.List;
import net.minecraft.server.v1_15_R1.AxisAlignedBB;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.PlayerChunk;
import net.minecraft.server.v1_15_R1.VoxelShape;
import net.minecraft.server.v1_15_R1.WorldServer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.WorldAccess;

/** Direct Bukkit and CraftBukkit 1.15 access used by the server adapter. */
public class Bukkit115Access implements WorldAccess {
    public Bukkit115Access() { }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        net.minecraft.server.v1_15_R1.Entity entityHandle = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bounds = entityHandle.getBoundingBox();
        if (bounds == null) {
            return AxisAlignedBox.at(entity.getLocation());
        }
        return new AxisAlignedBox(
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    @Override
    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().clearActiveItem();
    }

    public float getBlockFriction(Block block) {
        return ((net.minecraft.server.v1_15_R1.Block) getBlockState(block)).l();
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = world.getType(blockPosition);
        return blockData.getDamage(((CraftPlayer) player).getHandle(), world, blockPosition);
    }

    public float getMovementSpeed(LivingEntity entity) {
        return (float) entity.getAttribute(BukkitAttributes.INDEX.movementSpeedValue()).getValue();
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = world.getType(blockPosition);
        VoxelShape shape = shapeKind == BlockShapeMode.INTERACT
                ? blockData.getShape(world, blockPosition)
                : blockData.getCollisionShape(world, blockPosition);
        List<?> localBoxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[localBoxes.size()];
        int index = 0;
        for (Object localBoxValue : localBoxes) {
            AxisAlignedBB localBox = (AxisAlignedBB) localBoxValue;
            result[index++] = new AxisAlignedBox(
                    block.getX() + localBox.minX, block.getY() + localBox.minY, block.getZ() + localBox.minZ,
                    block.getX() + localBox.maxX, block.getY() + localBox.maxY, block.getZ() + localBox.maxZ);
        }
        return result;
    }

    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity user) {
        net.minecraft.server.v1_15_R1.ItemStack nativeItem = CraftItemStack.asNMSCopy(item);
        return nativeItem.getItem().f_(nativeItem);
    }

    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    @Override
    public Object getBlockState(Block block) {
        return ((CraftBlock) block).getNMS().getBlock();
    }

    public boolean isOnGround(Entity entity) {
        return ((CraftEntity) entity).getHandle().onGround;
    }

    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().onGround = onGround;
    }

    private boolean exceedsPlayerChunkTicketLevel(PlayerChunk playerChunk, int ticketLevel) {
        return playerChunk == null || playerChunk.oldTicketLevel > ticketLevel;
    }

    public boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        WorldServer worldHandle = ((CraftWorld) world).getHandle();
        return worldHandle.getChunkProvider().isLoaded(chunkX, chunkZ);
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox bounds = block.getBoundingBox();
        return new AxisAlignedBox(
                bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }
}
