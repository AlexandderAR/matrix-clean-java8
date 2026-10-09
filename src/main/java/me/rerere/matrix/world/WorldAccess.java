package me.rerere.matrix.world;

import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.thirdparty.kotlin.NotImplementedError;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Bukkit-facing queries used by the version-specific world adapters. */
public interface WorldAccess {
    int getItemUseDuration(ItemStack stack, LivingEntity user);

    void setOnGround(Entity entity, boolean enabled);

    default int getPotionAmplifierPlusOne(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    boolean isUsingItem(Player player);

    float getBlockFriction(Block block);

    Object getBlockState(Block block);

    default float updateSupportingBlock(Entity entity, boolean flag, Vector vector) {
        throw new NotImplementedError("Not implemented yet.");
    }

    float getMovementSpeed(LivingEntity entity);

    boolean isOnGround(Entity entity);

    AxisAlignedBox getEntityBounds(Entity entity);

    default void stopUsingItem(Player player) {
        throw new NotImplementedError("Not implemented yet.");
    }

    AxisAlignedBox[] getCollisionBoxes(Block block, BlockShapeMode mode);

    default RaytraceResult rayTraceBlocks(World world, Vector start, Vector direction, double distance) {
        org.bukkit.util.RayTraceResult hit = world.rayTraceBlocks(
                new Location(world, start.getX(), start.getY(), start.getZ()), direction, distance);
        if (hit == null) return null;
        Vector hitPosition = hit.getHitPosition();
        Block hitBlock = hit.getHitBlock();
        org.bukkit.block.BlockFace hitFace = hit.getHitBlockFace();
        Entity hitEntity = hit.getHitEntity();
        return new RaytraceResult(hitPosition, hitBlock, hitEntity, hitFace);
    }

    boolean isChunkLoaded(World world, int x, int z);

    float getBlockBreakSpeed(Player player, Block block);

    AxisAlignedBox getBlockBounds(Block block);

}
