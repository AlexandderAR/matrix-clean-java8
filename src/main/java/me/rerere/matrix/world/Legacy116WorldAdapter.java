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
import me.rerere.matrix.license.LicenseResponseException;

/** Version-specific Bukkit/CraftBukkit bridge for the 1.16_R1 server API. */
public final class Legacy116WorldAdapter implements WorldAccess {
    private static boolean compatibilityFlag;

    public int getItemUseDuration(ItemStack stack, LivingEntity entity) {
        net.minecraft.server.v1_16_R1.ItemStack nmsStack =
                org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack.asNMSCopy(stack);
        return nmsStack.getItem().e_(nmsStack);
    }

    public void setOnGround(Entity entity, boolean onGround) {
        ((org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity) entity).getHandle().c(onGround);
    }

    public AxisAlignedBox[] getCollisionBoxes(Block block, me.rerere.matrix.geometry.BlockShapeMode shapeMode) {
        org.bukkit.craftbukkit.v1_16_R1.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_16_R1.CraftWorld) block.getWorld();
        net.minecraft.server.v1_16_R1.WorldServer world = craftWorld.getHandle();
        net.minecraft.server.v1_16_R1.BlockPosition position = new net.minecraft.server.v1_16_R1.BlockPosition(
                block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_16_R1.IBlockData blockData = world.getType(position);
        net.minecraft.server.v1_16_R1.VoxelShape shape = shapeMode ==
                me.rerere.matrix.geometry.BlockShapeMode.INTERACT
                ? blockData.getShape(world, position)
                : blockData.getCollisionShape(world, position);
        List<net.minecraft.server.v1_16_R1.AxisAlignedBB> boxes = shape.d();
        AxisAlignedBox[] worldBoxes = new AxisAlignedBox[boxes.size()];
        int index = 0;
        for (net.minecraft.server.v1_16_R1.AxisAlignedBB box : boxes) {
            double blockX = block.getX();
            double blockY = block.getY();
            double blockZ = block.getZ();
            worldBoxes[index++] = new AxisAlignedBox(
                    blockX + box.minX, blockY + box.minY, blockZ + box.minZ,
                    blockX + box.maxX, blockY + box.maxY, blockZ + box.maxZ);
        }
        return worldBoxes;
    }

    public boolean isOnGround(Entity entity) {
        return ((org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity) entity).getHandle().isOnGround();
    }

    public float getMovementSpeed(LivingEntity entity) {
        net.minecraft.server.v1_16_R1.EntityHuman human =
                (net.minecraft.server.v1_16_R1.EntityHuman)
                        ((org.bukkit.craftbukkit.v1_16_R1.entity.CraftLivingEntity) entity).getHandle();
        return human.dM();
    }

    public AxisAlignedBox getEntityBounds(Entity entity) {
        org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity craftEntity =
                (org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity) entity;
        net.minecraft.server.v1_16_R1.AxisAlignedBB box = craftEntity.getHandle().getBoundingBox();
        if (box == null) {
            return AxisAlignedBox.at(entity.getLocation());
        }
        return new AxisAlignedBox(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public void stopUsingItem(Player player) {
        ((org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer) player).getHandle().clearActiveItem();
    }

    public AxisAlignedBox getBlockBounds(Block block) {
        BoundingBox box = block.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public Object getBlockState(Block block) {
        return ((org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock) block).getNMS().getBlock();
    }

    public float getBlockFriction(Block block) {
        return ((net.minecraft.server.v1_16_R1.Block) getBlockState(block)).getFrictionFactor();
    }

    public boolean isUsingItem(Player player) {
        return player.isHandRaised();
    }

    public boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    public float getBlockBreakSpeed(Player player, Block block) {
        org.bukkit.craftbukkit.v1_16_R1.CraftWorld craftWorld =
                (org.bukkit.craftbukkit.v1_16_R1.CraftWorld) block.getWorld();
        net.minecraft.server.v1_16_R1.WorldServer world = craftWorld.getHandle();
        net.minecraft.server.v1_16_R1.BlockPosition position = new net.minecraft.server.v1_16_R1.BlockPosition(
                block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_16_R1.IBlockData blockData = world.getType(position);
        net.minecraft.server.v1_16_R1.EntityPlayer nmsPlayer =
                ((org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer) player).getHandle();
        return blockData.getDamage(nmsPlayer, world, position);
    }

    public static void setCompatibilityFlag(boolean enabled) {
        compatibilityFlag = enabled;
    }

    public static boolean getCompatibilityFlag() {
        return compatibilityFlag;
    }

    public static boolean isCompatibilityFlagDisabled() {
        try {
            return !getCompatibilityFlag();
        } catch (LicenseResponseException error) {
            throw error;
        }
    }

    static {
        if (getCompatibilityFlag()) setCompatibilityFlag(true);
    }
}
