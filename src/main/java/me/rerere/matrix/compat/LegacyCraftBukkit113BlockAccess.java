package me.rerere.matrix.compat;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import net.minecraft.server.v1_13_R1.AxisAlignedBB;
import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityHuman;
import net.minecraft.server.v1_13_R1.IBlockData;
import net.minecraft.server.v1_13_R1.Item;
import net.minecraft.server.v1_13_R1.VoxelShape;
import net.minecraft.server.v1_13_R1.WorldServer;

/** Direct 1.13 CraftBukkit/NMS bridge used by movement and collision checks. */
public final class LegacyCraftBukkit113BlockAccess implements me.rerere.matrix.world.WorldAccess {
    public LegacyCraftBukkit113BlockAccess() {}

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        CraftBlock craftBlock = (CraftBlock) block;
        IBlockData state = craftBlock.getNMS();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer world = craftBlock.getCraftWorld().getHandle();
        EntityHuman human = ((CraftPlayer) player).getHandle();
        return state.getDamage(human, world, position);
    }

    @Override
    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().clearActiveItem();
    }

    @Override
    public boolean isOnGround(org.bukkit.entity.Entity entity) {
        return ((CraftEntity) entity).getHandle().onGround;
    }

    @Override
    public me.rerere.matrix.geometry.AxisAlignedBox getEntityBounds(org.bukkit.entity.Entity entity) {
        Entity handle = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bounds = handle.getBoundingBox();
        if (bounds == null) return me.rerere.matrix.geometry.AxisAlignedBox.at(entity.getLocation());
        return new me.rerere.matrix.geometry.AxisAlignedBox(bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }

    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity user) {
        net.minecraft.server.v1_13_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        Item nmsType = nmsItem.getItem();
        return nmsType.c(nmsItem);
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        org.bukkit.attribute.Attribute attribute = me.rerere.matrix.world.BukkitAttributes.INDEX.movementSpeedValue();
        AttributeInstance instance = entity.getAttribute(attribute);
        return (float) instance.getValue();
    }

    @Override
    public Object getBlockState(Block block) {
        return ((CraftBlock) block).getNMS().getBlock();
    }

    @Override
    public float getBlockFriction(Block block) {
        return ((net.minecraft.server.v1_13_R1.Block) getBlockState(block)).n();
    }

    @Override
    public me.rerere.matrix.geometry.AxisAlignedBox[] getCollisionBoxes(
            Block block, me.rerere.matrix.geometry.BlockShapeMode mode) {
        CraftBlock craftBlock = (CraftBlock) block;
        IBlockData state = craftBlock.getNMS();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer world = craftBlock.getCraftWorld().getHandle();
        state.getBlock();
        VoxelShape shape;
        if (mode == me.rerere.matrix.geometry.BlockShapeMode.INTERACT) {
            shape = state.g(world, position);
        } else {
            shape = state.h(world, position);
        }
        if (shape.b()) {
            return new me.rerere.matrix.geometry.AxisAlignedBox[] {
                    me.rerere.matrix.geometry.AxisAlignedBox.at(block.getLocation()) };
        }
        List<AxisAlignedBB> localBoxes = shape.d();
        me.rerere.matrix.geometry.AxisAlignedBox[] result = new me.rerere.matrix.geometry.AxisAlignedBox[localBoxes.size()];
        int index = 0;
        for (AxisAlignedBB local : localBoxes) {
            result[index++] = new me.rerere.matrix.geometry.AxisAlignedBox(
                    block.getX() + local.a, block.getY() + local.b, block.getZ() + local.c,
                    block.getX() + local.d, block.getY() + local.e, block.getZ() + local.f);
        }
        return result;
    }

    @Override
    public boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        WorldServer handle = ((CraftWorld) world).getHandle();
        return handle.getChunkProviderServer().isLoaded(chunkX, chunkZ);
    }

    @Override
    public void setOnGround(org.bukkit.entity.Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().onGround = onGround;
    }

    @Override
    public me.rerere.matrix.geometry.AxisAlignedBox getBlockBounds(Block block) {
        CraftBlock craftBlock = (CraftBlock) block;
        IBlockData state = craftBlock.getNMS();
        state.getBlock();
        WorldServer world = ((CraftWorld) craftBlock.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        VoxelShape shape = state.g(world, position);
        if (shape.b()) return me.rerere.matrix.geometry.AxisAlignedBox.at(block.getLocation());
        AxisAlignedBB local = shape.a();
        Location origin = block.getLocation();
        return new me.rerere.matrix.geometry.AxisAlignedBox(
                origin.getX() + local.a, origin.getY() + local.b, origin.getZ() + local.c,
                origin.getX() + local.d, origin.getY() + local.e, origin.getZ() + local.f);
    }

    @Override
    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }
}
