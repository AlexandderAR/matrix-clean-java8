package me.rerere.matrix.prediction;

import org.bukkit.block.Block;

/** A raycast result describing the obstructing block and both measured distances. */
public final class BlockedRay {
    private final int checkedRays;
    private final Block block;
    private final double blockDistance;
    private final double targetDistance;

    public BlockedRay(Block block, double blockDistance, double targetDistance, int checkedRays) {
        this.block = block;
        this.blockDistance = blockDistance;
        this.targetDistance = targetDistance;
        this.checkedRays = checkedRays;
    }

    public BlockedRay copy(Block block, double blockDistance, double targetDistance, int checkedRays) {
        return new BlockedRay(block, blockDistance, targetDistance, checkedRays);
    }

    public Block getBlock() { return block; }
    public Block block() { return block; }
    public int getCheckedRays() { return checkedRays; }
    public int checkedRays() { return checkedRays; }
    public double getBlockDistance() { return blockDistance; }
    public double blockDistance() { return blockDistance; }
    public double getTargetDistance() { return targetDistance; }
    public double targetDistance() { return targetDistance; }

    @Override
    public String toString() {
        return "BlockedRay(block=" + block
                + ", blockDistance=" + blockDistance
                + ", targetDistance=" + targetDistance
                + ", checkedRays=" + checkedRays + ")";
    }

    @Override
    public int hashCode() {
        int result = block.hashCode();
        result = 31 * result + Double.hashCode(blockDistance);
        result = 31 * result + Double.hashCode(targetDistance);
        result = 31 * result + checkedRays;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockedRay)) {
            return false;
        }
        BlockedRay otherRay = (BlockedRay) other;
        return equal(block, otherRay.block)
                && Double.compare(blockDistance, otherRay.blockDistance) == 0
                && Double.compare(targetDistance, otherRay.targetDistance) == 0
                && checkedRays == otherRay.checkedRays;
    }

    private static boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
