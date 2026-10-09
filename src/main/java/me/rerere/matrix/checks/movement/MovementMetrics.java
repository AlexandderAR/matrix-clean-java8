package me.rerere.matrix.checks.movement;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.rerere.matrix.util.GeometryMath;

/** Scalar helpers used by movement checks. */
public final class MovementMetrics {
    public MovementMetrics() { }

    public static double magnitude(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static int toInt(double value) {
        return GeometryMath.floorToInt(value);
    }

    public static double horizontalDistance(Vector from, Vector to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static float floatMagnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static double locationDistance(Location from, Location to) {
        return GeometryMath.horizontalDistance(from, to);
    }

    public static int integerAverage(List<Integer> values) {
        if (values.size() == 0) {
            return 0;
        }
        int total = 0;
        for (Integer value : values) {
            total += value.intValue();
        }
        return total / values.size();
    }

}
