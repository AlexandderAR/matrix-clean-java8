package me.rerere.matrix.world;

import me.rerere.matrix.util.time.MillisecondSampler;
import org.bukkit.Material;

/** Immutable client-side block coordinates, type and packet timestamp. */
public final class ClientBlock {
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final Material type;
    private final long sendTime;

    public ClientBlock(String world, int x, int y, int z, Material type) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.sendTime = MillisecondSampler.currentTimeMillis();
    }

    public final int getZ() { return z; }
    public final long getSendTime() { return sendTime; }
    public final int getY() { return y; }
    public final String getWorld() { return world; }
    public final int getX() { return x; }
    public final Material getType() { return type; }

    @Override public String toString() {
        return formatBlock(world, x, y, z, type, sendTime);
    }

    private static String formatBlock(String worldName, int x, int y, int z,
                                      Material material, long packetSendTime) {
        return new StringBuilder("ClientBlock(world='").append(worldName)
                .append("', x=").append(x).append(", y=").append(y)
                .append(", z=").append(z).append(", type=").append(material)
                .append(", sendTime=").append(packetSendTime).append(")").toString();
    }
}
