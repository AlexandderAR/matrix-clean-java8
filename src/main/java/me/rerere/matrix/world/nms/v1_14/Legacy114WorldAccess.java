package me.rerere.matrix.world.nms.v1_14;

import java.util.Iterator;
import java.util.List;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.WorldAccess;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

/** Bukkit and NMS operations for v1_14_R1 servers. */
public class Legacy114WorldAccess implements WorldAccess {
    public Legacy114WorldAccess() { }

    @Override
    public float getBlockFriction(Block block) {
        net.minecraft.server.v1_14_R1.Block nmsBlock =
                (net.minecraft.server.v1_14_R1.Block) getBlockState(block);
        return nmsBlock.m();
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode) {
        net.minecraft.server.v1_14_R1.WorldServer world =
                ((org.bukkit.craftbukkit.v1_14_R1.CraftWorld) block.getWorld()).getHandle();
        net.minecraft.server.v1_14_R1.BlockPosition position =
                new net.minecraft.server.v1_14_R1.BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_14_R1.IBlockData data = world.getType(position);
        net.minecraft.server.v1_14_R1.VoxelShape shape = mode == BlockShapeMode.INTERACT
                ? data.getShape(world, position) : data.getCollisionShape(world, position);
        List<?> boxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int index = 0;
        for (Iterator<?> it = boxes.iterator(); it.hasNext();) {
            net.minecraft.server.v1_14_R1.AxisAlignedBB box =
                    (net.minecraft.server.v1_14_R1.AxisAlignedBB) it.next();
            result[index++] = new AxisAlignedBox(
                    block.getX() + box.minX, block.getY() + box.minY, block.getZ() + box.minZ,
                    block.getX() + box.maxX, block.getY() + box.maxY, block.getZ() + box.maxZ);
        }
        return result;
    }

    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity unusedUser) {
        net.minecraft.server.v1_14_R1.ItemStack nmsItem =
                org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack.asNMSCopy(item);
        return nmsItem.getItem().f_(nmsItem);
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        net.minecraft.server.v1_14_R1.Entity nmsEntity =
                ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity) entity).getHandle();
        net.minecraft.server.v1_14_R1.AxisAlignedBB box = nmsEntity.getBoundingBox();
        if (box == null) return AxisAlignedBox.at(entity.getLocation());
        return new AxisAlignedBox(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    @Override
    public boolean isOnGround(Entity entity) {
        return ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity) entity).getHandle().onGround;
    }

    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity) entity).getHandle().onGround = onGround;
    }

    @Override
    public Object getBlockState(Block block) {
        return ((org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock) block).getNMS().getBlock();
    }

    @Override
    public void stopUsingItem(Player player) {
        ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer) player).getHandle().clearActiveItem();
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        Attribute attribute = BukkitAttributes.INDEX.movementSpeedValue();
        AttributeInstance instance = entity.getAttribute(attribute);
        return (float) instance.getValue();
    }

    @Override
    public boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        net.minecraft.server.v1_14_R1.WorldServer handle =
                ((org.bukkit.craftbukkit.v1_14_R1.CraftWorld) world).getHandle();
        return handle.getChunkProvider().isLoaded(chunkX, chunkZ);
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public boolean isUsingItem(Player player) { return player.isHandRaised(); }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        net.minecraft.server.v1_14_R1.WorldServer world =
                ((org.bukkit.craftbukkit.v1_14_R1.CraftWorld) block.getWorld()).getHandle();
        net.minecraft.server.v1_14_R1.BlockPosition position =
                new net.minecraft.server.v1_14_R1.BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_14_R1.IBlockData data = world.getType(position);
        net.minecraft.server.v1_14_R1.EntityPlayer handle =
                ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer) player).getHandle();
        return data.getDamage(handle, world, position);
    }

}
