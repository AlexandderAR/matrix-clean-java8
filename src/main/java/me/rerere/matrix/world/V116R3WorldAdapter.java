package me.rerere.matrix.world;

import java.util.List;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MobEffect;
import net.minecraft.server.v1_16_R3.MobEffectList;
import net.minecraft.server.v1_16_R3.VoxelShape;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;

public class V116R3WorldAdapter implements WorldAccess {
    @Override
    public int getItemUseDuration(ItemStack item, LivingEntity user) {
        net.minecraft.server.v1_16_R3.ItemStack nativeItemStack = CraftItemStack.asNMSCopy(item);
        return nativeItemStack.getItem().e_(nativeItemStack);
    }

    @Override
    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    @Override
    public void setOnGround(Entity entity, boolean onGround) {
        ((CraftEntity) entity).getHandle().setOnGround(onGround);
    }

    @Override
    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    @Override
    public AxisAlignedBox getEntityBounds(Entity entity) {
        AxisAlignedBB bounds = ((CraftEntity) entity).getHandle().getBoundingBox();
        if (bounds == null) return AxisAlignedBox.at(entity.getLocation());
        return new AxisAlignedBox(bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    @Override
    public float getBlockFriction(Block block) {
        net.minecraft.server.v1_16_R3.Block nativeBlock =
                (net.minecraft.server.v1_16_R3.Block) getBlockState(block);
        return nativeBlock.getFrictionFactor();
    }

    @Override
    public float getBlockBreakSpeed(Player player, Block block) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockState = world.getType(blockPosition);
        return blockState.getDamage(((CraftPlayer) player).getHandle(), world, blockPosition);
    }

    @Override
    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeKind) {
        WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_16_R3.Chunk chunk = ((CraftChunk) block.getChunk()).getHandle();
        IBlockData blockState = chunk.getType(blockPosition);
        VoxelShape shape = shapeKind == BlockShapeMode.INTERACT
                ? blockState.getShape(world, blockPosition)
                : blockState.getCollisionShape(world, blockPosition);
        List<AxisAlignedBB> nativeBoxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[nativeBoxes.size()];
        int index = 0;
        for (AxisAlignedBB nativeBox : nativeBoxes) {
            double minX = block.getX() + nativeBox.minX;
            double minY = block.getY() + nativeBox.minY;
            double minZ = block.getZ() + nativeBox.minZ;
            double maxX = block.getX() + nativeBox.maxX;
            double maxY = block.getY() + nativeBox.maxY;
            double maxZ = block.getZ() + nativeBox.maxZ;
            result[index++] = new AxisAlignedBox(minX, minY, minZ, maxX, maxY, maxZ);
        }
        return result;
    }

    @Override
    public Object getBlockState(Block block) {
        net.minecraft.server.v1_16_R3.Chunk chunk = ((CraftChunk) block.getChunk()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockState = chunk.getType(blockPosition);
        return blockState.getBlock();
    }

    @Override
    public int getPotionAmplifierPlusOne(Player player, PotionEffectType type) {
        EntityLiving livingEntity = ((CraftLivingEntity) player).getHandle();
        MobEffect effect = livingEntity.getEffect(MobEffectList.fromId(type.getId()));
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    @Override
    public float getMovementSpeed(LivingEntity entity) {
        EntityHuman human = (EntityHuman) ((CraftLivingEntity) entity).getHandle();
        return human.dN();
    }

    @Override
    public void stopUsingItem(Player player) {
        ((CraftPlayer) player).getHandle().clearActiveItem();
    }

    @Override
    public boolean isOnGround(Entity entity) {
        return ((CraftEntity) entity).getHandle().isOnGround();
    }
}
