package me.rerere.matrix.world;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Result of a Bukkit ray trace: position, block, entity and face. */
public final class RaytraceResult {
    private final Entity hitEntity;
    @Nullable
    private final Block hitBlock;
    private final Vector hitPosition;
    private final BlockFace hitFace;

    public RaytraceResult(Vector hitPosition, @Nullable Block hitBlock, Entity hitEntity, BlockFace hitFace) {
        this.hitPosition = hitPosition;
        this.hitBlock = hitBlock;
        this.hitEntity = hitEntity;
        this.hitFace = hitFace;
    }

    public BlockFace getHitFace() { return hitFace; }
    public Vector component1() { return hitPosition; }
    public Entity getHitEntity() { return hitEntity; }
    public Vector getHitPosition() { return hitPosition; }
    public BlockFace component4() { return hitFace; }
    @Nullable public Block component2() { return hitBlock; }
    @Nullable public Block getHitBlock() { return hitBlock; }
    public Entity component3() { return hitEntity; }

    public RaytraceResult copy(Vector hitPosition, @Nullable Block hitBlock, Entity hitEntity, BlockFace hitFace) {
        return new RaytraceResult(hitPosition, hitBlock, hitEntity, hitFace);
    }

    @Override public int hashCode() {
        int result = hitPosition.hashCode();
        result = 31 * result + (hitBlock == null ? 0 : hitBlock.hashCode());
        result = 31 * result + (hitEntity == null ? 0 : hitEntity.hashCode());
        return 31 * result + (hitFace == null ? 0 : hitFace.hashCode());
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RaytraceResult)) return false;
        RaytraceResult that = (RaytraceResult) other;
        return Intrinsics.areEqual(hitPosition, that.hitPosition)
                && Intrinsics.areEqual(hitBlock, that.hitBlock)
                && Intrinsics.areEqual(hitEntity, that.hitEntity)
                && hitFace == that.hitFace;
    }

    @Override public String toString() {
        return format(hitPosition, hitBlock, hitEntity, hitFace);
    }

    private static String format(Vector position, Block block, Entity entity, BlockFace face) {
        return new StringBuilder("RaytraceResultWrap(hitPosition=").append((Object) position)
                .append(", hitBlock=").append((Object) block)
                .append(", hitEntity=").append((Object) entity)
                .append(", hitFace=").append((Object) face)
                .append(")").toString();
    }
}
