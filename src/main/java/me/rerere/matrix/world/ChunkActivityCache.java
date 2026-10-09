package me.rerere.matrix.world;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

/** Tracks the time at which a world chunk was recorded. */
public final class ChunkActivityCache {
    private static final long CACHE_EXPIRY_SECONDS = 15L;
    private static int[] configuration = new int[5];
    private final Cache<ChunkInfo, Long> lastRecordedAt;

    public ChunkActivityCache() {
        this.lastRecordedAt = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /** True when this chunk was recorded no more than maxAgeMillis ago. */
    public boolean wasRecordedRecently(Chunk chunk, long maxAgeMillis) {
        ChunkInfo key = new ChunkInfo(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Long recordedAt = lastRecordedAt.getIfPresent(key);
        return recordedAt != null && System.currentTimeMillis() - recordedAt <= maxAgeMillis;
    }

    /** True when the location's containing chunk was recorded no more than maxAgeMillis ago. */
    public boolean wasRecordedRecently(Location location, long maxAgeMillis) {
        World world = location.getWorld();
        Intrinsics.checkNotNull(world);
        String worldName = world.getName();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        ChunkInfo key = new ChunkInfo(worldName, chunkX, chunkZ);
        Long recordedAt = lastRecordedAt.getIfPresent(key);
        return recordedAt != null && System.currentTimeMillis() - recordedAt <= maxAgeMillis;
    }

    public void record(Chunk chunk) {
        ChunkInfo key = new ChunkInfo(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        lastRecordedAt.put(key, System.currentTimeMillis());
    }

    public static void setConfiguration(int[] value) { configuration = value; }
    public static int[] getConfiguration() { return configuration; }
}
