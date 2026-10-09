package me.rerere.matrix.util;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.math.MathKt;
import me.rerere.matrix.player.PlayerCheckContext;

/** Geometry, angle, rounding, and tolerance helpers used by movement checks. */
public final class GeometryMath {
    private static final String blockLocationMessage = "The location must be a block location";
    private GeometryMath() {}

    public static double horizontalDistance(Vector from, Vector to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
    public static double horizontalManhattanDistance(Vector from, Vector to) {
        return Math.abs(to.getX() - from.getX()) + Math.abs(to.getZ() - from.getZ());
    }
    public static double distance(Location from, Location to) {
        double dx = to.getX() - from.getX(), dy = to.getY() - from.getY(), dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public static double horizontalDistance(Location from, Location to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
    public static double horizontalManhattanDistance(Location from, Location to) {
        return Math.abs(to.getX() - from.getX()) + Math.abs(to.getZ() - from.getZ());
    }
    public static int floorToInt(double value) {
        int truncatedValue = (int) value;
        return value < (double) truncatedValue ? truncatedValue - 1 : truncatedValue;
    }
    public static double horizontalLength(Vector vector) {
        return Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ());
    }
    public static Vector copy(Vector vector) { return new Vector(vector.getX(), vector.getY(), vector.getZ()); }

    public static boolean absoluteDifferenceAtMost(double left, double right,
            double tolerance) {
        double absoluteDifference = Math.abs(left - right);
        return !Double.isNaN(absoluteDifference) && !Double.isNaN(tolerance)
                && absoluteDifference <= tolerance;
    }

    public static boolean withinTolerance(double left, double right, double tolerance,
            int mask) {
        if ((mask & 2) != 0) tolerance = 0.01D;
        return absoluteDifferenceAtMost(left, right, tolerance);
    }

    public static Vector directionFromYawPitch(me.rerere.matrix.thirdparty.kotlin.Pair<?, ?> yawPitch) {
        Vector direction = new Vector();
        float yaw = ((Number) yawPitch.getFirst()).floatValue();
        float pitch = ((Number) yawPitch.getSecond()).floatValue();
        double pitchRadians = Math.toRadians((double) pitch);
        direction.setY(-Math.sin(pitchRadians));
        double pitchCosine = Math.cos(pitchRadians);
        direction.setX(-pitchCosine * Math.sin(Math.toRadians((double) yaw)));
        direction.setZ(pitchCosine * Math.cos(Math.toRadians((double) yaw)));
        return direction;
    }
    public static int ceilToInt(double value) {
        int truncatedValue = (int) value;
        return value > (double) truncatedValue ? truncatedValue + 1 : truncatedValue;
    }
    public static float angularDistance(float first, float second) {
        float difference = Math.abs(first - second) % 360.0F;
        return difference > 180.0F ? 360.0F - difference : difference;
    }

    public static int roundedAverage(List<Integer> values) {
        if (values.isEmpty()) return 0;
        return MathKt.roundToInt(CollectionsKt.averageOfInt((Iterable<Integer>) values));
    }
    public static long roundedLongAverage(List<Long> values) {
        if (values.isEmpty()) return 0L;
        return MathKt.roundToLong(CollectionsKt.averageOfLong((Iterable<Long>) values));
    }

    public static Vector directionFromClientVersion(
            PlayerCheckContext player, float yaw, float pitch) {
        ClientVersion version = player.clientVersion();
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            float yawRadians = -yaw * 0.017453292F - 3.1415927F;
            float yawCos = (float) Math.cos((double) yawRadians);
            float yawSin = (float) Math.sin((double) yawRadians);
            float pitchRadians = -pitch * 0.017453292F;
            float pitchCos = -(float) Math.cos((double) pitchRadians);
            float pitchSin = (float) Math.sin((double) pitchRadians);
            return new Vector(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
        }
        float pitchRadians = pitch * 0.017453292F;
        float yawRadians = -yaw * 0.017453292F;
        float yawCos = (float) Math.cos((double) yawRadians);
        float yawSin = (float) Math.sin((double) yawRadians);
        float pitchCos = (float) Math.cos((double) pitchRadians);
        float pitchSin = (float) Math.sin((double) pitchRadians);
        return new Vector((double) (yawSin * pitchCos), (double) (-pitchSin), (double) (yawCos * pitchCos));
    }

    public static double blockCenterDistance(Location from, Location to) {
        if (to.getX() != Math.floor(to.getX()) || to.getY() != Math.floor(to.getY()) || to.getZ() != Math.floor(to.getZ()))
            throw new IllegalArgumentException(blockLocationMessage.toString());
        double dx = to.getX() + 0.5D - from.getX();
        double dy = to.getY() + 0.5D - from.getY();
        double dz = to.getZ() + 0.5D - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static String blockLocationMessage() { return blockLocationMessage; }
    public static String decode(byte[] input) {
        int charCount = 0;
        int length = input.length;
        char[] chars = new char[length];
        int index = 0;
        while (index < length) {
            int first = input[index] & 255;
            if (first < 192) {
                chars[charCount++] = (char) first;
            } else if (first < 224) {
                int value = (first & 31) << 6;
                first = input[++index];
                value |= first & 63;
                chars[charCount++] = (char) value;
            } else if (index < length - 2) {
                int value = (first & 15) << 12;
                first = input[++index];
                value |= (first & 63) << 6;
                first = input[++index];
                value |= first & 63;
                chars[charCount++] = (char) value;
            }
            index++;
        }
        return new String(chars, 0, charCount);
    }
}



