package me.rerere.matrix.world;

import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.LifecycleTaskReference;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;


/** Asynchronous world/block snapshot cache reconstructed from its ASM. */
public final class BlockLocationCache implements Runnable {
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, ClientBlock>> worlds;
    private static final String nullWorldMessage;

    public BlockLocationCache() {
        worlds = new ConcurrentHashMap<>();
        FoliaSupport.INSTANCE.runTaskTimerAsync(
                (Function0<Unit>) new LifecycleTaskReference(this), 20L, 2L);
    }

    @Override public void run() {
        long now = System.currentTimeMillis();
        for (ConcurrentHashMap<Long, ClientBlock> blocks : worlds.values()) {
            try {
                blocks.forEach((key, block) -> removeIfExpired(now, blocks, key, block));
            } catch (NullPointerException failure) {
                throw failure;
            }
        }
    }

    private static Unit removeIfExpired(long now, ConcurrentHashMap<Long, ClientBlock> blocks,
                                        Long key, ClientBlock block) {
        if (now - block.getSendTime() > 1500L)
            blocks.remove(key, block);
        return Unit.INSTANCE;
    }

    private long packedKey(int x, int y, int z) {
        return ((long) x & 0x3ffffffL)
                | (((long) z & 0x3ffffffL) << 26)
                | (((long) y & 0xfffL) << 52);
    }

    private void putSnapshot(String world, long key, ClientBlock snapshot) {
        ConcurrentHashMap<Long, ClientBlock> blocks = worlds.get(world);
        if (blocks == null) blocks = worlds.computeIfAbsent(world, ignored -> new ConcurrentHashMap<>());
        blocks.put(Long.valueOf(key), snapshot);
    }

    public void queueAir(World world, int x, int y, int z) {
        String name = world.getName();
        long key = packedKey(x, y, z);
        ClientBlock snapshot = new ClientBlock(world.getName(), x, y, z, Material.AIR);
        putSnapshot(name, key, snapshot);
    }

    public void cacheBlock(Block block) {
        String name = block.getWorld().getName();
        long key = packedKey(block.getX(), block.getY(), block.getZ());
        ClientBlock snapshot = new ClientBlock(name, block.getX(), block.getY(),
                block.getZ(), block.getType());
        putSnapshot(name, key, snapshot);
    }

    public ClientBlock get(Block worldBlock) {
        ConcurrentHashMap<Long, ClientBlock> blocks = worlds.get(worldBlock.getWorld().getName());
        if (blocks == null) return null;
        long key = packedKey(worldBlock.getX(), worldBlock.getY(), worldBlock.getZ());
        return blocks.get(Long.valueOf(key));
    }

    public boolean contains(Block block) {
        if (block == null) return false;
        ConcurrentHashMap<Long, ClientBlock> blocks = worlds.get(block.getWorld().getName());
        if (blocks == null) return false;
        long key = packedKey(block.getX(), block.getY(), block.getZ());
        return blocks.containsKey(Long.valueOf(key));
    }

    public void insertFromLocation(Location location) {
        World worldObject = location.getWorld();
        if (worldObject == null) throw new NullPointerException(nullWorldMessage);
        String world = worldObject.getName();
        if (world == null) throw new NullPointerException(nullWorldMessage);
        ClientBlock snapshot = new ClientBlock(world, location.getBlockX(),
                location.getBlockY(), location.getBlockZ(), location.getBlock().getType());
        long key = packedKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        putSnapshot(world, key, snapshot);
    }

    static {
        nullWorldMessage = "null world in the location obj (JEB4F5)".intern();
    }
}


