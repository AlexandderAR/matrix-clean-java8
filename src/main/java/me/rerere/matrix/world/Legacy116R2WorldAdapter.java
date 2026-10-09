package me.rerere.matrix.world;

import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;

/** Version-specific Bukkit/CraftBukkit bridge for the 1.16_R2 server API. */
public final class Legacy116R2WorldAdapter implements WorldAccess {
    @Override
    public int getItemUseDuration(ItemStack stack, LivingEntity user) {
        net.minecraft.server.v1_16_R2.ItemStack nmsStack =
                org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack.asNMSCopy(stack);
        return nmsStack.getItem().e_(nmsStack);
    }



    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        ((org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity) entity).getHandle().setOnGround(onGround);
    }



    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeMode) {
        org.bukkit.craftbukkit.v1_16_R2.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_16_R2.CraftWorld) block.getWorld();
        net.minecraft.server.v1_16_R2.WorldServer world = craftWorld.getHandle();
        net.minecraft.server.v1_16_R2.BlockPosition position = new net.minecraft.server.v1_16_R2.BlockPosition(
                block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_16_R2.IBlockData blockData = world.getType(position);
        net.minecraft.server.v1_16_R2.VoxelShape shape = shapeMode == BlockShapeMode.INTERACT
                ? blockData.getShape(world, position)
                : blockData.getCollisionShape(world, position);
        List<net.minecraft.server.v1_16_R2.AxisAlignedBB> boxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[boxes.size()];
        int index = 0;
        for (net.minecraft.server.v1_16_R2.AxisAlignedBB box : boxes) {
            double x = block.getX();
            double y = block.getY();
            double z = block.getZ();
            result[index++] = new AxisAlignedBox(
                    x + box.minX, y + box.minY, z + box.minZ,
                    x + box.maxX, y + box.maxY, z + box.maxZ);
        }
        return result;
    }



    @Override
    public boolean isOnGround(Entity entity) {
        return ((org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity) entity).getHandle().isOnGround();
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        net.minecraft.server.v1_16_R2.EntityHuman human =
                (net.minecraft.server.v1_16_R2.EntityHuman)
                        ((org.bukkit.craftbukkit.v1_16_R2.entity.CraftLivingEntity) entity).getHandle();
        return human.dM();
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity craftEntity =
                (org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity) entity;
        net.minecraft.server.v1_16_R2.AxisAlignedBB box = craftEntity.getHandle().getBoundingBox();
        if (box == null) {
            return AxisAlignedBox.at(entity.getLocation());
        }
        return new AxisAlignedBox(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }



    @Override
    public void stopUsingItem(Player player) {
        ((org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer) player).getHandle().clearActiveItem();
    }



    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public Object getBlockState(Block block) {
        return ((org.bukkit.craftbukkit.v1_16_R2.block.CraftBlock) block).getNMS().getBlock();
    }



    @Override
    public float getBlockFriction(Block block) {
        return ((net.minecraft.server.v1_16_R2.Block) getBlockState(block)).getFrictionFactor();
    }

    @Override
    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        org.bukkit.craftbukkit.v1_16_R2.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_16_R2.CraftWorld) block.getWorld();
        net.minecraft.server.v1_16_R2.WorldServer world = craftWorld.getHandle();
        net.minecraft.server.v1_16_R2.BlockPosition position = new net.minecraft.server.v1_16_R2.BlockPosition(
                block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_16_R2.IBlockData blockData = world.getType(position);
        net.minecraft.server.v1_16_R2.EntityPlayer nmsPlayer =
                ((org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer) player).getHandle();
        return blockData.getDamage(nmsPlayer, world, position);
    }


}
