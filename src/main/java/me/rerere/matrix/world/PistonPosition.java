package me.rerere.matrix.world;

import org.bukkit.Location;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;

/** Integer block position used for piston-related position checks. */
public final class PistonPosition {
    private static String sharedText;
    private final int x;
    private final int y;
    private final int z;
    private final long createdAtMillis;

    public PistonPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAtMillis = MillisecondSampler.currentTimeMillis();
    }

    public int getY() { return y; }
    public int getZ() { return z; }
    public int getZAlias() { return z; }
    public int getX() { return x; }
    public long getCreatedAtMillis() { return createdAtMillis; }

    public boolean isNear(@NotNull Location location) {
        return Math.abs(location.getY() - y) <= 3.0
                && Math.abs(location.getZ() - z) <= 2.0
                && Math.abs(location.getX() - x) <= 2.0;
    }

    public int getXAlias() { return x; }

    private static String formatCoordinates(int x, int y, int z) {
        return "Piston(x=" + x + ", y=" + y + ", z=" + z + ")";
    }

    @NotNull
    public PistonPosition fromValues(int x, int y, int z) {
        return new PistonPosition(x, y, z);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        result = 31 * result + Integer.hashCode(z);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof PistonPosition)) return false;
        PistonPosition other = (PistonPosition) candidate;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    @NotNull
    public String toString() {
        return formatCoordinates(x, y, z);
    }

    public static void setSharedText(String text) {
        sharedText = text;
    }

    public static String getSharedText() {
        return sharedText;
    }
}
