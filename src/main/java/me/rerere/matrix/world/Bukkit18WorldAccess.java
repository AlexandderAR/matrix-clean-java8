package me.rerere.matrix.world;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.util.BoundingBox;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.WorldAccess;

/** Bukkit 1.18 bridge for entity, item, block-state and collision queries. */
public final class Bukkit18WorldAccess implements WorldAccess {

    public Bukkit18WorldAccess() {
    }

    public int getItemUseDuration(ItemStack itemStack, LivingEntity ignoredEntity) {
        net.minecraft.world.item.ItemStack nativeItem = CraftItemStack.asNMSCopy(itemStack);
        return nativeItem.c().b(nativeItem);
    }



    public float getMovementSpeed(LivingEntity livingEntity) {
        Intrinsics.checkNotNull(livingEntity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity");
        CraftLivingEntity craftEntity = (CraftLivingEntity) livingEntity;
        EntityLiving entityHandle = craftEntity.getHandle();
        Intrinsics.checkNotNull(entityHandle, "null cannot be cast to non-null type net.minecraft.world.entity.player.EntityHuman");
        EntityHuman human = (EntityHuman) entityHandle;
        return human.eC();
    }

    public void setOnGround(Entity entity, boolean value) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity");
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity entityHandle = craftEntity.getHandle();
        entityHandle.c(value);
    }



    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox bounds = block.getBoundingBox();
        return new AxisAlignedBox(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(),
                bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    public float getBlockFriction(Block block) {
        Object nmsBlock = getBlockState(block);
        Intrinsics.checkNotNull(nmsBlock, "null cannot be cast to non-null type net.minecraft.world.level.block.Block");
        return ((net.minecraft.world.level.block.Block) nmsBlock).i();
    }

    public void stopUsingItem(Player player) {
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer");
        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityPlayer playerHandle = craftPlayer.getHandle();
        playerHandle.eR();
    }



    public AxisAlignedBox getEntityBounds(Entity entity) {
        Intrinsics.checkNotNull(entity, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity");
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity entityHandle = craftEntity.getHandle();
        AxisAlignedBB bounds = entityHandle.cw();
        if (bounds == null) {
            return AxisAlignedBox.at(craftEntity.getLocation());
        }
        return new AxisAlignedBox(bounds.a, bounds.b, bounds.c, bounds.d, bounds.e, bounds.f);
    }



    public float getBlockBreakSpeed(Player player, Block block) {
        org.bukkit.World bukkitWorld = block.getWorld();
        Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.CraftWorld");
        CraftWorld craftWorld = (CraftWorld) bukkitWorld;
        WorldServer worldHandle = craftWorld.getHandle();
        World world = (World) worldHandle;
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = world.a_(blockPosition);
        Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer");
        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityPlayer playerHandle = craftPlayer.getHandle();
        EntityHuman human = (EntityHuman) playerHandle;
        return blockData.a(human, (IBlockAccess) world, blockPosition);
    }



    public AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode shapeMode) {
        org.bukkit.World bukkitWorld = block.getWorld();
        Intrinsics.checkNotNull(bukkitWorld, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.CraftWorld");
        CraftWorld craftWorld = (CraftWorld) bukkitWorld;
        WorldServer worldHandle = craftWorld.getHandle();
        World world = (World) worldHandle;
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        IBlockData blockData = world.a_(blockPosition);
        VoxelShape shape;
        if (shapeMode == BlockShapeMode.INTERACT) {
            shape = blockData.j((IBlockAccess) world, blockPosition);
        } else {
            shape = blockData.k((IBlockAccess) world, blockPosition);
        }
        List<?> localBoxes = shape.d();
        AxisAlignedBox[] result = new AxisAlignedBox[localBoxes.size()];
        int index = 0;
        for (Object localBoxValue : localBoxes) {
            AxisAlignedBB localBox = (AxisAlignedBB) localBoxValue;
            result[index++] = new AxisAlignedBox(
                    block.getX() + localBox.a, block.getY() + localBox.b, block.getZ() + localBox.c,
                    block.getX() + localBox.d, block.getY() + localBox.e, block.getZ() + localBox.f);
        }
        return result;
    }



    public boolean isOnGround(org.bukkit.entity.Entity entity) {
        return entity.isOnGround();
    }
    public boolean isChunkLoaded(org.bukkit.World world, int chunkX, int chunkZ) {
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public Object getBlockState(Block block) {
        Intrinsics.checkNotNull(block, "null cannot be cast to non-null type org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock");
        CraftBlock craftBlock = (CraftBlock) block;
        IBlockData state = craftBlock.getNMS();
        return state.b();
    }



    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }
}
