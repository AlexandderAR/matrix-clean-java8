package me.rerere.matrix.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.ProtocolTranslationSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

/** Eye-height and block-iteration helpers used by collision checks. */
public final class BlockRayTraceHelper {
    private static int[] runtimeArray;

    public BlockRayTraceHelper() { }

    public static void setRuntimeArray(int[] value) { runtimeArray = value; }
    public static int[] getRuntimeArray() { return runtimeArray; }

    public static double eyeHeight(Player player, PlayerCheckContext protocolData) {
        boolean protocolTranslationAvailable = ProtocolTranslationSupport.isAvailable();
        if (!protocolTranslationAvailable) return player.getEyeHeight();
        if (!Matrix.getServerVersion()
                .isOlderThan(ServerVersion.V_1_14)) return player.getEyeHeight();
        if (protocolData.protocolVersion() < 477)
            return player.getEyeHeight();
        return player.isSneaking() ? player.getEyeHeight() - 0.27D : player.getEyeHeight();
    }

    public static List<Block> collectBlocks(
            Location origin, double distance, Set<Material> stopMaterials,
            int blockLimit) {
        return collectBlocksInternal(origin, distance, stopMaterials, blockLimit, 0);
    }

    public static List<Block> collectBlocksInternal(
            Location origin, double distance, Set<Material> stopMaterials,
            int blockLimit, int retainedBlockLimit) {
        Objects.requireNonNull(stopMaterials);
        if (blockLimit > 120) blockLimit = 120;
        List<Block> blocks = new ArrayList<>();
        try {
            BlockIterator iterator = new BlockIterator(origin, distance, blockLimit);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                blocks.add(block);
                if (retainedBlockLimit != 0 && blocks.size() > retainedBlockLimit) {
                    blocks.remove(0);
                }
                if (!stopMaterials.contains(block.getType())) {
                    break;
                }
            }
        } catch (IllegalStateException ignored) {
            // The original exception table returns the blocks accumulated so far.
        }
        return blocks;
    }

}
