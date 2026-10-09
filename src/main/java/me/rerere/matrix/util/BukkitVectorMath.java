package me.rerere.matrix.util;

import org.bukkit.util.Vector;

public final class BukkitVectorMath {
    private BukkitVectorMath() {
    }

    public static final double getX(Vector vector) {
        return vector.getX();
    }

    public static final double getY(Vector vector) {
        return vector.getY();
    }

    public static final double getZ(Vector vector) {
        return vector.getZ();
    }

    public static final Vector multiplyCopy(Vector vector, int multiplier) {
        return vector.clone().multiply(multiplier);
    }

    public static final Vector multiplyCopy(Vector vector, double multiplier) {
        return vector.clone().multiply(multiplier);
    }

    public static final Vector addCopy(Vector vector, Vector addition) {
        return vector.clone().add(addition);
    }

    public static final Vector subtractCopy(Vector vector, Vector subtraction) {
        return vector.clone().subtract(subtraction);
    }
}
