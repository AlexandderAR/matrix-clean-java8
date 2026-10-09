package me.rerere.matrix.entity;

import java.util.Optional;
import org.bukkit.block.Block;

/** Optional block and volume result from movement collision evaluation. */
public final class MovementBlockResult {
    private final Optional<Block> optionalBlock;
    private final boolean volume;

    public MovementBlockResult(Optional<Block> optionalBlock, boolean volume) {
        if (optionalBlock == null) {
            throw new NullPointerException("block is marked non-null but is null");
        }
        this.optionalBlock = optionalBlock;
        this.volume = volume;
    }

    public boolean hasVolume() { return volume; }
    public Optional<Block> getOptionalBlock() { return optionalBlock; }
}
